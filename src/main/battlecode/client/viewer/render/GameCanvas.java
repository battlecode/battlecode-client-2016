package battlecode.client.viewer.render;

import battlecode.client.util.ImageFile;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Observer;

public final class GameCanvas extends BaseCanvas {

    private static final long serialVersionUID = 0; // don't serialize
    private volatile GameRenderer renderer = null;
    private boolean gotPreferredSize = false;
    private GraphicsDevice gd = null;

    //private Window fullscreen;
    public GameCanvas() {
        super();
    }

    public void setTournamentMode() {
        RenderConfiguration.setTournamentMode(true);
        setVisible(true);
    }

    public void setGraphicsDevice(GraphicsDevice gd) {
        this.gd = gd;
    }

    protected GameRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(GameRenderer r) {
        renderer = (GameRenderer) r;

        Observer observer = (o, arg) -> {
            if (renderer.getTimeline() != null && renderer.getTimeline()
                    .isActive()) {
                forceRepaint();
            }
        };
        if (renderer.getTimeline() != null)
            renderer.getTimeline().addObserver(observer);

        battlecode.client.viewer.DebugState dbg = renderer.getDebugState();
        if (dbg != null) {
            addMouseListener(dbg);
            addMouseMotionListener(dbg);
            dbg.addObserver(observer);
        }

        repaint();
    }

    public static ImageFile bracketFile = null;
    public final GameRenderer bracketRenderer = new GameRenderer() {

        @Override
        public void draw(Graphics g) {
            if (RenderConfiguration.isTournamentMode()) {
                Graphics2D g2 = (Graphics2D) g;
                Dimension d = getSize();
                g2.setColor(Color.GRAY);
                g2.fill(new Rectangle(0, 0, d.width, d.height));
                BufferedImage img = bracketFile.image;
                if (img != null) {
                    float scale = Math.min((float) d.width / img.getWidth(),
                            (float) d.height / img.getHeight());
                    AffineTransform trans = AffineTransform
                            .getTranslateInstance(0.5 * (d.width - scale *
                                    img.getWidth()), 0.5 * (d.height - scale
                                    * img.getHeight()));
                    trans.scale(scale, scale);
                    g2.drawImage(img, trans, null);
                }
            }
        }
    };
    private final battlecode.client.util.SettableObservable paintObservable =
            new battlecode.client.util.SettableObservable();

    public void addPaintObserver(Observer o) {
        paintObservable.addObserver(o);
    }

    @Override
    public void paint(Graphics g) {
        if (renderer != null) {
            renderer.setCanvasSize(getSize());
            renderer.draw(g);
            paintObservable.setChanged();
            paintObservable.notifyObservers();
        } else {
            g.setColor(Color.BLACK);
            Dimension d = getSize();
            g.fillRect(0, 0, d.width, d.height);
        }
    }

    public void forceRepaint() {
        if (!gotPreferredSize) {
            Dimension preferredSize = renderer.getPreferredSize(this.gd);
            if (preferredSize.getHeight() != 0 && preferredSize.getWidth() !=
                    0) {
                setPreferredSize(preferredSize);
                gotPreferredSize = true;
                setVisible(true);
            }
        }
        repaint();
    }
}
