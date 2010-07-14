package battlecode.client.viewer;

import battlecode.serial.MatchHeader;

public interface GameStateFactory<E extends GameState> {

  public abstract E createState(MatchHeader header);

  public abstract E cloneState(E state);

  public abstract void copyState(E src, E dst);
}
