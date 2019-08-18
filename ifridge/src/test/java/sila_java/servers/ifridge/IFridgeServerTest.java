package sila_java.servers.ifridge;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import sila2.de.igripper.ifridge.v1.IFridgeGrpc;
import sila2.de.igripper.ifridge.v1.IFridgeOuterClass;
import sila2.org.silastandard.SiLAFramework;
import sila2.org.silastandard.core.silaservice.v1.SiLAServiceGrpc;
import sila2.org.silastandard.core.silaservice.v1.SiLAServiceOuterClass;

import sila2.org.silastandard.core.simulationcontroller.v1.SimulationControllerGrpc;
import sila2.org.silastandard.core.simulationcontroller.v1.SimulationControllerOuterClass;
import sila_java.library.core.sila.clients.ChannelFactory;
import sila_java.library.manager.ServerFinder;
import sila_java.library.manager.ServerManager;
import sila_java.library.manager.models.Server;
import sila_java.library.server_base.utils.ArgumentHelper;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static sila_java.library.core.sila.types.SiLAErrors.retrieveSiLAError;

/**
 * Simple Integration Test for the IFridge Server
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IFridgeServerTest {
    private IFridgeServer server;
    private ManagedChannel channel;
    private IFridgeGrpc.IFridgeBlockingStub  blockingStub;
    private SimulationControllerGrpc.SimulationControllerBlockingStub  simulationBlockingStub;
    @BeforeAll
    void IFridgeServerTest() {
        log.info("Starting IFridgeServer...");
        final String[] args = {"-n", "local"};
        this.server = new IFridgeServer(new ArgumentHelper(args, IFridgeServer.SERVER_TYPE));

        final Server server = ServerFinder
                .filterBy(ServerFinder.Filter.type(IFridgeServer.SERVER_TYPE))
                .scanAndFindOne(Duration.ofMinutes(1))
                .orElseThrow(RuntimeException::new);

        this.channel = ChannelFactory.withEncryption(server.getHost(), server.getPort());
        this.blockingStub = IFridgeGrpc.newBlockingStub(this.channel);
        this.simulationBlockingStub = SimulationControllerGrpc.newBlockingStub(this.channel);
    }

    @AfterAll
    void cleanup() throws InterruptedException {
        if (server != null) {
            server.close();
            server = null;
        }
        ServerManager.getInstance().close();
        this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testIFridgeService() {
        System.out.println("Testing iFridgeService");
        final SiLAServiceGrpc.SiLAServiceBlockingStub serviceStub = SiLAServiceGrpc
                .newBlockingStub(this.channel);
        final List<SiLAServiceOuterClass.DataType_FeatureIdentifier> featureIdentifierList = serviceStub
                .getImplementedFeatures(SiLAServiceOuterClass.Get_ImplementedFeatures_Parameters
                        .newBuilder()
                        .build())
                .getImplementedFeaturesList();

        assertEquals(2, featureIdentifierList.size());

        Assertions.assertTrue(
                featureIdentifierList
                        .stream()
                        .anyMatch(featureIdentifier ->
                                "de.igripper/IFridge/v1".equals(
                                        featureIdentifier.getFeatureIdentifier().getValue()
                                )
                        )
        );
    }

    @Test
    void SwitchToRealModeTest() {
        try {
            simulationBlockingStub.startRealMode(
                    SimulationControllerOuterClass.StartRealMode_Parameters.newBuilder().build());
         } catch (StatusRuntimeException e) {
            System.out.println("Testing for wrong platform...");
            final SiLAFramework.SiLAError siLAError = retrieveSiLAError(e);
            System.out.println("Message was "+e.getMessage());
            assertNotNull(siLAError);
            assertTrue(siLAError.hasDefinedExecutionError());
            return;
        }

        fail("iFridge Server did not throw an error when not on the right platform assuming tests do not run on a RPi ");
    }
    @Test
    void OpenDoorTest() {
        IFridgeOuterClass.OpenDoor_Responses openDoor;
        openDoor = blockingStub.openDoor(
                IFridgeOuterClass.OpenDoor_Parameters.newBuilder().build() );
    }

/*
    @Test
    void DeviceStatusTest() {
        IFridgeOuterClass.DeviceStatus_Responses deviceStatus;
        deviceStatus = blockingStub.deviceStatus(
                IFridgeOuterClass.DeviceStatus_Parameters.newBuilder().build() );
        assertEquals("OK", deviceStatus.getStatus().getValue());
    }

    @Test
    void OpenDoorTest() {
        IFridgeOuterClass.OpenDoor_Responses openDoor;
        openDoor = blockingStub.openDoor(
                IFridgeOuterClass.OpenDoor_Parameters.newBuilder().build() );
        //assertEquals("OK", openDoor.getStatus().getValue());
    }

    @Test
    void CloseDoorTest() {
        IFridgeOuterClass.CloseDoor_Responses closeDoor;
        closeDoor = blockingStub.closeDoor(
                IFridgeOuterClass.CloseDoor_Parameters.newBuilder().build() );
        //assertEquals("OK", closeDoor.getStatus().getValue());
    }

    @Test
    void SimulationModeTest() {
        SimulationControllerOuterClass.Get_SimulationMode_Responses simulationStatus;
        simulationStatus = simulationBlockingStub.getSimulationMode(
                SimulationControllerOuterClass.Get_SimulationMode_Parameters.newBuilder().build() );
        assertEquals(true, simulationStatus.getSimulationMode().getValue());
    }
 */
/*
    @Test
    void SwitchToSimulationModeTest() {
        System.out.println("Testing Simulation Mode");
        SimulationControllerOuterClass.StartSimulationMode_Responses startSimMode;
        startSimMode = simulationBlockingStub.startSimulationMode(
            SimulationControllerOuterClass.StartSimulationMode_Parameters.newBuilder().build());
    }


    @Test
    void SwitchToRealModeTest() {

            SimulationControllerOuterClass.StartRealMode_Responses startRealMode;
            startRealMode = simulationBlockingStub.startRealMode(
                    SimulationControllerOuterClass.StartRealMode_Parameters.newBuilder().build());
        try {
            simulationBlockingStub.startRealMode(SimulationControllerOuterClass.StartRealMode_Parameters.newBuilder().build());
        } catch (Exception e) {

            return;
        }
    }

/*
    @Test
    void testEmptyParameter() {
        final GreetingProviderOuterClass.SayHello_Parameters.Builder parameter =
                GreetingProviderOuterClass.SayHello_Parameters.newBuilder();

        // Has to throw a validation error
        try {
            blockingStub.sayHello(parameter.build());
        } catch (StatusRuntimeException e) {
            final SiLAFramework.SiLAError siLAError = retrieveSiLAError(e);
            assertNotNull(siLAError);
            assertTrue(siLAError.hasValidationError());
            return;
        }
        fail("HelloSiLAServer did not throw a validation error with empty parameter");
    }*/
}