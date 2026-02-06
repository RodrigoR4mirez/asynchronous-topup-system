package pe.com.topup.consumer.config;

import io.smallrye.reactive.messaging.kafka.DeserializationFailureHandler;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.Headers;

@ApplicationScoped
@Identifier("failure-logger")
public class DeserializationLogger implements DeserializationFailureHandler<Object> {

    @Override
    public Object handleDeserializationFailure(String topic, boolean isKey, String deserializer, byte[] data,
            Exception exception, Headers headers) {
        // Log the error and return null to ignore the message
        System.err.println("SerializationException: " + exception.getMessage() + " in topic " + topic);
        return null;
    }
}
