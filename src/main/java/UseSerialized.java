package VisualServo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class UseSerialized {
	private static final long serialVersionUID = 1L;
	FileInputStream fileIn;
	ObjectInputStream in;
	List<Image> imageSet;
	List<Image> processedImages;
	
	public static void main(String[] args) {
		UseSerialized tester = new UseSerialized("C:\\Users\\Katharine\\rss-team-6\\src\\rosjava_pkg\\lab4\\snapshots\\imageObjects.ser");
		//UseSerialized tester = new UseSerialized("C:\\Katharine\\MIT\\Classes\\6.141\\images.ser");
	}
		
	public UseSerialized(String fileName) {
		try {
			fileIn = new FileInputStream(fileName);
			in = new ObjectInputStream(fileIn);
			try {
				System.out.println("here000");
				imageSet = (List<Image>) in.readObject();
				System.out.println("here3");
				interpretImages(imageSet);
				ImageGUI gui = new ImageGUI(processedImages);
			} catch (ClassNotFoundException e) {
				System.out.println("here2");
				e.printStackTrace();
			}
			in.close();
			fileIn.close();			
		}
		catch(IOException e) {
		}
	}

	private void interpretImages(List<Image> imageSet) {
		System.out.println("here!");
		processedImages = new ArrayList<Image>();
		//BlobTrackingChallenge blobTracker = new BlobTrackingChallenge(50,50,false,false,1,1,200);
		BlobTrackingChallenge blobTracker = new BlobTrackingChallenge(160, 120, false, false, 2, 60, 1, 200);
		int index = 0;
		for (Image image : imageSet) {
			System.out.println("Image " + index);
			index++;
			Image dest = new Image(image);
			blobTracker.applyFiducial(image, dest);
			processedImages.add(image);
			processedImages.add(dest);
		}
	}

}
