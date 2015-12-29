package battlecode.client.util;

import battlecode.client.resources.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ImageFile {
    public BufferedImage image;

    public ImageFile(String pathname) {
        try {
            BufferedImage tmp = ImageIO.read(ResourceLoader.getUrl(pathname));
            image = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration()
                    .createCompatibleImage(tmp.getWidth(), tmp.getHeight(),
                            Transparency.TRANSLUCENT);
            Graphics2D g2 = image.createGraphics();
            g2.drawRenderedImage(tmp, null);
            g2.dispose();
            tmp.flush();
        } catch (IOException e) {
            image = null;
        }
    }
}
