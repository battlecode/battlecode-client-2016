package battlecode.client.viewer.render;

import battlecode.client.resources.ResourceLoader;
import battlecode.client.util.ImageFile;
import battlecode.common.Team;
import battlecode.world.DominationFactor;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;

public class DrawCutScene {

    public enum Step {

        INTRO, GAME, OUTRO, NEXT
    }

    public Step step = Step.INTRO;
    private final Rectangle2D.Float rect = new Rectangle2D.Float();
    private final ImageFile imgTeamA, imgTeamB;
    private final String teamA, teamB;
    private final String mapName;
    private int aWins, bWins;
    private ImageFile imgWinner;
    private String winner;
    private Color winnerColor;
    private DominationFactor dom = null;
    private static final Color neutralColor = Color.WHITE;
    private static final Color backgroundColor = Color.BLACK;
    private static final Color teamAColor = Color.RED;
    private static final Color teamBColor = Color.BLUE;
    private volatile boolean visible = false;
    private static Map<Integer, String> teamNames = Collections.emptyMap();
    private Font font;

    public static void setTeamNames(Map<Integer, String> names) {
        System.out.println(names.entrySet().size());
        teamNames = names;
    }

    public static String getTeamName(String genericName) {
        try {
            int id = Integer.parseInt(genericName.substring(4, 7));
            if (teamNames.containsKey(id))
                return teamNames.get(id);
            else
                return genericName;
        } catch (Exception e) {
            return genericName;
        }
    }

