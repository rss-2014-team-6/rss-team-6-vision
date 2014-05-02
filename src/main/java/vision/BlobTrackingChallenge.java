package vision;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.lang.time.StopWatch;

/**
 * BlobTracking performs image processing and tracking for the VisualServo
 * module. BlobTracking filters raw pixels from an image and classifies blobs,
 * generating a higher-level vision image.
 * 
 * @author previous TA's, prentice
 */
public class BlobTrackingChallenge {
	private int width;
	private int height;
	private boolean serialize;
	private boolean useBlurred;
	private int hueThreshold;
	private int satThreshold;
	private int skipThreshold;
	private int sizeThreshold;

	public boolean targetDetected = false;
    PrintWriter out;
    StopWatch watch;
    FileOutputStream fileOut;
    ObjectOutputStream outStream;
    List<Image> capturedImages;
    Image currentImage;
    Image destinationImage;
    int[][][] currentHSV;
    
    int red_r = 255; int red_g = 0; int red_b = 0;
    int orange_r = 252; int orange_g = 134; int orange_b = 16;
    int yellow_r = 249; int yellow_g = 244; int yellow_b = 24;
    int green_r = 106; int green_g = 215; int green_b = 45;
    int blue_r = 50; int blue_g = 183; int blue_b = 210;
    int purple_r = 119; int purple_g = 36; int purple_b = 128;
    int blob_r; int blob_g; int blob_b;

	public BlobTrackingChallenge(int width, int height, boolean serialize, boolean useBlurred, int hueThreshold, int satThreshold, int skipThreshold, int sizeThreshold) {
		this.width = width;
		this.height = height;
		this.serialize = serialize;
		this.useBlurred = useBlurred;
		this.hueThreshold = hueThreshold;
		this.satThreshold = satThreshold;
		this.skipThreshold = skipThreshold;
		this.sizeThreshold = sizeThreshold;
		currentHSV = new int[height][width][3];
		
		if (serialize) {
			try {
			    out = new PrintWriter("/home/rss-student/rss-challenge/rss-team-6/src/rosjava_pkg/lab4/snapshots/imageObjects.txt");
			    fileOut = new FileOutputStream("/home/rss-student/rss-challenge/rss-team-6/src/rosjava_pkg/lab4/snapshots/imageObjects.ser");
			    outStream = new ObjectOutputStream(fileOut);
			}
			catch (IOException e) {
			}
			watch = new StopWatch();
			watch.start();
		}
		capturedImages = new ArrayList<Image>();
	}

	public CompleteBallMessage applyBlock(Image src, Image dest) {
		currentImage = src;
		destinationImage = dest; 
		
		// Change current image to blurred image if boolean activated
		if (useBlurred) {
			byte[] blurredPixels = new byte[width * height * 3];
			GaussianBlur.apply(src.toArray(), blurredPixels, width, height);
			currentImage = new Image(blurredPixels, width, height);
		}
		
		// Compute the hues of the current image (unfiltered and filtered)
		currentHSV = currentImage.getHSVArray();

		if(serialize) storeImage();

		//computeUpperLeftAverage();
		
		// Interpret the image
		Set<Blob> hueConstantRegions = findHueConstantRegions(false, new HashSet<Integer>());
		Set<Blob> discoveredObjects = findObjectRegions(hueConstantRegions);
		List<Blob> discoveredBlocks = findSpheres(discoveredObjects);
		
		//Color blobs grey
		int grayscale = 100;
		for (Blob blob : discoveredObjects) {
			Set<Point2D.Double> blobPoints = blob.getPoints();
			for (Point2D.Double point : blobPoints) {
				dest.setPixel((int) point.x, (int) point.y, (byte) grayscale,
						(byte) grayscale, (byte) grayscale);
			}
		}
		
		CompleteBallMessage completeBallMsg = new CompleteBallMessage();
		for (Blob blob : discoveredBlocks) {
			Set<Point2D.Double> blobPoints = blob.getPoints();
			getBlobColors(blob.colorClassifier());
			for (Point2D.Double point : blobPoints) {
				dest.setPixel((int) point.x, (int) point.y, (byte) blob_r,
						(byte) blob_g, (byte) blob_b);
			}
			double currentRange = blob.calculateRangeBlock();
			double currentBearing = blob.calculateBearing(width);
			if (!completeBallMsg.sendMessage || currentRange < completeBallMsg.range) {
				completeBallMsg = new CompleteBallMessage(currentRange, currentBearing, blob.color);
			}		
		}
		
		return completeBallMsg;
	}
	
