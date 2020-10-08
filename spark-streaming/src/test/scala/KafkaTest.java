import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaTest {

    public static void main(String[] args) {

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        JsonObject json = new JsonObject();
        json.addProperty("type","click");
        json.addProperty("ip","127.0.0.1");
        json.addProperty("event_time","1500028835");
        json.addProperty("url","https://blog.griddynamics.com/in-stream-processing-service-blueprint");


        //create Producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        //create a producer record
        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<String, String>("bots", json.toString());

        //send Data
        for (int i = 0; i < 20; i++)
            producer.send(producerRecord);

        producer.flush();
        producer.close();

    }
}
