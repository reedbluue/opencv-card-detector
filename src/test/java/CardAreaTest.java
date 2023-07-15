import dev.ioliver.services.CamService;
import dev.ioliver.services.OpenCVService;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class CardAreaTest {
  public static void main(String[] args) {
    OpenCVService opencvService = OpenCVService.getInstance();
    CamService camService = new CamService(1, frame -> {
      List<MatOfPoint> contours = opencvService.returnOrderedAndExternalContours(frame).stream().filter(c -> Imgproc.contourArea(c) > 100).toList();
      Imgproc.drawContours(frame, contours, -1, new Scalar(0, 0, 0), 5);
      Imgproc.drawContours(frame, contours, -1, new Scalar(255, 0, 0), 2);
      contours.forEach(c -> {
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        for (Point point : c.toArray()) {
          if (point.x > maxX) {
            maxX = point.x;
          }
          if (point.y > maxY) {
            maxY = point.y;
          }
        }
        Imgproc.putText(frame, String.valueOf(Imgproc.contourArea(c)), new Point(maxX, maxY), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0), 2);
      });
      return frame;
    });
  }
}
