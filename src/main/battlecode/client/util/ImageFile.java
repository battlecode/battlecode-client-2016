package battlecode.client.util;

import battlecode.client.resources.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageFile {
    public BufferedImage image;

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
    }

    public ImageFile(String pathname, int width, int height) {
        this(pathname);
        this.image = getScaledInstance(image, width, height, RenderingHints
                .VALUE_INTERPOLATION_BILINEAR);
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
}