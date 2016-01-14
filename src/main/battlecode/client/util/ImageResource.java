package battlecode.client.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class ImageResource<E> {

    private final Map<E, Map<Dimension, ImageFile>> cache;


    public ImageResource() {
        cache = new HashMap<>();
    }

    public ImageFile getResource(E key, String path) {
        return getResource(key, path, 0, 0);
    }

    public ImageFile getResource(E key, String path, int width, int height) {
        if (cache.get(key) != null) {
            ImageFile img = cache.get(key).get(new Dimension(width, height));
            if (img != null) {
                return img;
            }
        }
        ImageFile img = new ImageFile(path);
        if (width == 0 || height == 0) {
            return img;
        }
        img.resize(width, height);
        if (cache.get(key) == null) {
            cache.put(key, new HashMap<>());
        }
        cache.get(key).put(new Dimension(width, height), img);
        return img;
    }
}