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

# get rid of old stuff
docker kill speed

docker rmi -f $(docker images | grep "^<none>" | awk "{print $3}")
docker rm $(docker ps -q -f status=exited)
#docker system prune

docker build -f DockerfileAPI -t maayanlab/speedrichr .
docker push maayanlab/speedrichr

docker run --env-file .env -p 8666:8080 --name speed maayanlab/speedrichr
