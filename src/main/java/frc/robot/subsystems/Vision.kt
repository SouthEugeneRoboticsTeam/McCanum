package frc.robot.subsystems

import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj2.command.SubsystemBase
import java.lang.System.currentTimeMillis

object Vision : SubsystemBase() {
    private val visionTable = NetworkTableInstance.getDefault().getTable("vision")
    private val targetAngleEntry = visionTable.getEntry("target_angle")
    private val targetDistanceEntry = visionTable.getEntry("distance")
    private val isTargetEntry = visionTable.getEntry("is_target")

    private var targetAngleFromRobot = Drivetrain.angle

    private var lastUpdate: Long? = null

    fun getTargetAngle() = targetAngleFromRobot - Drivetrain.angle
    fun getTargetDistance() = targetDistanceEntry.getNumber(0.0) as Double
    fun isTarget() = isTargetEntry.getBoolean(false)
    fun getMillisSinceUpdate() = if (lastUpdate == null) { null } else { currentTimeMillis() - lastUpdate!! }

    override fun periodic() {
        if (isTarget()) {
            targetAngleFromRobot = Drivetrain.angle + targetAngleEntry.getNumber(0.0) as Double
            lastUpdate = currentTimeMillis()
        }
    }
}