package city.sane.wot.binding.akka;

import akka.actor.ActorSystem;
import city.sane.wot.binding.ProtocolClientException;
import city.sane.wot.content.Content;
import city.sane.wot.thing.form.Form;
import io.reactivex.rxjava3.internal.observers.LambdaObserver;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AkkaProtocolClientTest {
    private ActorSystem system;
    private Form form;
    private Duration askTimeout;
    private Duration discoverTimeout;

    @Before
    public void setUp() {
        system = mock(ActorSystem.class);
        form = mock(Form.class);
        askTimeout = Duration.ofSeconds(60);
        discoverTimeout = Duration.ofSeconds(5);
    }

    @Test
    public void subscribeResourceShouldCreateActor() throws ProtocolClientException {
        when(form.getHref()).thenReturn("akka://foo/bar");

        AkkaProtocolClient client = new AkkaProtocolClient(system, askTimeout, discoverTimeout);
        client.observeResource(form).subscribe();

        verify(system).actorOf(any());
    }

    @Test
    public void subscribeResourceShouldStopActorWhenObserverIsDone() throws ProtocolClientException {
        when(form.getHref()).thenReturn("akka://foo/bar");
        LambdaObserver<Content> observer = new LambdaObserver<>(n -> {
        }, e -> {
        }, () -> {
        }, s -> {
        });

        AkkaProtocolClient client = new AkkaProtocolClient(system, askTimeout, discoverTimeout);
        client.observeResource(form).subscribe(observer);
        observer.dispose();

        verify(system).stop(any());
    }
}