package com.orta.gos.model.utils;

import static com.orta.gos.model.rules.BlockEdge.END;
import static com.orta.gos.model.rules.BlockEdge.START;
import static com.orta.gos.model.rules.BlockType.MAIN;
import static com.orta.gos.model.rules.BlockType.MAIN_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.orta.gos.model.Payload;
import com.orta.gos.model.PlatformMessage;
import com.orta.gos.model.PlatformWorkflow;
import com.orta.gos.model.ProcessOutcome;
import com.orta.gos.model.ProcessOutcomeFailure;
import com.orta.gos.model.ProcessOutcomeSucces;
import com.orta.gos.model.StringBody;
import com.orta.gos.model.rules.BlockIndicator;
import com.orta.gos.model.rules.BlockRange;
import com.orta.gos.model.rules.Step;
import com.orta.gos.model.rules.Tracker;
import com.orta.gos.model.utils.PlatformMessageUtils;

import io.vavr.collection.List;
import io.vavr.control.Either;

@DisplayName("PlatformMessageUtils")
class PlatformMessageUtilsTest {

  private static final Step STEP = Step.newBuilder()
      .setName("test-step")
      .addIndicators(BlockIndicator.newBuilder()
          .setType(MAIN)
          .setEdge(START)
          .build())
      .addIndicators(BlockIndicator.newBuilder()
          .setType(MAIN)
          .setEdge(END)
          .build())
      .putAttributes("key1", "value1")
      .build();

  private static final Tracker TRACKER = Tracker.newBuilder()
      .setCurrentStep(0)
      .setCurrentBlock(MAIN)
      .setErrorRaised(false)
      .putRanges(MAIN_VALUE, BlockRange.newBuilder().setType(MAIN).setStartIdx(0).setEndIdx(1).build())
      .build();

  private static final Payload INPUT_PAYLOAD = Payload.newBuilder()
      .setStringBody(StringBody.newBuilder().setBody("input").build())
      .build();

  @Nested
  @DisplayName("processInput")
  class ProcessInputTest {

    @Test
    @DisplayName("should extract last payload and first step attributes")
    void test0() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .addSteps(STEP)
          .setTracker(TRACKER)
          .build();

      var message = PlatformMessage.newBuilder()
          .setWorkflowLog(workflow)
          .addPayloads(INPUT_PAYLOAD)
          .build();

      var utils = new PlatformMessageUtils(message);

      // Act
      var result = utils.processInput();

