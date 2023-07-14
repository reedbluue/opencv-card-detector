package dev.ioliver.dtos;

import lombok.Builder;
import org.opencv.core.Mat;

@Builder
public record CardCorner(
  Mat fullCorner,
  Mat cornerRank,
  Mat cornerSuit
) {
}
