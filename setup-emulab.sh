#rodar comom "source setup-emulab.sh"
#perf:
#sudo apt-get -y install linux-tools-4.4.0-193-generic
sudo apt-get update
sudo apt-get -y install openjdk-8-jdk
sudo apt-get -y install maven
echo "\n\n############## JAVA ################"
java -version
echo "\n\n############## MAVEN ################"
mvn --version
echo "\n\n############## JAVA_HOME ################"
echo "java in" 
where java
setenv JAVA_HOME /usr
echo "JAVA_HOME="$JAVA_HOME
