# This script is copied to the master node and calls scripts to 1) start hadoop and 2) run
# bigram code
source ~/.bashrc
echo 'Starting hadoop'
./start-hadoop.sh
echo 'Waiting to finish setup'
sleep 5
echo 'Starting bigram code'
./run-bigram.sh