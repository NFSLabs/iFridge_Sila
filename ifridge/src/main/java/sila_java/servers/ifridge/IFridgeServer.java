package sila_java.servers.ifridge;

import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import sila2.de.igripper.ifridge.v1.IFridgeGrpc;
import sila2.de.igripper.ifridge.v1.IFridgeOuterClass;
import sila2.org.silastandard.SiLAFramework;
import sila2.org.silastandard.core.simulationcontroller.v1.SimulationControllerOuterClass;
import sila2.org.silastandard.core.simulationcontroller.v1.SimulationControllerGrpc;
import sila_java.library.core.encryption.SelfSignedCertificate;
import sila_java.library.core.sila.types.SiLABoolean;
import sila_java.library.core.sila.types.SiLAErrors;
import sila_java.library.core.sila.types.SiLAString;
import sila_java.library.server_base.SiLAServer;
import sila_java.library.server_base.identification.ServerInformation;
import sila_java.library.server_base.utils.ArgumentHelper;

import java.io.IOException;

import static sila_java.library.core.utils.Utils.blockUntilStop;
import static sila_java.library.core.utils.FileUtils.getResourceContent;


/**
 * SiLA Server for iFridge
 *
 * @implNote
 */
@Slf4j
public class IFridgeServer implements AutoCloseable {
    public static final String SERVER_TYPE = "iFridge";
    private IIFridgeDriver driver;
    private final SiLAServer siLAServer;
    private Boolean isPI;
    private final static Boolean START_WITH_REAL_MODE = true;

