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
- [🛠 Usage Guide](#-usage-guide)
  - [Bluetooth (BLE / RFCOMM) Mode](#bluetooth-ble--rfcomm-mode)
  - [Wi-Fi (TCP Socket) Mode](#wi-fi-tcp-socket-mode)
  - [Testing with Python Mock Server](#testing-with-python-mock-server)
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
app/src/main/java/com/abcomm/
├── protocol/
│   ├── MicrohilProtocolConstants.kt       # Protocol frame delimiters and command keywords
│   ├── CommandFormatter.kt                # Contract for outbound command formatting
│   ├── MicrohilCommandFormatter.kt        # Implementation of CommandFormatter
│   ├── FrameParser.kt                     # Stream framing contract (<...>)
│   ├── MicrohilFrameParser.kt             # Chunked stream frame extractor
│   ├── DeviceResponse.kt                  # Sealed interface for typed device responses
│   ├── ResponseParser.kt                  # Response parsing contract
│   ├── ResponseMatcher.kt                 # Extensible response matcher interface (OCP)
│   ├── MicrohilResponseParser.kt          # ResponseParser delegating to matcher list
│   └── matchers/                          # Individual pattern matchers for each response
│       ├── ChannelStateMatcher.kt
│       ├── AllChannelsStateMatcher.kt
│       ├── AllChannelsSnapshotMatcher.kt
│       ├── MaskAppliedMatcher.kt
│       ├── BoardIdMatcher.kt
│       ├── FirmwareVersionMatcher.kt
│       └── SystemResettingMatcher.kt
│
├── communication/
│   ├── ConnectionMode.kt                  # Enum: BLE, WIFI
│   ├── ConnectionTarget.kt                # Sealed interface: Bluetooth(device), Wifi(host, port)
│   ├── ConnectionStatus.kt                # Sealed interface: Disconnected, Connecting, Connected, Error
│   ├── ConnectionController.kt            # Lifecycle management contract
│   ├── CommandSender.kt                   # Command dispatch contract
│   ├── ConnectionObservable.kt            # Status and response observer contract
│   ├── CommunicationProvider.kt           # Composite provider interface
│   ├── CommunicationProviderRegistry.kt   # Dynamic provider resolution contract
│   ├── DefaultCommunicationProviderRegistry.kt
│   ├── BluetoothService.kt                # RFCOMM Bluetooth provider (Coroutines / Dispatchers.IO)
│   └── WifiService.kt                     # TCP Socket Wi-Fi provider (Coroutines / Dispatchers.IO)
│
├── settings/
│   ├── AppSettings.kt                     # Configuration data model and port boundaries
│   ├── AppSettingsRepository.kt           # Storage abstraction contract
│   └── SharedPreferencesSettingsRepository.kt
│
├── ui/
│   ├── MainUiState.kt                     # Immutable UI State data model
│   ├── MainViewModel.kt                   # State machine orchestrating UI & hardware
│   ├── MainViewModelFactory.kt            # Dependency injection factory
│   ├── BluetoothPermissionChecker.kt      # Permission checker interface
│   ├── BluetoothPermissionHelper.kt       # Android SDK version-aware permission helper
│   ├── BluetoothDeviceProvider.kt         # Bluetooth adapter abstraction interface
│   └── BluetoothDeviceManager.kt          # Paired device manager
│
└── MainActivity.kt                        # Primary Android Activity view layer
```

---

## 🛠 Usage Guide

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

### Testing with Python Mock Server

You can test Wi-Fi communication without physical hardware using the included mock server:

```bash
# Run the mock server from the repository root
python3 wifi/wifi_server.py
```

The mock server binds to `0.0.0.0:5000` and emulates real microHIL firmware behavior (board ID, version, channel toggling, and snapshots).

---

## 👥 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

---

## 📄 License

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Copyright (C) 2026 by [electux.github.io/abcomm](https://github.com/electux)

**ABComm** is open-source software licensed under the **MIT License**.
