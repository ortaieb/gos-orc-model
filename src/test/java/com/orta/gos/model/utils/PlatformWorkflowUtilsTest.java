package com.orta.gos.model.utils;

import static com.orta.gos.model.rules.BlockEdge.START;
import static com.orta.gos.model.rules.BlockEdge.END;
import static com.orta.gos.model.rules.BlockType.MAIN;
import static com.orta.gos.model.rules.BlockType.MAIN_VALUE;
import static com.orta.gos.model.rules.BlockType.TYPE_NA;
import static com.orta.gos.model.rules.BlockType.FINALLY;
import static com.orta.gos.model.rules.BlockType.FINALLY_VALUE;
import static com.orta.gos.model.rules.BlockType.GENERAL_ERROR;
import static com.orta.gos.model.rules.BlockType.GENERAL_ERROR_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.orta.gos.model.ProcessOutcome;
import com.orta.gos.model.ProcessOutcomeSucces;
import com.orta.gos.model.ProcessOutcomeFailure;
import com.orta.gos.model.PlatformWorkflow;

import com.orta.gos.model.rules.Step;
import com.orta.gos.model.rules.BlockEdge;
import com.orta.gos.model.rules.BlockIndicator;
import com.orta.gos.model.rules.BlockRange;
import com.orta.gos.model.rules.BlockType;
import com.orta.gos.model.rules.Tracker;
import com.orta.gos.model.utils.PlatformWorkflowUtils;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;

@DisplayName("PlatformWorkflowUtils")
class PlatformWorkflowUtilsTest {

  final static Step step1 = createStep("step1", MAIN, START);
  final static Step step2 = createStep("step2");
  final static Step step3 = createStep("step3", MAIN, END);
  final static Step step4 = createStep("error1", GENERAL_ERROR, START);
  final static Step step5 = createStep("error2", GENERAL_ERROR, END);
  final static Step step6 = createStep("finally1", FINALLY, START);
  final static Step step7 = createStep("finally2", FINALLY, END);

  final static List<Step> allSteps = List.of(step1, step2, step3, step4, step5, step6, step7);

  final static Tracker.Builder allStepsTracker = Tracker.newBuilder()
      .setErrorRaised(false)
      .setTermination(allSteps.length())
      .putRanges(MAIN_VALUE,
          BlockRange.newBuilder().setType(MAIN).setStartIdx(0).setEndIdx(2).build())
      .putRanges(GENERAL_ERROR_VALUE,
          BlockRange.newBuilder().setType(GENERAL_ERROR).setStartIdx(3).setEndIdx(4).build())
      .putRanges(FINALLY_VALUE,
          BlockRange.newBuilder().setType(FINALLY).setStartIdx(5).setEndIdx(6).build());

  @Nested
  @DisplayName("updateTracker")
  class UpdateTrackerTest {

    @Test
    @DisplayName("should move to next step on success")
    void test0() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .setTracker(buildTracker(0, 1, 0, MAIN, false, null))
          .addSteps(step1)
          .addSteps(step2)
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setSuccess(ProcessOutcomeSucces.newBuilder().build())
          .build();

      var expected = buildTracker(0, 1, 1, MAIN, false, null).build();

      // Act
      var result = PlatformWorkflowUtils.updateTracker(workflow, outcome);

      // Assert
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("should switch to error block on failure")
    void test1() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .setTracker(allStepsTracker.setCurrentBlock(MAIN).setCurrentStep(1))
          .addAllSteps(allSteps)
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setFailure(ProcessOutcomeFailure.newBuilder()
              .setErrorMessage("Test error")
              .build())
          .build();

      var expected = allStepsTracker
          .setCurrentBlock(GENERAL_ERROR)
          .setCurrentStep(3)
          .setErrorRaised(true)
          .setErrorRaisedStep(1)
          .build();

      // Act
      var result = PlatformWorkflowUtils.updateTracker(workflow, outcome);

      // Assert
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("should move to finally block after main block")
    void test2() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .setTracker(allStepsTracker.setCurrentBlock(MAIN).setCurrentStep(2))
          .addAllSteps(allSteps)
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setSuccess(ProcessOutcomeSucces.newBuilder().build())
          .build();

      var expected = allStepsTracker
          .setCurrentBlock(FINALLY)
          .setCurrentStep(5)
          .build();
      // Act
      var result = PlatformWorkflowUtils.updateTracker(workflow, outcome);

      // Assert
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("should not change tracker on error in error path")
    void test3() {
      // Arrange
      var tracker = allStepsTracker
          .setCurrentBlock(GENERAL_ERROR)
          .setCurrentStep(3)
          .setErrorRaised(true)
          .setErrorRaisedStep(1);

      var workflow = PlatformWorkflow.newBuilder()
          .setTracker(tracker)
          .addAllSteps(allSteps)
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setFailure(ProcessOutcomeFailure.newBuilder()
              .setErrorMessage("Test error")
              .build())
          .build();

      var expected = allStepsTracker
          .setCurrentBlock(TYPE_NA)
          .setCurrentStep(allSteps.length())
          .setErrorRaised(true)
          .setErrorRaisedStep(1)
          .build();

      // Act
      var result = PlatformWorkflowUtils.updateTracker(workflow, outcome);

      // Assert
      assertThat(result).isEqualTo(expected);
    }
  }

  private static Step createStep(final String stepName) {
    return createStep(stepName, null, null);
  }

  private static Step createStep(final String stepName, final BlockType type, final BlockEdge... edges) {
    var stepBuilder = Step.newBuilder()
        .setName(stepName)
        .setAddress(stepName);
    if (type != null && edges != null) {
      stepBuilder.addAllIndicators(indicators(type, edges));
    }
    return stepBuilder.build();
  }

  private static Seq<BlockIndicator> indicators(final BlockType type, final BlockEdge... edges) {
    return List.ofAll(Stream.of(edges))
        .map(edge -> BlockIndicator.newBuilder().setType(type).setEdge(edge).build());
  }

  static Tracker.Builder buildTracker(final int stepStart, final int stepEnd, final int current, final BlockType block,
      final Boolean error,
      final Integer errorStep) {
    var builder = Tracker.newBuilder()
        .setCurrentStep(current)
        .setCurrentBlock(block)
        .putRanges(block.getNumber(), BlockRange.newBuilder().setStartIdx(stepStart).setEndIdx(stepEnd).build());

    if (error != null) {
      builder.setErrorRaised(error);
    }

    if (errorStep != null) {
      builder.setErrorRaisedStep(errorStep);
    }

    return builder;
  }
}
