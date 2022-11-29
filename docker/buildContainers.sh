#!/bin/sh
#####################################
#   AUTHOR: ALEXANDER LACHMANN      #
#   DATE: 7/9/2019                 #
#   Mount Sinai School of Medicine  #
#####################################

now=$(date +"%T")
echo "Current time : $now"
echo "------------------------------------"

cd ..
./gradlew build

cd docker

docker build -f DockerfileAPI -t maayanlab/speedrichr:1.51 .
docker push maayanlab/speedrichr:1.51
