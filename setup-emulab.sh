#!/bin/bash
sudo apt-get update
sudo apt-get -y install openjdk-8-jdk
sudo apt-get -y install maven
setenv JAVA_HOME /usr
echo $JAVA_HOME
java -version
where java
mvn --version