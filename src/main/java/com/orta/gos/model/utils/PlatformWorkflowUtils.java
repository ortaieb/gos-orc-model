package com.orta.gos.model.utils;

import static com.orta.gos.model.rules.BlockEdge.END;
import static com.orta.gos.model.rules.BlockEdge.START;
import static com.orta.gos.model.rules.TrackerUtils.nextStepOnFailure;
import static com.orta.gos.model.rules.TrackerUtils.nextStepOnSuccess;

import com.orta.gos.model.PlatformWorkflow;
import com.orta.gos.model.ProcessOutcome;
import com.orta.gos.model.rules.BlockIndicator;
import com.orta.gos.model.rules.BlockType;
import com.orta.gos.model.rules.Step;
import com.orta.gos.model.rules.Tracker;

import io.vavr.collection.List;
import io.vavr.control.Option;

/**
 * Utility class for PlatformWorkflow-related operations.
 */
public class PlatformWorkflowUtils {

  private PlatformWorkflowUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Updates the tracker based on the process outcome and current workflow state.
   *
   * @param workflow the current workflow
   * @param outcome  the process outcome
   * @return updated tracker
   */
  public static Tracker updateTracker(PlatformWorkflow workflow, ProcessOutcome outcome) {
    var updatedTracker = switch (outcome.getOutcomeCase()) {
      case SUCCESS:
        yield nextStepOnSuccess(workflow.getTracker());
      case FAILURE:
        yield nextStepOnFailure(workflow.getTracker());
      default:
        throw new IllegalStateException("Attempt to handle ProcessOutcome is not SUCCESS/FAILURE");
    };

    return updatedTracker.build();
  }

  private static Tracker.Builder handleSuccess(Tracker tracker, List<Step> steps) {
    var builder = Tracker.newBuilder(tracker);

    if (!tracker.getErrorRaised()) {
      // Success path without errors
      if (isLastStepInBlock(steps, tracker.getCurrentStep(), tracker.getCurrentBlock())) {
        // Move to next block's first step
        var nextBlockStep = findNextBlockFirstStep(steps, tracker.getCurrentStep(), BlockType.FINALLY)
            .fold(() -> tracker.getCurrentStep() + 1, idx -> idx);
        builder.setCurrentStep(nextBlockStep);
      } else {
        // Move to next step in current block
        builder.setCurrentStep(tracker.getCurrentStep() + 1);
      }
    } else {
      // Already in error path, just move to next step
      builder.setCurrentStep(tracker.getCurrentStep() + 1);
    }

    return builder;
  }

  private static Tracker.Builder handleFailure(Tracker tracker, List<Step> steps) {
    var builder = Tracker.newBuilder(tracker);

    if (!tracker.getErrorRaised()) {
      builder
          .setErrorRaised(true)
          .setErrorRaisedStep(tracker.getCurrentStep());
    }

    int nextStep = switch (tracker.getCurrentBlock()) {
      case TYPE_NA, UNRECOGNIZED -> 0;
      case MAIN ->
        findNextBlockFirstStep(steps, 0, BlockType.GENERAL_ERROR).fold(() -> tracker.getCurrentStep() + 1, v -> v);
      case GENERAL_ERROR, FINALLY -> tracker.getCurrentStep() + 1;
    };
    builder.setCurrentStep(nextStep);

    return builder;
  }

  private static boolean isLastStepInBlock(List<Step> steps, long currentStep, BlockType currentBlock) {
    var targetBlockIndicator = BlockIndicator.newBuilder().setType(currentBlock).setEdge(END).build();
    return steps.get((int) currentStep)
        .getIndicatorsList()
        .stream()
        .anyMatch(bi -> bi.equals(targetBlockIndicator));
  }

  private static Option<Integer> findNextBlockFirstStep(List<Step> steps, long startFrom, BlockType targetBlock) {
    var targetBlockIndicator = BlockIndicator.newBuilder().setType(targetBlock).setEdge(START).build();
    var stepId = steps.drop((int) startFrom)
        .indexWhere(step -> step.getIndicatorsList().contains(targetBlockIndicator)) + (int) startFrom;

    return stepId == -1 ? Option.none() : Option.of(stepId);
  }
}
