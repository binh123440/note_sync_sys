#!/bin/bash

echo "Compiling Note Sync System..."

# Create build directory
mkdir -p build

# Compile common models
echo "Compiling models..."
javac -d build src/common/models/*.java
if [ $? -ne 0 ]; then
    echo "Failed to compile models"
    exit 1
fi

# Compile common network
echo "Compiling network classes..."
javac -d build -cp build src/common/network/*.java
if [ $? -ne 0 ]; then
    echo "Failed to compile network classes"
    exit 1
fi

# Compile common utils
echo "Compiling utilities..."
javac -d build -cp build src/common/utils/*.java
if [ $? -ne 0 ]; then
    echo "Failed to compile utilities"
    exit 1
fi

# Compile server
echo "Compiling server..."
javac -d build -cp build src/server/*.java
if [ $? -ne 0 ]; then
    echo "Failed to compile server"
    exit 1
fi

# Compile client
echo "Compiling client..."
javac -d build -cp build src/client/*.java
if [ $? -ne 0 ]; then
    echo "Failed to compile client"
    exit 1
fi

echo ""
echo "Compilation successful!"
echo ""
echo "To run:"
echo "  Server: ./run-server.sh"
echo "  Client: ./run-client.sh [client_name]"