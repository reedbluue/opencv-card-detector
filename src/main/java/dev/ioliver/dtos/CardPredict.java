package dev.ioliver.dtos;

import dev.ioliver.enums.Ranks;
import dev.ioliver.enums.Suits;
import lombok.Builder;

@Builder
public record CardPredict(
  Ranks rank,
  Suits suit
) {
}
