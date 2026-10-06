package dev.bykowski.ssisw.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageCodec {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot encode ACL message payload", exception);
        }
    }

    public <T> T decode(String content, Class<T> payloadType) {
        try {
            return objectMapper.readValue(content, payloadType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot decode ACL message payload as " + payloadType.getSimpleName(), exception);
        }
    }
}
