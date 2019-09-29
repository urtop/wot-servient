package city.sane.wot.content;

import city.sane.wot.thing.schema.ObjectSchema;
import city.sane.wot.thing.schema.StringSchema;
import org.junit.Test;

import java.io.*;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ContentManagerTest {
    @Test
    public void contentToValueWithUnsupportedFormat() throws ContentCodecException, IOException {
        // serialize 42
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(42);
        oos.flush();
        byte[] body = bos.toByteArray();

        Content content = new Content("none/none", body);
        Object value = ContentManager.contentToValue(content, new StringSchema());

        assertEquals(42, value);
    }

    @Test
    public void contentToValueObject() throws ContentCodecException {
        Content content = new Content("application/json", "{\"foo\":\"bar\"}".getBytes());
        Map value = ContentManager.contentToValue(content, new ObjectSchema());

        assertEquals("bar", value.get("foo"));
    }

    @Test
    public void contentToValueWithMediaTypeParameters() throws ContentCodecException {
        Content content = new Content("text/plain; charset=utf-8", "Hello World".getBytes());
        String value = ContentManager.contentToValue(content, new StringSchema());

        assertEquals("Hello World", value);
    }

    @Test
    public void valueToContentWithUnsupportedFormat() throws ContentCodecException, IOException, ClassNotFoundException {
        Content content = ContentManager.valueToContent(42, "none/none");

        // deserialize byte array
        ByteArrayInputStream bis = new ByteArrayInputStream(content.getBody());
        ObjectInputStream ois = new ObjectInputStream(bis);

        assertEquals(42, ois.readObject());
    }
}