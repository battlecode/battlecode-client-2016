package battlecode.client.viewer.render;

import battlecode.client.viewer.AbstractAnimation;
import java.awt.Dimension;

import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.GameStateTimeline;
import battlecode.client.viewer.InfoPanel;
import battlecode.common.Team;

public abstract class BaseRenderer {

	public abstract GameStateTimeline getTimeline();

	public abstract void setMatchStarter(Runnable starter);

	public abstract void setCanvasSize(Dimension dim);

	//public abstract void setInfoPanel(InfoPanel panel);

	public abstract void beginIntroCutScene(long targetMillis);

	public abstract void setCutSceneVisible(boolean visible);

	public abstract void fadeOutCutScene();

	//public abstract void doRepaint();
	
	public abstract void resetMatches();
	
	public abstract void addWin(Team t);

	public abstract AbstractDrawObject<AbstractAnimation> getRobotByID(int id);

	public abstract void setDebugState(battlecode.client.viewer.DebugState dbg);

	protected abstract boolean trySkipRounds(int rounds);

	protected abstract void toggleFastForward();

	@SuppressWarnings("empty")
	public void handleAction(char actionCommand) {

        switch (actionCommand) {
		case 'A':
		case 'a':
			RenderConfiguration.toggleRangeHatch();
		break;
		case 'B':
		case 'b':
			RenderConfiguration.toggleBroadcast();
		break;
		case 'C':
		case 'c':
			RenderConfiguration.toggleDetonates();
		break;
		case 'D':
		case 'd':
			RenderConfiguration.toggleDiscrete();
		break;
		case 'E':
		case 'e':
			RenderConfiguration.toggleEnergon();
		break;
		case 'G':
		case 'g':
			RenderConfiguration.toggleGridlines();
		break;
		case 'H':
		case 'h':
			RenderConfiguration.toggleDrawHeight();
		break;
		case 'N':
		case 'n':
			RenderConfiguration.toggleBlocks();
		break;
		case 'P':
		case 'p':
			RenderConfiguration.toggleTeleport();
		break;
		case 'Q':
		case 'q':
			RenderConfiguration.toggleTeleportGhosts();
		break;
		case 'R':
		case 'r':
			RenderConfiguration.toggleSpawnRadii();
		break;
		case 'T':
		case 't':
			RenderConfiguration.toggleTransfers();
		break;
		case 'X':
		case 'x':
			RenderConfiguration.toggleExplosions();
		break;
		case 'F':
		case 'f':
			toggleFastForward();
		break;
		case 'S':
		case 's':
			// stupid Java compiler gives me a "not a statement" error if I take the if() out
			if(trySkipRounds(100)||trySkipRounds(50)||trySkipRounds(25));
		break;
		case 'M':
		case 'm':
			RenderConfiguration.toggleAmbientMusic();
		break;
		default:
			return;
        }
    }

}