package sila_java.servers.ifridge;
import java.io.IOException;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import de.pi3g.pi.oled.Font;
import de.pi3g.pi.oled.OLEDDisplay;



/**
 * iFridge Driver communicating over I2C with iFridge
 *
 *
 *  */
@Slf4j
public class IFridgeDriver implements IIFridgeDriver {
  
    private static final int MSG_TIMEOUT = 5000; // [ms] timeout on receiving message via I2C
    private static final int INITIALIZATION_TIMEOUT = 60; // [ms] timeout on initializing the system
    public static final int IFridge_I2C_Address = 0x26; // address pin not connected (FLOATING)
    private I2CBus i2c ;
    private Boolean IfridgeCommWorking=true;
    private static I2CDevice i2cDevice ;
    private Future<Boolean> future;
    private Boolean SimulationMode = false;
    public  Boolean isSimulationMode(){
        return SimulationMode;
    }

    public  IFridgeDriver() throws DoorException{
       try{
           startUp();}
       catch(Exception e){
            throw new DoorException(" Could not establish communication with iFridge ");
        }
    }

    /**
     * Check if Communication with iFridge can be stopped.
     * Wait until safe shutdown is possible. If it cannot be stopped after wait time, return false
     *
     *
     *  */

    private Boolean safeToShutdownI2C() {
        return true;
    }

    /**
     * Central class to establish connection and initialize iFridge
     *
     *  */
    private Boolean startUp() throws IOException{
        if (startI2CCom(INITIALIZATION_TIMEOUT))
            return true;
        else{
            throw new IOException(" Could not establish communication to iFridge electronics ");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        cancel();
        if (safeToShutdownI2C() ){
            i2cDevice = null;
        }
    }

    public boolean isDriverUp () {
        return (startI2CCom(INITIALIZATION_TIMEOUT));
    }

    /**
     * Check if I2C Can be started
     */
    private boolean startI2CCom(int timeout)   {
       if (new CheckOS().isPiUnix==false)
            {return false;}
       if (i2cDevice == null){
            try{
                i2c = I2CFactory.getInstance(I2CBus.BUS_1);
                i2cDevice = i2c.getDevice(IFridge_I2C_Address);
                // TODO: Some more checks if communication works
                SendToDisplay("i-Fridge v0.1");
                this.IfridgeCommWorking = true;
                return true;
                }
            catch (Exception e) {
                this.IfridgeCommWorking=false;
                return false;
            }
        }
        return true;
    }

    private Boolean stopI2CComm(int initialization_timeout){
        if (safeToShutdownI2C()){
            i2c = null;
            i2cDevice = null;
            return true;
        }
        return false;
    }
    /**
     * move the motor with a defined number of steps
     * positive means open, negative means close
     * Used for testing, should not be used in standard operation
     * m1 = unlock motor
     * m2 = door motor
     * @param motor_id which motor to move
     * @param steps steps to go. plus is open, minus close
     */
    private void move(int motor_id , int steps) throws IOException {
        try{
            SendToIFridge("m" + motor_id + IFridgeUtils.COMMAND_SEPARATOR + steps);}
        catch (IOException | IllegalStateException e) {
            String errorMessage = "Could not send command to IFridge. Error: " + e.getMessage();
            log.info(errorMessage);
            throw new IOException(errorMessage);
        }
    }

    /**
     * Send command to open the door to the iFridge
     * The iFridge takes care about the right steps
     **/

    public synchronized Boolean openDoor(long timeout) throws DoorException {
        // Start Read Buffer
        future = openDoorAsync(timeout);
        final boolean result;
        try {
            result = future.get(timeout,TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error(e.getMessage());
            throw new DoorException(" Door could not be opened in " + timeout + " seconds.");
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
            throw new DoorException(" Failed unexpectedly. ");
        } finally {
            future.cancel(true);
        }
        return result;
    }

     /**
     * Send command to open the door to the IFridge
     * The IFridge system takes care about the right steps
     **/
     public synchronized Boolean closeDoor(long timeout) throws DoorException {
         // Start Read Buffer
         future = closeDoorAsync(timeout);
         final boolean result;
         try {
             result = future.get(timeout,TimeUnit.SECONDS);
         } catch (TimeoutException e) {
             log.error(e.getMessage());
             throw new DoorException(" Door could not be close in " + timeout + " seconds. ");
         } catch (InterruptedException | ExecutionException e) {
             log.error(e.getMessage());
             throw new DoorException(" Failed unexpectedly. ");
         } finally {
             future.cancel(true);
         }
         return result;
     }

    // @Override
    public void cancel() {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    private static Future<Boolean> closeDoorAsync(long timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String LogInfo = "Send to iFridge: door=close";
                String i2cCmd = ("close");
                SendToIFridge(i2cCmd);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Future<Boolean> openDoorAsync(long timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String LogInfo = "Send to IFridge: Door open";
                String i2cCmd = ("open");
                SendToIFridge(i2cCmd);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Send command to IFridge
     * handshake to be implemented
     *
     */
    private static Boolean SendToIFridge(String cmd) throws IOException {
        try{
            I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1); // get bus
            i2cDevice  = bus.getDevice(IFridge_I2C_Address);  // Arduino
            byte[] buffer = cmd.getBytes();
            i2cDevice.write(buffer); // send something
            return true;
        }
         catch(Exception e) {
                throw new IOException(" Could not send command to iFridge ");
        }
    }

    /**
     * Send data to display
     *
     * @implNote Simulation mode to be implemented
     */
    private Boolean SendToDisplay(String infoString) throws IOException{

            try{
                OLEDDisplay display = new OLEDDisplay();
                display.drawStringCentered(infoString, Font.FONT_5X8, 25, true);
                display.update();
                log.info("Sent to display: "+ infoString);
                return true;}
            catch(Exception e) {
                //  Block of code to handle errors
                log.info(" Communication to display failed ");
                throw new IOException(" Communication not possible. Check connection. ");
            }
         }
}