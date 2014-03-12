/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.ghs.ra;


import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.Dashboard;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.SimpleRobot;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.buttons.Button;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

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
         **/
        Victor tankLeft = new Victor(1,3);
        Victor tankRight = new Victor(1,2);
        RobotDrive tankDrive = new RobotDrive(tankLeft, tankRight);
         // The motors can be inverted by changing the value of these booleans in the network table.
        boolean invertLeft;
        boolean invertRight;
        
//        Be very careful to plug Joysticks in in the correct order,
//        otherwise the robot can behave erratically
        Joystick at3Left = new Joystick(1);
        Joystick at3Right = new Joystick(2);
        Joystick sideWinder = new Joystick(3);
        
        /**
         * These variables are used for the deadband implementation.
         **/
        double DEADBAND;
        double speedY;
        double speedTwist;
        double jsY;
        double jsTwist;
        double jsCal;
        double jsRightCal;
        double speedLeft;
        double speedRight;
        
        /**
         * This allow the update of variables without a reboot!
         */
        boolean compOn;
        
       
        /**
         * Pneumatics:
         */
        Compressor compressor = new Compressor(1,1);
        Solenoid pistonIn = new Solenoid(1,1);
        Solenoid pistonOut = new Solenoid(1,2);
        Solenoid vent = new Solenoid(1,4);
        Solenoid shootersolenoid = new Solenoid(1,3);
        
        boolean pistonState;
        /**
         * Autonomous Control Tools:
         */
        Timer autoTimer = new Timer();
        double autoSpeed;
        double autoTime;
        double autoLeft;
        double autoRight;
        Gyro gyro;
                
        double autoKp;
        double autoAngle;
        boolean autoKpBool;
        double autoKpInvert;
       
        //buttons
        boolean leftButtonUp;
        boolean leftButtonDown;
        boolean rightButtonVent;
    
    /**
     * This function is called once each time the robot turns on.
     */
    public void robotInit(){

        compressor.start();
        
        pistonIn.set(true);
        pistonOut.set(false);
        
     
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
        
        autoSpeed = ((DriverStation.getInstance().getAnalogIn(3))/5); // How fast to go during Autonomous
        autoTime = (DriverStation.getInstance().getAnalogIn(4)); // How long to drive forward during Autonomous
        autoKp = ((DriverStation.getInstance().getAnalogIn(1))/100);
    
        DriverStation.getInstance().setDigitalOut(5, autoKpBool);
        
        
        
        while(autoTimer.get() < autoTime) {
            getWatchdog().feed();
            autoAngle = gyro.getAngle(); // Get the heading.
            tankDrive.drive(autoSpeed, (-autoAngle*autoKp));
            
        }
        tankDrive.drive(0.0, 0.0);
    }
    /**   
    {  tankDrive.tankDrive(0.5,0.0);
        Timer.delay(2.0);
    }
    {
        tankDrive.tankDrive(0.0,0.0);
    }
    **/
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
    
        while (isOperatorControl()){
            getWatchdog().feed();
            jsCal = ((DriverStation.getInstance().getAnalogIn(1))/5);
      
   // first JS   
            jsY = (at3Right.getY());
            if ((Math.abs(jsY))<DEADBAND){
                    speedY = 0;
                }
                else {
                    speedY = (jsCal*(jsY-(Math.abs(jsY)/
                            jsY*DEADBAND)/(1-DEADBAND)));
                }
   // second JS         
            jsTwist = (at3Right.getTwist());
            if ((Math.abs(jsTwist))<DEADBAND){
                    speedTwist = 0;
                }
                else {
                    speedTwist = (jsCal*(jsTwist-(Math.abs(jsTwist)/
                            jsTwist*DEADBAND)/(1-DEADBAND)));
                }
            speedLeft = (speedY - speedTwist);
            speedRight = (speedY + speedTwist);
            if (Math.abs(speedLeft)>1){
                speedLeft = (Math.abs(speedLeft)/speedLeft);
                speedRight = (Math.abs(speedLeft)/speedRight);
            }
            if (Math.abs(speedRight)>1){
                speedRight = (Math.abs(speedRight)/speedRight);
                speedLeft = (Math.abs(speedRight)/speedLeft);
            }
            
            tankDrive.tankDrive(speedLeft, speedRight);
            
            // These buttons control the fork
            leftButtonUp = at3Left.getRawButton(3);
            leftButtonDown = at3Left.getRawButton(2);
            if (leftButtonDown){
                pistonState = true;
            }
            if (leftButtonUp) {
                pistonState = false;
            }
            pistonIn.set(pistonState);
            pistonOut.set(!pistonState);
            
            shootersolenoid.set(at3Right.getTrigger());
            //Compressor control
             compOn = DriverStation.getInstance().getDigitalIn(3);
            DriverStation.getInstance().setDigitalOut(3, compOn);
            if(compOn){
                compressor.start();
            }
            else{
                compressor.stop();
            }
            rightButtonVent = at3Left.getRawButton(11);
            vent.set(rightButtonVent);
        }
        
       
        
    }
    
    /**
     * This function is called once each time the robot enters test mode.
     */
    
    public void test() {
        while(isTest()){
            compOn = DriverStation.getInstance().getDigitalIn(3);
            DriverStation.getInstance().setDigitalOut(3, compOn);
            if(compOn){
                compressor.start();
            }
            else{
                compressor.stop();
            }
        }
  
        
        
    }
}
