package battlecode.client.viewer.renderer3d;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.Timer;

import battlecode.client.util.ImageFile;
import battlecode.client.viewer.render.DrawCutScene;
import battlecode.common.Team;

import com.sun.opengl.util.awt.TextRenderer;

class GLDrawCutScene {

    public enum Step {

        INTRO, GAME, OUTRO, NEXT
    }
    public Step step = Step.INTRO;
    private final Rectangle2D.Float rect = new Rectangle2D.Float();
    private Color darkMask = new Color(0, 0, 0, 0.75f);
    private float fade = 0.00f;
    private Timer fadeTimer;
    //private static final ImageFile imgVersus = new ImageFile("art/overlay_vs.png");
    //private static final ImageFile imgWinnerLabel = new ImageFile("art/overlay_win.png");
//	private final ImageFile imgTeamA, imgTeamB;
    private String teamAName, teamBName;
    //private ImageFile imgWinner;
    private String winningTeam;
    private long targetEnd;
    private boolean visible = false;
    private static String teamPath = null;
    private static Map<Integer, String> teamNames = null;

    public GLDrawCutScene(float width, float height, String teamA, String teamB) {

        rect.width = width;
        rect.height = height;

        // HACK: make this into a normal string parsing
        try {
			teamAName = DrawCutScene.getTeamName(teamA);
			teamBName = DrawCutScene.getTeamName(teamB);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public void setTargetEnd(long millis) {
        targetEnd = millis;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setWinner(Team team) {
        //imgWinner = (team == Team.A ? imgTeamA : imgTeamB);
        winningTeam = (team == Team.A ? teamAName : teamBName);
    }

    public void draw(GL2 gl, int width, int height, TextRenderer txt, FontMetrics fm) {
        if (visible) {
            switch (step) {
                case INTRO:
                    drawIntro(gl, width, height, txt, fm);
                    break;
                case OUTRO:
                    drawOutro(gl, width, height, txt, fm);
                    break;
                default:
                    break;
            }
        }
    }

    /*private void drawImage(BufferedImage img, Graphics2D g2) {
    if (img != null) {
    double scale = rect.width / img.getWidth();
    AffineTransform trans = AffineTransform.getScaleInstance(scale, scale);
    trans.translate(-img.getWidth() / 2, -img.getHeight() / 2);
    g2.drawImage(img, trans, null);
    }
    }*/
    private int findSpaceInMiddle(String s) {
        int l = s.length();

        String p1 = s.substring(0, l / 2);
        String p2 = s.substring(l / 2);

        int i = p2.indexOf(" ");
        if (i < 0)
            return p1.length();
        else
            return p1.length() + i;
    }

    private void drawIntro(GL2 gl, int width, int height, TextRenderer txt, FontMetrics fm) {
        if (txt != null) {
            float until = Math.max((targetEnd - System.currentTimeMillis()) / 1000.0f, 0);
            float horizontalOffset = 0.0f;
            txt.beginRendering(width, height);

            int aWidth = fm.stringWidth(teamAName);
            txt.setColor(1.0f, 0.0f, 0.0f, 1.0f);
            if (aWidth < width) {
                txt.draw(teamAName, width / 2 - aWidth / 2, height / 2 + fm.getHeight() / 2);
            } else {
                String part1, part2;
                int split = findSpaceInMiddle(teamAName);
                part1 = teamAName.substring(0, split);
                part2 = teamAName.substring(split);
                txt.draw(part2, width / 2 - fm.stringWidth(part2) / 2, height / 2 + fm.getHeight() / 2);
                txt.draw(part1, width / 2 - fm.stringWidth(part1) / 2, height / 2 + (3 * fm.getHeight()) / 2);
            }

            int bWidth = fm.stringWidth(teamBName);
            //txt.setColor(0.0f, 0.0f, 1.0f, 1.0f);
            txt.setColor(0.7f, 0.95f, 1.0f, 1.0f);
            if (bWidth < width) {
                txt.draw(teamBName, width / 2 - bWidth / 2, height / 2 - (int) (1.5 * fm.getHeight()));
            } else {
                String part1, part2;
                int split = findSpaceInMiddle(teamBName);
                part1 = teamBName.substring(0, split);
                part2 = teamBName.substring(split);
                txt.draw(part1, width / 2 - fm.stringWidth(part1) / 2, height / 2 - (3 * fm.getHeight()) / 2);
                txt.draw(part2, width / 2 - fm.stringWidth(part2) / 2, height / 2 - (5 * fm.getHeight()) / 2);
            }

            txt.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            txt.draw("VS", width / 2 - fm.stringWidth("VS") / 2, height / 2 - fm.getHeight() / 2);

            txt.endRendering();
        }
        /*AffineTransform pushed = g2.getTransform(); {
        float until = Math.max((targetEnd-System.currentTimeMillis())/1000.0f, 0);
        float horizontalOffset = 2 * rect.width * until;
        float avatarOffset = rect.width/4;
        g2.translate(rect.width/2 - horizontalOffset - avatarOffset, rect.height/3);
        drawImage(imgTeamA.image, g2);
        g2.translate(horizontalOffset + avatarOffset, rect.height/6);
        if (until < 0.1f) {
        g2.scale(0.1, 0.1);
        drawImage(imgVersus.image, g2);
        g2.scale(10, 10);
        }
        g2.translate(horizontalOffset + avatarOffset, rect.height/6);
        drawImage(imgTeamB.image, g2);
        } g2.setTransform(pushed);*/
    }

    private void drawOutro(GL2 gl, int width, int height, TextRenderer txt, FontMetrics fm) {

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, width, 0, height, -10, 10);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glEnable(GL2.GL_BLEND);
        gl.glColor4f(0.0f, 0.0f, 0.0f, fade);
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glVertex3i(0, 0, 5);
        gl.glVertex3i(width, 0, 5);
        gl.glVertex3i(width, height, 5);
        gl.glVertex3i(0, height, 5);
        gl.glEnd();

        if (txt != null && winningTeam != null) {
            txt.beginRendering(width, height);

            if (winningTeam == teamAName)
                txt.setColor(1.0f, 0.0f, 0.0f, 1.0f);
            else
                txt.setColor(0.7f, 0.95f, 1.0f, 1.0f);
            //txt.setColor(0.0f, 0.0f, 1.0f, 1.0f);

            int stringLength = fm.stringWidth(winningTeam);
            if (stringLength > width) {
                String part1, part2;
                int split = findSpaceInMiddle(winningTeam);
                part1 = winningTeam.substring(0, split);
                part2 = winningTeam.substring(split);
                txt.draw(part1, width / 2 - fm.stringWidth(part1) / 2, height / 2 + fm.getHeight());
                txt.draw(part2, width / 2 - fm.stringWidth(part2) / 2, height / 2);
            } else {
                txt.draw(winningTeam, width / 2 - fm.stringWidth(winningTeam) / 2, height / 2);
            }
            txt.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            txt.draw("WINS!", width / 2 - fm.stringWidth("WINS!") / 2, height / 2 - fm.getHeight());
            txt.endRendering();
        }
        /*AffineTransform pushed = g2.getTransform(); {
        g2.setColor(new Color(0, 0, 0, fade));
        g2.fill(rect);
        //g2.setColor(Color.WHITE);
        g2.translate(rect.width/2, rect.height/3);
        g2.scale(0.5, 0.5);
        drawImage(imgWinnerLabel.image, g2);
        g2.scale(2, 2);
        if (imgWinner == imgTeamA) {
        g2.translate(-rect.width/5, rect.height/4);
        }
        else {
        g2.translate(rect.width/5, rect.height/4);
        }
        //g2.drawImage(imgWinnerLabel.image,
        //g2.drawString("WINNER:", (int) rect.width/2 - 3, (int) rect.height/2 - 2);
        //g2.translate(0, rect.height/3);
        drawImage(imgWinner.image, g2);
        } g2.setTransform(pushed);*/
    }

    public void fadeOut() {
        final long startTime = System.currentTimeMillis();
        final float startFade = fade;
        fadeTimer = new Timer(30, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fade = startFade + (System.currentTimeMillis() - startTime) / 2000.0f;
                if (fade >= 1) {
                    fade = 1;
                    fadeTimer.stop();
                    fadeTimer = null;
                }
            }
        });
        fadeTimer.start();
    }

    /*protected void finalize() throws Throwable {
    try {*/
    /*imgTeamA.unload(); //TODO: switch over to weak references
    imgTeamB.unload();*/
    /*}
    finally {
    super.finalize();
    }
    }*/
}
