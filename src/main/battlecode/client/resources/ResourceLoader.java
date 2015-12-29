package battlecode.client.resources;

import battlecode.server.ErrorReporter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Loads resources from the battlecode-client jar.
 *
 * @author james
 */
public final class ResourceLoader {

    /**
     * The path of resource files in the jar.
     */
    private static final String RESOURCE_PREFIX = "battlecode/client/resources/";

    /**
     * The ClassLoader we use to access resources.
     */
    private static final ClassLoader loader = ResourceLoader.class.getClassLoader();

    /**
     * Get the URL of a resource.
     * Must be a relative path, like "art/hats/batman.png".
     *
     * @param resource the resource to look up
     * @return the url of the resource
     */
    public static URL getUrl(String resource) {
        final File file = new File(resource);
        if (file.isAbsolute()) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                ErrorReporter.report(e, true);
            }
        }

        return loader.getResource(RESOURCE_PREFIX + resource);
    }
}
