package frc.robot.subsystems

import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.TalonSRXControlMode
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX
import com.kauailabs.navx.frc.AHRS
import edu.wpi.first.math.geometry.Pose2d
import edu.wpi.first.math.kinematics.ChassisSpeeds
import edu.wpi.first.math.kinematics.MecanumDriveKinematics
import edu.wpi.first.math.kinematics.MecanumDriveOdometry
import edu.wpi.first.math.kinematics.MecanumDriveWheelSpeeds
import edu.wpi.first.wpilibj.SPI
import edu.wpi.first.wpilibj2.command.SubsystemBase
import frc.robot.*

object Drivetrain : SubsystemBase() {
    private val frontLeft = WPI_TalonSRX(3)
    private val frontRight = WPI_TalonSRX(4)
    private val rearLeft = WPI_TalonSRX(2)
    private val rearRight = WPI_TalonSRX(1)

    private val gyro = AHRS(SPI.Port.kMXP)
    private val kinematics = MecanumDriveKinematics(frontLeftLocation, frontRightLocation, rearLeftLocation, rearRightLocation)
    private val odometry = MecanumDriveOdometry(kinematics, gyro.rotation2d)

    init {
        frontLeft.isSafetyEnabled = true
        frontRight.isSafetyEnabled = true
        rearLeft.isSafetyEnabled = true
        rearRight.isSafetyEnabled = true

        frontRight.inverted = true
        rearRight.inverted = true

        configMotor(frontLeft)
        configMotor(frontRight)
        configMotor(rearLeft)
        configMotor(rearRight)
    }

    private fun configMotor(motor: WPI_TalonSRX) {
        motor.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder)

        motor.config_kF(0, driveF)
        motor.config_kP(0, driveP)
        motor.config_kI(0, driveI)
        motor.config_kD(0, driveD)
    }

    private fun getWheelSpeeds(): MecanumDriveWheelSpeeds {
        return MecanumDriveWheelSpeeds(upsToMPS(frontLeft.selectedSensorVelocity), upsToMPS(frontRight.selectedSensorVelocity), upsToMPS(rearLeft.selectedSensorVelocity), upsToMPS(rearRight.selectedSensorVelocity))
    }

    val angle: Double
        get() = gyro.rotation2d.degrees

    var pose: Pose2d
        get() = odometry.poseMeters
        set(newPose) {
            odometry.resetPosition(newPose, gyro.rotation2d)
        }

    override fun periodic() {
        odometry.update(gyro.rotation2d, getWheelSpeeds())
    }

    fun mpsToUPS(mps: Double): Double {
        return (mps / driveDistancePerPulse) / 10.0
    }

    fun upsToMPS(mps: Double): Double {
        return (mps * driveDistancePerPulse) * 10.0
    }

    fun drive(chassisSpeeds: ChassisSpeeds) {
        val wheelSpeeds = kinematics.toWheelSpeeds(chassisSpeeds)

        frontLeft.set(TalonSRXControlMode.Velocity, mpsToUPS(wheelSpeeds.frontLeftMetersPerSecond))
        frontLeft.feed()

        frontRight.set(TalonSRXControlMode.Velocity, mpsToUPS(wheelSpeeds.frontRightMetersPerSecond))
        frontRight.feed()

        rearLeft.set(TalonSRXControlMode.Velocity, mpsToUPS(wheelSpeeds.rearLeftMetersPerSecond))
        rearLeft.feed()

        rearRight.set(TalonSRXControlMode.Velocity, mpsToUPS(wheelSpeeds.rearRightMetersPerSecond))
        rearRight.feed()
    }

    fun stop() {
        frontLeft.stopMotor()
        frontRight.stopMotor()
        rearLeft.stopMotor()
        rearRight.stopMotor()
    }
}