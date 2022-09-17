package frc.robot

import edu.wpi.first.math.geometry.Translation2d

val frontLeftLocation = Translation2d(0.381, 0.381)
val frontRightLocation = Translation2d(0.381, -0.381)
val rearLeftLocation = Translation2d(-0.381, 0.381)
val rearRightLocation = Translation2d(-0.381, -0.381)

const val driveF = 0.5
const val driveP = 0.5
const val driveI = 0.0
const val driveD = 0.0

const val wheelPerimeter = 1.2566
const val driveDistancePerPulse = wheelPerimeter / 8192
