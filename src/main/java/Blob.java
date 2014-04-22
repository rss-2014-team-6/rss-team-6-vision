package VisualServo;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Blob {
	private final double circleThreshold = 0.3;
	private final double verticalAlignThreshold = 0.2;
	private final double horizontalAlignThreshold = 0.15;
	private final double blockSize = 0.06;
	private final double sphereSize = 0.1;
	
	private Set<Point2D.Double> points;
	private Set<Point2D.Double> hullPoints;
	Map<Integer, Integer> minYforGivenX;
	Map<Integer, Integer> maxYforGivenX;
	Map<Integer, Integer> minXforGivenY;
	Map<Integer, Integer> maxXforGivenY;
	
	private int hueAvg;
	private String color;
	
	public double centroidX;
	public double centroidY;
	public double width;
	public double height;
	
	private double hue;

	public static enum ObjectMatch {
		RED(246,13), 
		ORANGE(14,25),
		YELLOW(26,30),
		GREEN(31,110),
		BLUE(111,168),
		PURPLE(169,245);
		
		public final int minHue;
		public final int maxHue;
		public final int minSat;
		public final int maxSat;
	
		private ObjectMatch(int minHue, int maxHue) {
			this.minHue = minHue;
			this.maxHue = maxHue;
			this.minSat = 100;
			this.maxSat = 255;
		}
	}

	public Blob(Set<Point2D.Double> points) {
		this.points = points;
		hullPoints = new HashSet<Point2D.Double>();
		minYforGivenX = new HashMap<Integer,Integer>();
		maxYforGivenX = new HashMap<Integer,Integer>();
		minXforGivenY = new HashMap<Integer,Integer>();
		maxXforGivenY = new HashMap<Integer,Integer>();
	}
	
	public int getSize() {
		return points.size();
	}
	
	public boolean pointsOnEdge(int width, int height) {
		for (Point2D.Double point : points) {
			if ((int)point.x == 0 || (int)point.x == width-1 || (int)point.y == 0 || (int)point.y == height-1)
				return true;
		}
		return false;
	}
	
	public boolean isObject(int[][][] hsv) {
		int hueSum = 0;
		for (Point2D.Double point : points) {
			hueSum += hsv[(int)point.y][(int)point.x][0];
		}
		hueAvg = (int) hueSum / points.size();
		
		for (ObjectMatch potential : ObjectMatch.values()) {
			if (Image.hueWithinRange(hueAvg, potential.minHue, potential.maxHue)) {
				color = potential.name();
				return true;
			}
		}
		color = "NA";
		return false;
	}
	
	public boolean isCircle() {
		findConvexHull();
		Point2D.Double centroid = new Point2D.Double(centroidX, centroidY);
		double sumDist = 0.0;
		for (Point2D.Double point : hullPoints) {
			sumDist += point.distance(centroid);
		}
		double avgDist = sumDist/hullPoints.size();
		
		double sumError = 0.0;
		for (Point2D.Double point : hullPoints) {
			sumError += Math.pow(point.distance(centroid) - avgDist, 2);
		}
		double stdDev = sumError/hullPoints.size();
		
		return (stdDev <= circleThreshold*avgDist);
	}
	
	private void findConvexHull() {
		int x;
		int y;
		for (Point2D.Double point : points) {
			x = (int)point.x;
			y = (int)point.y;
			minYforGivenX.put(x, !minYforGivenX.containsKey(x) ? y : Math.min(y, minYforGivenX.get(x)));
			maxYforGivenX.put(x, !maxYforGivenX.containsKey(x) ? y : Math.max(y, maxYforGivenX.get(x)));
			minXforGivenY.put(y, !minXforGivenY.containsKey(y) ? x : Math.min(x, minXforGivenY.get(y)));
			maxXforGivenY.put(y, !maxXforGivenY.containsKey(y) ? x : Math.max(x, maxXforGivenY.get(y)));
		}
		
		for (Map.Entry<Integer, Integer> point : minYforGivenX.entrySet()) {
			hullPoints.add(new Point2D.Double(point.getKey(),point.getValue()));
		}
		for (Map.Entry<Integer, Integer> point : maxYforGivenX.entrySet()) {
			hullPoints.add(new Point2D.Double(point.getKey(),point.getValue()));
		}
		for (Map.Entry<Integer, Integer> point : minXforGivenY.entrySet()) {
			hullPoints.add(new Point2D.Double(point.getValue(),point.getKey()));
		}
		for (Map.Entry<Integer, Integer> point : maxXforGivenY.entrySet()) {
			hullPoints.add(new Point2D.Double(point.getValue(),point.getKey()));
		}
	}

	public void calculateBasics(int imgWidth, int imgHeight) {
		double sumX = 0;
		double minX = imgWidth;
		double maxX = 0;
		double sumY = 0;
		double minY = imgHeight;
		double maxY = 0;
		double sumHue = 0;

		for (Point2D.Double point : points) {
			sumX += point.x;
			sumY += point.y;
			minX = Math.min(minX, point.x);
			maxX = Math.max(maxX, point.x);
			minY = Math.min(minY, point.y);
			maxY = Math.max(maxY, point.y);
		}

		centroidX = sumX / points.size();
		centroidY = sumY / points.size();
		width = maxX - minX;
		height = maxY - minY;
		hue = sumHue / points.size();
	}
	
	public boolean formsFiducial(Blob second, int imgWidth, int imgHeight) {
		return (this.isValidHorizontalFiducial(imgHeight) && second.isValidHorizontalFiducial(imgHeight) &&
			(Math.abs(this.width - second.width) <= verticalAlignThreshold*this.width) &&
			(Math.abs(this.centroidX - second.centroidX) <= verticalAlignThreshold*imgWidth));
	}
	
	public boolean isValidHorizontalFiducial(int imgHeight) {
		return ((Math.abs(this.centroidY + this.height/2 - imgHeight/2) <= horizontalAlignThreshold*imgHeight) ||
				(Math.abs(this.centroidY - this.height/2 - imgHeight/2) <= horizontalAlignThreshold*imgHeight));
	}

	private double calculateRangeBlock() {
		return blockSize*160/this.width*0.29/0.28;
	}
	
	private double calculateRangeFiducial() {
		return sphereSize*160/this.width*0.29/0.28;
	}

	private double calculateBearing(int imgWidth) {
		return (imgWidth/2 - centroidX)*Math.atan2(14.0, 29.0)/80;
	}

	public Set<Point2D.Double> getPoints() {
		return points;
	}

	public String getColor() {
		return color;
	}
}
