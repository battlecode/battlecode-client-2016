package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;
import battlecode.client.viewer.BufferedMatch;
import battlecode.client.viewer.AbstractDrawObject.RobotInfo;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

class DrawHUD {

	private static final int numArchons = 1;
	private static final float slotSize = 0.8f / (numArchons + 1);
	private static final Font footerFont;

	private static final ImageFile bg = new ImageFile("art/hud_bg.png");
	private static final ImageFile unitUnder = new ImageFile("art/hud_unit_underlay.png");
	private static final ImageFile gameText = new ImageFile("art/game.png");
	private static ImageFile numberText;
	private static BufferedImage[] numbers;
	private static BufferedImage negativeSign;
	private static BufferedMatch match;

	//  private static final RobotType[] drawnTypes = new RobotType[] {
	//    RobotType.BEAVER, 
	//    
	//    RobotType.SOLDIER,
	//    RobotType.BASHER,
	//    RobotType.TANK,
	//    
	//    RobotType.DRONE,
	//    RobotType.LAUNCHER,
	//    RobotType.MISSILE,
	//    
	//    RobotType.MINER,
	//    
	//    RobotType.COMPUTER,  
	//    RobotType.COMMANDER,
	//
	//    RobotType.TOWER,
	//    RobotType.SUPPLYDEPOT,
	//    
	//    RobotType.BARRACKS,
	//    RobotType.TANKFACTORY,
	//        
	//    RobotType.HELIPAD,
	//    RobotType.AEROSPACELAB,
	//    
	//    RobotType.HANDWASHSTATION,
	//    
	//    RobotType.MINERFACTORY,
	//        
	//    RobotType.TECHNOLOGYINSTITUTE, 
	//    RobotType.TRAININGFIELD,    
	//  };


	// [team][types]
	private static final Map<Team, Map<RobotType, ImageFile>> rImages = new HashMap<Team, Map<RobotType, ImageFile>>();

	static {
		negativeSign = (new ImageFile("art/negative.png")).image;
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

		for (Team t : new Team[]{Team.NEUTRAL, Team.A, Team.B}){
			rImages.put(t, new HashMap<RobotType, ImageFile>());
			for (RobotType rt: RobotType.values()){
				rImages.get(t).put(rt, new ImageFile("art/" + rt.toString().toLowerCase() + (t == Team.NEUTRAL ? "1" : (t == Team.A ? "1" : "2")) + ".png"));
			}
		}
	}
	private final DrawState ds;
	private final Team team;
	private final Rectangle2D.Float bgFill = new Rectangle2D.Float(0, 0, 1, 1);
	private float width;
	private float spriteScale;
	private String footerText = "";
	private static final AffineTransform textScale =
			AffineTransform.getScaleInstance(1 / 64.0, 1 / 64.0);
	private static final AffineTransform textScaleSmall =
			AffineTransform.getScaleInstance(1 / 256.0, 1 / 256.0);

	public DrawHUD(DrawState ds, Team team, BufferedMatch match) {
		this.ds = ds;
		this.team = team;
		DrawHUD.match = match;
		setRatioWidth(2.0f / 9.0f);
	}

	public float getRatioWidth() {
		return width;
	}

	public void setRatioWidth(float widthToHeight) {
		bgFill.width = width = widthToHeight;
		spriteScale = Math.min(slotSize / 2.5f, width / 2);
	}

	public void setPointsText(int value) {
	}

	public void setFooterText(String text) {
		footerText = text;
	}
	// stuff for win display
	int aWins = 0, bWins = 0;

	public void setWins(int a, int b) {
		aWins = a;
		bWins = b;
	}

