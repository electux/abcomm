#!/bin/bash

echo "--- Debugging Bluetooth Connection ---"

# 1. Čišćenje
sudo pkill -9 rfcomm 2>/dev/null
sudo rfcomm release all 2>/dev/null
sudo systemctl stop ModemManager 2>/dev/null

# 2. Reset adaptera (opciono ali pomaže)
sudo hciconfig hci0 down
sudo hciconfig hci0 up

# 3. Dodaj Serial Port na Kanalu 4 (još dalje od uobičajenih portova)
sudo sdptool add --channel=4 SP

echo "Listening on /dev/rfcomm10 (Channel 4)..."
# Koristimo rfcomm 10 i kanal 4
sudo rfcomm listen 10 4