      // Assert
      assertThat(result.getPayloadsList()).containsExactly(INPUT_PAYLOAD);
      assertThat(result.getAttributesMap()).containsExactlyEntriesOf(STEP.getAttributesMap());
    }
  }

  @Nested
  @DisplayName("appendOutcome")
  class AppendOutcomeTest {

    @Test
    @DisplayName("should append success outcome with payload and headers")
    void test0() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .addSteps(STEP)
          .setTracker(TRACKER)
          .build();

      var message = PlatformMessage.newBuilder()
          .setWorkflowLog(workflow)
          .addPayloads(INPUT_PAYLOAD)
          .build();

      var outputPayload = Payload.newBuilder()
          .setStringBody(StringBody.newBuilder().setBody("output").build())
          .build();

      var success = ProcessOutcomeSucces.newBuilder()
          .setOutputPayload(outputPayload)
          .putHeaders("header1", "value1")
          .putHeaders("header2", "value2")
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setSuccess(success)
          .build();

      var utils = new PlatformMessageUtils(message);

      // Act
      var result = utils.appendOutcome(outcome);

      // Assert
      assertThat(result.getPayloadsList())
          .containsExactly(INPUT_PAYLOAD, outputPayload);
      assertThat(result.getHeadersMap())
          .containsExactlyEntriesOf(success.getHeadersMap());
      assertThat(result.getWorkflowLog().getTracker().getCurrentStep())
          .isEqualTo(1); // Verify tracker was updated
    }

    @Test
    @DisplayName("should handle success outcome without payload")
    void test1() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .addSteps(STEP)
          .setTracker(TRACKER)
          .build();

      var message = PlatformMessage.newBuilder()
          .setWorkflowLog(workflow)
          .addPayloads(INPUT_PAYLOAD)
          .build();

      var success = ProcessOutcomeSucces.newBuilder()
          .putHeaders("header1", "value1")
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setSuccess(success)
          .build();

      var utils = new PlatformMessageUtils(message);

      // Act
      var result = utils.appendOutcome(outcome);

      // Assert
      assertThat(result.getPayloadsList())
          .containsExactly(INPUT_PAYLOAD); // Original payload only
      assertThat(result.getHeadersMap())
          .containsExactlyEntriesOf(success.getHeadersMap());
    }

    @Test
    @DisplayName("should merge new headers with existing ones")
    void test4() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .addSteps(STEP)
          .setTracker(TRACKER)
          .build();

      var message = PlatformMessage.newBuilder()
          .setWorkflowLog(workflow)
          .addPayloads(INPUT_PAYLOAD)
          .putHeaders("existing-key", "value1")
          .build();

      var success = ProcessOutcomeSucces.newBuilder()
          .putHeaders("new-key", "value2")
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setSuccess(success)
          .build();

      var utils = new PlatformMessageUtils(message);

      // Act
      var result = utils.appendOutcome(outcome);

      // Assert
      assertThat(result.getHeadersMap())
          .containsOnly(
              entry("existing-key", "value1"),
              entry("new-key", "value2"));
    }

    @Test
    @DisplayName("should preserve existing header value on key collision")
    void test5() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .addSteps(STEP)
          .setTracker(TRACKER)
          .build();

      var message = PlatformMessage.newBuilder()
          .setWorkflowLog(workflow)
          .addPayloads(INPUT_PAYLOAD)
          .putHeaders("my-key", "value1")
          .build();

      var success = ProcessOutcomeSucces.newBuilder()
          .putHeaders("my-key", "value2")
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setSuccess(success)
          .build();

      var utils = new PlatformMessageUtils(message);

      // Act
      var result = utils.appendOutcome(outcome);

      // Assert
      assertThat(result.getHeadersMap())
          .containsOnly(
              entry("my-key", "value1") // Original value preserved
          );
    }

    @Test
    @DisplayName("should handle failure outcome")
    void test2() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .addSteps(STEP)
          .setTracker(TRACKER)
          .build();

      var message = PlatformMessage.newBuilder()
          .setWorkflowLog(workflow)
          .addPayloads(INPUT_PAYLOAD)
          .putHeaders("original", "header")
          .build();

      var failure = ProcessOutcomeFailure.newBuilder()
          .setErrorMessage("test error")
          .build();

      var outcome = ProcessOutcome.newBuilder()
          .setFailure(failure)
          .build();

      var utils = new PlatformMessageUtils(message);

      // Act
      var result = utils.appendOutcome(outcome);

      // Assert
      assertThat(result.getPayloadsList())
          .containsExactly(INPUT_PAYLOAD); // No change to payloads
      assertThat(result.getHeadersMap())
          .containsExactlyEntriesOf(message.getHeadersMap()); // No change to headers
      assertThat(result.getWorkflowLog().getTracker().getErrorRaised())
          .isTrue(); // Verify tracker was updated
    }

    @Test
    @DisplayName("should handle unset outcome")
    void test3() {
      // Arrange
      var workflow = PlatformWorkflow.newBuilder()
          .addSteps(STEP)
          .setTracker(TRACKER)
          .build();

      var message = PlatformMessage.newBuilder()
          .setWorkflowLog(workflow)
          .addPayloads(INPUT_PAYLOAD)
          .putHeaders("original", "header")
          .build();

      var outcome = ProcessOutcome.newBuilder().build();
      var utils = new PlatformMessageUtils(message);

      // Act and Assert
      assertThrows(IllegalStateException.class, () -> utils.appendOutcome(outcome));
    }
  }

  @Nested
  @DisplayName("currentStep()")
  public class CurrentStepTest {

    static List<Step> createStepsRange(Integer startIdx, Integer endIdx) {
      return List.range(startIdx, endIdx)
          .map(id -> String.format("step-%s", id))
          .map(CurrentStepTest::createStep);
    }

    static Step createStep(final String stepName) {
      return Step.newBuilder()
          .setName(stepName)
          .build();
    }

    static PlatformMessage createMessage(List<Step> steps, Integer currentStepIdx) {
      return PlatformMessage.newBuilder()
          .setWorkflowLog(PlatformWorkflow.newBuilder()
              .setTracker(Tracker.newBuilder()
                  .setCurrentBlock(MAIN)
                  .setCurrentStep(currentStepIdx)
                  .setErrorRaised(false)
                  .putRanges(MAIN_VALUE, BlockRange.newBuilder()
                      .setType(MAIN)
                      .setStartIdx(0)
                      .setEndIdx(4)
                      .build()))
              .addAllSteps(steps))
          .putHeaders("h1", "value1")
          .putHeaders("h2", "value2")
          .build();
    }

    @Test
    @DisplayName("should return the step pointed by tracker")
    void test0() {
      var message = createMessage(createStepsRange(0, 4), 2);
      var currentStepName = PlatformMessageUtils.withMessage(message).currentStep().map(Step::getName);

      org.assertj.vavr.api.VavrAssertions.assertThat(currentStepName).isRight().containsOnRight("step-2");
    }

    @Test
    @DisplayName("should return error message when point to invalid step")
    void test1() {
      var message = createMessage(createStepsRange(0, 4), 12);
      var currentStepName = PlatformMessageUtils.withMessage(message).currentStep().map(Step::getName);

      org.assertj.vavr.api.VavrAssertions.assertThat(currentStepName).isLeft()
          .containsOnLeft("Index 12 out of bounds for length 4");
    }

  }

}
