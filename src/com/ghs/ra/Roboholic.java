/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/
package com.ghs.ra;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.SimpleRobot;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the SimpleRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Roboholic extends SimpleRobot {

    /**
     * These variables and objects are initialized at the beginning of the class
     * so that all methods have access to them.
     */
    /**
     * The CIM Motors are controlled by Victors, which are connected to the
     * sidecar on Digital Outputs 1 & 2. The servo power jumper should not be
     * attached to those outputs.
     *
     */
    Victor tankLeftFront;
    Victor tankRightFront;
    Victor tankLeftRear;
    Victor tankRightRear;
    CustomDrive tankDrive;
    // The motors can be inverted by changing the value of these booleans in the network table.
    boolean invertLeft;
    boolean invertRight;

//        Be very careful to plug Joysticks in in the correct order,
//        otherwise the robot can behave erratically
    Joystick at3Left = new Joystick(1); // Logitec Attack 3 Joystick, put in port 1
    Joystick triAxis = new Joystick(2); // Calibrated 3 axis Joystick, put in port 2

    /**
     * These variables are used for the deadband implementation.
     *
     */
    double DEADBAND; //The radius from the center of the joystick defined to be zero
    double speedY; // Forward vector of joystick
    double speedTwist; // Rotational Vector of joystick
    double jsY;
    double jsTwist;
    double jsTwistCal;
    double jsYCal;
    double speedLeft;
    double speedRight;

    /**
     * Autonomous Control Tools:
     */
    Timer autoTimer = new Timer(); // Controls how long the robot moves forward
    double autoSpeed;
    double autoTime;
    double autoLeft;
    double autoRight;
    Gyro gyro;

    double autoKp;
    double autoAngle;
    boolean autoKpBool;
    double autoKpInvert;

    //Disable Stuff
    boolean killLeft, killRight;
    boolean enableMotors;

    /**
     * This function is called once each time the robot turns on.
     */
    public void robotInit() {
        tankLeftFront = new Victor(1, 1);
        // tankLeftFront.startLiveWindowMode();
        tankRightFront = new Victor(1, 3);
        // tankRightFront.startLiveWindowMode();
        tankLeftRear = new Victor(1, 2);
        // tankLeftRear.startLiveWindowMode();
        tankRightRear = new Victor(1, 4);
        // tankRightRear.startLiveWindowMode();
        tankDrive = new CustomDrive(this, tankLeftFront, tankLeftRear, tankRightFront, tankRightRear);

    }

    /**
     * This function is called once each time the robot enters autonomous mode.
     */
    public void autonomous() {
        gyro = new Gyro(1);
        gyro.reset();
        autoTimer.reset();
        autoTimer.start();
        // These are sliders 3-5 on the DS IO tab, and Digital In 5.

        autoSpeed = ((DriverStation.getInstance().getAnalogIn(3)) / 5); // How fast to go during Autonomous
        autoTime = (DriverStation.getInstance().getAnalogIn(4)); // How long to drive forward during Autonomous
        autoKp = ((DriverStation.getInstance().getAnalogIn(1)) / 100);

        DriverStation.getInstance().setDigitalOut(5, autoKpBool);

        while (autoTimer.get() < autoTime) {
            getWatchdog().feed();
            autoAngle = gyro.getAngle(); // Get the heading.
            tankDrive.drive(autoSpeed, (-autoAngle * autoKp));

        }
        tankDrive.drive(0.0, 0.0);
    }

    /**
     * { tankDrive.tankDrive(0.5,0.0); Timer.delay(2.0); } {
     * tankDrive.tankDrive(0.0,0.0); }
     *
     */
    /**
     * This function is called once each time the robot enters operator control.
     */
    public void operatorControl() {
        getWatchdog().setExpiration(.2);
        DEADBAND = .2;
        // These correspond to button on the operator console.
        invertLeft = DriverStation.getInstance().getDigitalIn(1);
        invertRight = DriverStation.getInstance().getDigitalIn(2);

        // If a motor runs backward, toggle its boolean value
        tankDrive.setInvertedMotor(RobotDrive.MotorType.kFrontRight, invertRight);
        tankDrive.setInvertedMotor(RobotDrive.MotorType.kFrontLeft, invertLeft);
        tankDrive.setInvertedMotor(RobotDrive.MotorType.kRearRight, invertRight);
        tankDrive.setInvertedMotor(RobotDrive.MotorType.kRearLeft, invertLeft);

        while (isOperatorControl()) {
            getWatchdog().feed();
            jsTwistCal = ((at3Left.getRawAxis(3) + 1) / 2);
            jsYCal = ((at3Left.getRawAxis(3) + 1) / 2);
            if (at3Left.getRawButton(6)||at3Left.getRawButton(7)||
                    at3Left.getRawButton(10)||at3Left.getRawButton(11)) {
                enableMotors = false;
            }
            if (at3Left.getRawButton(8)&&at3Left.getRawButton(9)) {
                enableMotors = true;
            }
           

            speedY = scaleToDeadband(triAxis, 2, DEADBAND, jsYCal);
            speedTwist = scaleToDeadband(triAxis, 3, DEADBAND, jsTwistCal);
            //The following code takes a forward and a rotational vector, and
            // combines them into a left and right speed vector for the motors.
            tankDrive.triAxisArcade(speedY, speedTwist, enableMotors);
        }

    }

    /**
     * This function is called once each time the robot enters test mode.
     */
    public void test() {

    }

    /**
     * Thanks to Colby Skeggs for effectively creating this subclass of
     * RobotDrive for us while queing at a competition
     */
    private static class CustomDrive extends RobotDrive {

        private final Roboholic main;
        boolean printed = false;

        public CustomDrive(Roboholic main, SpeedController frontLeftMotor, SpeedController rearLeftMotor, SpeedController frontRightMotor, SpeedController rearRightMotor) {
            super(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor);
            this.main = main;
        }

      /**  public void setLeftRightMotorOutputs(double leftOutput, double rightOutput) {
            try {
                if (main.killLeft) {
                    m_frontLeftMotor = null;
                    main.tankLeftFront.disable();
                } else {
                    m_frontLeftMotor = main.tankLeftFront;
                }

                if (main.killRight) {
                    m_frontRightMotor = null;
                    main.tankRightFront.disable();
                } else {
                    m_frontRightMotor = main.tankRightFront;
                }
            } catch (Exception e) {
                if (!printed) {
                    e.printStackTrace();
                    printed = true;
                }
            }
            super.setLeftRightMotorOutputs(leftOutput, rightOutput);
        }**/

        public void triAxisArcade(double speedValue, double rotateValue, boolean enable) {

            double speedLeft;
            double speedRight;

            if (enable) {

                speedLeft = (speedValue - rotateValue);
                speedRight = (speedValue + rotateValue);
                if (Math.abs(speedLeft) > 1) {
                    speedLeft = (Math.abs(speedLeft) / speedLeft);
                    speedRight = speedRight / (Math.abs(speedLeft));
                }
                if (Math.abs(speedRight) > 1) {
                    speedRight = (Math.abs(speedRight) / speedRight);
                    speedLeft = speedLeft / (Math.abs(speedRight)); //Thank you Ian
                    //Bow down and worship me! -Ian
                }
            } else {
                speedLeft = speedRight = 0;
            }

            super.tankDrive(speedLeft, speedRight);
        }

    }

    /**
     * scaleToDeadband takes a joystick, axis, the desired deadband and a speed
     * multiplier, and returns either zero or a scaled value, depending on
     * whether or not the raw value is within the deadband.
     *
     * @param joystick
     * @param axis
     * @param deadband
     * @param multiplier
     * @return
     */
    public double scaleToDeadband(Joystick joystick, int axis, double deadband, double multiplier) {

        double rawValue = (joystick.getRawAxis(axis));
        double returnValue;

        if ((Math.abs(rawValue)) < deadband) {
            returnValue = 0;
        } else {
            returnValue = (multiplier * (rawValue - (Math.abs(rawValue)
                    / rawValue * deadband) / (1 - deadband)));
        }
        return returnValue;

    }

    public double[] combineForwardTwist(double forwardSpeed, double twistSpeed, double maxValue) {

        double[] returnValue = new double[2];
        speedLeft = (forwardSpeed - twistSpeed);
        speedRight = (forwardSpeed + twistSpeed);
        if (Math.abs(speedLeft) > 1) {
            speedLeft = (Math.abs(speedLeft) / speedLeft);
            speedRight = speedRight / (Math.abs(speedLeft));
        }
        if (Math.abs(speedRight) > 1) {
            speedRight = (Math.abs(speedRight) / speedRight);
            speedLeft = speedLeft / (Math.abs(speedRight)); //Thank you Ian
            //Bow down and worship me! -Ian
        }
        returnValue[0] = speedLeft;
        returnValue[1] = speedRight;

        return returnValue;
    }

}
