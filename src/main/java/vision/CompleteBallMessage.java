package vision;

public class CompleteBallMessage {

	double range;
	double bearing;
	boolean sendMessage;
	
	public CompleteBallMessage(double range, double bearing) {
		this.range = range;
		this.bearing = bearing;
		this.sendMessage = true;
	}
	
	public CompleteBallMessage() {
		this.sendMessage = false;
	}
}
