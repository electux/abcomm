# ABComm - Advanced Bluetooth Relay Control

<img align="right" src="https://raw.githubusercontent.com/electux/abcomm/main/docs/logo.svg" width="25%">

**ABComm** is a futuristic Android application designed for high-performance control of relay devices via Bluetooth Low Energy (BLE). 

Developed with **[Kotlin](https://kotlinlang.org/)** and **Jetpack Compose**.

This application provides a "Cyberpunk" styled interface to manage up to 8 independent channels (relays) with real-time status monitoring and secure communication protocols.

[![Build Status](https://github.com/electux/abcomm/actions/workflows/android.yml/badge.svg)](https://github.com/electux/abcomm/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub issues open](https://img.shields.io/github/issues/electux/abcomm.svg)](https://github.com/electux/abcomm/issues)
[![GitHub contributors](https://img.shields.io/github/contributors/electux/abcomm.svg)](https://github.com/electux/abcomm/graphs/contributors)

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [🚀 Installation](#-installation)
    - [Build from Source](#build-from-source)
    - [Download APK](#download-apk)
- [📦 Dependencies](#-dependencies)
- [📁 Project Structure](#-project-structure)
- [✨ Features](#-features)
- [🛠 Usage](#-usage)
- [👥 Contributing](#-contributing)
- [📄 Copyright and licence](#-copyright-and-licence)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

### 🚀 Installation

Developed and tested on **Android 14 (API 34)** and newer.

##### Build from Source

You can build **ABComm** using Android Studio or Gradle.

```bash
# Clone the repository
git clone https://github.com/electux/abcomm.git
cd abcomm

# Build Debug APK
./gradlew assembleDebug
```

##### Download APK

Navigate to the **[Releases](https://github.com/electux/abcomm/releases/)** page to download the latest signed APK or App Bundle.

### 📦 Dependencies

**ABComm** requires the following permissions and hardware:

* **Bluetooth Low Energy (BLE)** capable device.
* **Android 7.0 (API 24)** or higher.
* Permissions: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`.

### 📁 Project Structure

**ABComm** follows the MVVM (Model-View-ViewModel) architecture.

Project structure

<details>
<summary><b>Click to expand app structure</b></summary>

```bash
    app/
     ├── src/
     │   ├── main/
     │   │   ├── java/com/abcomm/
     │   │   │   ├── MainActivity.kt      # Main UI Entry Point
     │   │   │   ├── MainViewModel.kt     # UI State & Logic
     │   │   │   └── BluetoothService.kt  # BLE Communication Provider
     │   │   └── res/
     │   │       ├── drawable/            # Cyber-style Icons
     │   │       └── values/              # Futuristic Color Palette
     │   └── test/                        # Unit Tests (MockK)
     └── build.gradle.kts                 # Build Configuration
```
</details>

#### ✨ Features

* **Futuristic UI**: High-contrast "Cyberpunk" design with custom vector graphics.
* **8-Channel Control**: Independent toggle for each relay with individual status indicators.
* **Real-time Monitoring**: Instant feedback on connection status and relay states.
* **Secure BLE Link**: Efficient communication protocol using unique UUIDs.
* **Master Control**: Single-tap "All Channels ON" and "Force Shutdown" functions.
* **Unit Tested**: Robust logic verified with 100% core coverage.

### 🛠 Usage

1. **Enable Bluetooth**: Ensure Bluetooth is active on your smartphone.
2. **Scan for Device**: Launch ABComm and tap **CONNECT** to scan for available relay boards.
3. **Control**: Use the channel grid to toggle specific relays or use the Master Control for group actions.

### 👥 Contributing

[Contributing to abcomm](CONTRIBUTING.md)

### 📄 Copyright and licence

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Copyright (C) 2026 by [electux.github.io/abcomm](https://github.com/electux)

**ABComm** is open-source software licensed under the **MIT License**.

Feel free to fork, modify, and improve the project!
