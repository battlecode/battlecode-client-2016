package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;

public class SuicideAnim extends ExplosionAnim {
  public void draw(Graphics2D g2){
    super.draw(g2);
    if (shouldDraw()) {
      AffineTransform pushed = g2.getTransform();
      if (loc != null) {
        g2.translate(loc.x, loc.y);
      }
      g2.translate(-0.5*(width - 1), -0.5*(width - 1));
      g2.setColor(new Color(1.0f, 0, 1, 0.3f));
      g2.setStroke(new BasicStroke(1.0f));
      g2.draw(new Ellipse2D.Float(-.5f, -.5f, 2, 2));
      g2.setTransform(pushed);
    }
  }
}
