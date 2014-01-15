package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;
import battlecode.client.viewer.BufferedMatch;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

class DrawHUD {
  // space between hud items in units of TBD
  private static final float slotSize = .4f;
  // for the names of teams
  private static final Font footerFont;
  // percent of client window that is  hud
  private static final float clientRatio = 2.0f / 9.0f;

  private static final ImageFile bg = new ImageFile("art/hud_bg.png");
  private static final ImageFile gameText = new ImageFile("art/game.png");
  private static ImageFile numberText;
  private static BufferedImage[] numbers;
  private static BufferedMatch match;
  private ImageFile avatar;

  private static final RobotType[] drawnTypes = new RobotType[] {
    RobotType.SOLDIER,
    //RobotType.WALL, not currently in the game
    RobotType.PASTR,
    RobotType.NOISETOWER,
  };
  // [team][types]
  private static final ImageFile [][] rImages = new ImageFile[3][3];		

  static {
    numberText = new ImageFile("art/numbers.png");
    numbers = new BufferedImage[10];
    for (int i = 0; i < 10; i++) {
      try {
        numbers[i] = numberText.image.getSubimage(48 * i, 0, 48, 64);
      } catch (NullPointerException e) {
        // e.printStackTrace();
      }
    }
    Font font;
    try {
      font = Font.createFont(Font.TRUETYPE_FONT,new File("art/computerfont.ttf")).deriveFont(14.f);
    } catch(Exception e) {
      font = new Font("Serif",Font.PLAIN,18);
    }
    footerFont = font;

    for (Team t : new Team[]{Team.NEUTRAL, Team.A, Team.B})
      for (int x=0; x<drawnTypes.length; x++)
      {
        RobotType rt = drawnTypes[x];
        rImages[t.ordinal()][x] = new ImageFile("art/" + rt.toString().toLowerCase() + (t == Team.NEUTRAL ? "0" : (t == Team.A ? "1" : "2")) + ".png");
      }
  }
  private final DrawState ds;
  private final Team team;
  private final Rectangle2D.Float bgFill = new Rectangle2D.Float(0, 0, 1, 1);
  private float width;
  private float spriteScale;
  private String footerText = "";
  private int points = 0;
  // would like to know where these numbers came from
  private static final AffineTransform textScale =
    AffineTransform.getScaleInstance(1 / 64.0, 1 / 64.0);
  private static final AffineTransform textScaleSmall =
    AffineTransform.getScaleInstance(1 / 256.0, 1 / 256.0);
  public static final float textSmallWidth = .75f/4;
  public DrawHUD(DrawState ds, Team team, BufferedMatch match) {
    this.ds = ds;
    this.team = team;
		this.match = match;
    setRatioWidth(clientRatio);
  }

  public float getRatioWidth() {
    return width;
  }

  public void setRatioWidth(float widthToHeight) {
    bgFill.width = width = widthToHeight;
    spriteScale = Math.min(slotSize / 2.5f, width / 2);
  }

  public void setPointsText(int value) {
    points = value;
  }

  public void setFooterText(String text) {
    footerText = text;
  }
  // stuff for win display
  public int aWins = 0, bWins = 0;

  public void setWins(int a, int b) {
    aWins = a;
    bWins = b;
  }

	public void tryLoadAvatar() {
		if(avatar==null) {
			String teamName = team==Team.A ? match.getTeamA() : match.getTeamB();
			if(teamName!=null) {
				avatar = new ImageFile("avatars/" + teamName+".png");
			}
		}
	}

