package battlecode.client.viewer;

public interface Controller {

	int getStepSize();
	void setPlayer(MatchPlayer player);

	void setPlayEnabled(boolean enabled);
	void enableNext();

	void updateRoundLabel(int round, int max);
	void updateRoundLabel(GameStateTimeline gst);

	class ControlAdapter implements Controller {
		public int getStepSize() { return 1; }
		public void setPlayer(MatchPlayer player) {}

		public void setPlayEnabled(boolean enabled) {}
		public void enableNext() {}

		public void updateRoundLabel(int round, int max) {}
		public void updateRoundLabel(GameStateTimeline gst) {}
	}
}
