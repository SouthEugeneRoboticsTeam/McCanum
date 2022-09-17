package frc.robot

import edu.wpi.first.math.geometry.Pose2d
import edu.wpi.first.math.geometry.Rotation2d
import edu.wpi.first.wpilibj.XboxController
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import edu.wpi.first.wpilibj2.command.InstantCommand
import edu.wpi.first.wpilibj2.command.button.JoystickButton
import frc.robot.subsystems.Drivetrain

object Input {
    private val joystick = XboxController(0)

    fun onInit() {
        SmartDashboard.putBoolean("Slow Mode", true)
        SmartDashboard.putBoolean("Field Oriented", false)

        JoystickButton(joystick, 1).whenPressed(InstantCommand( { Drivetrain.pose = Pose2d(0.0, 0.0, Rotation2d(0.0)) } ))
    }

    val xAxis
        get() = joystick.leftX

    val yAxis
        get() = -joystick.leftY

    val zAxis
        get() = joystick.rightX
}