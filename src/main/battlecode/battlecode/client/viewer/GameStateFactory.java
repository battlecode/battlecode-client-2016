package battlecode.client.viewer;

import battlecode.world.GameMap;

public interface GameStateFactory<E extends GameState> {

    E createState(GameMap map);

    E cloneState(E state);

    void copyState(E src, E dst);
}
