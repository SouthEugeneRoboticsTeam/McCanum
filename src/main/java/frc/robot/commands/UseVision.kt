package frc.robot.commands

import edu.wpi.first.math.controller.PIDController
import edu.wpi.first.math.kinematics.ChassisSpeeds
import edu.wpi.first.wpilibj2.command.CommandBase
import frc.robot.subsystems.Drivetrain
import frc.robot.subsystems.Vision
import kotlin.math.abs

class UseVision : CommandBase() {
    private val anglePID = PIDController(1.1, 1.5, 0.0)
    private val forwardPID = PIDController(0.3, 0.3, 0.0)

    init {
        addRequirements(Drivetrain, Vision)
    }

    override fun initialize() {
        anglePID.reset()
        forwardPID.reset()
    }
    
    override fun execute() {
        var angleError = 0.0
        var forwardError = 0.0

        val lastUpdateTime = Vision.getMillisSinceUpdate()
        if (lastUpdateTime != null && lastUpdateTime <= 250) {
            angleError = Vision.getTargetAngle()
            if (abs(angleError) < 0.25) {
                forwardError = Vision.getTargetDistance() - 1
            }
        }

        val angle = anglePID.calculate(angleError)
        val forward = forwardPID.calculate(forwardError)

        Drivetrain.drive(ChassisSpeeds(0.0, -forward, angle))
    }

    override fun end(interrupted: Boolean) {
        Drivetrain.stop()
    }
}