	public CompleteFiducialMessage applyFiducial(Image src, Image dest) {
		currentImage = src;
		destinationImage = dest; 
		
		// Change current image to blurred image if boolean activated
		if (useBlurred) {
			byte[] blurredPixels = new byte[width * height * 3];
			GaussianBlur.apply(src.toArray(), blurredPixels, width, height);
			currentImage = new Image(blurredPixels, width, height);
		}
		
		// Compute the hues of the current image (unfiltered and filtered)
		currentHSV = currentImage.getHSVArray();

		if(serialize) storeImage();
		//computeUpperLeftAverage();
		
		// Attempt to find the wall
		Set<Blob> wallPotentialRegions = findHueConstantRegions(true, new HashSet<Integer>());
		int maxSize = 0;
		Blob potentialWall = null;
		for (Blob blob : wallPotentialRegions) {
			if (blob.getPoints().size() > maxSize) {
				maxSize = blob.getPoints().size();
				potentialWall = blob;
			}
		}
		Set<Integer> disallowedHues = new HashSet<Integer>();
		if (maxSize > 5000) {
			Map<Integer,Integer> wallHueHistogram = new HashMap<Integer,Integer>();
			for (Point2D.Double point : potentialWall.getPoints()) {
				int hue = currentHSV[(int)point.y][(int)point.x][0];
				if (wallHueHistogram.containsKey(hue)) {
					wallHueHistogram.put(hue, wallHueHistogram.get(hue) + 1);
				}
				else {
					wallHueHistogram.put(hue,  1);
				}
			}
			
			/*for (int hue : wallHueHistogram.keySet()) {
				System.out.println("hue: " + hue + " num: " + wallHueHistogram.get(hue));
			}*/
			
			for (int i=0; i<8; i++) {
				int maxHue = 0;
				for (int hue : wallHueHistogram.keySet()) {
					if (wallHueHistogram.get(hue) > (wallHueHistogram.containsKey(maxHue) ? wallHueHistogram.get(maxHue) : 0)) {
						maxHue = hue;
					}
				}
				wallHueHistogram.remove(maxHue);
				disallowedHues.add(maxHue);
			}
	
			/*for (int hue : disallowedHues) {
				System.out.println("disallowed hue: " + hue);
			}*/
		}
		
		//System.out.println("maximum size blob: " + maxSize);
		if (potentialWall != null) {
			for (Point2D.Double point : potentialWall.getPoints()) {
				dest.setPixel((int) point.x, (int) point.y, (byte) 0, (byte) 0, (byte) 0);
			}
		}
		
		Set<Blob> hueConstantRegions = findHueConstantRegions(false, disallowedHues);
		Set<Blob> discoveredObjects = findObjectRegions(hueConstantRegions);
		List<Blob> discoveredSpheres = findSpheres(discoveredObjects);
		
		//Color blobs grey
		int grayscale = 100;
		for (Blob blob : discoveredObjects) {
			Set<Point2D.Double> blobPoints = blob.getPoints();
			for (Point2D.Double point : blobPoints) {
				dest.setPixel((int) point.x, (int) point.y, (byte) grayscale,
						(byte) grayscale, (byte) grayscale);
			}
		}
		
		//Color spheres white and spaced correctly spheres pink
		for (Blob blob : discoveredSpheres) {
			Set<Point2D.Double> blobPoints = blob.getPoints();
			for (Point2D.Double point : blobPoints) {
				dest.setPixel((int) point.x, (int) point.y, (byte) 255, (byte) 255, (byte) 255);
			}
			if (blob.isValidHorizontalFiducial(height)) {
				for (Point2D.Double point : blobPoints) {
					dest.setPixel((int) point.x, (int) point.y, (byte) 255, (byte) 102, (byte) 153);
				}
			}
		}
			
		//Color fiducials
		CompleteFiducialMessage completeFidMsg = new CompleteFiducialMessage();
		for (int i=0; i<discoveredSpheres.size(); i++) {
			for (int j=i+1; j<discoveredSpheres.size(); j++) {
				
				Blob blob1 = discoveredSpheres.get(i);
				Blob blob2 = discoveredSpheres.get(j);
				if (blob1.formsFiducial(blob2, width, height)) {
					// send message
					Set<Point2D.Double> blobPoints1 = blob1.getPoints();
					Set<Point2D.Double> blobPoints2 = blob2.getPoints();
					getBlobColors(blob1.colorClassifier());
					for (Point2D.Double point : blobPoints1) {
						dest.setPixel((int) point.x, (int) point.y, (byte) blob_r,
								(byte) blob_g, (byte) blob_b);
					}
					getBlobColors(blob2.colorClassifier());
					for (Point2D.Double point : blobPoints2) {
						dest.setPixel((int) point.x, (int) point.y, (byte) blob_r,
								(byte) blob_g, (byte) blob_b);
					}
					double currentRange = (blob1.calculateRangeFiducial() + blob2.calculateRangeFiducial()) / 2.0;
					double currentBearing = (blob1.calculateBearing(width) + blob2.calculateBearing(width)) / 2.0;
					if (!completeFidMsg.sendMessage || currentRange < completeFidMsg.range) {
						if (blob1.centroidY > blob2.centroidY) {
							completeFidMsg = new CompleteFiducialMessage(currentRange, currentBearing, blob2.color, blob1.color);
						}
						else {
							completeFidMsg = new CompleteFiducialMessage(currentRange, currentBearing, blob1.color, blob2.color);
						}						
					}
				}
			}
		}
		
		return completeFidMsg;
	}

