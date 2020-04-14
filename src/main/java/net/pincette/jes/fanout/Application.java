package net.pincette.jes.fanout;

import static java.lang.System.exit;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.parse;
import static java.util.logging.Logger.getLogger;
import static net.pincette.jes.elastic.Logging.log;
import static net.pincette.jes.util.Configuration.loadDefault;
import static net.pincette.jes.util.Fanout.connect;
import static net.pincette.jes.util.Kafka.createReliableProducer;
import static net.pincette.jes.util.Kafka.fromConfig;
import static net.pincette.jes.util.Streams.start;
import static net.pincette.util.Util.tryToDoWithRethrow;
import static net.pincette.util.Util.tryToGetSilent;

import com.typesafe.config.Config;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.pincette.jes.util.JsonSerializer;
import net.pincette.jes.util.Streams;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;

/**
 * A microservice that forwards all messages to fanout.io channels. The usernames are used as the
 * channel names. They are extracted from the fields <code>_jwt/sub</code> and <code>
 * _subscriptions/sub</code>.
 *
 * @author Werner Donn\u00e9
 */
public class Application {
  private static final String ENVIRONMENT = "environment";
  private static final String EXCLUDE = "exclude";
  private static final String KAFKA = "kafka";
  private static final String LOG_LEVEL = "logLevel";
  private static final String LOG_TOPIC = "logTopic";
  private static final String REALM_ID = "realmId";
  private static final String REALM_KEY = "realmKey";
  private static final String TOPICS = "topics";
  private static final String VERSION = "1.1";

  private static Set<String> exclude(final Config config) {
    return new HashSet<>(config.getStringList(EXCLUDE));
  }

  public static void main(final String[] args) {
    final StreamsBuilder builder = new StreamsBuilder();
    final Config config = loadDefault();
    final String environment = tryToGetSilent(() -> config.getString(ENVIRONMENT)).orElse("dev");
    final Level logLevel = parse(tryToGetSilent(() -> config.getString(LOG_LEVEL)).orElse("INFO"));
    final String logTopic =
        tryToGetSilent(() -> config.getString(LOG_TOPIC)).orElse("log-" + environment);
    final Logger logger = getLogger("pincette-jes-fanout");
    final String realmId = config.getString(REALM_ID);
    final String realmKey = config.getString(REALM_KEY);

    logger.setLevel(logLevel);

    config
        .getStringList(TOPICS)
        .forEach(
            topic -> connect(builder.stream(topic), exclude(config), realmId, realmKey, logger));

    tryToDoWithRethrow(
        () ->
            createReliableProducer(
                fromConfig(config, KAFKA), new StringSerializer(), new JsonSerializer()),
        producer -> {
          final Topology topology = builder.build();

          log(logger, logLevel, VERSION, environment, producer, logTopic);
          logger.log(INFO, "Topology:\n\n {0}", topology.describe());

          if (!start(topology, Streams.fromConfig(config, KAFKA))) {
            exit(1);
          }
        });
  }
}
