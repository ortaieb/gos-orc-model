package com.orta.gos.model.rules;

import static io.vavr.API.Stream;

import io.vavr.collection.List;
import io.vavr.collection.Map;

/**
 * Utility class for handling Step-related operations in the rules system.
 */
public class RulesUtils {

  private RulesUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Creates a flattened list of steps from a map of block types to step lists,
   * adding appropriate BlockIndicators to the first and last steps of each block.
   *
   * @param rulesSteps map of BlockType to its corresponding list of steps
   * @return flattened list of steps with block indicators set
   */
  public static List<Step> fromRule(final Map<BlockType, List<Step>> rulesSteps) {
    return List.of(BlockType.values())
        .flatMap(blockType -> StepUtils.markEdges(
            blockType,
            rulesSteps.getOrElse(blockType, List.empty())));
  }

}