	public void computeUpperLeftAverage() {
		int ht = height / 10;
		int wt = width / 10;
		int ht_start = 0;
		int wt_start = 0;

		double hueSum = 0;
		double satSum = 0;
		double valSum = 0;

		// Determine the average rgb/hsv pixel values in the upper left hand corner
		for (int x = wt_start; x < wt_start + wt; x++) {
			for (int y = ht_start; y < ht_start + ht; y++) {
				hueSum += currentHSV[y][x][0];
				satSum += currentHSV[y][x][1];
				valSum += currentHSV[y][x][2];
			}
		}
		int hueApprox = (int) hueSum / (ht * wt);
		int satApprox = (int) satSum / (ht * wt);
		int valApprox = (int) valSum / (ht * wt);
		System.out.println("Upper left:: hue: " + hueApprox + " sat: " + satApprox + " val: " + valApprox);
	}
	
	public void storeImage() {
		if (watch.getTime() > 1000*3) {
			watch.reset();
			watch.start();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
				    out.print(currentHSV[y][x][0] + " ");
			    }
			    out.println();
			}
			out.println(); out.println(); out.flush();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					out.print(currentHSV[y][x][1] + " ");
			    }
			    out.println();
			}
			out.println(); out.println(); out.flush();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					out.print(currentHSV[y][x][2] + " ");
			    }
			    out.println();
			}
			out.println(); out.println(); out.flush();
			
			capturedImages.add(currentImage);
			System.out.println("Size of captured images: " + capturedImages.size());
			if (capturedImages.size() == 20) {
				closeSerialization();
			}
		}
    }
		
	public Set<Blob> findHueConstantRegions(boolean findWall, Set<Integer> forbiddenHues) {
		Set<Point2D.Double> examinedPoints = new HashSet<Point2D.Double>();
		Set<Blob> discoveredBlobs = new HashSet<Blob>();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				Point2D.Double startPoint = new Point2D.Double(x, y);
				if (!examinedPoints.contains(startPoint) && doesPixelQualify(findWall, currentHSV[y][x][0], currentHSV[y][x][1], forbiddenHues)) {
					Set<Point2D.Double> currentBlobPoints = findNewBlob(startPoint, findWall, forbiddenHues, examinedPoints);
					examinedPoints.addAll(currentBlobPoints);
					discoveredBlobs.add(new Blob(currentBlobPoints));
				}
			}
		}

		return discoveredBlobs;
	}
	
	private boolean doesPixelQualify(boolean findWall, int hue, int sat, Set<Integer> forbiddenHues) {
		if (findWall) {
			return (hue > 10 && hue < 32 && sat > 80);
		}
		else {
			return (!forbiddenHues.contains(hue) && sat > 80 && !(hue > 17 && hue < 25));
		}
	}
	
