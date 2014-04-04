package VisualServo;

public class Blob{
    private Set<Point2D.Double> points;
    private double centroidX;
    private double centroidY;
    private double width;
    private double height;
    private double hue;
    private boolean isSphere;
    
    private BlobColor color;
    private double range;
    private double bearing;
    
	
    public static enum BlobColor {
    	RED(0,20), GREEN(70,100), BLUE(150,220);
    	
    	public final int minHue;
    	public final int maxHue;
    	
    	private BlobColor(int minHue, int maxHue) {
    		this.minHue = minHue;
    		this.maxHue = maxHue;
    	}
    }
    
	public Blob(Set<Point2D.Double> points, int[][] hues) {
		this.points = points;
		calculateBasics();		
	}
	
	private void calculateBasics() {
		double sumX = 0;
		double minX = width;
		double maxX = 0;
		double sumY = 0;
		double minY = height;
		double maxY = 0;
		double sumHue = 0;
		
		for (Point2D.Double point : points) {
			sumX += point.x;
			sumY += point.y;
			minX = Math.min(minX, point.x);
			maxX = Math.max(maxX, point.x);
			minY = Math.min(minY, point.y);
			maxY = Math.max(maxY, point.y);
			sumHue += hues[point.y; point.x];
		}
		
		centroidX = sumX / blob.size();
		centroidY = sumY / blob.size();
		width = maxX - minX;
		height = maxY - minY;
		hue = sumHue / blob.size();
		
		calculateRange();
		calculateBearing();
		calculateColor();
		calculateShape();
	}
	
	private void calculateRange() {
		
	}
	
	private void calculateBearing() {
		
	}
	
	private void calculateColor() {
		for (BlobColor potentialColor : BlobColor.values()) {
			if (hue >= potentialColor.minHue && hue <= potentialColor.maxHue) {
				color = potentialColor;
			}
		}
	}
	
	public Set<Point2D.Double> getPoints() {
		return points;
	}
	
	public double getRange() {
		return range;
	}
	
	public double getBearing() {
		return bearing;
	}
	
	public BlobColor getColor() {
		return color;
	}
	
	public boolean getIsSphere() {
		return isSphere;
	}
}
