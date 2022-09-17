#!/usr/bin/env python3

# Copyright (c) FIRST and other WPILib contributors.
# Open Source Software; you can modify and/or share it under the terms of
# the WPILib BSD license file in the root directory of this project.

import json
import time
import sys

from cscore import CameraServer, VideoSource, UsbCamera, MjpegServer
from networktables import NetworkTablesInstance
import numpy as np
import cv2
import math

#   JSON format:
#   {
#       "team": <team number>,
#       "ntmode": <"client" or "server", "client" if unspecified>
#       "cameras": [
#           {
#               "name": <camera name>
#               "path": <path, e.g. "/dev/video0">
#               "pixel format": <"MJPEG", "YUYV", etc>   // optional
#               "width": <video mode width>              // optional
#               "height": <video mode height>            // optional
#               "fps": <video mode fps>                  // optional
#               "brightness": <percentage brightness>    // optional
#               "white balance": <"auto", "hold", value> // optional
#               "exposure": <"auto", "hold", value>      // optional
#               "properties": [                          // optional
#                   {
#                       "name": <property name>
#                       "value": <property value>
#                   }
#               ],
#               "stream": {                              // optional
#                   "properties": [
#                       {
#                           "name": <stream property name>
#                           "value": <stream property value>
#                       }
#                   ]
#               }
#           }
#       ]
#       "switched cameras": [
#           {
#               "name": <virtual camera name>
#               "key": <network table key used for selection>
#               // if NT value is a string, it's treated as a name
#               // if NT value is a double, it's treated as an integer index
#           }
#       ]
#   }

configFile = "/boot/frc.json"


class CameraConfig: pass


team = None
server = False
cameraConfigs = []
switchedCameraConfigs = []
cameras = []


def parseError(str):
    """Report parse error."""
    print("config error in '" + configFile + "': " + str, file=sys.stderr)


def readCameraConfig(config):
    """Read single camera configuration."""
    cam = CameraConfig()

    # name
    try:
        cam.name = config["name"]
    except KeyError:
        parseError("could not read camera name")
        return False

    # path
    try:
        cam.path = config["path"]
    except KeyError:
        parseError("camera '{}': could not read path".format(cam.name))
        return False

    # stream properties
    cam.streamConfig = config.get("stream")

    cam.config = config

    cameraConfigs.append(cam)
    return True


def readSwitchedCameraConfig(config):
    """Read single switched camera configuration."""
    cam = CameraConfig()

    # name
    try:
        cam.name = config["name"]
    except KeyError:
        parseError("could not read switched camera name")
        return False

    # path
    try:
        cam.key = config["key"]
    except KeyError:
        parseError("switched camera '{}': could not read key".format(cam.name))
        return False

    switchedCameraConfigs.append(cam)
    return True


def readConfig():
    """Read configuration file."""
    global team
    global server

    # parse file
    try:
        with open(configFile, "rt", encoding="utf-8") as f:
            j = json.load(f)
    except OSError as err:
        print("could not open '{}': {}".format(configFile, err), file=sys.stderr)
        return False

    # top level must be an object
    if not isinstance(j, dict):
        parseError("must be JSON object")
        return False

    # team number
    try:
        team = j["team"]
    except KeyError:
        parseError("could not read team number")
        return False

    # ntmode (optional)
    if "ntmode" in j:
        str = j["ntmode"]
        if str.lower() == "client":
            server = False
        elif str.lower() == "server":
            server = True
        else:
            parseError("could not understand ntmode value '{}'".format(str))

    # cameras
    try:
        cameras = j["cameras"]
    except KeyError:
        parseError("could not read cameras")
        return False
    for camera in cameras:
        if not readCameraConfig(camera):
            return False

    # switched cameras
    if "switched cameras" in j:
        for camera in j["switched cameras"]:
            if not readSwitchedCameraConfig(camera):
                return False

    return True


def startCamera(config):
    """Start running the camera."""
    print("Starting camera '{}' on {}".format(config.name, config.path))
    inst = CameraServer.getInstance()
    camera = UsbCamera(config.name, config.path)
    server = inst.startAutomaticCapture(camera=camera, return_server=True)

    camera.setConfigJson(json.dumps(config.config))
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen)

    if config.streamConfig is not None:
        server.setConfigJson(json.dumps(config.streamConfig))

    return camera


