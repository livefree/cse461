#!/usr/bin/bash

# Update APT repositories
sudo apt-get -y update;
sudo apt install -y software-properties-common;
sudo add-apt-repository -y ppa:deadsnakes/ppa;

# Install Python and python3.8 Virtual environment
sudo apt install -y python3-pip;
sudo apt install -y build-essential zlib1g-dev libncurses5-dev libgdbm-dev libnss3-dev libssl-dev libreadline-dev libffi-dev wget;
sudo apt install -y python3.8-venv;
sudo apt-get update -y ;
python3 -m venv "venv";
source venv/bin/activate;


# Install mininet (have to do both ways)
git clone https://github.com/mininet/mininet;
cd mininet;
git checkout -b mininet-2.3.0 2.3.0 
sudo PYTHON=python3 ~/mininet/util/install.sh -n
sudo apt-get install -y mininet;

# Installing matplotlib with Python3
python3 -m pip install matplotlib
sudo apt-get install -y python3-matplotlib;
sudo apt-get install -y openvswitch-testcontroller
sudo cp /usr/bin/ovs-testcontroller /usr/bin/ovs-controller

# Removing the process id for the test controller
sudo lsof -i TCP:6653 | tail -n +2 | awk '{print $2}' | xargs sudo kill -9