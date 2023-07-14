package dev.ioliver.dtos;

import lombok.Builder;
import org.opencv.core.Mat;

@Builder
public record Card(
  CardDimensions dimensions,
  CardContour contour,
  Mat cardImage,
  CardPredict predict
) {
}
