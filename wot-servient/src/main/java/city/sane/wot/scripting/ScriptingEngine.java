package city.sane.wot.scripting;

import city.sane.wot.Wot;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * A ScriptingEngine describes how a WoT script can be executed in a certain scripting language.
 */
interface ScriptingEngine {
    /**
     * Returns the media type supported by the codec (e.g. application/javascript).
     *
     * @return
     */
    String getMediaType();

    /**
     * Returns the file extension supported by the codec (e.g. .js).
     *
     * @return
     */
    String getFileExtension();

    /**
     * Runs <code>script</code> in sandboxed context.
     *
     * @param script
     * @param wot
     * @param executorService
     * @return
     */
    CompletableFuture<Void> runScript(String script, Wot wot, ExecutorService executorService);

    /**
     * Runs <code>script</code> in privileged context (dangerous) - means here: Script can import
     * classes and make system calls.
     *
     * @param script
     * @param wot
     * @param executorService
     * @return
     */
    CompletableFuture<Void> runPrivilegedScript(String script,
                                                Wot wot,
                                                ExecutorService executorService);
}
