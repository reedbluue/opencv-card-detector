package dev.ioliver.dtos;

import lombok.Builder;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

@Builder
public record CardContour(
  Mat originalFrame,
  MatOfPoint contour,
  MatOfPoint2f contour2f,
  double area,
  double perimeter,
  MatOfPoint2f approx
) {
}
