# create a new Bash session in the master container
# docker exec -it master bash
# This script is run on local machine to start bigram hadoop job.

eval "$(docker-machine env default)"

echo "moving bigram code to master node"
docker cp ../code master:/root
docker cp run-bigram.sh master:/root
docker cp run-master.sh master:/root
docker exec master /bin/bash -c 'cd /root; chmod +x run-bigram.sh; chmod +x run-master.sh; ./run-master.sh'

# docker exec -it master /bin/bash 
# docker exec master /bin/bash -c 'cd /root; ./start-hadoop.sh; ./bigram-master.sh'