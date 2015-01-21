package battlecode.client.viewer.render;

import battlecode.common.Team;
import battlecode.client.util.ImageFile;
import battlecode.serial.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Timer;

import java.util.Collections;

public class DrawCutScene {

    public enum Step {

        INTRO, GAME, OUTRO, NEXT
    }
    public Step step = Step.INTRO;
    private final Rectangle2D.Float rect = new Rectangle2D.Float();
    private Color darkMask = new Color(0, 0, 0, 0.75f);
    private volatile float fade = 0.75f;
    private volatile Timer fadeTimer;
    private static final ImageFile imgVersus = new ImageFile("art/overlay_vs.png");
    private static final ImageFile imgWinnerLabel = new ImageFile("art/overlay_win.png");
    private final ImageFile imgTeamA, imgTeamB;
    private final String teamA, teamB;
    private final String mapName;
    private ImageFile imgWinner;
    private String winner;
    private Color winnerColor;
    private DominationFactor dom = null;
	private static final Color neutralColor = Color.WHITE;
	private static final Color backgroundColor = Color.BLACK;
	private static final Color teamAColor = Color.RED;
	private static final Color teamBColor = Color.BLUE;
	private volatile long targetEnd;
    private volatile boolean visible = false;
    private static String teamPath = null;
    private static Map<Integer, String> teamNames = Collections.emptyMap();
	private Font font;

	public static void setTeamNames(Map<Integer,String> names) {
		System.out.println(names.entrySet().size());
		teamNames = names;
	}

	public static String getTeamName(String genericName) {
		try {
			int id = Integer.parseInt(genericName.substring(4,7));
			if (teamNames.containsKey(id))
				return teamNames.get(id);
			else
				return genericName;
		} catch(Exception e) {
			return genericName;
		}
	}

    public DrawCutScene(float width, float height, String teamA, String teamB, String mapName) {

        rect.width = width;
        rect.height = height;
        System.out.println("&&&&&&&&&&&&&&& " + teamA + " " + teamB);
        this.teamA = getTeamName(teamA);
		this.teamB = getTeamName(teamB);
        this.mapName = mapName;
		try {
			font = Font.createFont(Font.TRUETYPE_FONT,new File("art/computerfont.ttf")).deriveFont(48.f);
		} catch(Exception e) {
			throw new RuntimeException("Failed to load font",e);
		}
		imgTeamA = new ImageFile("avatars/" + teamA+".png");
		imgTeamB = new ImageFile("avatars/" + teamB+".png");
    }

