package battlecode.client.util;

import java.util.Observable;

public class SettableObservable extends Observable {

	public void setChanged() { super.setChanged(); }
	public void clearChanged() { super.clearChanged(); }
	
}