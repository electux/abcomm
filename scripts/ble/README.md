# Ubuntu 24.04 Bluetooth Serial Port Setup Guide

This guide explains how to configure Ubuntu 24.04 to act as a Bluetooth Serial Port (SPP) server to receive data from the ABCommander app.

## 1. Install Required Tools
```bash
sudo apt update
sudo apt install bluez bluez-tools
```

## 2. Enable Compatibility Mode (MANDATORY)
1. Edit the service: `sudo nano /lib/systemd/system/bluetooth.service`
2. Change `ExecStart` line to: `ExecStart=/usr/libexec/bluetooth/bluetoothd --compat`
3. Reload: `sudo systemctl daemon-reload && sudo systemctl restart bluetooth`
4. Permissions: `sudo chmod 777 /var/run/sdp` (repeat after restart).

## 3. Disable Conflicting Services
Ubuntu's ModemManager often "hijacks" serial ports, causing "Address already in use" errors.
```bash
sudo systemctl stop ModemManager
```

## 4. The "Sniffer" Script (ble_listen.sh)
Create a script with these contents for a clean connection:
```bash
#!/bin/bash
echo "--- Resetting Bluetooth Stack ---"
sudo pkill -9 rfcomm
sudo rfcomm release all
sudo hciconfig hci0 down
sudo hciconfig hci0 up
sudo sdptool add --channel=4 SP
echo "Waiting for connection on CHANNEL 4..."
sudo rfcomm listen 10 4  # Uses /dev/rfcomm10
```

---

## 5. Step-by-Step Operational Flow

Follow these steps exactly to ensure a successful connection without "Address already in use" errors:

### Phase 1: Server Preparation (Ubuntu)
1. **Stop ModemManager:** `sudo systemctl stop ModemManager`
2. **Run the Script:** Execute `./ble_listen.sh` in Terminal A.
   - It should say: `Waiting for connection on channel 4`.
   - **Do NOT** start the `cat` command yet.

### Phase 2: App Connection (Android)
3. **Open App:** Launch ABCommander on your phone.
4. **Click Connect:** Select your Ubuntu laptop from the list.
5. **Verify Handshake:**
   - Look at Terminal A (Ubuntu). It should change to:
     `Connection from [MAC] to /dev/rfcomm10`
   - The App status should change to: `Status: Connected to...` and the button should say **Disconnect**.

### Phase 3: Data Sniffing
6. **Read Data:** Open **Terminal B** (New Tab) and run:
   ```bash
   sudo cat /dev/rfcomm10
   ```
7. **Action:** Press buttons in the app. Commands like `1_ON` will appear in Terminal B.

### Phase 4: Clean Disconnect
8. **Stop Sniffer:** In Terminal B, press `Ctrl+C` to stop `cat`.
9. **Disconnect App:** Click **Disconnect** on the phone.
10. **Reset Server:** In Terminal A, press `Ctrl+C` and run `sudo rfcomm release all` before the next test.

---

## Troubleshooting
- **Address already in use:** Run `sudo rfcomm release all` and ensure no `cat` or `tail` processes are running on `/dev/rfcomm*`.
- **Send failed:** Usually means the physical connection is up but the TTY device (`/dev/rfcomm10`) failed to initialize. Restart the script.
