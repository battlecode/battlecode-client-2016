package battlecode.client.util;

import java.util.*;

public class ImageResource<E> {

	private Map<E, ImageFile> cache;

	public ImageResource() {
		cache = new HashMap<E, ImageFile>();
	}
        public ImageFile getResource(E key, String path) {
	    return getResource(key, path, false);
		}
        public ImageFile getResource(E key, String path, boolean useSpriteSheet) {
		ImageFile img = cache.get(key);
		if (img != null) {
			return img;
		}
		//System.out.println("caching " + path);
		if (useSpriteSheet) {
		    img = new SpriteSheetFile(path);
		} else {
		    img = new ImageFile(path);
		}
		cache.put(key, img);
		return img;
	}
}

