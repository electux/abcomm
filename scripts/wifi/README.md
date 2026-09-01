# WiFi TCP/IP Mock Server Guide for ABComm

This directory contains the Python TCP server script (`wifi_server.py`) that emulates the Raspberry Pi Pico microHIL firmware protocol over WiFi (TCP socket).

## Prerequisites

- Python 3 installed on your computer.
- Computer and Android device connected to the same WiFi network (or local hotspot).

## How to Run

1. Open terminal in the project root.
2. Run the server:
   ```bash
   python3 scripts/wifi/wifi_server.py --port 5000
   ```
3. The script will display its running IP addresses:
   ```text
   ============================================================
     microHIL WiFi TCP Mock Server Running
   ============================================================
   Listening on: 0.0.0.0:5000
   Use one of these IP addresses in the ABComm app:
     -> 192.168.1.150 (Port: 5000)
   ============================================================
   Waiting for incoming connections...
   ```

## Connecting from ABComm App

1. Launch **ABComm** on your Android device.
2. Select **WiFi** mode in the connection selector.
3. Enter the IP address shown by `wifi_server.py` (e.g. `192.168.1.150`) and Port (`5000`).
4. Tap **CONNECT**.
5. Test toggling channels 1-8 or Master ON/OFF buttons. You will see command logs and responses printed in the terminal.
