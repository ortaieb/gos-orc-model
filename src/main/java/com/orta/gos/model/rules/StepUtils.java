package com.orta.gos.model.rules;

import static com.orta.gos.model.rules.BlockEdge.END;
import static com.orta.gos.model.rules.BlockEdge.START;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;

/**
 * Utility class for Step-related operations.
 */
public class StepUtils {

  private StepUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Marks the first and last steps in a list with block indicators.
   * First step gets START edge and last step gets END edge.
   * For single step lists, only START edge is applied.
   *
   * @param blockType the block type to set in the indicators
   * @param input     the list of steps to process
   * @return a new list with block indicators set on edge steps
   */
  public static List<Step> markEdges(final BlockType blockType, final List<Step> input) {
    if (input == null || input.isEmpty()) {
      return List.empty();
    }

    if (input.size() == 1) {
      return List.of(blockDecoration(input.head(), blockType, START, END));
    }

    Step firstStep = blockDecoration(input.head(), blockType, START);
    Step lastStep = blockDecoration(input.last(), blockType, END);

    return input.size() == 2
        ? List.of(firstStep, lastStep)
        : List.of(firstStep).appendAll(input.tail().dropRight(1)).append(lastStep);
  }

  private static Step blockDecoration(final Step step, final BlockType type, final BlockEdge... edges) {
    return step.toBuilder()
        .addAllIndicators(indicators(type, edges))
        .build();
  }

  private static Seq<BlockIndicator> indicators(final BlockType type, final BlockEdge... edges) {
    return List.ofAll(Stream.of(edges))
        .map(edge -> BlockIndicator.newBuilder().setType(type).setEdge(edge).build());
  }
}
