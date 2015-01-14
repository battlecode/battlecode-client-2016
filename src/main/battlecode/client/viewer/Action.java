package battlecode.client.viewer;

import battlecode.common.MapLocation;

public class Action {
    public ActionType type;
    public int roundStarted;
    public int length;
    public MapLocation target;

    public Action(ActionType type,
		  int roundStarted,
		  int length) {
	this(type, roundStarted, length, null);
    }

    public Action(ActionType type,
		  int roundStarted,
		  int length,
		  MapLocation target) {
	this.type = type;
	this.roundStarted = roundStarted;
	this.length = length;
	this.target = target;
    }
}
