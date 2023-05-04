#!/bin/bash

echo "Start the Docker container" 
docker-compose up -d
echo "Waiting for ACA-Py startup to get DID and VerKey..."
sleep 20
docker logs acapy_container 2>&1 | grep "get_my_did_with_meta: <<< res:" 
echo "Docker container is stopping"
docker-compose down -v
echo "Docker container stopped and has been removed"
