package dev.ioliver.services;

import lombok.extern.log4j.Log4j2;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.highgui.HighGui.waitKey;

/**
 * The CamService class provides a service for capturing frames from a camera using the OpenCV library.
 * It initializes the camera, reads frames, and processes them using a callback function.
 */
@Log4j2
public class CamService {
  static {
    nu.pattern.OpenCV.loadLocally();
  }

  private final VideoCapture cam;        // The VideoCapture object for accessing the camera
  private final FrameProcessorCb cb;     // Callback function for processing frames
  private final int camIndex;            // Index of the camera to be used

  /**
   * Constructs a new CamService instance with the specified camera index and frame processing callback.
   *
   * @param camIndex The index of the camera to be used for capturing frames.
   * @param cb       The callback function for processing frames.
   * @throws RuntimeException If the camera fails to open.
   */
  public CamService(int camIndex, FrameProcessorCb cb) {
    this.camIndex = camIndex;
    this.cb = cb;
    cam = new VideoCapture(camIndex);
    if (!cam.isOpened()) throw new RuntimeException("Can't open the camera");
    processFrame().start();
  }

  /**
   * Creates a new thread for continuously capturing and processing frames from the camera.
   *
   * @return The thread responsible for capturing and processing frames.
   */
  private Thread processFrame() {
    return new Thread(() -> {
      while (true) {
        Mat frame = new Mat();
        cam.read(frame);
        if (frame.empty()) {
          log.info("Can't read the frame!");
          break;
        }
        imshow("CamService " + camIndex, cb.processFrame(frame));
        waitKey(30);

        if (HighGui.n_closed_windows != 0) Runtime.getRuntime().exit(0);
      }
    });
  }

  /**
   * Functional interface representing a frame processing callback.
   */
  @FunctionalInterface
  public interface FrameProcessorCb {
    /**
     * Processes the input frame and returns the processed frame.
     *
     * @param frame The input frame to be processed.
     * @return The processed frame.
     */
    Mat processFrame(Mat frame);
  }
}
