package net.petrovicky.zonkybot.remote;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

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
