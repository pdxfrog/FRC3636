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
        Victor tankLeft = new Victor(1,1);
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
        double speedLeft;
        double speedRight;
        double jsLeftX;
        double jsRightX;
        double jsLeftCal;
        double jsRightCal;
        
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
       
    
    /**
     * This function is called once each time the robot turns on.
     */
    public void robotInit(){

        compressor.start();
        
        
        
     
    }
    
    /**
     * This function is called once each time the robot enters autonomous mode.
     */
    public void autonomous() {
        gyro.reset();
        autoTimer.reset();
        autoTimer.start();
        // These are sliders 3-5 on the DS IO tab, and Digital In 5.
        
        autoSpeed = ((DriverStation.getInstance().getAnalogIn(3))/5); // How fast to go during Autonomous
        autoTime = (DriverStation.getInstance().getAnalogIn(4)); // How long to drive forward during Autonomous
        autoKp = ((DriverStation.getInstance().getAnalogIn(5))/100);
        autoKpBool = (DriverStation.getInstance().getDigitalIn(5));
        if(autoKpBool){
            autoKpInvert = -1;
        }
        if(!autoKpBool){
            autoKpInvert = 1;
        }
        DriverStation.getInstance().setDigitalOut(5, autoKpBool);
        
        
        
        while(autoTimer.get() < autoTime) {
            getWatchdog().feed();
            autoAngle = gyro.getAngle(); // Get the heading.
            tankDrive.drive(autoSpeed, (-autoAngle*autoKp*autoKpInvert));
            
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
        
        DEADBAND = .2;
        // These correspond to button on the operator console.
        invertLeft = DriverStation.getInstance().getDigitalIn(1);
        invertRight = DriverStation.getInstance().getDigitalIn(2);
        
      
        // If a motor runs backward, toggle its boolean value
        tankDrive.setInvertedMotor(RobotDrive.MotorType.kFrontRight, invertRight); 
        tankDrive.setInvertedMotor(RobotDrive.MotorType.kFrontLeft, invertLeft);   
        
    
        while (isOperatorControl()){
            getWatchdog().feed();
            jsLeftCal = ((DriverStation.getInstance().getAnalogIn(1))/5);
            jsRightCal = ((DriverStation.getInstance().getAnalogIn(2))/5);
   // first JS   
            jsLeftX = (at3Left.getY());
            if ((Math.abs(jsLeftX))<DEADBAND){
                    speedLeft = 0;
                }
                else {
                    speedLeft = (jsLeftCal*(jsLeftX-(Math.abs(jsLeftX)/
                            jsLeftX*DEADBAND)/(1-DEADBAND)));
                }
   // second JS         
            jsRightX = (at3Right.getY());
            if ((Math.abs(jsRightX))<DEADBAND){
                    speedRight = 0;
                }
                else {
                    speedRight = (jsRightCal*(jsRightX-(Math.abs(jsRightX)/
                            jsRightX*DEADBAND)/(1-DEADBAND)));
                }
            
            tankDrive.tankDrive(speedLeft, speedRight);
            if (at3Left.getTwist()==1){
                pistonState = true;
            }
            if (at3Left.getTwist()==(-1)) {
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
