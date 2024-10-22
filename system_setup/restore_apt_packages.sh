#!/bin/bash
sudo apt update
sudo apt install -y $(cat apt_manual_packages_cleaned.txt)