	public void draw(Graphics2D g2) {
		AffineTransform trans = AffineTransform.getScaleInstance(bgFill.width, bgFill.height);
		BufferedImage bgImg = bg.image;
		trans.scale(1.0 / bgImg.getWidth(), 1.0 / bgImg.getHeight());
		//g2.drawImage(bgImg, trans, null);

		drawFooter(g2);

		g2.translate(0.5f * (width - spriteScale),
				0.5f * (slotSize - spriteScale));

		try {
			double sixth = 1.0 / 6;
			DrawObject hq = ds.getHQ(team);
			drawRobot(g2,hq, 1.0, -2.5 * sixth, (1 + 1.5 * sixth), false);
			ArrayList<DrawObject> towers = new ArrayList<DrawObject>();
			towers.addAll(ds.getTowers(team).values());
			for(int i = 0; i < towers.size() - 1; i++) {
				drawRobot(g2, towers.get(i), sixth, 2.0, 0, false);
			}
			if (towers.size() > 0) {
				drawRobot(g2, towers.get(towers.size() - 1), sixth,
						-2 * (towers.size() - 2.25), 2.5, false);
			}
			
			drawTeamResource(g2, hq);
		} catch (ConcurrentModificationException e) {
			e.printStackTrace();
		}    
		g2.translate(0,-0.3);
		drawCount(g2);
	}

	private void drawFooter(Graphics2D g2){
		AffineTransform pushed = g2.getTransform();

		g2.translate(width / 2, 0.9);
		g2.scale(width / 4.5, width / 4.5);
		AffineTransform pushed2 = g2.getTransform();

		//draw team name
		g2.translate(-1.875, -1);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setFont(footerFont);

		g2.translate(width / 2, .9);
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

		// possibly use alternate font
		boolean isASCII = true;
		for (int i = 0; i < teamName.length(); ++i) {
			if ((int) teamName.charAt(i) >= 128) {
				isASCII = false;
			}
		}
		if (!isASCII) {
			g2.setFont(new Font("Monospaced", Font.BOLD, 12).deriveFont(14.f));
		}
		g2.drawString(teamName, 0, 0);


		g2.setTransform(pushed2);

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

		g2.setTransform(pushed);
	}

	public void drawCount(Graphics2D g2){       
		AffineTransform pushed = g2.getTransform();
		{
			g2.scale(spriteScale, spriteScale);

			AffineTransform pushed2 = g2.getTransform();
			g2.translate(-0.5,0);     

			{
				for (RobotType rt : ds.getAppearedUnitTypes(team)){
					drawTypeCount(g2, rImages.get(team).get(rt), ds.getRobotTypeCount(team, rt)); 
				}

				g2.setTransform(pushed2);
				g2.translate(0.5, 0);
				for (RobotType rt : ds.getAppearedBuildingTypes(team)){
					if (rt != RobotType.COMMANDER)
						drawTypeCount(g2, rImages.get(team).get(rt), ds.getRobotTypeCount(team, rt)); 
				}
			}
		}  

		g2.setTransform(pushed);

	}

	public void drawTypeCount(Graphics2D g2, ImageFile image, int number){ 	
		BufferedImage under = unitUnder.image;
		AffineTransform trans = new AffineTransform();
		trans.scale(.32/under.getWidth(), .32/under.getHeight());
		g2.drawImage(under, trans, null);

		BufferedImage img = image.image;
		trans = new AffineTransform();
		trans.scale(.3/img.getWidth(), .3/img.getHeight());
		g2.drawImage(img, trans, null);

		AffineTransform pushed = g2.getTransform();
		g2.translate(0.4, 0.025);
		String numString = String.format("%03d", number);
		for (int i = 0; i < 3; i++) {
			int digit = Integer.decode(numString.substring(i, i + 1));
			if((digit != 0) || (i == 1 && number >= 100) || (i == 2 && number >= 10)) {
				g2.drawImage(numbers[digit],
						textScaleSmall, null);
			}
			g2.translate(0.75/4, 0);
		}
		g2.setTransform(pushed);
		g2.translate(0, .35);
	}

