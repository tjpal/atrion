#!/bin/bash

# Creates a master key in the .atrion folder
openssl rand -base64 32 > ~/.atrion/secrets_master_key
chmod 600 ~/.atrion/secrets_master_key