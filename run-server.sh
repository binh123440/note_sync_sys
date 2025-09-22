#!/bin/bash

echo "Starting Note Sync Server..."

# Check if build directory exists
if [ ! -d "build" ]; then
    echo "Build directory not found. Please run compile.sh first."
    exit 1
fi

# Copy config file to build directory
cp config.properties build/ 2>/dev/null

# Change to build directory and run server
cd build
java server.NoteSyncServer