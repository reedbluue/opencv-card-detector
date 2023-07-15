package dev.ioliver.services;

import dev.ioliver.dtos.*;
import dev.ioliver.enums.Ranks;
import dev.ioliver.enums.Suits;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The OpenCVService class provides various image processing operations using the OpenCV library.
 * It includes methods for treating images, finding contours, extracting card information, and drawing on images.
 * This class follows the singleton design pattern.
 * To get an instance of the OpenCVService class, use the {@link #getInstance()} method.
 * To perform image processing operations, use the methods provided by this class.
 * <p>
 * This class requires the OpenCV library to be loaded locally using {@link nu.pattern.OpenCV#loadLocally()}.
 * It also uses environment variables to configure certain parameters.
 * The required environment variables are:
 * - MIN_CARD_AREA: Minimum area of a card contour (default: 20000)
 * - MAX_CARD_AREA: Maximum area of a card contour (default: 30000)
 * - MAX_BLACK_PERCENTAGE: Maximum percentage of black pixels in a card corner (default: 90)
 * You can refer to the CardAreaTest class for consulting the minimum and maximum card area values.
 * </p>
 * <p>
 * This class logs important information using Log4j2.
 * </p>
 * <p>
 * The main operations provided by this class are:
 * - Treating an image by blurring and applying Canny edge detection ({@link #treatImage(Mat)}).
 * - Finding ordered and external contours in an image ({@link #returnOrderedAndExternalContours(Mat)}).
 * - Extracting all card contours from an image ({@link #getAllCardsContours(Mat)}).
 * - Getting the dimensions of a card contour ({@link #getCardDimensions(CardContour)}).
 * - Cutting and flattening a card image ({@link #cutAndFlatCard(Mat, CardDimensions, boolean)}).
 * - Extracting the corner of a card image ({@link #getCorner(Mat)}).
 * - Predicting the rank and suit of a card based on its corner image ({@link #predictCard(CardCorner)}).
 * - Processing a card by extracting its information ({@link #getProcessedCard(CardContour, CardDimensions)}).
 * - Drawing text, contours, and cards on an image ({@link #drawText(Mat, String, int)}, {@link #drawContours(Mat, List)}, {@link #drawCards(Mat, List)}).
 * </p>
 *
 * @author Igor Oliveira
 */
@Log4j2
public class OpenCVService {
  /**
   * INSTANCE field holds the singleton instance of the OpenCVService class.
   */
  private static OpenCVService INSTANCE;

  static {
    nu.pattern.OpenCV.loadLocally();
  }

  /**
   * ENV field holds the Dotenv instance to load environment variables.
   * The following environment variables are used:
   * - MIN_CARD_AREA: Minimum area of a card contour (default: 20000)
   * - MAX_CARD_AREA: Maximum area of a card contour (default: 30000)
   * - MAX_BLACK_PERCENTAGE: Maximum percentage of black pixels in a card corner (default: 90)
   * Use the CardAreaTest class to consult the minimum and maximum card area.
   */
  private final Dotenv ENV = Dotenv.load();
  private final double MIN_CARD_AREA = Optional.of(Double.parseDouble(ENV.get("MIN_CARD_AREA"))).orElse(20000D);
  private final double MAX_CARD_AREA = Optional.of(Double.parseDouble(ENV.get("MAX_CARD_AREA"))).orElse(30000D);
  private final int MAX_BLACK_PER_CENT = Optional.of(Integer.parseInt(ENV.get("MAX_BLACK_PER_CENT"))).orElse(90);

  /**
   * Constructs a new OpenCVService object.
   * This constructor is private to enforce the singleton design pattern.
   * It initializes the OpenCVService and logs important information.
   */
  private OpenCVService() {
    log.info("OpenCVService started!");
    log.info("MIN_CARD_AREA: " + MIN_CARD_AREA + " MAX_CARD_AREA: " + MAX_CARD_AREA + " MAX_BLACK_PER_CENT: " + MAX_BLACK_PER_CENT);
  }

  /**
   * Returns the singleton instance of the OpenCVService class.
   * If the instance doesn't exist, it creates a new one.
   *
   * @return The OpenCVService instance.
   */
  public static OpenCVService getInstance() {
    if (INSTANCE == null) INSTANCE = new OpenCVService();
    return INSTANCE;
  }

  /**
   * Applies image processing operations to the input frame.
   * It blurs the frame using a 2x2 kernel and applies Canny edge detection.
   *
   * @param frame The input frame to be processed.
   * @return The processed frame with highlighted edges.
   */
  public Mat treatImage(Mat frame) {
    Mat blured = new Mat();
    Imgproc.blur(frame, blured, new Size(2, 2));
    Mat highlightedEdges = new Mat();
    Imgproc.Canny(blured, highlightedEdges, 50, 200);
    return highlightedEdges;
  }

  /**
   * Finds the ordered and external contours in the input frame.
   * It uses the {@link #treatImage(Mat)} method to preprocess the frame and then finds the contours using the findContours method.
   * The contours are ordered based on their area, and only the external contours are returned.
   *
   * @param frame The input frame to find contours in.
   * @return The list of external contours ordered by area.
   */
  public List<MatOfPoint> returnOrderedAndExternalContours(Mat frame) {
    Mat treatedImage = treatImage(frame);

    List<MatOfPoint> contours = new ArrayList<>();
    Mat hierarchy = new Mat();
    Imgproc.findContours(treatedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

    List<Integer> indexList = new ArrayList<>(contours.size() > 0 ? IntStream.rangeClosed(0, contours.size() - 1).boxed().toList() : new ArrayList<>());
    indexList.sort((i1, i2) -> Double.compare(Imgproc.contourArea(contours.get(i2)), Imgproc.contourArea(contours.get(i1))));

    List<MatOfPoint> orderedContours = new ArrayList<>();
    List<double[]> orderedHierarchy = new ArrayList<>();
    indexList.forEach(i -> {
      orderedContours.add(contours.get(i));
      orderedHierarchy.add(hierarchy.get(0, i));
    });

    List<MatOfPoint> externalContours = new ArrayList<>();
    for (int i = 0; i < orderedContours.size(); i++)
      if (orderedHierarchy.get(i)[3] < 0) externalContours.add(orderedContours.get(i));

    return externalContours;
  }

  /**
   * Extracts all card contours from the input frame.
   * It uses the {@link #returnOrderedAndExternalContours(Mat)} method to get the ordered and external contours,
   * and then filters the contours based on their area and shape to extract the card contours.
   *
   * @param frame The input frame to extract card contours from.
   * @return The list of card contours.
   */
  public List<CardContour> getAllCardsContours(Mat frame) {
    List<MatOfPoint> externContours = returnOrderedAndExternalContours(frame);
    List<CardContour> cardsContours = new ArrayList<>();

    for (MatOfPoint contour : externContours) {
      double area = Imgproc.contourArea(contour);
      MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
      double perimeter = Imgproc.arcLength(contour2f, true);
      MatOfPoint2f approx = new MatOfPoint2f();
      Imgproc.approxPolyDP(contour2f, approx, 0.02 * perimeter, true);
      if (approx.total() == 4 && area > MIN_CARD_AREA && area < MAX_CARD_AREA)
        cardsContours.add(CardContour.builder().originalFrame(frame).contour(contour).contour2f(contour2f).area(area).perimeter(perimeter).approx(approx).build());
    }

    return cardsContours;
  }

  /**
   * Calculates the dimensions of a card contour.
   * It calculates the center coordinates, width, and height of the card contour's bounding rectangle.
   *
   * @param contour The card contour to calculate dimensions for.
   * @return The dimensions of the card contour.
   */
  public CardDimensions getCardDimensions(CardContour contour) {
    MatOfPoint2f average = new MatOfPoint2f();
    Core.reduce(contour.approx(), average, 0, Core.REDUCE_AVG);
    double[] averageArray = average.get(0, 0);
    int centX = (int) averageArray[0];
    int centY = (int) averageArray[1];
    Rect rect = Imgproc.boundingRect(contour.contour());
    return CardDimensions.builder().contour(contour).width(rect.width).height(rect.height).centerX(centX).centerY(centY).build();
  }

  /**
   * Cuts and flattens a card from the original frame based on the card dimensions.
   * It uses perspective transformation to cut and flatten the card image.
   * The output image is resized to a fixed width and height.
   *
   * @param originalFrame  The original frame containing the card.
   * @param cardDimensions The dimensions of the card contour.
   * @param invert         Indicates whether to invert the card image.
   * @return The cut and flattened card image.
   */
  public Mat cutAndFlatCard(Mat originalFrame, CardDimensions cardDimensions, boolean invert) {
    int outputWidth = 200, outputHeight = 300;
    Point[] outputArray;

    if (!invert) {
      outputArray = new Point[]{new Point(0, outputHeight), new Point(0, 0), new Point(outputWidth, 0), new Point(outputWidth, outputHeight)};
    } else {
      outputArray = new Point[]{new Point(0, 0), new Point(outputWidth, 0), new Point(outputWidth, outputHeight), new Point(0, outputHeight)};
    }

    MatOfPoint2f perspectiveTransform = new MatOfPoint2f();
    perspectiveTransform.fromList(List.of(outputArray));
    Mat transformationMatrix = Imgproc.getPerspectiveTransform(cardDimensions.contour().approx(), perspectiveTransform);

    Mat outputImage = new Mat(outputHeight, outputWidth, originalFrame.type());
    Imgproc.warpPerspective(originalFrame, outputImage, transformationMatrix, outputImage.size());

    Imgproc.cvtColor(outputImage, outputImage, Imgproc.COLOR_BGR2GRAY);
    Core.flip(outputImage, outputImage, 1);

    return outputImage;
  }

  /**
   * Extracts the corner of a flattened card image.
   * It takes a submat of the card image to extract the corner region.
   * The corner region is then resized and thresholded to get the corner rank and suit images.
   *
   * @param flattenedCardImage The flattened card image.
   * @return The corner information including the full corner image, corner rank image, and corner suit image.
   */
  public CardCorner getCorner(Mat flattenedCardImage) {
    final int CORNER_WIDTH = 50;
    final int CORNER_HEIGHT = 125;
    final int zoomFactor = 4;

    Mat corner = flattenedCardImage.submat(new Rect(0, 0, CORNER_WIDTH, CORNER_HEIGHT));
    Mat cornerZoom = new Mat();
    Imgproc.resize(corner, cornerZoom, new Size(), zoomFactor, zoomFactor);

    double[] whiteLevel = cornerZoom.get(5, (CORNER_WIDTH * zoomFactor / 2));
    double threshLevel = whiteLevel[0] - 60;
    if (threshLevel <= 0) threshLevel = 1;

    Mat threshCorner = new Mat();
    Imgproc.threshold(cornerZoom, threshCorner, threshLevel, 255, Imgproc.THRESH_BINARY_INV);

    int offSet = 65 * zoomFactor;

    Mat cornerUp = threshCorner.submat(new Rect(0, 0, threshCorner.width(), offSet));
    Mat cornerDown = threshCorner.submat(new Rect(0, offSet, threshCorner.width(), threshCorner.height() - offSet));

    return CardCorner.builder().fullCorner(threshCorner).cornerRank(cornerUp).cornerSuit(cornerDown).build();
  }


  /**
   * Predicts the rank and suit of a card based on its corner images.
   * It performs template matching to find the best match for the corner rank and suit images.
   * The rank and suit with the highest match score are selected as the predicted values.
   *
   * @param corner The corner information including the corner rank and suit images.
   * @return The predicted rank and suit of the card.
   */
  public CardPredict predictCard(CardCorner corner) {
    final Ranks[] rank = new Ranks[]{Ranks.A};
    final Suits[] suit = new Suits[]{Suits.SPADES};

    final double[] maxVal = {Double.MIN_VALUE};
    Ranks.getList().forEach(r -> {
      Mat result = new Mat();
      Mat src = new Mat();

      corner.cornerRank().convertTo(src, CvType.CV_8U);
      if (src.channels() > 1) Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);

      Imgproc.matchTemplate(src, r.getImgReference(), result, Imgproc.TM_CCOEFF_NORMED);
      Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
      if (mmr.maxVal > maxVal[0]) {
        maxVal[0] = mmr.maxVal;
        rank[0] = r;
      }
    });

    maxVal[0] = Double.MIN_VALUE;
    Suits.getList().forEach(s -> {
      Mat result = new Mat();
      Mat src = new Mat();

      corner.cornerSuit().convertTo(src, CvType.CV_8U);
      if (src.channels() > 1) Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);

      Imgproc.matchTemplate(src, s.getImgReference(), result, Imgproc.TM_CCOEFF_NORMED);
      Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
      if (mmr.maxVal > maxVal[0]) {
        maxVal[0] = mmr.maxVal;
        suit[0] = s;
      }
    });

    return CardPredict.builder().rank(rank[0]).suit(suit[0]).build();
  }

  /**
   * Processes a card by extracting its information.
   * It cuts and flattens the card image, extracts the corner information, and predicts the rank and suit of the card.
   * If the percentage of black pixels in the corner exceeds the maximum threshold,
   * it cuts and flattens the card image again with an inverted perspective transformation to improve prediction accuracy.
   *
   * @param cardContour    The card contour information.
   * @param cardDimensions The dimensions of the card contour.
   * @return The processed card object containing the card image, contour information, dimensions, and predicted rank and suit.
   */
  public Card getProcessedCard(CardContour cardContour, CardDimensions cardDimensions) {
    Mat cardImg = cutAndFlatCard(cardContour.originalFrame(), cardDimensions, false);
    CardCorner corner = getCorner(cardImg);

    int blackPixels = 0;
    for (int i = 0; i < corner.fullCorner().rows(); i++) {
      for (int j = 0; j < corner.fullCorner().cols(); j++) {
        if (corner.fullCorner().get(i, j)[0] == 0) {
          blackPixels++;
        }
      }
    }

    double totalPixels = corner.fullCorner().rows() * corner.fullCorner().cols();
    double blackPercentage = (blackPixels / totalPixels) * 100;

    if (blackPercentage > MAX_BLACK_PER_CENT) {
      cardImg = cutAndFlatCard(cardContour.originalFrame(), cardDimensions, true);
      corner = getCorner(cardImg);
    }

    CardPredict cardPredict = predictCard(corner);

    return Card.builder().cardImage(cardImg).contour(cardContour).dimensions(cardDimensions).predict(cardPredict).build();
  }

  /**
   * Draws text on the input frame.
   *
   * @param frame The input frame to draw text on.
   * @param text  The text to be drawn.
   * @param line  The line number to position the text vertically.
   */
  public void drawText(Mat frame, String text, int line) {
    double fontScale = 2;
    Size textSize = Imgproc.getTextSize(text, 1, fontScale, 10, new int[]{0});
    Point textPos = new Point(10, textSize.height * line + 20 * line);
    Imgproc.putText(frame, text, textPos, 1, fontScale, new Scalar(0, 0, 0), 10);
    Imgproc.putText(frame, text, textPos, 1, fontScale, new Scalar(0, 0, 255), 2);
  }

  /**
   * Draws the card contours on the input frame.
   *
   * @param frame        The input frame to draw the card contours on.
   * @param cardContours The list of card contours.
   */
  public void drawContours(Mat frame, List<CardContour> cardContours) {
    List<MatOfPoint> contours = cardContours.stream().map(CardContour::contour).toList();
    Imgproc.drawContours(frame, contours, -1, new Scalar(0, 0, 0), 5);
    Imgproc.drawContours(frame, contours, -1, new Scalar(255, 0, 0), 2);
  }

  /**
   * Draws the cards on the input frame.
   * It draws the card contours and predicts the rank and suit for each card, and draws the corresponding text on the frame.
   *
   * @param frame The input frame to draw the cards on.
   * @param cards The list of cards.
   */
  public void drawCards(Mat frame, List<Card> cards) {
    List<CardContour> cardsContours = cards.stream().map(Card::contour).toList();
    drawContours(frame, cardsContours);

    cards.forEach(c -> {
      String text;
      double fontScale = c.dimensions().contour().contour().width() * 1.5;
      if (c.predict().suit() == Suits.JOKER || c.predict().rank() == Ranks.JOKER) text = "JOKER";
      else text = c.predict().rank().getLabel() + " of " + c.predict().suit().getLabel();

      Size textSize = Imgproc.getTextSize(text, 1, fontScale, 10, new int[]{0});

      Point rankPos = new Point(
        c.dimensions().centerX() - textSize.width / 2,
        c.dimensions().centerY() + c.dimensions().height() / 2 + textSize.height + 10
      );

      Imgproc.putText(frame, text, rankPos, 1, fontScale, new Scalar(0, 0, 0), 10);
      Imgproc.putText(frame, text, rankPos, 1, fontScale, new Scalar(255, 0, 0), 2);
    });
  }
}
