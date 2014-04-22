package VisualServo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.MemoryImageSource;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class VisionGUI extends JPanel {
    public static final String APPNAME = "VisionGUI";
    static final long serialVersionUID = 42;
    public static final int DEFAULT_WIDTH = 320;
    public static final int DEFAULT_HEIGHT = 240;

    /**
     * <p>
     * Whether to use <code>RenderingHints.VALUE_ANTIALIAS_ON</code> and
     * <code>RenderingHints.VALUE_TEXT_ANTIALIAS_ON</code>.
     * </p>
     **/
    public static final boolean ANTIALIASING = true;

    /**
     * <p>
     * The rendering quality to use.
     * </p>
     **/
    public static final Object RENDERING_QUALITY = RenderingHints.VALUE_RENDER_QUALITY;

    /**
     * <p>
     * The interpolation to use.
     * </p>
     **/
    public static final Object INTERPOLATION = RenderingHints.VALUE_INTERPOLATION_BICUBIC;

    /**
     * <p>
     * Whether to use <code>RenderingHints.VALUE_FRACTIONALMETRICS_ON</code>.
     * </p>
     **/
    public static final boolean FRACTIONALMETRICS = true;

    /**
     * <p>
     * Whether to use <code>RenderingHints.VALUE_STROKE_NORMALIZE</code>.
     * </p>
     **/
    public static final boolean STROKE_NORMALIZATION = true;

    protected boolean firstUpdate = true;
    protected boolean renderFastest = false;
    protected boolean visionImageEnabled = true;

    protected JFrame frame;

    // Total time it took in milliseconds to render the last frame.
    protected double lastFrameTime = 0.0;

    /**
     * <p>
     * Frame time in ms above which to force fast rendering.
     * </p>
     **/
    public static final double FORCE_FAST_RENDER_THRESHOLD = 300.0;

    /**
     * <p>
     * Frame time in ms below which to un-force fast rendering.
     * </p>
     **/
    public static final double UN_FORCE_FAST_RENDER_THRESHOLD = 5.0;

    /**
     * A paintable graphical object.
     **/
    protected abstract class Glyph {
        public abstract void paint(Graphics2D g2d);
    }

    /**
     * Displays images from the robot's camera.
     **/
    protected class VisionImage extends Glyph {
        // The pixel buffer for the displayed image.
        int packedImage[] = null;

        // Java image animation machinery
        MemoryImageSource source = null;

        //The actual image we paint, null if none
        java.awt.Image image = null;

        int width = -1;
        int height = -1;
        int x_start;
        int y_start;

        VisionImage(int x_start, int y_start) {
            unset();
            this.x_start = x_start;
            this.y_start = y_start;
        }

        void set(byte[] unpackedImage, int width, int height) {
            if ((unpackedImage == null) || (width <= 0) || (height <= 0)) {
                unset();
                return;
            }

            boolean reConsedPacked = false;
            if ((packedImage == null) || (this.width != width)
                    || (this.height != height)) {
                packedImage = new int[width * height];
                reConsedPacked = true;
            }

            int srcIndex = 0;
            int destIndex = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int red = unpackedImage[srcIndex++] & 0xff;
                    int green = unpackedImage[srcIndex++] & 0xff;
                    int blue = unpackedImage[srcIndex++] & 0xff;
                    packedImage[destIndex++] = (0xff << 24) | (red << 16)
                            | (green << 8) | blue;
                }
            }

            if (reConsedPacked || (image == null) || (this.width != width)
                    || (this.height != height)) {

                source = new MemoryImageSource(width, height, packedImage, 0,
                        width);
                source.setAnimated(true);

                image = createImage(source);

            } else {
                source.newPixels();
            }

            this.width = width;
            this.height = height;
        }

        void unset() {
            image = null;
            width = -1;
            height = -1;
        }

        @Override
        public void paint(Graphics2D g2d) {
            if (image == null)
                return;

            g2d.drawImage(image, x_start, y_start, VisionGUI.this);
        }
    }

    protected VisionImage srcFiducialImage = new VisionImage(0,0);
    protected VisionImage srcBlockImage = new VisionImage(0,120);
    protected VisionImage destFiducialImage = new VisionImage(160,0);
    protected VisionImage destBlockImage = new VisionImage(160,120);

    /**
     * Construct a new VisionGUI.
     **/
    public VisionGUI() {

        // do this here, not in invokeLater, so we can configure frame in
        // instanceMain()
        frame = new JFrame(getAppName());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                setBackground(Color.WHITE);
                setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
                setOpaque(true);
                setDoubleBuffered(true);

                Container contentPane = frame.getContentPane();
                contentPane.setBackground(Color.WHITE);
                contentPane.setLayout(new BorderLayout());
                contentPane.add(VisionGUI.this, "Center");

                frame.pack(); // do this before working on focus!

                setFocusable(true);
                requestFocusInWindow();

                frame.setLocationByPlatform(true);
                frame.setVisible(true);
            }
        });
    }

    /**
     * <p>
     * Get the frame containing this GUI.
     * </p>
     * 
     * @return the frame containing this GUI
     **/
    public JFrame getFrame() {
        return frame;
    }

    /**
     * <p>
     * Get the title for the GUI frame.
     * </p>
     * 
     * <p>
     * Default impl returns {@link #APPNAME}.
     * </p>
     * 
     * @return the title for the GUI frame
     **/
    public String getAppName() {
        return APPNAME;
    }
    
    public void setVisionImage(byte[] srcBlock, byte[] srcFid, byte[] destBlock, byte[] destFid, int width, int height) {
    	synchronized(srcBlockImage) {
    		srcBlockImage.set(srcBlock, width, height);
    	}
    	synchronized(srcFiducialImage) {
    		srcFiducialImage.set(srcFid, width, height);
    	}
    	synchronized(destBlockImage) {
    		destBlockImage.set(destBlock, width, height);
    	}
    	synchronized(destFiducialImage) {
    		destFiducialImage.set(destFid, width, height);
    	}
    	repaint();
    }
    
    public void eraseVisionImage() {
    	synchronized(srcBlockImage) {
    		srcBlockImage.unset();
    	}
    	synchronized(srcFiducialImage) {
    		srcFiducialImage.unset();
    	}
    	synchronized(destBlockImage) {
    		destBlockImage.unset();
    	}
    	synchronized(destFiducialImage) {
    		destFiducialImage.unset();
    	}
    }

    @Override
    public void paintComponent(Graphics g) {
        paintComponent((Graphics2D) g);
    }

    protected void paintComponent(Graphics2D g2d) {
        super.paintComponent(g2d);
        paintContents(g2d);
    }

    protected void paintContents(Graphics2D g2d) {
        double startTime = System.currentTimeMillis();

        if (lastFrameTime > FORCE_FAST_RENDER_THRESHOLD)
            renderFastest = true;

        if (lastFrameTime < UN_FORCE_FAST_RENDER_THRESHOLD)
            renderFastest = false;

        if (renderFastest) {
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);

            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
        } else {

            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RENDERING_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    INTERPOLATION);

            if (ANTIALIASING) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }

            if (FRACTIONALMETRICS)
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            if (STROKE_NORMALIZATION)
                g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        RenderingHints.VALUE_STROKE_NORMALIZE);

        }

        if (visionImageEnabled)
            paintVisionImage(g2d);

        lastFrameTime = System.currentTimeMillis() - startTime;
    }

    protected void paintVisionImage(Graphics2D g2d) {
        if (srcBlockImage != null) {
	        synchronized (srcBlockImage) {
	            srcBlockImage.paint(g2d);
	        }
        }
        if (srcFiducialImage != null) {
        	synchronized (srcFiducialImage) {
        		srcFiducialImage.paint(g2d);
        	}
        }
        if (destBlockImage != null) {
        	synchronized (destBlockImage) {
        		destBlockImage.paint(g2d);
        	}
        }
        if (destFiducialImage != null) {
        	synchronized (destFiducialImage) {
        		destFiducialImage.paint(g2d);
        	}
        }
    }
}
