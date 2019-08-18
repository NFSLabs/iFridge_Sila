package sila_java.servers.ifridge;

import java.util.HashMap;
import java.util.Map;

class IFridgeUtils {
    static final int OPEN_STEPS = 100; // [-]
    static final int CLOSE_STEPS = 100; // [-]
    static final int MAX_SPEED = 20; // [1/s]
    static final String SIMULATION_MODE_PREFIX= "SIMULATION_MODE: "; //
    static final String COMMAND_SEPARATOR= " "; //
    enum DeviceType {
         iFridge_1 , iInkubator_1;
    }
    enum SimulationStatus {
        SIMULATION , REAL, UNDEFINED;
    }
    enum IFridgeCommands {
        OPEN , CLOSE, STOP_IMMEDIATELY, GET_STATUS, M;
    }

    static class IFridgeError {
        final String message;
        final String action;
        final String code;

        IFridgeError(String code, String message, String action) {
            this.code = code;
            this.message = message;
            this.action = action;
        }
    }

    static final Map<String, IFridgeError> ERRORS = new HashMap<>();
    static {
        ERRORS.put("ER1",  new IFridgeError("ER1", "1 - something happpened.", "E")); // EnterInError
        ERRORS.put("ER2",  new IFridgeError("ER2", "2 - Somethig else happened.", "E")); // EnterInError
        ERRORS.put("ER",   new IFridgeError("", " - unknown error number. Take a look to the manual or contact service.", "E")); // EnterInError
    }
}
