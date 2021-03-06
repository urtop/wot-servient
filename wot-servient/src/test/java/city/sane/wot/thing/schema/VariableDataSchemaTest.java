package city.sane.wot.thing.schema;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VariableDataSchemaTest {
    @Test
    public void testEquals() {
        VariableDataSchema schema1 = new VariableDataSchema.Builder().setType("string").build();
        VariableDataSchema schema2 = new VariableDataSchema.Builder().setType("string").build();

        assertEquals(schema1, schema2);
    }

    @Test
    public void builder() {
        VariableDataSchema schema = new VariableDataSchema.Builder()
                .setType("string")
                .build();

        assertEquals("string", schema.getType());
    }
}