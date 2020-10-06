# JSON Event Sourcing Fanout Service

This microservice sends messages from Kafka topics to the [fanout.io](https://fanout.io) service. The topics should use the [serialisation of JSON Event Sourcing](https://www.javadoc.io/static/net.pincette/pincette-jes-util/1.1.3/net/pincette/jes/util/JsonSerde.html). The usernames from the messages are used for the channel names. They are extracted from the ```_jwt/sub``` and ```_subscriptions/sub``` fields.

## Configuration

The configuration is managed by the 
[Lightbend Config package](https://github.com/lightbend/config). By default it will try to load ```conf/application.conf```. An alternative configuration may be loaded by adding ```-Dconfig.resource=myconfig.conf```, where the file is also supposed to be in the ```conf``` directory. The following entries are available.

|Entry|Description|
|---|---|
|environment|The name of the environment, e.g. "tst", "prd".|
|kafka|All Kafka settings come below this entry. So for example, the setting ```bootstrap.servers``` would go to the entry ```kafka.bootstrap.servers```.|
|kafka.num.stream.threads|The number of worker threads per instance.|
|kafka.replication.factor|When using Confluent Cloud this should be 3.|
|logLevel|The log level as defined in [java.util.logging.Level](https://docs.oracle.com/javase/8/docs/api/java/util/logging/Level.html).|
|logTopic|The Kafka topic where the errors will be logged in the [Elastic Common Schema](https://www.elastic.co/guide/en/ecs/current/index.html).|
|realmIn|The realm ID for the fanout.io service.|
|realmKey|The realm key for the fanout.io service.|
|topics|The list of Kafka topics from which messages should be propagated.|
|topologyTopic|When that Kafka topic is set topology life cycle events will be sent to it.|

## Building and Running

You can build the tool with ```mvn clean package```. This will produce a self-contained JAR-file in the ```target``` directory with the form ```pincette-jes-fanout-<version>-jar-with-dependencies.jar```. You can launch this JAR with ```java -jar```, without further options.

The total number of threads across all the instances should not exceed the number of partitions for the Kafka topics. Additional threads will be idle.

You can run the JVM with the option ```-mx128m```.

## Docker

Docker images can be found at [https://hub.docker.com/repository/docker/jsoneventsourcing/pincette-jes-fanout](https://hub.docker.com/repository/docker/jsoneventsourcing/pincette-jes-fanout). You should add a configuration layer with a Docker file that looks like this:

```
FROM registry.hub.docker.com/jsoneventsourcing/pincette-jes-fanout:<version>
COPY conf/tst.conf /conf/application.conf
```

So wherever your configuration file comes from, it should always end up at ```/conf/application.conf```.
