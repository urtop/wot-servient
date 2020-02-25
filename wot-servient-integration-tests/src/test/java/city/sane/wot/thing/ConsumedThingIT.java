package city.sane.wot.thing;

import city.sane.Pair;
import city.sane.wot.Servient;
import city.sane.wot.ServientException;
import city.sane.wot.binding.ProtocolClientFactory;
import city.sane.wot.binding.ProtocolClientNotImplementedException;
import city.sane.wot.binding.ProtocolServer;
import city.sane.wot.binding.akka.AkkaProtocolClientFactory;
import city.sane.wot.binding.akka.AkkaProtocolServer;
import city.sane.wot.binding.coap.CoapProtocolClientFactory;
import city.sane.wot.binding.coap.CoapProtocolServer;
import city.sane.wot.binding.http.HttpProtocolClientFactory;
import city.sane.wot.binding.http.HttpProtocolServer;
import city.sane.wot.binding.mqtt.MqttProtocolClientFactory;
import city.sane.wot.binding.mqtt.MqttProtocolServer;
import city.sane.wot.binding.websocket.WebsocketProtocolClientFactory;
import city.sane.wot.binding.websocket.WebsocketProtocolServer;
import city.sane.wot.thing.action.ConsumedThingAction;
import city.sane.wot.thing.action.ThingAction;
import city.sane.wot.thing.event.ThingEvent;
import city.sane.wot.thing.property.ConsumedThingProperty;
import city.sane.wot.thing.property.ThingProperty;
import city.sane.wot.thing.schema.IntegerSchema;
import city.sane.wot.thing.schema.ObjectSchema;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(Parameterized.class)
public class ConsumedThingIT {
    @Parameter
    public Pair<Class<? extends ProtocolServer>, Class<? extends ProtocolClientFactory>> servientClasses;
    private Servient servient;

    @Before
    public void setup() throws ServientException {
        Config config = ConfigFactory
                .parseString("wot.servient.servers = [\"" + servientClasses.first().getName() + "\"]\n" +
                        "wot.servient.client-factories = [\"" + servientClasses.second().getName() + "\"]")
                .withFallback(ConfigFactory.load());

        servient = new Servient(config);
        servient.start().join();
    }

    @After
    public void teardown() {
        servient.shutdown().join();
    }

    @Test(timeout = 20 * 1000L)
    public void readProperty() throws ExecutionException, InterruptedException {
        ExposedThing exposedThing = getExposedCounterThing();
        servient.addThing(exposedThing);
        exposedThing.expose().join();

        ConsumedThing thing = new ConsumedThing(servient, exposedThing);

        ConsumedThingProperty<Object> counter = thing.getProperty("count");

        try {
            assertEquals(42, counter.read().get());
        }
        catch (CompletionException e) {
            if (!(e.getCause() instanceof NoFormForInteractionConsumedThingException)) {
                throw e;
            }
        }
    }

