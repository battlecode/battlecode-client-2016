package battlecode.client.resources;

import battlecode.server.ErrorReporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
     * If the resource is relative, it will be loaded from the
     * "battlecode.client.resources" package.
     * If the resource is absolute, it will be loaded as-is.
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

    /**
     * Get a file representing a resource.
     * If the resource is relative, it will be loaded from the
     * "battlecode.client.resources" package.
     * If the resource is absolute, it will be loaded as-is.
     *
     * @param resource the resource to look up
     * @return the url of the resource
     */
    public static File getFile(String resource) {
        URL result = getUrl(resource);

        if (result == null) {
            throw new RuntimeException("Null URL: "+resource);
        }

        try {
            return new File(result.toURI());
        } catch (URISyntaxException e) {
            return new File(result.getPath());
        }
    }
}
