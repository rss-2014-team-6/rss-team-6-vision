package vision;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import org.ros.message.MessageListener;
import rss_msgs.MotionMsg;
import rss_msgs.BallLocationMsg;
import rss_msgs.FiducialMsg;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

/**
 * 
 * @author previous TA's, prentice, vona
 * 
 */
public class VisualServo extends AbstractNodeMain implements Runnable {

    private static final int width = 160;
    private static final int height = 120;
    
    private int count = 0;
    private final int maxCount = 3;

    /**
     * <p>
     * The blob tracker.
     * </p>
     **/
    private BlobTrackingChallenge blockTracker = null;
    private BlobTrackingChallenge fiducialTracker = null;

    private VisionGUI gui;
    private ArrayBlockingQueue<byte[]> blockQueue = new ArrayBlockingQueue<byte[]>(1);
    private ArrayBlockingQueue<byte[]> fiducialQueue = new ArrayBlockingQueue<byte[]>(1);

    protected boolean firstUpdate = true;

    public Subscriber<sensor_msgs.Image> vidSubBlock;
    public Subscriber<sensor_msgs.Image> vidSubFiducial;

    private Publisher<rss_msgs.BallLocationMsg> ballLocationPub;
    private Publisher<rss_msgs.FiducialMsg> fiducialLocationPub;

    /**
     * <p>
     * Create a new VisualServo object.
     * </p>
     */
    public VisualServo() {
        gui = new VisionGUI();
    }


    /**
     * <p>
     * Handle a CameraMessage. Perform blob tracking and servo robot towards
     * target.
     * </p>
     * 
     * @param rawImage
     *            a received camera message
     */
    public void handleBlock(byte[] rawImage) {
        blockQueue.offer(rawImage);
    }
    
    public void handleFiducial(byte[] rawImage) {
    	fiducialQueue.offer(rawImage);
    }

    @Override
    public void run() {
        while (true) {
            Image srcBlock = null;
            Image srcFiducial = null;
            try {
                srcBlock = new Image(blockQueue.take(), width, height);
                srcFiducial = new Image(fiducialQueue.take(), width, height);
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            
            count++;
            if (count == maxCount) {
            	count = 0;
	            Image destBlock = new Image(srcBlock);
	            CompleteBallMessage completeBallMsg = blockTracker.applyBlock(srcBlock, destBlock);
	            Image destFiducial = new Image(srcFiducial);
	            CompleteFiducialMessage completeFidMsg = fiducialTracker.applyFiducial(srcFiducial, destFiducial);
	
	            // update newly formed vision message
	            gui.setVisionImage(srcBlock.toArray(),srcFiducial.toArray(),destBlock.toArray(),destFiducial.toArray(),width,height);
	
	            if (completeBallMsg.sendMessage) {
	            	BallLocationMsg ballMsg = ballLocationPub.newMessage();
	            	ballMsg.setRange(completeBallMsg.range);
	            	ballMsg.setBearing(completeBallMsg.bearing);
	            	ballMsg.setColor(completeBallMsg.color);
	            	ballLocationPub.publish(ballMsg);
	            }
	            
	            if (completeFidMsg.sendMessage) {
	            	FiducialMsg fidMsg = fiducialLocationPub.newMessage();
	            	fidMsg.setRange(completeFidMsg.range);
	            	fidMsg.setBearing(completeFidMsg.bearing);
	            	fidMsg.setTop(completeFidMsg.topColor);
	            	fidMsg.setBottom(completeFidMsg.bottomColor);
	            	fiducialLocationPub.publish(fidMsg);
	            }
            }
        }
    }

    /**
     * <p>
     * Run the VisualServo process
     * </p>
     * 
     * @param node
     *            optional command-line argument containing hostname
     */
    @Override
    public void onStart(final ConnectedNode node) {
        blockTracker = new BlobTrackingChallenge(width, height, false, false, 2, 80, 1, 200);
        fiducialTracker = new BlobTrackingChallenge(width, height, true, false, 2, 80, 1, 200);

        // Begin Student Code

        // set parameters on blobTrack as you desire

        // initialize the ROS publication to command/MotorsBallLocation
        ballLocationPub = node.newPublisher("/vision/BallLocation", "rss_msgs/BallLocationMsg");
        fiducialLocationPub = node.newPublisher("/vision/FiducialLocation", "rss_msgs/FiducialMsg");

        // End Student Code

        final boolean reverseRGB = node.getParameterTree().getBoolean(
                "reverse_rgb", false);

        vidSubBlock = node.newSubscriber("/rss/low_video", "sensor_msgs/Image");
		vidSubBlock.addMessageListener(new MessageListener<sensor_msgs.Image>() {
            @Override
            public void onNewMessage(sensor_msgs.Image message) {
                byte[] rgbData;
                if (reverseRGB) {
                    rgbData = Image.RGB2BGR(message.getData().array(), (int) message.getWidth(),
                                            (int) message.getHeight());
                } else {
                    rgbData = message.getData().array();
                }
                assert ((int) message.getWidth() == width);
                assert ((int) message.getHeight() == height);
                if ((int) message.getWidth() != width) {
                	throw new RuntimeException ("Widths don't match: " + message.getWidth() + "," + width);
                }
                if ((int) message.getHeight() != height) {
                	throw new RuntimeException ("Heights don't match: " + message.getHeight() + "," + height);
                }
                if (rgbData.length != 3 * width * height) {
				    // Strip the first n characters to make the length right (yay hacks! P.S. don't let tej see this code)
				    byte[] rgbDataNew = Arrays.copyOfRange(rgbData, rgbData.length - 3*width*height, rgbData.length);
				    rgbData = rgbDataNew;
				}
                handleBlock(rgbData);
            }
        });
		
		vidSubFiducial = node.newSubscriber("/rss/high_video", "sensor_msgs/Image");
		vidSubFiducial.addMessageListener(new MessageListener<sensor_msgs.Image>() {
			@Override
			public void onNewMessage(sensor_msgs.Image message) {
				byte[] rgbData;
				if (reverseRGB) {
					rgbData = Image.RGB2BGR(message.getData().array(), (int) message.getWidth(),
							(int) message.getHeight());
				} else {
					rgbData = message.getData().array();
				}
				assert ((int) message.getWidth() == width);
				assert ((int) message.getHeight() == height);
				if ((int) message.getWidth() != width) {
					throw new RuntimeException ("Widths don't match: " + message.getWidth() + "," + width);
				}
				if ((int) message.getHeight() != height) {
					throw new RuntimeException ("Heights don't match: " + message.getHeight() + "," + height);
				}
				if (rgbData.length != 3 * width * height) {
					// Strip the first n characters to make the length right (yay hacks! P.S. don't let tej see this code)
					byte[] rgbDataNew = Arrays.copyOfRange(rgbData, rgbData.length - 3*width*height, rgbData.length);
					rgbData = rgbDataNew;
				}
				handleFiducial(rgbData);
			}
		});
        Thread runningStuff = new Thread(this);
        runningStuff.start();
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rss/visualservo");
    }
}