    public DrawCutScene(float width, float height, String teamA, String
            teamB, String mapName) {

        rect.width = width;
        rect.height = height;
        System.out.println("&&&&&&&&&&&&&&& " + teamA + " " + teamB);
        this.teamA = getTeamName(teamA);
        this.teamB = getTeamName(teamB);
        this.aWins = 0;
        this.bWins = 0;
        this.mapName = mapName;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader
                    .getInputStream("art/computerfont.ttf")).deriveFont(20.f);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load font", e);
        }
        imgTeamA = new ImageFile("avatars/" + teamA + ".png");
        imgTeamB = new ImageFile("avatars/" + teamB + ".png");
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setWinner(Team team) {
        if (team == Team.A) {
            imgWinner = imgTeamA;
            winner = teamA;
            winnerColor = teamAColor;
        } else {
            imgWinner = imgTeamB;
            winner = teamB;
            winnerColor = teamBColor;
        }
    }

    public void setScore(int aWins, int bWins) {
        this.aWins = aWins;
        this.bWins = bWins;
    }

    public void setDominationFactor(DominationFactor dom) {
        this.dom = dom;
    }

    public void draw(Graphics2D g2) {
        if (visible) {
            switch (step) {
                case INTRO:
                    drawIntro(g2);
                    break;
                case OUTRO:
                    drawOutro(g2);
                    break;
                default:
                    break;
            }
        }
    }

    private void drawLogo(Graphics2D g2, BufferedImage img, double height) {
        Rectangle rect = g2.getDeviceConfiguration().getBounds();
        double x = rect.getWidth() / 2 - 64;
        double y = height - 64;
        g2.drawImage(img, new AffineTransform(128. / img.getWidth(), 0, 0,
                128. / img.getHeight(), x, y), null);
    }

    private void drawIntro(Graphics2D g2) {
        AffineTransform pushed = g2.getTransform();
        g2.setTransform(new AffineTransform());
        int textHeight = g2.getFontMetrics(font).getHeight();
        Rectangle rect = g2.getDeviceConfiguration().getBounds();
        g2.setColor(backgroundColor);
        g2.fill(rect);
        DrawText drawText = new DrawText(g2, font);
        g2.setColor(teamAColor);
        drawText.drawTwoLine(teamA, rect.getCenterX(), rect.getCenterY() -
                textHeight, true);
        g2.setColor(neutralColor);
        drawText.draw("VS", rect.getCenterX(), rect.getCenterY());
        g2.setColor(teamBColor);
        drawText.drawTwoLine(teamB, rect.getCenterX(), rect.getCenterY() +
                textHeight, false);
        if (imgTeamA.image != null)
            drawLogo(g2, imgTeamA.image, rect.getCenterY() - 3 * textHeight -
                    50);
        if (imgTeamB.image != null)
            drawLogo(g2, imgTeamB.image, rect.getCenterY() + 3 * textHeight +
                    50);

        g2.setColor(neutralColor);
        drawText.draw("Map: " + mapName, rect.getCenterX(), rect.getCenterY()
                + 3 * textHeight + 80 + 80 + 15);
        drawText.draw("Match score: " + aWins + " - " + bWins, rect
                .getCenterX(), rect.getCenterY() + 5 * textHeight + 80 + 80 +
                15);

        g2.setTransform(pushed);
    }

    private static class DrawText {

        private final Graphics2D g2;
        private final Font font;
        private final FontMetrics metrics;
        private final FontRenderContext renderContext;
        private final Rectangle boundingRect;

        public static int findSpaceInMiddle(String s) {
            int before = s.lastIndexOf(' ', s.length() / 2);
            int after = s.indexOf(' ', s.length() / 2);
            if (before == -1) return after;
            if (after == -1) return before;
            return before + after < s.length() ? after : before;
        }

        public DrawText(Graphics2D g2, Font font) {
            this.g2 = g2;
            this.font = font;
            metrics = g2.getFontMetrics(font);
            renderContext = g2.getFontRenderContext();
            boundingRect = g2.getDeviceConfiguration().getBounds();
        }

        public void draw(String s, double centerx, double centery) {
            draw(s, (float) centerx, (float) centery);
        }

        public void draw(String s, float centerx, float centery) {
            GlyphVector glyphs = font.createGlyphVector(renderContext, s);

            // are there non-ascii characters?
            boolean isASCII = true;
            for (int i = 0; i < s.length(); ++i) {
                if ((int) s.charAt(i) >= 128) {
                    isASCII = false;
                }
            }
            if (!isASCII) {
                glyphs = (new Font("Monospaced", Font.BOLD, 12)).deriveFont
                        (48.f).createGlyphVector(renderContext, s);
            }

            // Apparently the x,y coordinates given to drawGlyphVector are
            // the bottom right corner?
            g2.drawGlyphVector(glyphs, centerx - metrics.stringWidth(s) / 2,
                    centery + metrics.getHeight() / 2);
        }

        public void drawTwoLine(String s, double centerx, double centery,
                                boolean up) {
            if (s == null || s.length() == 0)
                s = "ERROR";
            drawTwoLine(s, (float) centerx, (float) centery, up);
        }

        public void drawTwoLine(String s, float centerx, float centery,
                                boolean up) {
            int twidth = metrics.stringWidth(s), split;
            if (twidth < boundingRect.getWidth() || (split =
                    findSpaceInMiddle(s)) == -1)
                draw(s, centerx, centery);
            else {
                String part1 = s.substring(0, split);
                String part2 = s.substring(split + 1);
                int height = metrics.getHeight();
                if (up) {
                    draw(part1, centerx, centery - height);
                    draw(part2, centerx, centery);
                } else {
                    draw(part1, centerx, centery);
                    draw(part2, centerx, centery + height);
                }
            }
        }
    }

    private void drawOutro(Graphics2D g2) {
        AffineTransform pushed = g2.getTransform();
        g2.setTransform(new AffineTransform());
        int textHeight = g2.getFontMetrics(font).getHeight();
        Rectangle rect = g2.getDeviceConfiguration().getBounds();
        g2.setColor(backgroundColor);
        g2.fill(rect);
        DrawText drawText = new DrawText(g2, font);
        g2.setColor(winnerColor);
        drawText.drawTwoLine(winner, rect.getCenterX(), rect.getCenterY() -
                textHeight / 2, true);
        drawText.draw("WINS!", rect.getCenterX(), rect.getCenterY() +
                textHeight / 2);
        if (imgWinner != null && imgWinner.image != null)
            drawLogo(g2, imgWinner.image, rect.getCenterY() - 5 * textHeight
                    / 2 - 80);

        String s = "?";
        if (dom == DominationFactor.DESTROYED)
            s = "DESTRUCTION";
        else if (dom == DominationFactor.PWNED)
            s = "more archons left";
        else if (dom == DominationFactor.OWNED)
            s = "more archon health";
        else if (dom == DominationFactor.BARELY_BEAT)
            s = "more parts";
        else if (dom == DominationFactor.WON_BY_DUBIOUS_REASONS)
            s = "???";

        drawText.draw("Reason: " + s, rect.getCenterX(), rect.getCenterY() + textHeight * 5 / 2);
        g2.setColor(neutralColor);
        drawText.draw("Match score: " + aWins + " - " + bWins, rect
                .getCenterX(), rect.getCenterY() + textHeight * 8 / 2);
        g2.setTransform(pushed);
    }

    public void fadeOut() {}
}
