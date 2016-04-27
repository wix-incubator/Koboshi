package com.wix.hoopoe.koboshi.marshaller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;
import com.wix.hoopoe.koboshi.cache.TimestampedData;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import scala.reflect.ClassTag$;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JacksonMarshallerTest {

    private final Marshaller<LocalDateTime> marshaller = aMarshaller();
    private final TimestampedData<LocalDateTime> timestampedData = new TimestampedData<>(someData(), new Instant());

    @Test
    public void shouldReadAndWriteATimestampedInstance() throws IOException {
        byte[] marshalledData = marshaller.marshall(timestampedData);
        assertThat(marshaller.unmarshall(marshalledData), is(timestampedData));
    }

    @Test
    public void shouldReadAndWriteATimestampedInstance2() throws IOException {
        byte[] marshalledData = marshaller.marshall(timestampedData);
        byte[] marshalledFutureData = addFutureKeyAndValue(marshalledData);
        assertThat(marshaller.unmarshall(marshalledFutureData), is(timestampedData));
    }

    private byte[] addFutureKeyAndValue(final byte[] marshalledData) throws IOException {
        final ObjectMapper objectMapper = anObjectMapperWithJodaAndScala();
        JsonNode node = objectMapper.readTree(marshalledData);
        ((ObjectNode)node).put("futureField","futureValue");
        return objectMapper.writeValueAsBytes(node);
    }

    protected static ObjectMapper anObjectMapperWithJodaAndScala() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(new DefaultScalaModule());
        return objectMapper;
    }

    protected LocalDateTime someData() {
        return new LocalDateTime();
    }

    protected Marshaller<LocalDateTime> aMarshaller() {
        return new JacksonMarshaller<>(anObjectMapperWithJodaAndScala(),
                ClassTag$.MODULE$.<LocalDateTime>apply(LocalDateTime.class));
    }

}
