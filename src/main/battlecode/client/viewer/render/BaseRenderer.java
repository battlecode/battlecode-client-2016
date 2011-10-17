package battlecode.client.viewer.render;

import battlecode.client.viewer.AbstractAnimation;
import java.awt.Dimension;

import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.DebugState;
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

	public abstract void setDebugState(DebugState dbg);

	public abstract DebugState getDebugState();

	protected abstract boolean trySkipRounds(int rounds);

	protected abstract void toggleFastForward();

	@SuppressWarnings("empty")
	public void handleAction(char actionCommand) {

		actionCommand = Character.toUpperCase(actionCommand);

        switch (actionCommand) {
		case 'A':
			RenderConfiguration.toggleRangeHatch();
		break;
		case 'B':
			RenderConfiguration.toggleBroadcast();
		break;
		case 'C':
			RenderConfiguration.toggleDetonates();
		break;
		case 'D':
			RenderConfiguration.toggleDiscrete();
		break;
		case 'E':
			RenderConfiguration.toggleEnergon();
		break;
		case 'L':
			RenderConfiguration.toggleFlux();
		break;
		case 'G':
			RenderConfiguration.toggleGridlines();
		break;
		case 'H':
			RenderConfiguration.toggleDrawHeight();
		break;
		case 'N':
			RenderConfiguration.toggleBlocks();
		break;
		case 'P':
			RenderConfiguration.toggleTeleport();
		break;
		case 'Q':
			RenderConfiguration.toggleTeleportGhosts();
		break;
		case 'R':
			RenderConfiguration.toggleSpawnRadii();
		break;
		case 'T':
			RenderConfiguration.toggleTransfers();
		break;
		case 'X':
			RenderConfiguration.toggleExplosions();
		break;
		case 'F':
			toggleFastForward();
		break;
		case 'S':
			// stupid Java compiler gives me a "not a statement" error if I take the if() out
			if(trySkipRounds(100)||trySkipRounds(50)||trySkipRounds(25));
		break;
		case 'M':
			RenderConfiguration.toggleAmbientMusic();
		break;
		default:
			return;
        }
    }

}
