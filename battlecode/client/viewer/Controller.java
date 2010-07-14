package battlecode.client.viewer;

public interface Controller {

	public int getStepSize();
	public void setPlayer(MatchPlayer player);

	public void setPlayEnabled(boolean enabled);
	public void enableNext();

	public void updateRoundLabel(int round, int max);
	public void updateRoundLabel(GameStateTimeline gst);

	public class ControlAdapter implements Controller {
		public int getStepSize() { return 1; }
		public void setPlayer(MatchPlayer player) {}

		public void setPlayEnabled(boolean enabled) {}
		public void enableNext() {}

		public void updateRoundLabel(int round, int max) {}
		public void updateRoundLabel(GameStateTimeline gst) {}
	}
}
