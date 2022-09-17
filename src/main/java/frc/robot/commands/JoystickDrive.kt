package frc.robot.commands

import edu.wpi.first.math.kinematics.ChassisSpeeds
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import edu.wpi.first.wpilibj2.command.CommandBase
import frc.robot.Input
import frc.robot.subsystems.Drivetrain

class JoystickDrive : CommandBase() {
    init {
        addRequirements(Drivetrain)
    }

    override fun execute() {
        val speed = if (SmartDashboard.getBoolean("Slow Mode", true)) {
            0.8
        } else {
            2.0
        }

        //Make go sideways better
        if (SmartDashboard.getBoolean("Field Oriented", false)) {
            Drivetrain.drive(ChassisSpeeds.fromFieldRelativeSpeeds(Input.yAxis * speed, Input.xAxis * speed, -Input.zAxis * speed, Drivetrain.pose.rotation))
        } else {
            Drivetrain.drive(ChassisSpeeds(-Input.xAxis * speed, Input.yAxis * speed, -Input.zAxis * speed))
        }
    }

    override fun end(interrupted: Boolean) {
        Drivetrain.stop()
    }
}
