
# script which runs on master node to execute the bigram code
cd /root/code
echo 'Compiling BigramCount code'
export HADOOP_CLASSPATH=${JAVA_HOME}/lib/tools.jar
hadoop com.sun.tools.javac.Main BigramCount.java
echo 'Creating jar'
jar cf bigram.jar BigramCount*.class

cd /root

echo 'create input directory on HDFS'
hadoop fs -mkdir -p input

echo 'put input files to HDFS'
hdfs dfs -put ./code/file1.txt input
hdfs dfs -put ./code/file2.txt input

echo 'run bigram count'
hadoop jar code/bigram.jar BigramCount input output

# print the input files
#echo -e "\ninput file1.txt:"
#hdfs dfs -cat input/file1.txt

#echo -e "\ninput file2.txt:"
#hdfs dfs -cat input/file2.txt

# print the output of wordcount
echo -e '\nbigram count output:'
hdfs dfs -cat output/part-r-00000 > out-hadoop


javac code/AnalyseBigram.java 
echo 'analysing bigram output'
java -cp "./code" AnalyseBigram out-hadoop > out-bigram

cat out-bigram