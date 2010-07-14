package battlecode.client.util;

import java.util.*;

public class TextureResource<E> {

    private Hashtable<E, TextureFile> cache;

    public TextureResource() {
        cache = new Hashtable<E, TextureFile>();
    }

    public TextureFile getResource(E key, String path) {
        TextureFile img = cache.get(key);
        if (img != null) {
            return img;
        }

        img = new TextureFile(path);
        cache.put(key, img);
        return img;
    }

    @Override
    protected void finalize() throws Throwable {
        for (TextureFile t : cache.values()) {
            t.finalize();
        }

        super.finalize();
    }

    public void unloadAll() {
        for (TextureFile t : cache.values()) {
            try {
                t.finalize();
            } catch (Throwable e) {
                System.out.println("Can't finalize");
                e.printStackTrace();
            }
        }

        cache.clear();
    }
}
