package battlecode.client.util;

import java.awt.*;

import java.awt.image.*;
import java.io.*;
import java.net.URL;
import javax.imageio.*;

public class ImageFile extends DataFile {

    public BufferedImage image;

    public ImageFile(String pathname) {
        super(pathname);
    }

    protected void load(File file) {
        //System.out.println("loading FILE " + file.getName());
        try {
            BufferedImage tmp = ImageIO.read(file);
            image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(tmp.getWidth(), tmp.getHeight(),
                    Transparency.TRANSLUCENT);
            /*
            System.out.println(file + (image.getCapabilities(null).isAccelerated() ?
            " accelerated" : " not accelerated") +
            ", transparency: " + image.getTransparency());
             */
            Graphics2D g2 = image.createGraphics();
            g2.drawRenderedImage(tmp, null);
            g2.dispose();
            tmp.flush();
        } catch (IOException e) {
            image = null;
        }
    }

    protected void load(URL file) {
        //System.out.println("loading URL " + file.getPath());
        try {
            BufferedImage tmp = ImageIO.read(file);
            image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(tmp.getWidth(), tmp.getHeight(),
                    Transparency.TRANSLUCENT);
            /*
            System.out.println(file + (image.getCapabilities(null).isAccelerated() ?
            " accelerated" : " not accelerated") +
            ", transparency: " + image.getTransparency());
             */
            Graphics2D g2 = image.createGraphics();
            g2.drawRenderedImage(tmp, null);
            g2.dispose();
            tmp.flush();
        } catch (IOException e) {
            image = null;
        }
    }

    protected void reload(File file) {
        load(file);
    }

    protected void reload(URL url) {
        load(url);
    }
}