    public void setTargetEnd(long millis) {
        targetEnd = millis;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setWinner(Team team) {
		if(team==Team.A) {
			imgWinner = imgTeamA;
			winner = teamA;
			winnerColor = teamAColor;
		}
		else {
			imgWinner = imgTeamB;
			winner = teamB;
			winnerColor = teamBColor;
		}
    }

    public void setDominationFactor(DominationFactor dom) {
        this.dom = dom;
    }

    public void draw(Graphics2D g2) {
        //System.out.println("Cutscene Drawing");
        if (visible) {
            switch (step) {
                case INTRO:
                    //System.out.println("Drawing Intro");
                    drawIntro(g2);
                    break;
                case OUTRO:
                    //System.out.println("Drawing Outro");
                    drawOutro(g2);
                    break;
                default:
                    //System.out.println("Buh WHa?");
                    break;
            }
        }
    }

    private void drawImage(BufferedImage img, Graphics2D g2) {
        if (img != null) {
            double scale = rect.width / img.getWidth();
            AffineTransform trans = AffineTransform.getScaleInstance(scale, scale);
            trans.translate(-img.getWidth() / 2, -img.getHeight() / 2);
            g2.drawImage(img, trans, null);
        }
    }

	private void drawLogo(Graphics2D g2, BufferedImage img, double height) {
		Rectangle rect = g2.getDeviceConfiguration().getBounds();
		double x = rect.getWidth()/2 - 64;
		double y = height - 64;
		g2.drawImage(img,new AffineTransform(128./img.getWidth(),0,0,128./img.getHeight(),x,y),null);
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
		drawText.drawTwoLine(teamA,rect.getCenterX(),rect.getCenterY()-textHeight,true);
		g2.setColor(neutralColor);
		drawText.draw("VS",rect.getCenterX(),rect.getCenterY());
		g2.setColor(teamBColor);
		drawText.drawTwoLine(teamB,rect.getCenterX(),rect.getCenterY()+textHeight,false);
		if(imgTeamA.image!=null)
			drawLogo(g2,imgTeamA.image,rect.getCenterY()-3*textHeight-50);
		if(imgTeamB.image!=null)
			drawLogo(g2,imgTeamB.image,rect.getCenterY()+3*textHeight+50);

		g2.setColor(neutralColor);
        drawText.draw("map: " + mapName,rect.getCenterX(),rect.getCenterY()+3*textHeight+80+80+15);

		g2.setTransform(pushed);
    }

	private static class DrawText {

		private final Graphics2D g2;
		private final Font font;
		private final FontMetrics metrics;
		private final FontRenderContext renderContext;
		private final Rectangle boundingRect;

		public static int findSpaceInMiddle(String s) {
			int before = s.lastIndexOf(' ',s.length()/2);
			int after = s.indexOf(' ',s.length()/2);
			if(before==-1) return after;
			if(after==-1) return before;
			return before+after<s.length()?after:before;
		}

		public DrawText(Graphics2D g2, Font font) {
			this.g2 = g2;
			this.font = font;
			metrics = g2.getFontMetrics(font);
			renderContext = g2.getFontRenderContext();
			boundingRect = g2.getDeviceConfiguration().getBounds();
		}

		public void draw(String s, double centerx, double centery) {
			draw(s,(float)centerx,(float)centery);
		}
		
		public void draw(String s, float centerx, float centery) {
			GlyphVector glyphs = font.createGlyphVector(renderContext,s);

            // are there non-ascii characters?
            boolean isASCII = true;
            for (int i = 0; i < s.length(); ++i) {
                if ((int) s.charAt(i) >= 128) {
                    isASCII = false;
                }
            }
            if (!isASCII) {
                glyphs = (new Font("Monospaced", Font.BOLD, 12)).deriveFont(48.f).createGlyphVector(renderContext, s);
            }

			// Apparently the x,y coordinates given to drawGlyphVector are the bottom
			// right corner?
			g2.drawGlyphVector(glyphs,centerx-metrics.stringWidth(s)/2,centery+metrics.getHeight()/2);
		}

		public void drawTwoLine(String s, double centerx, double centery, boolean up) {
				if (s == null || s.length() == 0)
						s = "ERROR";
			drawTwoLine(s,(float)centerx,(float)centery,up);
		}

		public void drawTwoLine(String s, float centerx, float centery, boolean up) {
			int twidth = metrics.stringWidth(s), split;
			if(twidth<boundingRect.getWidth()||(split=findSpaceInMiddle(s))==-1)
				draw(s,centerx,centery);
			else {
				String part1 = s.substring(0,split);
				String part2 = s.substring(split+1);
				int height = metrics.getHeight();
				if(up) {
					draw(part1,centerx,centery-height);
					draw(part2,centerx,centery);
				}
				else {
					draw(part1,centerx,centery);
					draw(part2,centerx,centery+height);
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
		drawText.drawTwoLine(winner,rect.getCenterX(),rect.getCenterY()-textHeight/2,true);
		g2.setColor(neutralColor);
		drawText.draw("WINS!",rect.getCenterX(),rect.getCenterY()+textHeight/2);
		if(imgWinner != null && imgWinner.image!=null)
			drawLogo(g2,imgWinner.image,rect.getCenterY()-5*textHeight/2 - 80);

        String s = "?";
        if (dom == DominationFactor.DESTROYED)
            s = "DESTRUCTION";
        else if (dom == DominationFactor.PWNED)
            s = "more towers remaining";
        else if (dom == DominationFactor.OWNED)
            s = "more HQ health";
        else if (dom == DominationFactor.BEAT)
            s = "more TOWER health";
        else if (dom == DominationFactor.BARELY_BEAT)
            s = "SUPERIOR SANITATION";
        else if (dom == DominationFactor.BARELY_BARELY_BEAT)
            s = "more total ore value";
        else if (dom == DominationFactor.WON_BY_DUBIOUS_REASONS)
            s = "???";
        
		g2.setColor(winnerColor);
		drawText.draw("Reason: " + s,rect.getCenterX(),rect.getCenterY()+textHeight*5/2);
		g2.setTransform(pushed);
    }

    public void fadeOut() {
		fade = 0.f;
		/*
        final long startTime = System.currentTimeMillis();
        final float startFade = fade;
        fadeTimer = new Timer(40, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.out.println("timer "+Thread.currentThread());
				fade = startFade + (System.currentTimeMillis() - startTime) / 5000.0f;
                if (fade >= 1) {
                    fade = 1;
                    fadeTimer.stop();
                    fadeTimer = null;
                }
            }
        });
        fadeTimer.start();
		*/
    }

    protected void finalize() throws Throwable {
        try {
            //imgTeamA.unload(); //TODO: switch over to weak references
            //imgTeamB.unload();
        } finally {
            super.finalize();
        }
    }
}
