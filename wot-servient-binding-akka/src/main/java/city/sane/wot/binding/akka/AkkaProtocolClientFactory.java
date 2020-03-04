package city.sane.wot.binding.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import city.sane.RefCountResource;
import city.sane.RefCountResourceException;
import city.sane.Triple;
import city.sane.wot.binding.ProtocolClient;
import city.sane.wot.binding.ProtocolClientFactory;
import city.sane.wot.thing.ExposedThing;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Creates new {@link AkkaProtocolClient} instances. An Actor System is created for this purpose.
 * The Actor System is intended for use in an
 * <a href="https://doc.akka.io/docs/akka/current/index-cluster.html">Akka Cluster</a> to discover
 * and interaction with other Actor Systems.<br> The Actor System can be configured via the
 * configuration parameter "wot.servient.akka" (see https://doc.akka.io/docs/akka/current/general/configuration.html).
 */
public class AkkaProtocolClientFactory implements ProtocolClientFactory {
    private static final Logger log = LoggerFactory.getLogger(AkkaProtocolClientFactory.class);
    private final RefCountResource<Triple<ActorSystem, Map<String, ExposedThing>, ActorRef>> actorSystemProvider;
    private final Duration askTimeout;
    private final Duration discoverTimeout;
    private Triple<ActorSystem, Map<String, ExposedThing>, ActorRef> triple = null;

    public AkkaProtocolClientFactory(Config config) {
        this(
                SharedActorSystemProvider.singleton(config),
                config.getDuration("wot.servient.akka.ask-timeout"),
                config.getDuration("wot.servient.akka.discover-timeout"),
                null
        );
    }

    public AkkaProtocolClientFactory(RefCountResource<Triple<ActorSystem, Map<String, ExposedThing>, ActorRef>> actorSystemProvider,
                                     Duration askTimeout,
                                     Duration discoverTimeout,
                                     Triple<ActorSystem, Map<String, ExposedThing>, ActorRef> triple) {
        this.actorSystemProvider = actorSystemProvider;
        this.askTimeout = askTimeout;
        this.discoverTimeout = discoverTimeout;
        this.triple = triple;
    }

    @Override
    public String toString() {
        return "AkkaClient";
    }

    @Override
    public String getScheme() {
        return "akka";
    }

    @Override
    public ProtocolClient getClient() {
        return new AkkaProtocolClient(triple.first(), askTimeout, discoverTimeout);
    }

    @Override
    public CompletableFuture<Void> init() {
        log.debug("Init Actor System");

        if (triple == null) {
            return runAsync(() -> {
                try {
                    triple = actorSystemProvider.retain();
                }
                catch (RefCountResourceException e) {
                    throw new CompletionException(e);
                }
            });
        }
        else {
            return completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Void> destroy() {
        log.debug("Terminate Actor System");

        if (triple != null) {
            return runAsync(() -> {
                try {
                    actorSystemProvider.release();
                }
                catch (RefCountResourceException e) {
                    throw new CompletionException(e);
                }
            });
        }
        else {
            return completedFuture(null);
        }
    }

    public ActorSystem getActorSystem() {
        return triple.first();
    }
}
