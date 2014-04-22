package vision;

public class CompleteFiducialMessage {
	
	double range;
	double bearing;
	boolean sendMessage;
	
	public CompleteFiducialMessage(double range, double bearing) {
		this.range = range;
		this.bearing = bearing;
		this.sendMessage = true;
	}
	
	public CompleteFiducialMessage() {
		this.sendMessage = false;
	}
}