/*	private boolean notWallorFloor(int hue, int sat, int satThreshold) {
		return true;
//		if (hue > 17 && hue < 25) return false;
//		if (hue > 10 && hue < 32 && sat < 110) return false;
//		if (sat > satThreshold) return true;
//		return false;
	}*/

	public Set<Point2D.Double> findNewBlob(Point2D.Double startPoint, boolean findWall, Set<Integer> forbiddenHues, Set<Point2D.Double> examinedPoints) {
		// Initialize a set representing the blob and a queue of points to add
		// to the blob
		Set<Point2D.Double> currentPoints = new HashSet<Point2D.Double>();
		Queue<Point2D.Double> pointsToTest = new LinkedList<Point2D.Double>();
		pointsToTest.add(startPoint);

		while (!pointsToTest.isEmpty()) {
			// Add the first point in the queue to the blob if it isn't already
			// present
			Point2D.Double point = pointsToTest.remove();
			if (!currentPoints.contains(point)) {
				currentPoints.add(point);
				
				// System.out.println("Current blob just added : " + point.x + " " + point.y);

				// Add the surrounding points with similar hues to the queue of
				// points to examine
				for (int xSq = -1 * skipThreshold; xSq < skipThreshold + 1; xSq++) {
					for (int ySq = -1 * skipThreshold; ySq < skipThreshold + 1; ySq++) {
						int xPos = (int) point.x + xSq;
						int yPos = (int) point.y + ySq;

						// If the surrounding point is within the image and
						// satisfies the hue difference
						// criteria, add it to the queue
						if (xPos >= 0 && xPos <= width - 1 && yPos >= 0 && yPos <= height - 1) {
							int modifiedHueThreshold = hueThreshold;
							if (currentHSV[yPos][xPos][0] > 90) { 
								modifiedHueThreshold = 4;
							}
							if (currentHSV[yPos][xPos][0] > 10 && currentHSV[yPos][xPos][0] < 32) {
							    modifiedHueThreshold = 1;
							}
							Point2D.Double considerPoint = new Point2D.Double(xPos, yPos);
							if (!examinedPoints.contains(considerPoint)) {
								if (Image.hueWithinThreshold(currentHSV[yPos][xPos][0], currentHSV[(int)point.y][(int)point.x][0], modifiedHueThreshold)) {
									if (doesPixelQualify(findWall, currentHSV[yPos][xPos][0], currentHSV[yPos][xPos][1], forbiddenHues)) {
										pointsToTest.add(considerPoint);
									}			
								}
							}
						}
					}
				}
			}
		}

		return currentPoints;
	}
	
	public Set<Blob> findObjectRegions(Set<Blob> hueConstantRegions) {
		Set<Blob> objectBlobs = new HashSet<Blob>();
		for (Blob blob : hueConstantRegions) {
			if (blob.getSize() > sizeThreshold && !blob.pointsOnEdge(width, height)) {
				objectBlobs.add(blob);
				blob.calculateBasics(width, height, currentHSV);
			}
		}
		return objectBlobs;
	}
	
	public List<Blob> findSpheres(Set<Blob> objectBlobs) {
		List<Blob> sphereBlobs = new ArrayList<Blob>();
		for (Blob blob : objectBlobs) {
			if (blob.isCircle()) {
				sphereBlobs.add(blob);
			}
		}
		return sphereBlobs;
	}
	
	private void getBlobColors(int color) {
		switch(color) {
		case 0: blob_r = red_r; blob_g = red_g; blob_b = red_b; break;
		case 1: blob_r = orange_r; blob_g = orange_g; blob_b = orange_b; break;
		case 2: blob_r = yellow_r; blob_g = yellow_g; blob_b = yellow_b; break;
		case 3: blob_r = green_r; blob_g = green_g; blob_b = green_b; break;
		case 4: blob_r = blue_r; blob_g = blue_g; blob_b = blue_b; break;
		case 5: blob_r = purple_r; blob_g = purple_g; blob_b = purple_b; break;
		}
	}
	
	public void closeSerialization() {
		try {
			outStream.writeObject(capturedImages);
			outStream.close();
		    fileOut.close();
		    serialize = false;
		    System.out.println("SERIALIZATION STREAM ENDED");
		}
		catch (IOException e) {
		}
	}
}

