package battlecode.client.viewer.render;

import battlecode.client.viewer.*;
import battlecode.common.Team;

import java.awt.*;

public abstract class BaseRenderer {

    public abstract GameStateTimeline<DrawState>
    getTimeline();

    public abstract DrawState getDrawState();

    public abstract void setCanvasSize(Dimension dim);

    public abstract void beginIntroCutScene(long targetMillis);

    public abstract void setCutSceneVisible(boolean visible);

    public abstract void fadeOutCutScene();

    public abstract void resetMatches();

    public abstract void addWin(Team t);

    public abstract DrawObject getRobotByID(int id);

    public abstract void setDebugState(DebugState dbg);

    public abstract DebugState getDebugState();

    //public abstract BufferedMatch getMatch();

    protected void skipRounds(int rounds) {
        GameStateTimeline timeline = getTimeline();
        int newRound = Math.max(Math.min(timeline.getRound() + rounds,
                timeline.getNumRounds()), 0);
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
            case 'L':
                RenderConfiguration.toggleSupplyIndicators();
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
        }
    }

}
