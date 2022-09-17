package frc.robot

import edu.wpi.first.wpilibj.TimedRobot
import edu.wpi.first.wpilibj2.command.CommandScheduler
import frc.robot.commands.JoystickDrive
import frc.robot.commands.UseVision

object Robot : TimedRobot() {
    private val commandScheduler = CommandScheduler.getInstance()
    private val joystickDrive = JoystickDrive()
    private val useVision = UseVision()

    override fun robotInit() {
        Input.onInit()
    }

    override fun robotPeriodic() {
        commandScheduler.run()
    }

    override fun teleopInit() {
        joystickDrive.schedule()
    }

    override fun teleopExit() {
        joystickDrive.cancel()
    }

    override fun autonomousInit() {
        useVision.schedule()
    }

    override fun autonomousExit() {
        useVision.cancel()
    }
}