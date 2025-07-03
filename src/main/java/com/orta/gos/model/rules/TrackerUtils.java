package com.orta.gos.model.rules;

import static com.orta.gos.model.rules.BlockEdge.END;
import static com.orta.gos.model.rules.BlockEdge.START;
import static com.orta.gos.model.rules.BlockType.FINALLY;
import static com.orta.gos.model.rules.BlockType.TYPE_NA;
import static io.vavr.control.Either.left;

import io.quarkus.logging.Log;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;

public class TrackerUtils {

  public static Tracker.Builder updateStepsBlocks(final Tracker tracker, Map<BlockType, List<Step>> stepsMap) {
    var trackerBuilder = Tracker.newBuilder(tracker);

    List.of(BlockType.values())
        .foldLeft(
            Tuple.of(trackerBuilder, tracker.getCurrentStep()),
            (acc, blockType) -> {
              var steps = stepsMap.getOrElse(blockType, List.empty());
              if (steps.isEmpty()) {
                return acc;
              }

              var currentEnd = acc._2;
              var start = currentEnd + 1;
              var end = start + steps.size() - 1;

              return Tuple.of(acc._1, end);
            });

    return trackerBuilder;
  }

  /**
   * Orders steps by block type and marks the first and last steps of each block
   * with indicators.
   * The blocks are ordered according to the BlockType enum order:
   * MAIN -> GENERAL_ERROR -> FINALLY
   *
   * @param stepsMap Map containing steps grouped by block type
   * @return Ordered list of steps with block indicators added
   */
  public static final List<Step> orderedSteps(final Map<BlockType, List<Step>> stepsMap) {
    return List.of(BlockType.values()).foldLeft(List.empty(), (accumSteps, blockType) -> {
      return accumSteps.appendAll(StepUtils.markEdges(blockType, stepsMap.getOrElse(blockType, List.empty())));
    });
  }

  /**
   * Calculates the start and end indices for each block type in a list of steps.
   * The indices are calculated relative to a provided starting index.
   *
   * Each step can have block indicators marking it as the start or end of a
   * block.
   * This method processes those indicators to build BlockRange objects
   * containing:
   * - The block type
   * - The absolute start index (currentStepIdx + relative start index)
   * - The absolute end index (currentStepIdx + relative end index)
   *
   * @param currentStepIdx The base index to offset all block ranges from
   * @param steps          The list of steps to analyze for block indicators
   * @return A map from block type number to BlockRange containing the absolute
   *         indices
   */
  public static final Map<Integer, BlockRange> blockRanges(Integer currentStepIdx, List<Step> steps) {
    return steps.zipWithIndex().foldLeft(HashMap.empty(), (m, tuple) -> {
      var indicators = tuple._1.getIndicatorsList();
      if (indicators.isEmpty()) {
        return m;
      } else {
        var indicatorsMap = List.ofAll(indicators).toMap(k -> k.getEdge(), v -> tuple._2);

        var type = indicators.get(0).getType();

        var range = m.get(type.getNumber()).map(BlockRange::newBuilder)
            .getOrElse(BlockRange.newBuilder().setType(type));
        if (indicatorsMap.containsKey(START))
          range.setStartIdx(currentStepIdx + indicatorsMap.getOrElse(START, -1));
        if (indicatorsMap.containsKey(END))
          range.setEndIdx(currentStepIdx + indicatorsMap.getOrElse(END, -1));

        return m.put(type.getNumber(), range.build());
      }
    });
  }

  public static Tracker.Builder nextStepOnSuccess(final Tracker tracker) {
    var details = TrackerDetails.instance(tracker);
    var lastStep = details.lastStepInBlock();

    if (lastStep) {
      Either<String, Tuple2<Integer, BlockType>> result = switch (tracker.getCurrentBlock()) {
        case MAIN:
        case GENERAL_ERROR:
          yield details.jumpToBlock(FINALLY);
        case FINALLY:
        case UNRECOGNIZED:
        case TYPE_NA:
          yield left("Terminating workflow, nowhere to advance");
      };

      var nextStepIndex = result.fold(error -> {
        Log.error(error);
        return Tuple.of(tracker.getTermination(), TYPE_NA);
      }, v -> v);

      return Tracker.newBuilder(tracker).setCurrentBlock(nextStepIndex._2).setCurrentStep(nextStepIndex._1);
    } else {
      return Tracker.newBuilder(tracker).setCurrentStep(tracker.getCurrentStep() + 1);
    }
  }

  public static Tracker.Builder nextStepOnFailure(final Tracker tracker) {
    var details = TrackerDetails.instance(tracker);

    Either<String, Tuple2<Integer, BlockType>> nextStep = switch (tracker.getCurrentBlock()) {
      case MAIN:
        yield details.jumpToBlock(BlockType.GENERAL_ERROR);
      default:
        yield left(String.format("Failed on %s block, terminating", tracker.getCurrentBlock()));
    };

    var newPosition = nextStep.fold(errorMessage -> {
      Log.errorf("Next Step on Failure: %s", errorMessage);
      return Tuple.of(tracker.getTermination(), TYPE_NA);
    }, v -> v);

    return Tracker.newBuilder(tracker)
        .setCurrentBlock(newPosition._2)
        .setCurrentStep(newPosition._1)
        .setErrorRaised(true)
        .setErrorRaisedStep(tracker.getErrorRaised() ? Math.min(tracker.getCurrentStep(), tracker.getErrorRaisedStep())
            : tracker
                .getCurrentStep());

  }

}
