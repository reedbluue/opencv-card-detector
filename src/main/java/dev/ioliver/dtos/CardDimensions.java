package dev.ioliver.dtos;

import lombok.Builder;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

@Builder
public record CardDimensions(
  CardContour contour,
  double height,
  double width,
  int centerX,
  int centerY
) {
}
