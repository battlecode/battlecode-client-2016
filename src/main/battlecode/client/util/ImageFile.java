package battlecode.client.util;

import battlecode.client.resources.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

public class ImageFile {
    public BufferedImage image;
    private HashMap<Dimension, BufferedImage> cache;

    public ImageFile(String pathname) {
        try {
            BufferedImage tmp = ImageIO.read(ResourceLoader.getUrl(pathname));
            image = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration()
                    .createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.BITMASK);
            Graphics2D g2 = image.createGraphics();
            g2.drawRenderedImage(tmp, null);
            g2.dispose();
            tmp.flush();
        } catch (IOException e) {
            image = null;
        }
        cache = new HashMap<>();
        cache.put(new Dimension(image.getWidth(), image.getHeight()), image);
    }

    public ImageFile(BufferedImage image) {
        this.image = image;
    }

    public void resize(int width, int height) {
        this.image = getScaledInstance(image, width, height,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    // Temporary resizing algorithm, taken from http://stackoverflow.com/questions/24745147/java-resize-image-without-losing-quality
    public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth,
                                                  int targetHeight, Object hint) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int w = img.getWidth();
        int h = img.getHeight();

        do {
            if (w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            } else {
                w *= 2;
                if (w > targetWidth) {
                    w = targetWidth;
                }
            }

            if (h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            } else {
                h *= 2;
                if (h > targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
}