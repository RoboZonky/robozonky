package net.petrovicky.zonkybot.api.remote;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

class InstantDeserializer extends JsonDeserializer<Instant> {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        try {
            return DATE_FORMAT.parse(jsonParser.getText().replace("+02:00", "+0200")).toInstant();
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

}
