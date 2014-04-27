package vision;

public class CompleteFiducialMessage {
	
	double range;
	double bearing;
	int topColor;
	int bottomColor;
	boolean sendMessage;
	
	public CompleteFiducialMessage(double range, double bearing, int topColor, int bottomColor) {
		this.range = range;
		this.bearing = bearing;
		this.topColor = topColor;
		this.bottomColor = bottomColor;
		this.sendMessage = true;
	}
	
	public CompleteFiducialMessage() {
		this.sendMessage = false;
	}
}
