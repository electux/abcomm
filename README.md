# ABComm - Advanced Bluetooth & Wi-Fi Relay Control

<img align="right" src="https://raw.githubusercontent.com/electux/abcomm/main/docs/logo.svg" width="25%">

**ABComm** is a futuristic Android client application designed for high-performance, real-time control of 8-channel relay boards powered by **Raspberry Pi Pico** running **microHIL** firmware.

Developed with **[Kotlin](https://kotlinlang.org/)**, **Android Jetpack**, and **Kotlin Coroutines**.

The application features a Cyberpunk-styled interface supporting dual-mode connectivity (**Bluetooth Low Energy / RFCOMM** and **Wi-Fi TCP Socket**), automated hardware telemetry synchronization, and robust error handling.

[![Build Status](https://github.com/electux/abcomm/actions/workflows/android.yml/badge.svg)](https://github.com/electux/abcomm/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub issues open](https://img.shields.io/github/issues/electux/abcomm.svg)](https://github.com/electux/abcomm/issues)
[![GitHub contributors](https://img.shields.io/github/contributors/electux/abcomm.svg)](https://github.com/electux/abcomm/graphs/contributors)

---

## Table of Contents

- [✨ Features](#-features)
- [📡 microHIL Communication Protocol](#-microhil-communication-protocol)
- [🚀 Installation & Building](#-installation--building)
  - [Build from Source](#build-from-source)
  - [Run Unit Tests](#run-unit-tests)
- [📦 Dependencies & Permissions](#-dependencies--permissions)
- [📁 Project Architecture](#-project-architecture)
- [🛠 Usage & Hardware Emulation Guide](#-usage--hardware-emulation-guide)
  - [Bluetooth (BLE / RFCOMM) Mode](#bluetooth-ble--rfcomm-mode)
  - [Wi-Fi (TCP Socket) Mode](#wi-fi-tcp-socket-mode)
  - [Testing Bluetooth with Linux Laptop](#testing-bluetooth-with-linux-laptop)
  - [Testing Wi-Fi with Python Mock Server](#testing-wi-fi-with-python-mock-server)
- [👥 Contributing](#-contributing)
- [📄 License](#-license)

---

## ✨ Features

* **Dual Connectivity**: Seamlessly switch between **Bluetooth (BLE / RFCOMM)** and **Wi-Fi (TCP Socket)**.
* **Settings Persistence**: User-configured Wi-Fi IP address and Port are securely persisted via SharedPreferences.
* **8-Channel Independent Control**: Instant toggle for individual channels (1 to 8) with dynamic active/inactive states.
* **Master Controls**: Quick-action **ALL ON** and **ALL OFF** buttons for simultaneous relay switching.
* **Automated Telemetry Sync**: Automatically queries and displays hardware Board ID (`mh:333:2023:0`), Firmware Version (`microHIL v1.0.0`), and live relay states on connect.
* **Manual Sync & Device Reboot**: Dedicated **SYNC** button for manual state refreshing and **RESET** button with a confirmation dialog.
* **Robust Disconnection Handling**: Immediate socket cleanup and automatic UI state reset to `OFF` when the device disconnects or powers down.
* **Clean Architecture**: 100% Type-Safe (`ConnectionStatus`, `DeviceResponse`), Dependency Inversion (DIP), Open/Closed (OCP) response matchers, and Coroutine-based background I/O (`Dispatchers.IO`).
* **Hardware-Free Testing Scripts**: Ready-to-use scripts in `scripts/` to emulate both Bluetooth SPP and Wi-Fi TCP servers from a laptop without physical Pico hardware.

---

## 📡 microHIL Communication Protocol

All messages exchanged between the ABComm Android client and the Raspberry Pi Pico server are framed with `<` at the start and `>` at the end:

| Action | Command Frame | Response Format |
| :--- | :--- | :--- |
| **Toggle Channel ON** | `<mh#ch#1#on#end>` | `<mh#sys#channel 1 on#end>` |
| **Toggle Channel OFF** | `<mh#ch#1#off#end>` | `<mh#sys#channel 1 off#end>` |
| **All Channels ON** | `<mh#all#on#end>` | `<mh#sys#all channels on#end>` |
| **All Channels OFF** | `<mh#all#off#end>` | `<mh#sys#all channels off#end>` |
| **Query All Channels** | `<mh#all#stat#end>` | `<mh#sys#channels: 1:ON 2:OFF 3:OFF 4:OFF 5:OFF 6:OFF 7:OFF 8:OFF #end>` |
| **Query Board ID** | `<mh#sys#id#end>` | `<mh#sys#mh:333:2023:0#end>` |
| **Query Firmware Version** | `<mh#sys#version#end>` | `<mh#sys#microHIL v1.0.0#end>` |
| **System Reboot** | `<mh#sys#reset#end>` | `<mh#sys#system resetting...#end>` |
| **Set Channel Mask** | `<mh#all#mask#10101010#end>` | `<mh#sys#channels mask applied: 10101010#end>` |

---

## 🚀 Installation & Building

Developed and tested on **Android 14 (API 34)** and backwards compatible down to **Android 7.0 (API 24)**.

### Build from Source

```bash
# 1. Clone repository
git clone https://github.com/electux/abcomm.git
cd abcomm

# 2. Build Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit Tests

Execute the complete test suite (Protocol formatters, Stream parsers, OCP Matchers, ViewModel state, and Repositories):

```bash
./gradlew testDebugUnitTest
```

---

## 📦 Dependencies & Permissions

The app declares and dynamically requests appropriate permissions:

* **Bluetooth**: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` (Android 12+ / API 31+), `ACCESS_FINE_LOCATION` (Android 11 and earlier).
* **Wi-Fi / Network**: `INTERNET`, `ACCESS_NETWORK_STATE`.

---

## 📁 Project Architecture

The codebase strictly follows the **Single Type per File** and **SOLID** principles, organized into domain packages:

```bash
abcomm/
├── app/
│   └── src/
│       ├── main/java/com/abcomm/
│       │   ├── protocol/
│       │   │   ├── MicrohilProtocolConstants.kt       # Delimiters and command keywords
│       │   │   ├── CommandFormatter.kt                # Outbound formatting contract
│       │   │   ├── MicrohilCommandFormatter.kt        # Implementation of CommandFormatter
│       │   │   ├── FrameParser.kt                     # Stream framing contract (<...>)
│       │   │   ├── MicrohilFrameParser.kt             # Chunked stream extractor
│       │   │   ├── DeviceResponse.kt                  # Typed device response model
│       │   │   ├── ResponseParser.kt                  # Response parser contract
│       │   │   ├── ResponseMatcher.kt                 # Response matcher interface (OCP)
│       │   │   ├── MicrohilResponseParser.kt          # Parser delegating to matchers
│       │   │   └── matchers/                          # Individual pattern matchers
│       │   │       ├── ChannelStateMatcher.kt
│       │   │       ├── AllChannelsStateMatcher.kt
│       │   │       ├── AllChannelsSnapshotMatcher.kt
│       │   │       ├── MaskAppliedMatcher.kt
│       │   │       ├── BoardIdMatcher.kt
│       │   │       ├── FirmwareVersionMatcher.kt
│       │   │       └── SystemResettingMatcher.kt
│       │   │
│       │   ├── communication/
│       │   │   ├── ConnectionMode.kt                  # Enum: BLE, WIFI
│       │   │   ├── ConnectionTarget.kt                # Sealed: Bluetooth, Wifi
│       │   │   ├── ConnectionStatus.kt                # Sealed: Disconnected, Connecting, Connected, Error
│       │   │   ├── ConnectionController.kt            # Lifecycle contract
│       │   │   ├── CommandSender.kt                   # Dispatch contract
│       │   │   ├── ConnectionObservable.kt            # Observer contract
│       │   │   ├── CommunicationProvider.kt           # Composite provider contract
│       │   │   ├── CommunicationProviderRegistry.kt   # Provider registry contract
│       │   │   ├── DefaultCommunicationProviderRegistry.kt
│       │   │   ├── BluetoothService.kt                # RFCOMM provider (Coroutines / Dispatchers.IO)
│       │   │   └── WifiService.kt                     # TCP Socket provider (Coroutines / Dispatchers.IO)
│       │   │
│       │   ├── settings/
│       │   │   ├── AppSettings.kt                     # Config data model & port boundaries
│       │   │   ├── AppSettingsRepository.kt           # Storage contract
│       │   │   └── SharedPreferencesSettingsRepository.kt
│       │   │
│       │   ├── ui/
│       │   │   ├── MainUiState.kt                     # Immutable UI State model
│       │   │   ├── MainViewModel.kt                   # ViewModel state machine
│       │   │   ├── MainViewModelFactory.kt            # Dependency injection factory
│       │   │   ├── BluetoothPermissionChecker.kt      # Permission checker interface
│       │   │   ├── BluetoothPermissionHelper.kt       # SDK version-aware helper
│       │   │   ├── BluetoothDeviceProvider.kt         # Bluetooth adapter interface
│       │   │   └── BluetoothDeviceManager.kt          # Paired device manager
│       │   │
│       │   └── MainActivity.kt                        # Primary Android Activity view layer
│       │
│       └── test/java/com/abcomm/                      # Complete MockK Unit Test Suite
│
├── docs/                                              # Sphinx / ReadTheDocs Documentation
│   └── source/
│       ├── conf.py
│       └── index.rst
│
└── scripts/                                           # Hardware Emulation & Testing Scripts
    ├── ble/
    │   ├── ble_listen.sh                              # Linux RFCOMM SPP sniffer/server script
    │   └── README.md                                  # Bluetooth test setup guide
    └── wifi/
        ├── wifi_server.py                             # Python TCP microHIL mock server
        └── README.md                                  # Wi-Fi test setup guide
```

---

## 🛠 Usage & Hardware Emulation Guide

### Bluetooth (BLE / RFCOMM) Mode

1. Select the **BLE** mode toggle at the top of the screen.
2. Tap **CONNECT**.
3. Grant Bluetooth permissions if prompted.
4. Select your Raspberry Pi Pico device from the paired devices list.
5. Once connected, device info and current relay states will load automatically.

### Wi-Fi (TCP Socket) Mode

1. Select the **WIFI** mode toggle at the top.
2. Enter the **IP Address** and **Port** of your microHIL device (e.g. `192.168.1.100`, Port `5000`). Values are automatically saved for subsequent app launches.
3. Tap **CONNECT**.
4. Telemetry and relay buttons will update automatically upon connection.

### Testing Bluetooth with Linux Laptop

To test Bluetooth connectivity without physical Raspberry Pi Pico hardware, configure a Linux (Ubuntu) laptop as an RFCOMM server:

```bash
# In Terminal A on Ubuntu:
chmod +x scripts/ble/ble_listen.sh
./scripts/ble/ble_listen.sh

# In Terminal B (to monitor commands sent from phone):
sudo cat /dev/rfcomm10
```

Refer to [`scripts/ble/README.md`](scripts/ble/README.md) for full Bluetooth pairing and compatibility instructions.

### Testing Wi-Fi with Python Mock Server

To test Wi-Fi communication without physical hardware, run the Python mock server:

```bash
# Run the mock server from the repository root
python3 scripts/wifi/wifi_server.py --port 5000
```

1. The script will print the laptop's local IP address (e.g. `192.168.1.150`).
2. In the ABComm app, switch to **WIFI** mode, enter the printed IP and port `5000`, and tap **CONNECT**.
3. All button presses will update real-time terminal logs and reflect microHIL firmware behavior.

Refer to [`scripts/wifi/README.md`](scripts/wifi/README.md) for further details.

---

## 👥 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

---

## 📄 License

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Copyright (C) 2026 by [electux.github.io/abcomm](https://github.com/electux)

**ABComm** is open-source software licensed under the **MIT License**.
