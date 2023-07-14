import dev.ioliver.dtos.CardContour;
import dev.ioliver.dtos.CardCorner;
import dev.ioliver.dtos.CardDimensions;
import dev.ioliver.services.CamService;
import dev.ioliver.services.OpenCVService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class CardCornerTest {
  public static void main(String[] args) {
    OpenCVService opencvService = OpenCVService.getInstance();
    CamService camService = new CamService(1, frame -> {
      List<CardContour> cardsContours = opencvService.getAllCardsContours(frame);
      if(cardsContours.size() > 0) {
        CardDimensions cardDimensions = opencvService.getCardDimensions(cardsContours.get(0));
        Mat flatCard = opencvService.cutAndFlatCard(frame, cardDimensions, false);

        CardCorner corner = opencvService.getCorner(flatCard);

//        List<MatOfPoint> externContours = opencvService.returnOrderedAndExternalContours(corner.cornerRank());
//        if (!externContours.isEmpty()) {
//          Rect rect = Imgproc.boundingRect(externContours.get(0));
//          Mat cropped = corner.cornerRank().submat(rect);
//          Imgcodecs.imwrite("./assets/tests/JOKER.jpg", cropped);
//          return cropped;
//        }
        return corner.fullCorner();
      }
      return frame;
    });
  }
}
