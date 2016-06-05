
echo 'deleting existing containers if any'
docker rm -f master
docker rm -f slave1
docker rm -f slave2
docker rm -f slave3
docker rm -f slave4

echo 'deleting existing images if any'
docker rmi kiwenlau/hadoop-master:0.1.0
docker rmi kiwenlau/hadoop-slave:0.1.0
docker rmi kiwenlau/hadoop-base:0.1.0
docker rmi kiwenlau/serf-dnsmasq:0.1.0

echo 'Pulling all docker images'
docker pull kiwenlau/hadoop-master:0.1.0
docker pull kiwenlau/hadoop-slave:0.1.0
docker pull kiwenlau/hadoop-base:0.1.0
docker pull kiwenlau/serf-dnsmasq:0.1.0

echo 'Starting master and slaves'
cd hadoop-cluster-docker
chmod +x  start-container.sh
chmod +x  start-hadoop-job.sh
./start-container.sh

echo 'Setting number of slaves = 4'
./resize-cluster.sh 5
./start-container.sh 5

echo 'Starting hadoop jobs on master'
./start-hadoop-job.sh