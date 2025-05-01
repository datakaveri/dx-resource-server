#!/bin/bash

# To be executed from project root
docker build -t iudx/rs-depl:latest -f docker/depl.dockerfile .
docker build -t iudx/rs-dev:latest -f docker/dev.dockerfile .