  public void draw(Graphics2D g2) {
    AffineTransform trans = AffineTransform.getScaleInstance(bgFill.width, bgFill.height);
    BufferedImage bgImg = bg.image;
    trans.scale(1.0 / bgImg.getWidth(), 1.0 / bgImg.getHeight());
    g2.drawImage(bgImg, trans, null);
    AffineTransform pushed = g2.getTransform();
    // draw the names
    {
      g2.translate(width / 2, 0.9);
      g2.scale(width / 4.5, width / 4.5);
			AffineTransform pushed2 = g2.getTransform();
			tryLoadAvatar();
			if(avatar!=null&&avatar.image!=null && 1 == 0) {
				g2.setTransform(pushed);
				g2.translate(0.5f * (width - spriteScale), 0.5f * (slotSize - spriteScale)+7*slotSize);
				g2.scale(spriteScale,spriteScale);
				g2.translate(-.5,-.5);
				g2.scale(2.0/avatar.image.getWidth(),2.0/avatar.image.getHeight());
				g2.drawImage(avatar.image,null,null);
			} else {
				g2.translate(-1.875, -1);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setFont(footerFont);
				g2.translate(width / 2, .9);
				//g2.scale(width / 4.5, width / 4.5);
				FontMetrics fm = g2.getFontMetrics();
				String teamName;
				double scaleAmount = 4.5;
				if (team == Team.A) {
					g2.setColor(Color.RED);
					teamName = "Team A";
					if (match.getTeamA() != null) {
            teamName = DrawCutScene.getTeamName(match.getTeamA());
            scaleAmount = fm.stringWidth(teamName) / 16.0;
					}
				} else {
					assert team == Team.B;
					g2.setColor(Color.BLUE);
					teamName = "Team B";
					if (match.getTeamB() != null) {
            teamName = DrawCutScene.getTeamName(match.getTeamB());
            scaleAmount = fm.stringWidth(teamName) / 16.0;
					}
				}
				scaleAmount = Math.max(scaleAmount, 4.5);
				g2.scale(width / scaleAmount, width / scaleAmount);
				g2.drawString(teamName, 0, 0);
      }
      g2.setTransform(pushed2);
      
      // match information
      if (footerText.startsWith("GAME")) { // Game Number
        g2.translate(-2, 0);
        g2.drawImage(gameText.image, textScale, null);

        // if team A won more than one round, give it a red circle
        if (aWins > 0) {
          g2.translate(0.f, 1.25f);
          g2.setColor(Color.RED);
          g2.fillOval(0, 0, 1, 1);
          g2.translate(0.f, -1.25f);
        }

        g2.translate(3, 0);
        for (int i = 5; i < footerText.length(); i++) {
          g2.drawImage(numbers[Integer.decode(footerText.substring(i, i + 1))], textScale, null);
          g2.translate(0.5, 0);
        }
      } else if (footerText.length() == 4) { // round counter
        // if team B won more than one round, give it a blue circle
        if (bWins > 0) {
          // damn yangs magic offsets -_-
          g2.translate(0.75f, 1.25f);
          g2.setColor(Color.BLUE);
          g2.fillOval(0, 0, 1, 1);
          g2.translate(-0.75f, -1.25f);
        }

        g2.translate(-1.5, 0);
        for (int i = 0; i < 4; i++) {
          g2.drawImage(numbers[Integer.decode(footerText.substring(i, i + 1))], textScale, null);
          g2.translate(0.75, 0);
        }
      }
    }
    g2.setTransform(pushed);
    g2.translate(0.5f * (width - spriteScale),
                 0.5f * (slotSize - spriteScale));
    try {
      DrawObject hq = ds.getHQ(team);
      drawRobot(g2,hq);
      drawTeamResource(g2, hq);
    } catch (ConcurrentModificationException e) {
			e.printStackTrace();
    }
  }

  // draws the robots at the top then translates down
	public void drawRobot(Graphics2D g2, DrawObject r) {
    AffineTransform pushed = g2.getTransform();
    g2.scale(spriteScale, spriteScale);
    AffineTransform pushed2 = g2.getTransform();
    
    g2.setTransform(pushed2);
    if (r!=null) r.drawImmediateNoScale(g2, false);

    g2.setTransform(pushed);
    g2.translate(0, slotSize);
	}
	
	public void drawTeamResource(Graphics2D g2, DrawObject r) {
		if (r==null) return;
    AffineTransform pushed = g2.getTransform();
    g2.scale(spriteScale, spriteScale);
    BufferedImage underImg = bg.image;
    // milk points
    AffineTransform pushed2 = g2.getTransform();
    g2.translate(-0.5, -0.5); // unknown reason, but works
    if (r.getTeam() == Team.A) g2.setColor(Color.red);
    else g2.setColor(Color.blue);
    float percent = Math.min((float)ds.getTeamResources(r.getTeam())/(float)GameConstants.WIN_QTY, 1.0f);
    g2.fill(new Rectangle2D.Float(0, 1.0f - percent, 2.0f, percent));
    g2.setColor(Color.white);
    g2.fill(new Rectangle2D.Float(0, 0, 2.0f, .05f));
    g2.setTransform(pushed2); //reset after weird translate
      
    String resource = (int)(ds.getTeamResources(r.getTeam()))+"";
    while (resource.length() < 8) resource = "0"+resource;
    g2.translate(-.3, .5);
    for (int i = 0; i < 8; i++) {
      g2.drawImage(numbers[Integer.decode(resource.substring(i, i + 1))], textScaleSmall, null);
      g2.translate(textSmallWidth, 0);
    }
    g2.setTransform(pushed2); // reset after text			

    g2.translate(-0.5, -2.0); // move under the milk resource
    g2.translate(0.1, 0);
    
    int[] counts = ds.getRobotCounts(r.getTeam());
    for (int x=0; x<drawnTypes.length; x++)
    {
      BufferedImage target = rImages[r.getTeam().ordinal()][x].image;
      // assume a non-square sprite means a sprite sheet of squares
      if (target.getWidth() != target.getHeight()) {
        target = target.getSubimage(0, 0, target.getHeight(), target.getHeight());
      }
      AffineTransform trans = AffineTransform.getTranslateInstance(0,0);
      trans.scale(0.4 / target.getWidth(), 0.4 / target.getHeight());
      g2.drawImage(target, trans, null);
				
      String number = counts[drawnTypes[x].ordinal()]+"";
      while (number.length() < 3) number = "0"+number;
      g2.translate(0.0, 0.4);
      for (int i = 0; i < 3; i++) {
        g2.drawImage(numbers[Integer.decode(number.substring(i, i + 1))], textScaleSmall, null);
        g2.translate(textSmallWidth, 0);
      }
      g2.translate(-textSmallWidth*3, 0);

      g2.translate(0.65, -0.4);
      if (x == 2)
        g2.translate(-1.95, 0.7);
    }
			
    g2.setTransform(pushed);
  }
}
