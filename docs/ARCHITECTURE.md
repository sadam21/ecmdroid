# Architecture of EcmDroid

The application follows a service-based architecture to handle background hardware communication independently of the UI.

## Core Components

### 1. Hardware Communication Layer
- [**`ECM.java`**](../app/src/main/java/org/ecmdroid/ECM.java): The primary interface for all ECM operations. It manages connection state and executes high-level commands.
- [**`PDU.java`**](../app/src/main/java/org/ecmdroid/PDU.java): Represents a Protocol Data Unit, the basic block of the ECM communication protocol.
- [**`DataChannelAdapter.java`**](../app/src/main/java/org/ecmdroid/DataChannelAdapter.java): Abstraction layer for transport methods (Bluetooth, BLE, USB).

### 2. Data Management
- [**`EcmDroidService.java`**](../app/src/main/java/org/ecmdroid/EcmDroidService.java): A background service that manages the hardware connection. It contains a `ReaderThread` for continuous data polling and logging.
- [**`EEPROM.java`**](../app/src/main/java/org/ecmdroid/EEPROM.java): Handles the engine's EEPROM data, including map parsing and checksum calculation.
- [**`DBHelper.java`**](../app/src/main/java/org/ecmdroid/DBHelper.java): Manages the internal SQLite database (`ecmdroid.db`) which contains definitions for various ECM versions.

### 3. User Interface
- [**`MainActivity.java`**](../app/src/main/java/org/ecmdroid/activities/MainActivity.java): The main container using a `DrawerLayout` for navigation.
- **`fragments/`**: Each feature is implemented as a Fragment.
  - [`DataChannelFragment`](../app/src/main/java/org/ecmdroid/fragments/DataChannelFragment.java): Visualizes real-time data.
  - [`EEPROMFragment`](../app/src/main/java/org/ecmdroid/fragments/EEPROMFragment.java): Provides a grid editor for engine maps.
  - [`TroubleCodeFragment`](../app/src/main/java/org/ecmdroid/fragments/TroubleCodeFragment.java): Displays diagnostic error codes.

## Data Flow
1. **Request**: A Fragment calls a method on the [`ECM`](../app/src/main/java/org/ecmdroid/ECM.java) singleton.
2. **Execution**: [`ECM`](../app/src/main/java/org/ecmdroid/ECM.java) translates the request into [`PDU`](../app/src/main/java/org/ecmdroid/PDU.java)s sent via the [`DataChannelAdapter`](../app/src/main/java/org/ecmdroid/DataChannelAdapter.java).
3. **Response**: The hardware responds; data is parsed by [`ECM`](../app/src/main/java/org/ecmdroid/ECM.java) and returned to the UI or broadcasted by [`EcmDroidService`](../app/src/main/java/org/ecmdroid/EcmDroidService.java) for logging.
