eval "$(docker-machine env default)"

echo 'removing existing containers'
docker stop server
docker stop client
docker rm server
docker rm client
docker network rm mynetwork

# docker rmi dsuser/basic:v1
# docker rmi dsuser/datavolume:v1

echo 'build images'
docker build -t dsuser/basic:v1 -f Dockerfile-basic .

docker network create -d bridge mynetwork

echo 'running server'
docker run -d  --net=mynetwork --name server dsuser/basic:v1 sh -c 'cd /opt/; javac -cp .:my-rmi.jar *.java; java -cp .:my-rmi.jar PingPongServerFactory 9898'

echo 'running client'
docker run -it --net=mynetwork --name client dsuser/basic:v1 sh -c 'cd /opt/; javac -cp .:my-rmi.jar *.java; java -cp .:my-rmi.jar PingPongClient 9898'

docker logs client > output.log
echo 'done'
