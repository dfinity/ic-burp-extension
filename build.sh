#!/bin/sh

# exit on error
set -e

SCRIPT_PATH=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
LIB_NAME="rust_lib"
LIB_NAME_OSX="lib$LIB_NAME.dylib"
LIB_NAME_LIN="$LIB_NAME.so"

# delete eventually existing old lib files
if [ -f "$SCRIPT_PATH/java-extension/src/main/resources/$LIB_NAME_OSX" ]; then
	rm "$SCRIPT_PATH/java-extension/src/main/resources/$LIB_NAME_OSX"
fi
if [ -f "$SCRIPT_PATH/java-extension/src/main/resources/$LIB_NAME_LIN" ]; then
	rm "$SCRIPT_PATH/java-extension/src/main/resources/$LIB_NAME_LIN"
fi

# build new lib files and copy them into resources
cd "$SCRIPT_PATH/rust-lib"
cargo build --release
if [ -f "target/release/$LIB_NAME_OSX" ]; then
	cp "target/release/$LIB_NAME_OSX" "$SCRIPT_PATH/java-extension/src/main/resources/"
fi
if [ -f "target/release/$LIB_NAME_LIN" ]; then
	cp "target/release/$LIB_NAME_LIN" "$SCRIPT_PATH/java-extension/src/main/resources/"
fi
cd ../java-extension

# build
./gradlew clean jar