    private ExposedThing getExposedCounterThing() {
        ThingProperty counterProperty = new ThingProperty.Builder()
                .setType("integer")
                .setDescription("current counter content")
                .setObservable(true)
                .build();

        ThingProperty lastChangeProperty = new ThingProperty.Builder()
                .setType("string")
                .setDescription("last change of counter content")
                .setObservable(true)
                .setReadOnly(true)
                .build();

        ExposedThing thing = new ExposedThing(servient)
                .setId("counter")
                .setTitle("counter");

        thing.addProperty("count", counterProperty, 42);
        thing.addProperty("lastChange", lastChangeProperty, new Date().toString());

        thing.addAction("increment",
                new ThingAction.Builder()
                        .setDescription("Incrementing counter content with optional step content as uriVariable")
                        .setUriVariables(Map.of(
                                "step", Map.of(
                                        "type", "integer",
                                        "minimum", 1,
                                        "maximum", 250
                                )
                        ))
                        .setInput(new ObjectSchema())
                        .setOutput(new IntegerSchema())
                        .build(),
                (input, options) -> {
                    return thing.getProperty("count").read().thenApply(value -> {
                        int step;
                        if (input != null && ((Map) input).containsKey("step")) {
                            step = (Integer) ((Map) input).get("step");
                        }
                        else if (options.containsKey("uriVariables") && ((Map) options.get("uriVariables")).containsKey("step")) {
                            step = (int) ((Map) options.get("uriVariables")).get("step");
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

        thing.addAction("decrement", new ThingAction<Object, Object>(), (input, options) -> {
            return thing.getProperty("count").read().thenApply(value -> {
                int newValue = ((Integer) value) - 1;
                thing.getProperty("count").write(newValue);
                thing.getProperty("lastChange").write(new Date().toString());
                thing.getEvent("change").emit();
                return newValue;
            });
        });

        thing.addAction("reset", new ThingAction<Object, Object>(), (input, options) -> {
            return thing.getProperty("count").write(0).thenApply(value -> {
                thing.getProperty("lastChange").write(new Date().toString());
                thing.getEvent("change").emit();
                return 0;
            });
        });

        thing.addEvent("change", new ThingEvent<Object>());

        return thing;
    }

    @Test(timeout = 20 * 1000L)
    public void writeProperty() throws ExecutionException, InterruptedException {
        ExposedThing exposedThing = getExposedCounterThing();
        servient.addThing(exposedThing);
        exposedThing.expose().join();

        ConsumedThing thing = new ConsumedThing(servient, exposedThing);

        ConsumedThingProperty<Object> counter = thing.getProperty("count");

        try {
            Object o = counter.write(1337).get();
            assertEquals(1337, counter.read().get());
        }
        catch (CompletionException e) {
            if (!(e.getCause() instanceof NoFormForInteractionConsumedThingException)) {
                throw e;
            }
        }
    }

    @Test(timeout = 20 * 1000L)
    public void observePropertyShouldHandleMultipleSubscription() throws ConsumedThingException, InterruptedException, ExecutionException {
        try {
            ExposedThing exposedThing = getExposedCounterThing();
            servient.addThing(exposedThing);
            exposedThing.expose().join();

            ConsumedThing consumedThing = new ConsumedThing(servient, exposedThing);

            List<Object> results1 = new ArrayList<>();
            Disposable disposable1 = consumedThing.getProperty("count").observer()
                    .subscribe(next -> results1.add(next.orElse(null)));

            List<Object> results2 = new ArrayList<>();
            Disposable disposable2 = consumedThing.getProperty("count").observer()
                    .subscribe(next -> results2.add(next.orElse(null)));

            // wait until client establish subscription
            // TODO: This is error-prone. We need a feature that notifies us when the subscription is active.
            Thread.sleep(5 * 1000L);

            exposedThing.getProperty("count").write(1337).get();

            // wait until client fires next subscribe-request to server
            // TODO: This is error-prone. We need a feature that notifies us when the subscription is active.
            Thread.sleep(5 * 1000L);

            exposedThing.getProperty("count").write(1338).get();

            // Subscriptions are executed asynchronously. Therefore, wait "some" time before we check the result.
            // TODO: This is error-prone. We need a function that notifies us when all subscriptions have been executed.
            Thread.sleep(5 * 1000L);

            disposable1.dispose();
            disposable2.dispose();

            assertThat(results1, contains(1337, 1338));
            assertThat(results2, contains(1337, 1338));
        }
        catch (ExecutionException e) {
            if (!(e.getCause() instanceof ProtocolClientNotImplementedException)) {
                throw e;
            }
        }
    }

    @Test(timeout = 20 * 1000L)
    public void readProperties() throws ExecutionException, InterruptedException {
        ExposedThing exposedThing = getExposedCounterThing();
        servient.addThing(exposedThing);
        exposedThing.expose().join();

        ConsumedThing thing = new ConsumedThing(servient, exposedThing);

        try {
            Map values = thing.readProperties().get();
            assertEquals(2, values.size());
            assertEquals(42, values.get("count"));
        }
        catch (ExecutionException e) {
            if (!(e.getCause() instanceof NoFormForInteractionConsumedThingException)) {
                throw e;
            }
        }
    }

    @Test(timeout = 20 * 1000L)
    public void readMultipleProperties() throws ExecutionException, InterruptedException {
        ExposedThing exposedThing = getExposedCounterThing();
        servient.addThing(exposedThing);
        exposedThing.expose().join();

        ConsumedThing thing = new ConsumedThing(servient, exposedThing);

        try {
            Map values = thing.readProperties("count").get();
            assertEquals(1, values.size());
            assertEquals(42, values.get("count"));
        }
        catch (ExecutionException e) {
            if (!(e.getCause() instanceof NoFormForInteractionConsumedThingException)) {
                throw e;
            }
        }
    }

    @Test(timeout = 20 * 1000L)
    public void invokeAction() throws ExecutionException, InterruptedException {
        try {
            ExposedThing exposedThing = getExposedCounterThing();
            servient.addThing(exposedThing);
            exposedThing.expose().join();

            ConsumedThing thing = new ConsumedThing(servient, exposedThing);

            ConsumedThingAction increment = thing.getAction("increment");
            Object output = increment.invoke().get();

            if (MqttProtocolClientFactory.class.isAssignableFrom(servientClasses.second())) {
                // mqtt is not able to return a result
                assertNull(output);
            }
            else {
                assertEquals(43, output);
            }
        }
        catch (ExecutionException e) {
            if (!(e.getCause() instanceof ProtocolClientNotImplementedException)) {
                throw e;
            }
        }
    }

    @Test(timeout = 20 * 1000L)
    public void invokeActionWithParameters() throws ExecutionException, InterruptedException {
        try {
            ExposedThing exposedThing = getExposedCounterThing();
            servient.addThing(exposedThing);
            exposedThing.expose().join();

            ConsumedThing thing = new ConsumedThing(servient, exposedThing);

            ConsumedThingAction increment = thing.getAction("increment");
            Object output = increment.invoke(Map.of("step", 3)).get();

            if (MqttProtocolClientFactory.class.isAssignableFrom(servientClasses.second())) {
                // mqtt is not able to return a result
                assertNull(output);
            }
            else {
                assertEquals(45, output);
            }
        }
        catch (ExecutionException e) {
            if (!(e.getCause() instanceof ProtocolClientNotImplementedException)) {
                throw e;
            }
        }
    }

    @Test
    public void observeEventShouldHandleMultipleSubscription() throws InterruptedException, ConsumedThingException {
        ExposedThing exposedThing = getExposedCounterThing();
        servient.addThing(exposedThing);
        exposedThing.expose().join();

        ConsumedThing consumedThing = new ConsumedThing(servient, exposedThing);

        List<Object> results1 = new ArrayList<>();
        Disposable disposable1 = consumedThing.getEvent("change").observer()
                .subscribe(next -> results1.add(next.orElse(null)));

        List<Object> results2 = new ArrayList<>();
        Disposable disposable2 = consumedThing.getEvent("change").observer()
                .subscribe(next -> results2.add(next.orElse(null)));

        // wait until client establish subscription
        // TODO: This is error-prone. We need a feature that notifies us when the subscription is active.
        Thread.sleep(5 * 1000L);

        exposedThing.getEvent("change").emit();

        // wait until client fires next subscribe-request to server
        // TODO: This is error-prone. We need a feature that notifies us when the subscription is active.
        Thread.sleep(5 * 1000L);

        exposedThing.getEvent("change").emit();

        // Subscriptions are executed asynchronously. Therefore, wait "some" time before we check the result.
        // TODO: This is error-prone. We need a function that notifies us when all subscriptions have been executed.
        Thread.sleep(5 * 1000L);

        disposable1.dispose();
        disposable2.dispose();

        assertThat(results1, contains(nullValue(), nullValue()));
        assertThat(results2, contains(nullValue(), nullValue()));
    }

    @Parameters(name = "{0}")
    public static Collection<Pair<Class<? extends ProtocolServer>, Class<? extends ProtocolClientFactory>>> data() {
        return Arrays.asList(
                new Pair<>(AkkaProtocolServer.class, AkkaProtocolClientFactory.class),
                new Pair<>(CoapProtocolServer.class, CoapProtocolClientFactory.class),
                new Pair<>(HttpProtocolServer.class, HttpProtocolClientFactory.class),
                // Jadex platform discovery is unstable
//                new Pair<>(JadexProtocolServer.class, JadexProtocolClientFactory.class),
                new Pair<>(MqttProtocolServer.class, MqttProtocolClientFactory.class),
                new Pair<>(WebsocketProtocolServer.class, WebsocketProtocolClientFactory.class)
        );
    }
}
