package net.petrovicky.zonkybot.api.remote;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

class RatingDeserializer extends JsonDeserializer<Rating> {

    @Override
    public Rating deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        jsonParser.nextValue();
        return Rating.valueOf(jsonParser.getText());
    }

}
