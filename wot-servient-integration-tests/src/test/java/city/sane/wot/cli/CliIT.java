package city.sane.wot.cli;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import city.sane.relay.server.RelayServer;
import com.google.common.io.Files;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertTrue;

public class CliIT {
    private RelayServer relayServer;
    private Thread relayServerThread;

    @Before
    public void setUp() {
        relayServer = new RelayServer(ConfigFactory.load());
        relayServerThread = new Thread(relayServer);
        relayServerThread.start();
    }

    @After
    public void tearDown() throws InterruptedException {
        relayServer.close();
        relayServerThread.join();
    }

    @Test
    public void help() throws CliException {
        new Cli(new String[]{ "--help" });

        assertTrue(true);
    }

    @Test
    public void version() throws CliException {
        new Cli(new String[]{ "--version" });

        assertTrue(true);
    }

    @Test
    public void runScript() throws CliException, IOException {
        String script = "def thing = [\n" +
                "    id        : 'KlimabotschafterWetterstation',\n" +
                "    title     : 'KlimabotschafterWetterstation',\n" +
                "    '@type'   : 'Thing',\n" +
                "    '@context': [\n" +
                "        'http://www.w3.org/ns/td',\n" +
                "        [\n" +
                "            om   : 'http://www.wurvoc.org/vocabularies/om-1.8/',\n" +
                "            saref: 'https://w3id.org/saref#',\n" +
                "            sch  : 'http://schema.org/',\n" +
                "            sane : 'https://sane.city/',\n" +
                "        ]\n" +
                "    ],\n" +
                "]\n" +
                "\n" +
                "def exposedThing = wot.produce(thing)\n" +
                "\n" +
                "exposedThing.addProperty(\n" +
                "    'Temp_2m',\n" +
                "    [\n" +
                "        '@type'             : 'saref:Temperature',\n" +
                "        description         : 'Temperatur in 2m in Grad Celsisus',\n" +
                "        'om:unit_of_measure': 'om:degree_Celsius',\n" +
                "        type                : 'number',\n" +
                "        readOnly            : true,\n" +
                "        observable          : true\n" +
                "    ]\n" +
                ")\n" +
                "println(exposedThing.toJson(true))";

        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        File file = folder.newFile("my-thing.groovy");
        Files.write(script, file, Charset.defaultCharset());

        new Cli(new String[]{
                file.getAbsolutePath()
        });

        assertTrue(true);
    }

    @Test(expected = CliException.class)
    public void runBrokenScript() throws CliException, IOException {
        String script = "1/0";

        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        File file = folder.newFile("my-thing.groovy");
        Files.write(script, file, Charset.defaultCharset());

        new Cli(new String[]{
                file.getAbsolutePath()
        });
    }
}