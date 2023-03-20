#This is the chat Application using Rabbitmq.

To compile :
javac -cp amqp-client-5.14.2.jar chatApp.java

To run :
java -cp .:amqp-client-5.14.2.jar:slf4j-api-1.7.36.jar:slf4j-simple-1.7.36.jar chatApp <<name>>

Note: name refers to the client name used in the chat and should be unique assigned.
