#!/bin/bash

echo "Starting Note Sync Client..."

# Check if build directory exists
if [ ! -d "build" ]; then
    echo "Build directory not found. Please run compile.sh first."
    exit 1
fi

# Copy config file to build directory
cp config.properties build/ 2>/dev/null

# Get client name from command line argument or ask user
CLIENT_NAME="$1"
if [ -z "$CLIENT_NAME" ]; then
    read -p "Enter client name: " CLIENT_NAME
fi

if [ -z "$CLIENT_NAME" ]; then
    CLIENT_NAME="DefaultClient"
fi

echo "Starting client: $CLIENT_NAME"

# Change to build directory and run client
cd build
java client.NoteSyncClient "$CLIENT_NAME"