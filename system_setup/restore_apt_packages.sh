#!/bin/bash
sudo apt-get update
sudo apt-get install -y $(cat apt_manual_packages.txt)
