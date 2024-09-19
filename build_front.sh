#!/bin/bash

path=$(pwd)

cd "../../WebstormProjects/MTWireGuardEasy-frontend/" || { cd "$path" && exit 1; }
npm install
npm run build

if [ -d "./dist/" ]; then
    cp -r ./dist/* "$path/src/main/resources/static/"
else
    echo "Building error"
    cd "$path" || exit 1
    exit 1
fi