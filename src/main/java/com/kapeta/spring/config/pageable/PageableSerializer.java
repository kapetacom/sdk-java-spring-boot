package com.kapeta.spring.config.pageable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.data.domain.Pageable;

import java.io.IOException;

public class PageableSerializer extends StdSerializer<Pageable> {

    public PageableSerializer() {
        super(Pageable.class);
    }

    @Override
    public void serialize(Pageable pageable, JsonGenerator jg, SerializerProvider serializerProvider) throws IOException {
        jg.writeStartObject();
        jg.writeNumberField("page", pageable.getPageNumber());
        jg.writeNumberField("size", pageable.getPageSize());

        if (!pageable.getSort().isEmpty()) {
            jg.writeArrayFieldStart("sort");
            for (var order : pageable.getSort()) {
                jg.writeStartObject();
                jg.writeStringField("direction", order.getDirection().toString());
                jg.writeStringField("property", order.getProperty());
                jg.writeEndObject();
            }
            jg.writeEndArray();
        }
        jg.writeEndObject();
    }
}
