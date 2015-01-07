package foodprint.data.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;

/**
 * Created by bernard on 6/1/15.
 */
public class JsonObjectMapper<T> {

    private ObjectReader objectReader;
    private ObjectWriter objectWriter;

    public JsonObjectMapper(Class<T> clazz) {
        JavaType objectType = TypeFactory.defaultInstance().constructType(clazz);
        ObjectMapper objectMapper = new ObjectMapper();
        this.objectReader = objectMapper.reader().withType(objectType);
        this.objectWriter = objectMapper.writer().withType(objectType);
    }

    @SuppressWarnings("unchecked")
    public T parse(String json) {
        try {
            return (T)objectReader.readValue(json);
        } catch (IOException e) {
            return null;
        }
    }

    public String serialize(T object) {
        try {
            return objectWriter.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
