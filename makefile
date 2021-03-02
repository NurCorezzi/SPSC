QUEUE=3
MODE=cc

all:
	./runTestSuite.sh

mvn:
	mvn clean install

cc:
	taskset -c 0,3 ${JAVA_HOME}/bin/java -cp "./target/classes" uk.co.real_logic.queues.QueuePerfTest ${QUEUE} cc
sc:
	taskset -c 1   ${JAVA_HOME}/bin/java -cp "./target/classes" uk.co.real_logic.queues.QueuePerfTest ${QUEUE} sc
cs:
	taskset -c 0,3 ${JAVA_HOME}/bin/java -cp "./target/classes" uk.co.real_logic.queues.QueuePerfTest ${QUEUE} cs


#COMPILATION ETC

c:
	javac -d "./target/classes" -cp "./src/main/java" -Xlint:unchecked src/main/java/uk/co/real_logic/queues/QueuePerfTest.java

r:
	sudo perf stat -d -d -v ${JAVA_HOME}/bin/java -cp ./target/examples-1.0-SNAPSHOT-jar-with-dependencies.jar uk.co.real_logic.queues.QueuePerfTestAffinity ${QUEUE} ${MODE}
#	${JAVA_HOME}/bin/java -cp "./target/classes" uk.co.real_logic.queues.QueuePerfTestAffinity ${QUEUE} ${MODE}

perf:
	sudo perf stat -d -d -v ${JAVA_HOME}/bin/java -cp "./target/classes" uk.co.real_logic.queues.QueuePerfTest ${QUEUE} ${MODE}

clean:
	rm -f ./data/cs/*
	rm -f ./data/sc/*
	rm -f ./data/cc/*