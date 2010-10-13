package battlecode.client.viewer;

import battlecode.world.GameMap;

public interface GameStateFactory<E extends GameState> {

  public abstract E createState(GameMap map);

  public abstract E cloneState(E state);

  public abstract void copyState(E src, E dst);
}
