# ECM Communication Protocol Guide

This document describes how EcmDroid communicates with the Buell ECM using Protocol Data Units (PDUs) and how raw data is translated into human-readable information.

## 1. PDU Structure

All communication between the Android device and the ECM is encapsulated in PDUs. Each PDU has a strict framing format defined in [`PDU.java`](../app/src/main/java/org/ecmdroid/PDU.java).

| Byte Index | Field | Description |
| :--- | :--- | :--- |
| 0 | **SOH** | Start of Header (0x01) |
| 1 | **Sender ID** | ID of the sending device (0x00 for Droid, 0x42/0x55 for ECM) |
| 2 | **Recipient ID** | ID of the target device |
| 3 | **Length** | Length of the payload + 1 |
| 4 | **EOH** | End of Header (0xFF) |
| 5 | **SOT** | Start of Text (0x02) |
| 6 ... 6+N-1 | **Payload** | The actual command or data |
| 6+N | **EOT** | End of Text (0x03) |
| 6+N+1 | **Checksum** | Simple additive checksum of all bytes excluding SOH and Checksum itself |

## 2. Common Commands

| Command | Hex | Description |
| :--- | :--- | :--- |
| `CMD_RTDATA` | 0x43 | Request real-time runtime data (e.g., RPM, TPS). |
| `CMD_GET` | 0x52 | Read data from the ECM EEPROM. |
| `CMD_SET` | 0x57 | Write data to the ECM EEPROM. |
| `CMD_VERSION` | 0x56 | Request the ECM version and identity string. |

## 3. Data Translation (Raw to Human-Readable)

The transformation of raw bytes into meaningful values (like "1050 RPM" or "14.2 Volts") is handled by the [`Variable`](../app/src/main/java/org/ecmdroid/Variable.java) class.

### Step 1: Byte Extraction
The app extracts bytes from the payload at a predefined `offset`. Most numerical values are **little-endian**.

### Step 2: Linear Transformation
For most sensors, a linear equation is applied to the raw value:
**`HumanValue = (RawValue * Scale) + Translate`**

- **Scale**: Multiplier (e.g., 0.1 to convert decivolts to volts).
- **Translate**: Offset (e.g., used for temperature conversions).

### Step 3: Formatting
The final value is formatted using standard Java `DecimalFormat` patterns defined for each variable in the internal database.

## 4. Bitfields and Flags
Some data bytes represent a collection of status flags rather than a single number. These are managed by [`BitSet`](../app/src/main/java/org/ecmdroid/BitSet.java) and [`Bit`](../app/src/main/java/org/ecmdroid/Bit.java). 
Each bit index within a byte can represent a specific state (e.g., "Fuel Pump On/Off" or "Check Engine Light Active").

## 5. EEPROM Access
EEPROM data is organized into **Pages**. To read or write settings:
1. The app sends a `CMD_GET` request with the Page number and Offset.
2. The ECM responds with a PDU containing the raw bytes of that EEPROM section.
3. The app uses the `ecmdroid.db` definitions to map those bytes to specific engine parameters.
