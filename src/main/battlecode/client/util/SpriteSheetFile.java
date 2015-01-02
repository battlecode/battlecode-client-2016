package battlecode.client.util;

import java.awt.*;

import java.awt.image.*;
import java.io.*;
import java.net.URL;
import javax.imageio.*;
import java.lang.Math;
import java.awt.geom.AffineTransform;
import battlecode.common.Direction;

public class SpriteSheetFile extends ImageFile {

    public BufferedImage[] sprites;

    // currently correct for Max's sprites
    public static final Direction baseDir = Direction.EAST;

    public SpriteSheetFile(String pathname) {
        super(pathname);
    }

    protected void makeSheet() {
	if (false) { // TODO deal with sheets
	    // sprite sheet is East 0, clockwise
	    // direction sheet is North 0, clockwise
	    /*int sheetIndex = (dir.ordinal() - Direction.EAST.ordinal() + 8) % 8;
	    int soldierHeight = image.getHeight();
	    if (!isAttacking()) {
		sheetIndex += 8;
	    }
	    image = image.getSubimage(sheetIndex * soldierHeight, 0,
				      soldierHeight, soldierHeight);
	    */
	} else {
	    sprites = new BufferedImage[8];
	    sprites[0] = image;
	    for (int i = 1; i < 8; i++) {
		sprites[i] = new BufferedImage(image.getWidth(),
					       image.getHeight(),
					       BufferedImage.TYPE_INT_ARGB);
		double rotationRequired = Math.toRadians(i * 45);
		double locationX = image.getWidth() / 2;
		double locationY = image.getHeight() / 2;
		AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired,
								       locationX,
								       locationY);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		// Drawing the rotated image at the required drawing locations
		sprites[i].createGraphics().drawImage(op.filter(image, null), 0, 0, null);
	    }
	}
	
    }

    public BufferedImage spriteForDirection(Direction dir) {
	int index = dir.ordinal();
	if (index > 8) index = 0; // both NONE and OMNI should face NORTH
	int sheetIndex = (index - baseDir.ordinal() + 8) % 8;
	return sprites[sheetIndex];
    }

    protected void load(File file) {
	super.load(file);
	makeSheet();
    }

    protected void load(URL file) {
	super.load(file);
	makeSheet();
    }

    protected void reload(File file) {
        load(file);
    }

    protected void reload(URL url) {
        load(url);
    }
}
