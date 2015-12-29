package battlecode.client.util;

import battlecode.common.Direction;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class SpriteSheetFile extends ImageFile {

    public BufferedImage[] sprites;

    // currently correct for Max's sprites
    public static final Direction baseDir = Direction.EAST;

    public SpriteSheetFile(String pathname) {
        super(pathname);
        makeSheet();
    }

    protected void makeSheet() {
        sprites = new BufferedImage[8];
        sprites[0] = image;
        for (int i = 1; i < 8; i++) {
            sprites[i] = new BufferedImage(image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            double rotationRequired = Math.toRadians(0); // disable rotation
            double locationX = image.getWidth() / 2;
            double locationY = image.getHeight() / 2;
            AffineTransform tx = AffineTransform.getRotateInstance
                    (rotationRequired,
                    locationX,
                    locationY);
            AffineTransformOp op = new AffineTransformOp(tx,
                    AffineTransformOp.TYPE_BILINEAR);
            // Drawing the rotated image at the required drawing locations
            sprites[i].createGraphics().drawImage(op.filter(image, null),
                    0, 0, null);
        }

    }

    public BufferedImage spriteForDirection(Direction dir) {
        int index = dir.ordinal();
        if (index > 8) index = 0; // both NONE and OMNI should face NORTH
        int sheetIndex = (index - baseDir.ordinal() + 8) % 8;
        return sprites[sheetIndex];
    }
}