    public IFridgeServer(@NonNull final ArgumentHelper argumentHelper) {
         final ServerInformation serverInfo = new ServerInformation(
                SERVER_TYPE,
                "IFridge Server. Allows opening and closing "
                        + "a door with the help of a SiLABOX"
                        + "and iFridge electronics",
                "http://www.i-gripper.de",
                "v0.1"
        );

        this.isPI = new CheckOS().isPiUnix;
        try {
            if (this.isPI && START_WITH_REAL_MODE){
                driver = new IFridgeDriver();
            }
            else {
                driver  = new IFridgeDriverSim();
            }}
        catch (DoorException e){
            log.error(e.getMessage(),e);
        }

        try {
            final SiLAServer.Builder builder;
            if (argumentHelper.getConfigFile().isPresent()) {
                builder = SiLAServer.Builder.withConfig(argumentHelper.getConfigFile().get(), serverInfo);
            }
            else {
                builder = SiLAServer.Builder.withoutConfig(serverInfo);
            }
            argumentHelper.getPort().ifPresent(builder::withPort);
            argumentHelper.getInterface().ifPresent(builder::withDiscovery);
            if (argumentHelper.useEncryption()) {
                builder.withSelfSignedCertificate();
            }
            builder.addFeature(
                    getResourceContent("IFridge.xml"),
                    new IFridgeImpl())
            .addFeature(
                     getResourceContent("SimulationController.sila.xml"),
                     new SimulationImpl()) ;
                     this.siLAServer = builder.start();
        } catch (IOException | SelfSignedCertificate.CertificateGenerationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.siLAServer.close();
        this.driver.close();
    }

    public static void main(final String[] args) {
        final ArgumentHelper argumentHelper = new ArgumentHelper(args, SERVER_TYPE);
        try (final IFridgeServer server = new IFridgeServer(argumentHelper)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            blockUntilStop();
        }
        log.info("Termination complete.");
    }

    class IFridgeImpl extends IFridgeGrpc.IFridgeImplBase {
        @Override
        public void deviceStatus(
                IFridgeOuterClass.DeviceStatus_Parameters request,
                StreamObserver<IFridgeOuterClass.DeviceStatus_Responses> responseObserver
        ) {

            responseObserver.onNext(
                    IFridgeOuterClass.DeviceStatus_Responses
                            .newBuilder()
                            .setStatus(SiLAString.from("OK"))
                            .build()
            );
            responseObserver.onCompleted();
        }

        @Override
        public void openDoor(IFridgeOuterClass.OpenDoor_Parameters req,
                             StreamObserver<IFridgeOuterClass.OpenDoor_Responses> responseObserver) {
            try {
                log.info("Trying to open the door");
                IFridgeServer.this.driver.openDoor(60);
                responseObserver.onNext(IFridgeOuterClass.OpenDoor_Responses.newBuilder().build());
                responseObserver.onCompleted();
            } catch (DoorException e) {
                responseObserver.onError(SiLAErrors.generateDefinedExecutionError(
                        "openDoorError",
                        e.getMessage() + " Could not open the door"
                ));
            }
        }

        @Override
        public void closeDoor(IFridgeOuterClass.CloseDoor_Parameters req,
                              StreamObserver<IFridgeOuterClass.CloseDoor_Responses> responseObserver) {
            try {
                log.info("Trying to close the door");
                IFridgeServer.this.driver.closeDoor(60);
                responseObserver.onNext(IFridgeOuterClass.CloseDoor_Responses.newBuilder().build());
                responseObserver.onCompleted();
            } catch (DoorException e) {
                responseObserver.onError(SiLAErrors.generateDefinedExecutionError(
                        "closeDoorError",
                        e.getMessage() + " Could not close the door"
                ));
            }
        }

        private <T> void genericMoveDoor(
                IFridgeUtils.IFridgeCommands command,
                StreamObserver<T> responseObserver, T response) {

            /*
            if (!SilaDoorServer.this.driver.isDriverUp()) {
                responseObserver.onError(
                        SiLAErrors.generateDefinedExecutionError(
                                "DeviceNotUp",
                                "DeviceNotUp")
                );
                return;
            }
            try {
                switch (command){
                    case OPEN_DOOR:
                        SilaDoorServer.this.driver.OpenDoor();
                        break;
                    case CLOSE_DOOR:
                        SilaDoorServer.this.driver.CloseDoor();
                        break;
                    default:
                        SilaDoorServer.this.driver.CloseDoor();

                }

            } catch (IOException e ) {
                responseObserver.onError(SiLAErrors.generateDefinedExecutionError(
                        "SilaDoorError",
                        e.getMessage()));
                return;
            }
            */
            responseObserver.onCompleted();
        }
        }

        class SimulationImpl extends SimulationControllerGrpc.SimulationControllerImplBase {
            @Override
            public void startSimulationMode(SimulationControllerOuterClass.StartSimulationMode_Parameters req,
                                            StreamObserver<SimulationControllerOuterClass.StartSimulationMode_Responses> responseObserver) {
                simControl(true,
                        responseObserver,
                        SimulationControllerOuterClass.StartSimulationMode_Responses.newBuilder().build());
            }

            @Override
            public void startRealMode(SimulationControllerOuterClass.StartRealMode_Parameters req,
                                      StreamObserver<SimulationControllerOuterClass.StartRealMode_Responses> responseObserver) {
                simControl(false,
                        responseObserver,
                        SimulationControllerOuterClass.StartRealMode_Responses.newBuilder().build());
            }

            @Override
            public void getSimulationMode(SimulationControllerOuterClass.Get_SimulationMode_Parameters req,
                                          StreamObserver<SimulationControllerOuterClass.Get_SimulationMode_Responses> responseObserver) {
                try {
                    SiLAFramework.Boolean simStatus = SiLABoolean.from(IFridgeServer.this.driver.isSimulationMode());
                    SimulationControllerOuterClass.Get_SimulationMode_Responses sc =
                            SimulationControllerOuterClass.Get_SimulationMode_Responses.newBuilder().setSimulationMode(simStatus).build();
                    responseObserver.onNext(sc);
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    responseObserver.onError(SiLAErrors.generateDefinedExecutionError(
                            "Driver error",
                            e.getMessage() + " Driver not up or did not respond"));
                }
            }
            /**
             * starts and stops simulation
             * @param mode TRUE = simulation, FALSE = REAL
             */
             private <T> void simControl(Boolean mode,
                    StreamObserver<T> responseObserver, T response)  {
                try{
                    if (mode == IFridgeServer.this.driver.isSimulationMode())
                        {return;}
                    if (mode == false /*&& isPI*/) {
                         driver.close();
                         driver = new IFridgeDriver();
                    }
                    else {
                        driver.close();
                        driver = new IFridgeDriverSim();}
                    }
                catch (DoorException e) {
                        responseObserver.onError(SiLAErrors.generateDefinedExecutionError(
                            "Driver error",
                            e.getMessage() + " Could not switch driver mode to real mode or to simulation mode "));
                        return;
                    }
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
    }
}




