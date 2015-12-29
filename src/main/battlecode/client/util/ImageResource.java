package battlecode.client.util;

import java.util.HashMap;
import java.util.Map;

public class ImageResource<E> {

    private Map<E, ImageFile> cache;

    public ImageResource() {
        cache = new HashMap<>();
    }

    public ImageFile getResource(E key, String path) {
        return getResource(key, path, false);
    }

    public ImageFile getResource(E key, String path, boolean useSpriteSheet) {
        ImageFile img = cache.get(key);
        if (img != null) {
            return img;
        }
        if (useSpriteSheet) {
            img = new SpriteSheetFile(path);
        } else {
            img = new ImageFile(path);
        }
        cache.put(key, img);
        return img;
    }
}

