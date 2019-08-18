package sila_java.servers.ifridge;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import de.pi3g.pi.oled.Font;
import de.pi3g.pi.oled.OLEDDisplay;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.*;


/**
 * IFridge Driver that provides also simulation
 *
 * @implNote Simulation mode to be replaced
 *
 *  */
@Slf4j
public class IFridgeDriverSim implements IIFridgeDriver {

    private static final int INPUT_TIMEOUT = 5000; // [ms] timeout on receiving message
    private final static int SAMPLING_TIME = 2000; // [ms] time to sample heartBeat
    private boolean IfridgeCommWorking=true;  // is I2C communication with IFridge possible and status is ok?
    private IFridgeUtils.DeviceType type;
    public static final int IFridge_I2C_Address = 0x26; // address pin not connected (FLOATING)
    private I2CBus i2c ;
    private static I2CDevice i2cDevice ;
    private GpioController gpio;
    private static long timeForCommandSim =10; //[s]
    private Future<Boolean> future;
    private Boolean SimulationMode = true;

    public Boolean isSimulationMode(){
        return SimulationMode;
    }
    public IFridgeDriverSim() {

       // isDriverUp();
       // this.synchronousCommunication.startHeartbeat(SAMPLING_TIME, this::isDriverUp);

    }

    /**
     * Check if Communication with IFridge can be stopped.
     * Wait until safe shutdown is possible, if after wait time cannot be stopped, return false
     *
     * @implNote Still has to be implemented
     *
     *  */

    private Boolean safeToShutdownI2C() {
        return true;
    }

    private Boolean shutdownI2C(){
        if (safeToShutdownI2C()){
            i2c = null;
            i2cDevice = null;
            return true;
        }
        return false;
    }

    private Boolean shutdown(){
        if (safeToShutdownI2C()){
            return true;
        }
        return false;
    }
    /**
     * {@inheritDoc}
     */
    public void close() {
        cancel();
        if (safeToShutdownI2C()){
            shutdownI2C();
        }
    }

    public boolean isDriverUp () {
       return true;
    }


    /**
     * move the motor with a defined number of steps
     * positive means open, negative means close
     * Used for testing, should not be used in standard operation
     * m1 = unlock motor
     * m2 = door motor
     * @param motornumber which motor to move
     * @param steps steps to go. plus is open, minus close
     */
    private void move(int motornumber , int steps) throws IOException {

    }

    /**
     * Send command to open the door to the IFridge Arduino
     * The Arduino takes care about the right steps
     **/

    public synchronized Boolean openDoor(long timeout) throws DoorException {
        // Start Read Buffer
        future = openDoorAsync(timeout);
        final boolean result;
        try {
            result = future.get(timeout,TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error(e.getMessage());
            throw new DoorException("Door could not be opened in " + timeout + " seconds.");
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
            throw new DoorException("Failed unexpectedly.");
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

    public synchronized Boolean closeDoor(long timeout) throws DoorException {
        // Start Read Buffer
        future = closeDoorAsync(timeout);
        final boolean result;
        try {
            result = future.get(timeout,TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error(e.getMessage());
            throw new DoorException("Door could not be closed in " + timeout + " seconds.");
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
            throw new DoorException("Failed unexpectedly.");
        } finally {
            future.cancel(true);
        }
        return result;
    }

    private static Future<Boolean> openDoorAsync(long timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (long i= timeForCommandSim; i>0 ; i--){
                    log.info("Opening door. Time left " + i + " seconds");
                Thread.sleep(1000);
            } log.info("Door opened");
                 return true;
            } catch ( InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Future<Boolean> closeDoorAsync(long timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (long i= timeForCommandSim; i>0 ; i--){
                    log.info("Closing door. Time left " + i + " seconds");
                    Thread.sleep(1000);
                } log.info("Door closed");
                return true;
            } catch ( InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Boolean SendToIFridge(String cmd) throws IOException {
        try{
            return true;
        }
         catch(Exception e) {
                throw new IOException("Could not send command to IFridge");
        }
    }

    /**
     * Send data to display
     *
     * @implNote
     */
    private Boolean SendToDisplay(String infoString) throws IOException{
            try{
                log.info("Sent to display: "+ infoString);
                return true;}
            catch(Exception e) {
                //  Block of code to handle errors
                log.info("Communication to display failed");
                throw new IOException("communication not possible. check connection");
            }
         }

}