package net.petrovicky.zonkybot.remote;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

class RatingDeserializer extends JsonDeserializer<Rating> {

    @Override
    public Rating deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        jsonParser.nextValue();
        Rating r = Rating.valueOf(jsonParser.getText());
        jsonParser.nextValue();
        return r;
    }

}
