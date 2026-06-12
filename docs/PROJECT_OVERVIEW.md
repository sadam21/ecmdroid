# EcmDroid Project Overview

EcmDroid is a diagnostic and configuration tool for Buell motorcycles with DDFI-2 or DDFI-3 ECMs.

## Key Features
- **Diagnostics**: Read and clear engine trouble codes.
- **Real-time Data**: Monitor engine parameters (RPM, temperature, TPS, etc.) in real-time.
- **EEPROM Tuning**: Read, modify, and write back EEPROM data.
- **Logging**: Record engine runtime data for later analysis.
- **Active Tests**: Trigger engine components (injectors, coils, etc.) for testing.

## Technical Stack
- **Language**: Java 8
- **Android**: Min SDK 26, Target SDK 33.
- **Build System**: Gradle with Kotlin DSL.
- **UI**: Android Fragments within a single-activity architecture ([`MainActivity`](../app/src/main/java/org/ecmdroid/activities/MainActivity.java)).

## Communication
- **Protocol**: Custom PDU-based serial protocol. See [**Protocol Guide**](PROTOCOL_GUIDE.md) for details.
- **Bluetooth Serial**: Legacy Bluetooth adapters.
- **Bluetooth Low Energy (BLE)**: Modern BLE-to-serial adapters.
- **USB Serial**: USB OTG connection for wired adapters.
