#!/bin/sh
#####################################
#   AUTHOR: ALEXANDER LACHMANN      #
#   DATE: 7/9/2019                 #
#   Mount Sinai School of Medicine  #
#####################################

now=$(date +"%T")
echo "Current time : $now"
echo "------------------------------------"
# Ensure that you have s3 privileges
aws s3 cp --recursive s3://maayanlab-public/enrichr/ ../src/main/webapp/WEB-INF/data/genelibs
cd ..
./gradlew build

cd docker

docker build -f DockerfileAPI -t maayanlab/speedrichr:1.88 .
docker push maayanlab/speedrichr:1.88