def startSwitchedCamera(config):
    """Start running the switched camera."""
    print("Starting switched camera '{}' on {}".format(config.name, config.key))
    server = CameraServer.getInstance().addSwitchedCamera(config.name)

    def listener(fromObj, key, value, isNew):
        if isinstance(value, float):
            i = int(value)
            if 0 <= i < len(cameras):
                server.setSource(cameras[i])
        elif isinstance(value, str):
            for i in range(len(cameraConfigs)):
                if value == cameraConfigs[i].name:
                    server.setSource(cameras[i])
                    break

    NetworkTablesInstance.getDefault().getEntry(config.key).addListener(
        listener,
        NetworkTablesInstance.NotifyFlags.IMMEDIATE |
        NetworkTablesInstance.NotifyFlags.NEW |
        NetworkTablesInstance.NotifyFlags.UPDATE)

    return server


if len(sys.argv) >= 2:
    configFile = sys.argv[1]

# read configuration
if not readConfig():
    sys.exit(1)

# start NetworkTables
ntinst = NetworkTablesInstance.getDefault()
if server:
    print("Setting up NetworkTables server")
    ntinst.startServer()
else:
    print("Setting up NetworkTables client for team {}".format(team))
    ntinst.startClientTeam(team)
    ntinst.startDSClient()

# start cameras
for config in cameraConfigs:
    cameras.append(startCamera(config))

# start switched cameras
for config in switchedCameraConfigs:
    startSwitchedCamera(config)

server = CameraServer.getInstance()

camera = cameras[0]

width = 160
height = 120

height_fov = 0.599
width_fov = 1.065

target_height = 0.05
target_width = 0.167

input_stream = server.getVideo()
output_stream = server.putVideo('Processed', width, height)

# Table for vision output information
vision_table = ntinst.getTable('vision')

img = None

# Wait for NetworkTables to start
time.sleep(0.5)

while True:
    start_time = time.time()

    frame_time, input_img = input_stream.grabFrame(img)
    output_img = np.copy(input_img)

    # Notify output of error and skip iteration
    if frame_time == 0:
        output_stream.notifyError(input_stream.getError())
        continue

    # Convert to HSV and threshold image
    hsv_img = cv2.cvtColor(input_img, cv2.COLOR_BGR2HSV)
    binary_img = cv2.inRange(hsv_img, (30, 150, 120), (80, 255, 255))

    _, contour_list, _ = cv2.findContours(binary_img, mode=cv2.RETR_EXTERNAL, method=cv2.CHAIN_APPROX_SIMPLE)

    biggest = 0.0
    x = None
    pixel_width = None
    pixel_height = None

    for contour in contour_list:

        area = cv2.contourArea(contour)
        # Ignore small contours that could be because of noise/bad thresholding
        if cv2.contourArea(contour) < 15:
            continue

        cv2.drawContours(output_img, contour, -1, color=(255, 255, 255), thickness=-1)

        rect = cv2.minAreaRect(contour)
        center, size, target_angle = rect
        center = tuple([int(dim) for dim in center])  # Convert to int so we can draw

        # Draw rectangle and circle
        output_img = cv2.bitwise_or(output_img, output_img, mask=binary_img)
        cv2.drawContours(output_img, [cv2.boxPoints(rect).astype(int)], -1, color=(0, 0, 255), thickness=1)

        rect_size = size[0] * size[1]
        if area > biggest:
            cv2.circle(output_img, center=center, radius=1, color=(0, 0, 255), thickness=-1)

            biggest = area
            pixel_width = size[0]
            pixel_height = size[1]
            x = center[0] - (width / 2)

    vision_table.putBoolean('is_target', x is not None)
    if x is not None:
        # Try just for safety
        try:
            target_angle = x / width * width_fov
            vision_table.putNumber('target_angle', target_angle)
            height_angle = pixel_height / height * height_fov
            distance = target_height / (math.tan(height_angle / 2) * 2)
            vision_table.putNumber('distance', distance)
        except Exception as e:
            print(e)
            vision_table.putBoolean('is_target', False)

    processing_time = time.time() - start_time
    fps = 1 / processing_time
    cv2.putText(output_img, str(round(fps, 1)), (0, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255))
    output_stream.putFrame(output_img)
