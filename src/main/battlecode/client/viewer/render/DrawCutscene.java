package battlecode.client.viewer.render;

import battlecode.common.Team;
import battlecode.client.util.ImageFile;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.Timer;

class DrawCutScene {

	public enum Step { INTRO, GAME, OUTRO, NEXT }
	public Step step = Step.INTRO;

	private final Rectangle2D.Float rect = new Rectangle2D.Float();

	private Color darkMask = new Color(0, 0, 0, 0.75f);
	private float fade = 0.75f;
	private Timer fadeTimer;

	private static final ImageFile imgVersus = new ImageFile("art/overlay_vs.png");
	private static final ImageFile imgWinnerLabel = new ImageFile("art/overlay_win.png");
	private final ImageFile imgTeamA, imgTeamB;
	private ImageFile imgWinner;

	private long targetEnd;
	private boolean visible = false;

	public DrawCutScene(float width, float height, String teamA, String teamB) {
		rect.width  = width;
		rect.height = height;
		imgTeamA = new ImageFile("team-names/a/" + teamA + "-r.png");
		imgTeamB = new ImageFile("team-names/b/" + teamB + "-l.png");
	}

	public void setTargetEnd(long millis) {
		targetEnd = millis;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void setWinner(Team team) {
		imgWinner = (team == Team.A ? imgTeamA : imgTeamB);
	}

	public void draw(Graphics2D g2) {
		if (visible) {
			switch (step) {
			case INTRO: drawIntro(g2); break;
			case OUTRO: drawOutro(g2); break;
			default: break;
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

	private void drawIntro(Graphics2D g2) {
		AffineTransform pushed = g2.getTransform(); {
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
		} g2.setTransform(pushed);
	}

	private void drawOutro(Graphics2D g2) {
		AffineTransform pushed = g2.getTransform(); {
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
		} g2.setTransform(pushed);
	}

	public void fadeOut() {
		final long startTime = System.currentTimeMillis();
		final float startFade = fade;
		fadeTimer = new Timer(30, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fade = startFade + (System.currentTimeMillis() - startTime)/5000.0f;
				if (fade >= 1) {
					fade = 1;
					fadeTimer.stop();
					fadeTimer = null;
				}
			}
		});
		fadeTimer.start();
	}

	protected void finalize() throws Throwable {
		try {
			imgTeamA.unload(); //TODO: switch over to weak references
			imgTeamB.unload();
		}
		finally {
			super.finalize();
		}
	}
}
