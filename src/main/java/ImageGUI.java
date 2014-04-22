package VisualServo;

import java.awt.FlowLayout;
import java.awt.image.MemoryImageSource;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ImageGUI {
    public ImageGUI(List<Image> images) {
    	JFrame frame = new JFrame();
    	frame.setLayout(new FlowLayout());
    	frame.setSize(500,500);
    	for (int i=0; i<images.size(); i++) {
    		JLabel label = new JLabel();
    		label.setIcon(new ImageIcon(label.createImage(convert(images.get(i)))));
    		frame.add(label);
    	}
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    private MemoryImageSource convert(Image image) {
    	int srcIndex = 0;
    	int destIndex = 0;
    	
    	int height = image.getHeight();
    	int width = image.getWidth();
    	
    	byte[] unpackedImage = image.toArray();
    	int[] packedImage = new int[height*width];

    	for (int y = 0; y < height; y++) {
    		for (int x = 0; x < width; x++) {
    			int red = unpackedImage[srcIndex++] & 0xff;
    			int green = unpackedImage[srcIndex++] & 0xff;
    			int blue = unpackedImage[srcIndex++] & 0xff;
    			packedImage[destIndex++] = (0xff << 24) | (red << 16) | (green << 8) | blue;
    		}
    	}

        MemoryImageSource source = new MemoryImageSource(width, height, packedImage, 0, width);
        return source;
    }
}