package battlecode.client.viewer.render;

import battlecode.client.viewer.AbstractAnimation;
import java.awt.Dimension;

import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.AbstractDrawState;
import battlecode.client.viewer.BufferedMatch;
import battlecode.client.viewer.DebugState;
import battlecode.client.viewer.GameStateTimeline;
import battlecode.client.viewer.InfoPanel;
import battlecode.client.viewer.MatchPlayer;
import battlecode.common.Team;

public abstract class BaseRenderer {

  public abstract GameStateTimeline<? extends AbstractDrawState> getTimeline();

  public abstract AbstractDrawState getDrawState();

  public abstract void setMatchStarter(Runnable starter);

  public abstract void setCanvasSize(Dimension dim);

  public abstract void beginIntroCutScene(long targetMillis);

  public abstract void setCutSceneVisible(boolean visible);

  public abstract void fadeOutCutScene();
	
  public abstract void resetMatches();
	
  public abstract void addWin(Team t);

  public abstract AbstractDrawObject<AbstractAnimation> getRobotByID(int id);

  public abstract void setDebugState(DebugState dbg);

  public abstract DebugState getDebugState();

  //public abstract BufferedMatch getMatch();

  protected void skipRounds(int rounds) {
    GameStateTimeline timeline = getTimeline();
    int newRound = Math.max(Math.min(timeline.getRound()+rounds,timeline.getNumRounds()), 0);
    timeline.setRound(newRound);
  }

  protected final void toggleFastForward() {
    MatchPlayer.getCurrent().speedup();
    //MatchPlayer.getCurrent().toggleFastForward();
  }

  protected final void toggleSlowDown() {
    MatchPlayer.getCurrent().slowdown();
  }

  @SuppressWarnings("empty")
  public void handleAction(char actionCommand) {

    actionCommand = Character.toUpperCase(actionCommand);

    switch (actionCommand) {
    case 'A':
      RenderConfiguration.toggleSpawnRadii();
      break;
    case 'B':
      RenderConfiguration.toggleBroadcast();
      break;
    case 'D':
      RenderConfiguration.toggleDiscrete();
      break;
    case 'E':
      RenderConfiguration.toggleEnergon();
      break;
    case 'F':
      toggleFastForward();
      break;
    case 'G':
      RenderConfiguration.toggleGridlines();
      break;
    case 'H':
      RenderConfiguration.toggleActionLines();
      break;
    case 'I':
      skipRounds(-50);
      break;
    case 'J':
      toggleSlowDown();
      break;
    case 'K':
      RenderConfiguration.toggleAttack();
      break;
    case 'M':
      RenderConfiguration.toggleAmbientMusic();
      break;
    case 'O':
      RenderConfiguration.toggleShowHats();
      break;
    case 'R':
      RenderConfiguration.toggleRangeHatch();
      break;
    case 'S':
      skipRounds(100);
      break;
    case 'T':
      RenderConfiguration.toggleSupplyTransfers();
      break;
    case 'U':
      RenderConfiguration.toggleCows();
      break;
    case 'V':
      RenderConfiguration.toggleIndicatorDots();
      break;
    case 'X':
      RenderConfiguration.toggleExplosions();
      break;
    default:
      return;
    }
  }

}
