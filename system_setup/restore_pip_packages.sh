#!/bin/bash
if [ -f pip_packages.txt ]; then
    pip install -r pip_packages.txt
fi
#if [ -f pip3_packages.txt ]; then
#    pip3 install -r pip3_packages.txt
#fi
