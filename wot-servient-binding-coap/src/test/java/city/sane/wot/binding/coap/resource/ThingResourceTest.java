package city.sane.wot.binding.coap.resource;

import city.sane.wot.thing.ExposedThing;
import city.sane.wot.thing.action.ThingAction;
import city.sane.wot.thing.event.ThingEvent;
import city.sane.wot.thing.property.ThingProperty;
import city.sane.wot.thing.schema.NumberSchema;
import city.sane.wot.thing.schema.ObjectSchema;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ThingResourceTest {
    private CoapServer server;

    @Before
    public void setup() {
        server = new CoapServer(5683);
        server.add(new ThingResource(getCounterThing()));
        server.start();
    }
    
    @After
    public void teardown() {
        server.stop();

        // TODO: Wait some time after the server has shut down. Apparently the CoAP server reports too early that it was terminated, even though the port is
        //  still in use. This sometimes led to errors during the tests because other CoAP servers were not able to be started because the port was already
        //  in use. This error only occurred in the GitLab CI (in Docker). Instead of waiting, the error should be reported to the maintainer of the CoAP
        //  server and fixed. Because the isolation of the error is so complex, this workaround was chosen.
        try {
            Thread.sleep(1 * 1000L);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void getThing() {
        CoapClient client = new CoapClient("coap://localhost:5683/counter");
        CoapResponse response = client.get();

        assertEquals(CoAP.ResponseCode.CONTENT, response.getCode());
        assertEquals(MediaTypeRegistry.APPLICATION_JSON, response.getOptions().getContentFormat());
    }

    @Test
    public void getThingWithCustomContentType() {
        CoapClient client = new CoapClient("coap://localhost:5683/counter");
        Request request = new Request(CoAP.Code.GET);
        request.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_CBOR);
        CoapResponse response = client.advanced(request);

        assertEquals(CoAP.ResponseCode.CONTENT, response.getCode());
        assertEquals(MediaTypeRegistry.APPLICATION_CBOR, response.getOptions().getContentFormat());
    }

    private ExposedThing getCounterThing() {
        ExposedThing thing = new ExposedThing(null)
                .setId("counter")
                .setTitle("counter")
                .setDescription("counter example Thing");

        ThingProperty counterProperty = new ThingProperty.Builder()
                .setType("integer")
                .setDescription("current counter value")
                .setObservable(true)
                .setUriVariables(Map.of(
                        "step", Map.of(
                                "type", "integer",
                                "minimum", 1,
                                "maximum", 250
                        )
                ))
                .build();

        ThingProperty lastChangeProperty = new ThingProperty.Builder()
                .setType("string")
                .setDescription("last change of counter value")
                .setObservable(true)
                .setReadOnly(true)
                .build();

        thing.addProperty("count", counterProperty, 42);
        thing.addProperty("lastChange", lastChangeProperty, new Date().toString());

        thing.addAction("increment", new ThingAction.Builder()
                .setInput(new ObjectSchema())
                .setOutput(new NumberSchema())
                .build(), (input, options) -> {
            return thing.getProperty("count").read().thenApply(value -> {
                int step;
                if ((input instanceof Map) && ((Map) input).containsKey("step")) {
                    step = (int) ((Map) input).get("step");
                }
                else {
                    step = 1;
                }
                int newValue = ((Integer) value) + step;
                thing.getProperty("count").write(newValue);
                thing.getProperty("lastChange").write(new Date().toString());
                thing.getEvent("change").emit();
                return newValue;
            });
        });

        thing.addAction("decrement", new ThingAction(), (input, options) -> {
            return thing.getProperty("count").read().thenApply(value -> {
                int newValue = ((Integer) value) - 1;
                thing.getProperty("count").write(newValue);
                thing.getProperty("lastChange").write(new Date().toString());
                thing.getEvent("change").emit();
                return newValue;
            });
        });

        thing.addAction("reset", new ThingAction(), (input, options) -> {
            return thing.getProperty("count").write(0).thenApply(value -> {
                thing.getProperty("lastChange").write(new Date().toString());
                thing.getEvent("change").emit();
                return 0;
            });
        });

        thing.addEvent("change", new ThingEvent());

        return thing;
    }
}