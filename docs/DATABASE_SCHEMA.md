# Database Schema

The `ecmdroid.db` (found in `app/src/main/assets/ecmdroid.db.gz`) is a SQLite database that contains definitions for different ECM versions, including memory offsets and data translation parameters.

## Entity Relationship Diagram

```mermaid
erDiagram
    EEPROM ||--o{ RTOFFSETS : "defines RT data for"
    EEPROM ||--o{ EEOFFSETS : "defines EEPROM for"
    NAMES ||--o{ RTOFFSETS : "labels"
    NAMES ||--o{ EEOFFSETS : "labels"
    NAMES ||--o{ BITS : "defines bits for"

    EEPROM {
        string name PK "ECM ID (e.g. BUEGB)"
        string category "Link to offsets"
        string type "DDFI, DDFI-2, or DDFI-3"
    }

    NAMES {
        string varname PK "Internal variable key"
        string origname "Alternative name"
        string name "User-friendly label"
        int uniqueid
        int secret "Boolean flag"
        string remark
        string description
        string units
    }

    RTOFFSETS {
        string varname FK
        string category FK
        string type "SCALAR, BITS, etc."
        int offset
        int size
        float scale
        float translate
        float low
        float high
    }

    EEOFFSETS {
        string varname FK
        string category FK
        int offset
        int size
        int elemsize
        int cols
        int rows
    }

    BITS {
        string varname FK
        int byte "Byte index"
        string bitname1 "Name of Bit 0"
        string bit1 "Description of Bit 0"
        string dtc1 "Trouble code for Bit 0"
        string bitname8 "Name of Bit 7"
    }
```

## Tables Description

### 1. `eeprom`
This is the entry point for ECM identification. When the app connects to a motorcycle, it reads the ECM ID and looks it up in this table to determine which `category` of offsets to use.

### 2. `names`
A central repository for variable metadata. It decouples the internal variable keys (`varname`) from human-readable labels and descriptions.

### 3. `rtoffsets` (Real-Time Offsets)
Defines how to parse the live data stream sent by the ECM during runtime.
- **`offset`**: Position in the raw byte array.
- **`scale` / `translate`**: Parameters for the linear transformation `(raw * scale) + translate`.
- **`low` / `high`**: Recommended display ranges for gauges.

### 4. `eeoffsets` (EEPROM Offsets)
Defines the memory map for reading and writing to the ECM's permanent storage.
- **`cols` / `rows`**: Used for 2D tables (like Fuel Maps or Ignition Maps).
- **`elemsize`**: The size of a single element within an array or table.

### 5. `bits`
Used when a variable is of type `BITS` or `BITFIELD`. It maps individual bits within a byte to specific statuses or Diagnostic Trouble Codes (DTCs).