	public void drawRobot(Graphics2D g2, DrawObject r, double size,
			double right, double down, boolean drawXp) {
		AffineTransform pushed = g2.getTransform();
		{
			g2.scale(spriteScale * size, spriteScale * size);
			AffineTransform pushed2 = g2.getTransform();
			{
				BufferedImage underImg = unitUnder.image;
				g2.translate(-0.5, -0.5);
				g2.scale(2.0 / underImg.getWidth(), 2.0 / underImg.getHeight());
				g2.drawImage(underImg, null, null);
			}
			g2.setTransform(pushed2);
			if (r!=null && r.isAlive()) {
				r.drawImmediate(g2, false, true, false, drawXp);
			} else {
				ImageFile boom = new ImageFile("art/explode/explode64_f05.png");
				DrawObject.drawImageTransformed(g2, new AffineTransform(),
						boom.image, 1.0);
			}
		}
		g2.setTransform(pushed);
		g2.translate(spriteScale * size * right,
				spriteScale * size * down);
	}

	public void drawTeamResource(Graphics2D g2, DrawObject r) {
		if (r==null) return;
		AffineTransform pushed = g2.getTransform();
		double slotHeight;
		{
			g2.scale(spriteScale, spriteScale);
			AffineTransform pushed2 = g2.getTransform();
			{
				BufferedImage underImg = unitUnder.image;
				int maxHeight = (int)(.5 * underImg.getHeight());
				g2.translate(-0.5, 0.0);
				g2.scale(2.0 / underImg.getWidth(), 1.0 / underImg.getHeight());
				if (r.getTeam() == Team.A) g2.setColor(Color.red);
				else g2.setColor(Color.blue);
				double percent = Math.min(ds.getTeamResources(r.getTeam())/5000, 1.0);
				int height = (int)(maxHeight * percent);
				g2.fillRect(0, maxHeight-height, underImg.getWidth()/2, height);
				
				g2.setTransform(pushed2);			
				String resource = (int)(ds.getTeamResources(r.getTeam())%10000)+"";
				if (resource.charAt(0) != '-') {
					while (resource.length() < 4) resource = "0"+resource;
				} else {
					resource = resource.substring(1);
					while (resource.length() < 3) resource = "0"+resource;
					resource = "-" + resource;
				}
				g2.translate(-.3, .5);
				for (int i = 0; i < 4; i++) {
					if (resource.substring(i, i + 1).equals("-")) {
						g2.drawImage(negativeSign, textScaleSmall, null);
					} else {
						g2.drawImage(numbers[Integer.decode(resource.substring(i, i + 1))], textScaleSmall, null);
					}
					g2.translate(0.75/4, 0);
				}
			}
			
			g2.setTransform(pushed2);
			{
				BufferedImage underImg = unitUnder.image;
				int maxHeight = (int)(.5 * underImg.getHeight());
				g2.translate(0.5, 0.0);
				g2.scale(2.0 / underImg.getWidth(), 1.0 / underImg.getHeight());
				if (r.getTeam() == Team.A) g2.setColor(Color.red);
				else g2.setColor(Color.blue);
				double percent = Math.min(ds.getTeamStrength(r.getTeam())/1000.0, 1.0);
				int height = (int)(maxHeight * percent);
				g2.fillRect(0, maxHeight-height, underImg.getWidth()/2, height);
				
				g2.setTransform(pushed2);		
				g2.translate(1, 0.0);
				String resource = (int)(ds.getTeamStrength(r.getTeam())%10000)+"";
				if (resource.charAt(0) != '-') {
					while (resource.length() < 4) resource = "0"+resource;
				} else {
					resource = resource.substring(1);
					while (resource.length() < 3) resource = "0"+resource;
					resource = "-" + resource;
				}
				g2.translate(-.3, .5);
				for (int i = 0; i < 4; i++) {
					if (resource.substring(i, i + 1).equals("-")) {
						g2.drawImage(negativeSign, textScaleSmall, null);
					} else {
						g2.drawImage(numbers[Integer.decode(resource.substring(i, i + 1))], textScaleSmall, null);
					}
					g2.translate(0.75/4, 0);
				}
			}
			
			
			
		}
		g2.setTransform(pushed);
		g2.translate(0, .4);
	}

}
