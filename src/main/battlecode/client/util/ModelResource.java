package battlecode.client.util;

import java.util.*;

public class ModelResource<E> {

	private Hashtable<E, OBJFile> cache;

	public ModelResource() {
		cache = new Hashtable<E, OBJFile>();
	}

	public OBJFile getResource(E key, String path) {
		OBJFile img = cache.get(key);
		if (img != null) {
			return img;
		}
		//System.out.println("caching " + path);
		img = new OBJFile(path);
		cache.put(key, img);
		return img;
	}
}
