package battlecode.client.util;

import java.io.File;

import java.io.IOException;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import java.net.URL;

public class TextureFile extends DataFile {

    public Texture tex;

    public TextureFile(String pathname) {
        super(pathname);
    }

    protected void load(File file) {
        try {
            tex = TextureIO.newTexture(file, true);
        } catch (IOException e) {
            tex = null;
        }
    }

    protected void reload(File file) {
        load(file);
    }

    @Override
    protected void finalize() throws Throwable {
        //tex.dispose();
    }

    @Override
    protected void load(URL url) {
        try {
            tex = TextureIO.newTexture(url, true, "");
        } catch (IOException e) {
            tex = null;
        }
    }

    @Override
    protected void reload(URL url) {
        load(url);
    }
}
