package net.petrovicky.zonkybot.api.remote;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

class RatingDeserializer extends JsonDeserializer<Rating> {

    @Override
    public Rating deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        jsonParser.nextValue();
        return Rating.valueOf(jsonParser.getText());
    }

}
