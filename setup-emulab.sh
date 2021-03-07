#!/bin/bash
sudo apt-get update
sudo apt-get -y install openjdk-8-jdk
sudo apt-get -y install maven
export JAVA_HOME /usr
echo "############## JAVA ################\n\n"
java -version
echo "############## MAVEN ################\n\n"
mvn --version
echo "############## JAVA_HOME ################\n\n"
where java
echo $JAVA_HOME