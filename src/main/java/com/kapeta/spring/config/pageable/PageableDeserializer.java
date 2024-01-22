package com.kapeta.spring.config.pageable;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;

public class PageableDeserializer extends StdDeserializer<Pageable> {

    private final ObjectMapper objectMapper;

    public PageableDeserializer(ObjectMapper objectMapper) {
        super(Pageable.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public Pageable deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ObjectNode source = jp.readValueAs(ObjectNode.class);

        if (source.has("size") || source.has("page")) {
            var page = source.get("page").asInt(0);
            var size = source.get("size").asInt(30);
            var sort = source.get("sort");
            var pageRequest = PageRequest.of(page, size);
            if (sort == null) {
                return pageRequest;
            }

            var sortList = sort.elements();
            while (sortList.hasNext()) {
                var sortObject = sortList.next();
                var direction = sortObject.get("direction").asText("ASC");
                var property = sortObject.get("property").asText();
                if (direction == null || property == null) {
                    continue;
                }

                pageRequest = pageRequest.withSort(Sort.Direction.fromString(direction), property);
            }

            return pageRequest;
        }

        return objectMapper.convertValue(source, Pageable.class);
    }
}
