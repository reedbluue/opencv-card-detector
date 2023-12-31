package dev.ioliver;

import dev.ioliver.dtos.Card;
import dev.ioliver.dtos.CardContour;
import dev.ioliver.dtos.CardDimensions;
import dev.ioliver.services.CamService;
import dev.ioliver.services.OpenCVService;

import java.util.List;

public class ExampleGame {
  private static final OpenCVService OPENCV_SERVICE = OpenCVService.getInstance();

  public static void main(String[] args) {
    final int[] step = {0};
    final int[] score = {0};
    final long[] stepTime = {System.currentTimeMillis()};
    CamService camService = new CamService(1, frame -> {
      OPENCV_SERVICE.drawText(frame, "Can you twenty one?", 1);
      OPENCV_SERVICE.drawText(frame, "Score: " + score[0], 2);
      List<CardContour> cardsContours = OPENCV_SERVICE.getAllCardsContours(frame);
      List<Card> cards = cardsContours.stream().map(cc -> {
        CardDimensions cardDimensions = OPENCV_SERVICE.getCardDimensions(cc);
        return OPENCV_SERVICE.getProcessedCard(cc, cardDimensions);
      }).toList();
      OPENCV_SERVICE.drawCards(frame, cards);
      switch (step[0]) {
        case 0 -> {
          if (cardsContours.size() > 1) {
            OPENCV_SERVICE.drawText(frame, "More than one card detected.", 3);
            stepTime[0] = System.currentTimeMillis();
            return frame;
          }
          if (cardsContours.size() < 1) {
            OPENCV_SERVICE.drawText(frame, "No cards detected", 3);
            stepTime[0] = System.currentTimeMillis();
            return frame;
          }
          if (System.currentTimeMillis() - stepTime[0] > 500)
            ++step[0];
          return frame;
        }
        case 1 -> {
          Card card = cards.get(0);
          switch (card.predict().rank().toString()) {
            case "N2" -> score[0] += 2;
            case "N3" -> score[0] += 3;
            case "N4" -> score[0] += 4;
            case "N5" -> score[0] += 5;
            case "N6" -> score[0] += 6;
            case "N7" -> score[0] += 7;
            case "N8" -> score[0] += 8;
            case "N9" -> score[0] += 9;
            case "N10", "A", "K", "Q", "J" -> score[0] += 10;
            default -> {
            }
          }
          ++step[0];
          return frame;
        }
        case 2 -> {
          if (score[0] < 21) {
            if (cardsContours.isEmpty()) step[0] = 0;
            OPENCV_SERVICE.drawText(frame, "Remove the card.", 3);
            return frame;
          } else if (score[0] == 21) {
            step[0] = 3;
            return frame;
          } else {
            step[0] = 4;
            return frame;
          }
        }
        case 3 -> {
          OPENCV_SERVICE.drawText(frame, "Win! Restart the program to play again.", 3);
          return frame;
        }
        case 4 -> {
          OPENCV_SERVICE.drawText(frame, "You lose! Restart the program to play again.", 3);
          return frame;
        }
        default -> {
          return frame;
        }
      }
    });
  }
}
