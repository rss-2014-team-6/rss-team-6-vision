package vision;

public class CompleteBallMessage {

	double range;
	double bearing;
	int color;
	boolean sendMessage;
	
	public CompleteBallMessage(double range, double bearing, int color) {
		this.range = range;
		this.bearing = bearing;
		this.color = color;
		this.sendMessage = true;
	}
	
	public CompleteBallMessage() {
		this.sendMessage = false;
	}
}
