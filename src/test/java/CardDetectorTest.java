import dev.ioliver.dtos.Card;
import dev.ioliver.dtos.CardContour;
import dev.ioliver.dtos.CardDimensions;
import dev.ioliver.services.CamService;
import dev.ioliver.services.OpenCVService;

import java.util.ArrayList;
import java.util.List;

public class CardDetectorTest {
  static final OpenCVService OPENCV_SERVICE = OpenCVService.getInstance();

  public static void main(String[] args) {
    CamService camService = new CamService(1, frame -> {
      List<CardContour> cardsContours = OPENCV_SERVICE.getAllCardsContours(frame);
      if (cardsContours.isEmpty()) {
        OPENCV_SERVICE.drawText(frame, "No cards detected", 1);
      } else {
        List<Card> processedCards = new ArrayList<>();
        cardsContours.forEach(contour -> {
          CardDimensions cardDimension = OPENCV_SERVICE.getCardDimensions(contour);
          Card processedCard = OPENCV_SERVICE.getProcessedCard(contour, cardDimension);
          processedCards.add(processedCard);
        });
        OPENCV_SERVICE.drawCards(frame, processedCards);
      }
      return frame;
    });
  }
}
