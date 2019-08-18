package sila_java.servers.ifridge;


//mport sila_java.library.server_base.standard_features.SimulationController.Cancellable;

public interface IIFridgeDriver /*extends Cancellable*/ {

    Boolean openDoor(long timeout) throws DoorException;
    Boolean closeDoor(long timeout) throws DoorException;
    Boolean isSimulationMode() throws DoorException;
    void close( ) ;
}
