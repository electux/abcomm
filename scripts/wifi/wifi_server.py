#!/usr/bin/env python3
"""
microHIL TCP/IP Mock Server for ABComm Android Application.

Implements the Raspberry Pi Pico microHIL firmware protocol over TCP sockets.
Parses commands sent by ABComm and responds with system status messages.
"""

import sys
import socket
import threading
import argparse

# Default channel states for 8 channels (False = OFF, True = ON)
channels = [False] * 8
MICROHIL_VERSION = "microHIL v1.0.0"
MICROHIL_BOARD_ID = "mh:333:2023:0"

# ANSI Color codes for clean terminal debugging
COLOR_CYAN = "\033[96m"
COLOR_GREEN = "\033[92m"
COLOR_RED = "\033[91m"
COLOR_YELLOW = "\033[93m"
COLOR_RESET = "\033[0m"


def format_channel_status():
    status_parts = []
    for i, state in enumerate(channels, start=1):
        status_parts.append(f"{i}:{'ON' if state else 'OFF'}")
    return "channels: " + " ".join(status_parts)


def process_command(cmd_str):
    """
    Parses a single microHIL command and returns the response string.
    """
    cmd = cmd_str.strip()
    if cmd.startswith("<"):
        cmd = cmd[1:]
    if cmd.endswith(">"):
        cmd = cmd[:-1]
    cmd = cmd.strip()
    if not cmd:
        return None

    print(f"  {COLOR_YELLOW}[RECV]{COLOR_RESET} <{cmd}>")

    response = None

    # Channel control: mh#ch#<1..8>#<on|off>#end
    if cmd.startswith("mh#ch#") and len(cmd) >= 12:
        parts = cmd.split("#")
        # Format: ["mh", "ch", "<channel>", "<action>", "end"]
        if len(parts) >= 5 and parts[1] == "ch" and parts[4] == "end":
            ch_str = parts[2]
            action = parts[3]
            if ch_str.isdigit():
                ch_num = int(ch_str)
                if 1 <= ch_num <= 8:
                    if action == "on":
                        channels[ch_num - 1] = True
                        response = f"<mh#sys#channel {ch_num} on#end>"
                    elif action == "off":
                        channels[ch_num - 1] = False
                        response = f"<mh#sys#channel {ch_num} off#end>"
                    elif action == "stat":
                        state_str = "ON" if channels[ch_num - 1] else "OFF"
                        response = f"<mh#sys#channel {ch_num} status: {state_str}#end>"

    # Master control: mh#all#on#end or mh#all#off#end or mh#all#stat#end
    if cmd == "mh#all#on#end":
        for i in range(8):
            channels[i] = True
        response = "<mh#sys#all channels on#end>"
    elif cmd == "mh#all#off#end":
        for i in range(8):
            channels[i] = False
        response = "<mh#sys#all channels off#end>"
    elif cmd == "mh#all#stat#end":
        response = f"<mh#sys#{format_channel_status()}#end>"

    # System commands
    elif cmd == "mh#sys#id#end":
        response = f"<mh#sys#{MICROHIL_BOARD_ID}#end>"
    elif cmd == "mh#sys#version#end":
        response = f"<mh#sys#{MICROHIL_VERSION}#end>"
    elif cmd == "mh#sys#reset#end":
        for i in range(8):
            channels[i] = False
        response = "<mh#sys#system resetting...#end>"

    # Mask control: mh#all#mask#10101010#end
    elif cmd.startswith("mh#all#mask#") and cmd.endswith("#end"):
        mask_str = cmd[len("mh#all#mask#"):-len("#end")]
        if len(mask_str) == 8 and all(c in "01" for c in mask_str):
            for i in range(8):
                channels[i] = (mask_str[i] == '1')
            response = f"<mh#sys#channels mask applied: {mask_str}#end>"

    if response:
        print(f"  {COLOR_GREEN}[RESP]{COLOR_RESET} {response}")
    else:
        print(f"  {COLOR_RED}[WARN]{COLOR_RESET} Unknown or unhandled command: {cmd}")
        response = "<mh#sys#unknown command#end>"

    return response


def handle_client(client_socket, client_address):
    print(f"\n{COLOR_CYAN}[+] Client connected from {client_address[0]}:{client_address[1]}{COLOR_RESET}")
    buffer = ""

    try:
        while True:
            data = client_socket.recv(1024)
            if not data:
                break

            buffer += data.decode("utf-8", errors="ignore")

            # Extract frames between '<' and '>'
            while "<" in buffer and ">" in buffer:
                start_idx = buffer.find("<")
                end_idx = buffer.find(">", start_idx)

                if end_idx != -1:
                    raw_cmd = buffer[start_idx + 1 : end_idx]
                    buffer = buffer[end_idx + 1 :]

                    resp = process_command(raw_cmd)
                    if resp:
                        client_socket.sendall(resp.encode("utf-8"))
                else:
                    # Found '<' but not yet '>', wait for next chunk
                    buffer = buffer[start_idx:]
                    break

            # Fallback for un-framed legacy commands terminated by #end
            if "<" not in buffer and "#end" in buffer:
                idx = buffer.find("#end") + 4
                raw_cmd = buffer[:idx]
                buffer = buffer[idx:]
                resp = process_command(raw_cmd)
                if resp:
                    client_socket.sendall(resp.encode("utf-8"))

    except Exception as e:
        print(f"{COLOR_RED}[!] Error handling client {client_address}: {e}{COLOR_RESET}")
    finally:
        client_socket.close()
        print(f"{COLOR_CYAN}[-] Client {client_address[0]}:{client_address[1]} disconnected{COLOR_RESET}\n")


def get_local_ips():
    ips = []
    try:
        # Get primary outward IP
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        primary_ip = s.getsockname()[0]
        s.close()
        ips.append(primary_ip)
    except Exception:
        pass

    try:
        hostname = socket.gethostname()
        for ip in socket.gethostbynameex(hostname)[2]:
            if ip not in ips and not ip.startswith("127."):
                ips.append(ip)
    except Exception:
        pass

    return ips


def main():
    parser = argparse.ArgumentParser(description="microHIL TCP Mock Server for ABComm")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="Host IP to bind (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=5000, help="Port to listen on (default: 5000)")
    args = parser.parse_args()

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        server_socket.bind((args.host, args.port))
        server_socket.listen(5)
    except Exception as e:
        print(f"{COLOR_RED}[!] Failed to bind to {args.host}:{args.port} - {e}{COLOR_RESET}")
        sys.exit(1)

    print("=" * 60)
    print(f"{COLOR_CYAN}  microHIL WiFi TCP Mock Server Running{COLOR_RESET}")
    print("=" * 60)
    print(f"Listening on: {args.host}:{args.port}")
    local_ips = get_local_ips()
    if local_ips:
        print("Use one of these IP addresses in the ABComm app:")
        for ip in local_ips:
            print(f"  -> {COLOR_GREEN}{ip}{COLOR_RESET} (Port: {args.port})")
    print("=" * 60)
    print("Waiting for incoming connections...\n")

    try:
        while True:
            client_sock, client_addr = server_socket.accept()
            client_thread = threading.Thread(
                target=handle_client, args=(client_sock, client_addr), daemon=True
            )
            client_thread.start()
    except KeyboardInterrupt:
        print(f"\n{COLOR_YELLOW}[*] Shutting down TCP server.{COLOR_RESET}")
    finally:
        server_socket.close()


if __name__ == "__main__":
    main()
