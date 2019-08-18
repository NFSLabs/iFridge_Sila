package sila_java.servers.ifridge;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import sila2.org.silastandard.SiLAFramework;
import sila2.org.silastandard.core.silaservice.v1.SiLAServiceGrpc;
import sila2.org.silastandard.core.silaservice.v1.SiLAServiceOuterClass;
import sila2.de.igripper.ifridge.v1.IFridgeGrpc;
import sila2.de.igripper.ifridge.v1.IFridgeOuterClass;

import sila2.org.silastandard.core.simulationcontroller.v1.SimulationControllerGrpc;
import sila2.org.silastandard.core.simulationcontroller.v1.SimulationControllerOuterClass;
import sila_java.library.core.sila.clients.ChannelFactory;
import sila_java.library.manager.ServerFinder;
import sila_java.library.manager.ServerManager;
import sila_java.library.manager.models.Server;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static sila_java.library.core.sila.types.SiLAErrors.generateMessage;
import static sila_java.library.core.sila.types.SiLAErrors.retrieveSiLAError;

/**
 * Client to retrieve Connection status from the {@link IFridgeServer}.
 * and send commands to the {@link IFridgeServer}
 *
 * For Testing purposes
 */

@Slf4j
public class IFridgeClient {
    public static final String SERVER_TYPE = "iFridge";
    private IFridgeGrpc.IFridgeBlockingStub blockingStubIFridge;
    private SimulationControllerGrpc.SimulationControllerBlockingStub blockingStubSim;

    private void buildStub(final ManagedChannel channel) {
        this.blockingStubIFridge = IFridgeGrpc.newBlockingStub(channel);
    }

    private void buildStubSim(final ManagedChannel channel) {
        this.blockingStubSim = SimulationControllerGrpc.newBlockingStub(channel);
    }


    /**
     * Test if the server is in principle operational and can respond
     */
    private void ConnectionTest() {
        System.out.println("Will try to Connect ...");
        IFridgeOuterClass.DeviceStatus_Responses connectionStatus;
        try {
            connectionStatus = blockingStubIFridge.deviceStatus(
                    IFridgeOuterClass.DeviceStatus_Parameters.newBuilder().build()
            );
        } catch (StatusRuntimeException e) {
            final SiLAFramework.SiLAError siLAError = retrieveSiLAError(e);

            if (siLAError == null) {
                throw new RuntimeException("Not A SiLA Error: " + e.getMessage());
            }
            System.out.println(generateMessage(siLAError));
            return;
        }
        System.out.println("Connection Test: " + connectionStatus.getStatus());
    }
    /**
     * Test if the server is in principle operational and can respond
     */
    private void OpenDoor() {
        System.out.println("Will try to open the door");
        IFridgeOuterClass.OpenDoor_Responses openDoor;
        try {
            openDoor = blockingStubIFridge.openDoor(
                    IFridgeOuterClass.OpenDoor_Parameters.newBuilder().build()
            );
        } catch (StatusRuntimeException e) {
            final SiLAFramework.SiLAError siLAError = retrieveSiLAError(e);

            if (siLAError == null) {
                throw new RuntimeException("Not A SiLA Error: " + e.getMessage());
            }
            System.out.println(generateMessage(siLAError));
            return;
        }
        System.out.println("Open Door Result: " + openDoor.getStatus());
    }
    private void StartSimulationMode() {
        System.out.println("Will try to start simulation mode");
        SimulationControllerOuterClass.StartSimulationMode_Responses startSimulationMode;
        try {
            startSimulationMode = blockingStubSim.startSimulationMode(
                    SimulationControllerOuterClass.StartSimulationMode_Parameters.newBuilder().build()
            );
        }
        catch (StatusRuntimeException e) {
            final SiLAFramework.SiLAError siLAError = retrieveSiLAError(e);
            if (siLAError == null) {
                throw new RuntimeException("Not A SiLA Error: " + e.getMessage());
            }
            System.out.println(generateMessage(siLAError));
            return;
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("Start Real mode worked: ");
    }
    private void StartRealMode() {
        System.out.println("Will try to start real mode");

        SimulationControllerOuterClass.StartRealMode_Responses startRealMode;
        try {
            startRealMode = blockingStubSim.startRealMode(
                    SimulationControllerOuterClass.StartRealMode_Parameters.newBuilder().build()
            );
        } catch (StatusRuntimeException e) {
            final SiLAFramework.SiLAError siLAError = retrieveSiLAError(e);

            if (siLAError == null) {
                throw new RuntimeException("Not A SiLA Error: " + e.getMessage());
            }
            System.out.println(generateMessage(siLAError));
            return;
        }
        System.out.println("Start Real mode worked: ");
    }

    /**
     * Simple Client that stops after using the GreetingProvider Feature
     */
    public static void main(String[] args) throws InterruptedException {
        try (final ServerManager serverManager = ServerManager.getInstance()) {

            IFridgeClient client = new IFridgeClient();
            // Create Manager for clients and start discovery
            final Server server = ServerFinder
                    .filterBy(ServerFinder.Filter.type(IFridgeServer.SERVER_TYPE))
                    .scanAndFindOne(Duration.ofMinutes(1))
                    .orElseThrow(() -> new RuntimeException("No iFridge server found within time"));
            log.info("Found Server!");

            final ManagedChannel serviceChannelIFridge = ChannelFactory.withEncryption(server.getHost(), server.getPort());
            final ManagedChannel serviceChannelSim = ChannelFactory.withEncryption(server.getHost(), server.getPort());
            try {
                final SiLAServiceGrpc.SiLAServiceBlockingStub serviceStub = SiLAServiceGrpc.newBlockingStub(serviceChannelIFridge);
                System.out.println("Found Features:");
                final List<SiLAServiceOuterClass.DataType_FeatureIdentifier> featureIdentifierList = serviceStub
                        .getImplementedFeatures(SiLAServiceOuterClass.Get_ImplementedFeatures_Parameters.newBuilder().build())
                        .getImplementedFeaturesList();
                featureIdentifierList.forEach(featureIdentifier ->
                        System.out.println("\t" + featureIdentifier.getFeatureIdentifier())
                );
                client.buildStub(serviceChannelIFridge);
                client.buildStubSim(serviceChannelSim);
                client.ConnectionTest();
                client.OpenDoor();
                client.StartRealMode();
                client.StartSimulationMode();
            }
            catch( Exception e) {
            }
            finally {
                serviceChannelIFridge.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                serviceChannelSim.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }
}
