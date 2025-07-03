package com.orta.gos.model.rules;

import static com.orta.gos.model.rules.BlockEdge.END;
import static com.orta.gos.model.rules.BlockEdge.START;
import static com.orta.gos.model.rules.BlockType.FINALLY;
import static com.orta.gos.model.rules.BlockType.FINALLY_VALUE;
import static com.orta.gos.model.rules.BlockType.GENERAL_ERROR;
import static com.orta.gos.model.rules.BlockType.GENERAL_ERROR_VALUE;
import static com.orta.gos.model.rules.BlockType.MAIN;
import static com.orta.gos.model.rules.BlockType.MAIN_VALUE;

import static com.orta.gos.model.rules.TrackerUtils.orderedSteps;
import static com.orta.gos.model.rules.TrackerUtils.blockRanges;
import static com.orta.gos.model.rules.TrackerUtils.nextStepOnFailure;
import static com.orta.gos.model.rules.TrackerUtils.nextStepOnSuccess;

import static org.assertj.vavr.api.VavrAssertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;

@DisplayName("TrackerUtils")
public class TrackerUtilsTest {

  static final Step stepM1 = Step.newBuilder().setName("step-m-1").build();
  static final Step stepM2 = Step.newBuilder().setName("step-m-2").build();
  static final Step stepM3 = Step.newBuilder().setName("step-m-3").build();
  static final Step stepE1 = Step.newBuilder().setName("step-e-1").build();
  static final Step stepE2 = Step.newBuilder().setName("step-m-2").build();
  static final Step stepF1 = Step.newBuilder().setName("step-f-1").build();
  static final Step stepF2 = Step.newBuilder().setName("step-m-2").build();

  private static Step withIndicator(final Step step, final BlockType type, final BlockEdge... edges) {
    var indicators = List.of(edges)
        .map(edge -> BlockIndicator.newBuilder().setType(type).setEdge(edge).build());
    return Step.newBuilder(step).addAllIndicators(indicators.toJavaList()).build();
  }

  @Nested
  @DisplayName("orderedSteps")
  public class OrderedStepsTest {

    @Test
    @DisplayName("should handle empty steps map")
    void test0() {
      Map<BlockType, List<Step>> input = HashMap.empty();

      assertThat(orderedSteps(input)).isEmpty();
    }

    @Test
    @DisplayName("should handle map with a single (main) entry")
    void test1() {
      Map<BlockType, List<Step>> input = HashMap.of(MAIN, List.of(stepM1, stepM2, stepM3));

      var expected = List.of(withIndicator(stepM1, MAIN, START), stepM2, withIndicator(stepM3, MAIN, END));
      assertThat(orderedSteps(input)).containsAll(expected);
    }

    @Test
    @DisplayName("should handle steps map provided in the correct order")
    void test2() {
      Map<BlockType, List<Step>> input = HashMap.of(
          MAIN, List.of(stepM1, stepM2, stepM3),
          GENERAL_ERROR, List.of(stepE1, stepE2),
          FINALLY, List.of(stepF1, stepF2));

      var expected = new Step[] {
          withIndicator(stepM1, MAIN, START), stepM2, withIndicator(stepM3, MAIN, END),
          withIndicator(stepE1, GENERAL_ERROR, START), withIndicator(stepE2, GENERAL_ERROR, END),
          withIndicator(stepF1, FINALLY, START), withIndicator(stepF2, FINALLY, END)
      };
      assertThat(orderedSteps(input)).containsExactly(expected);
    }

    @Test
    @DisplayName("should handle steps map provided in a different order")
    void test3() {
      Map<BlockType, List<Step>> input = HashMap.of(
          FINALLY, List.of(stepF1, stepF2),
          GENERAL_ERROR, List.of(stepE1, stepE2),
          MAIN, List.of(stepM1, stepM2, stepM3));

      var expected = new Step[] {
          withIndicator(stepM1, MAIN, START), stepM2, withIndicator(stepM3, MAIN, END),
          withIndicator(stepE1, GENERAL_ERROR, START), withIndicator(stepE2, GENERAL_ERROR, END),
          withIndicator(stepF1, FINALLY, START), withIndicator(stepF2, FINALLY, END)
      };
      assertThat(orderedSteps(input)).containsExactly(expected);
    }

  }

  @Nested
  @DisplayName("blockRanges")
  public class BlockRangesTest {

    @Test
    @DisplayName("should handle empty list")
    void test0() {
      assertThat(blockRanges(0, List.empty())).isEmpty();
    }

    @Test
    @DisplayName("should handle list with MAIN block only")
    void test1() {
      var input = List.of(withIndicator(stepM1, MAIN, START), stepM2, withIndicator(stepM3, MAIN, END));
      var expected = HashMap.of(MAIN_VALUE, BlockRange.newBuilder().setType(MAIN).setStartIdx(0).setEndIdx(2).build());

      assertThat(blockRanges(0, input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("should handle a full list of steps ordered")
    void test2() {
      var input = List.of(
          withIndicator(stepM1, MAIN, START), stepM2, withIndicator(stepM3, MAIN, END),
          withIndicator(stepE1, GENERAL_ERROR, START), withIndicator(stepE2, GENERAL_ERROR, END),
          withIndicator(stepF1, FINALLY, START), withIndicator(stepF2, FINALLY, END));
      var expected = HashMap.of(
          MAIN_VALUE, BlockRange.newBuilder().setType(MAIN).setStartIdx(0).setEndIdx(2).build(),
          GENERAL_ERROR_VALUE, BlockRange.newBuilder().setType(GENERAL_ERROR).setStartIdx(3).setEndIdx(4).build(),
          FINALLY_VALUE, BlockRange.newBuilder().setType(FINALLY).setStartIdx(5).setEndIdx(6).build());

      assertThat(blockRanges(0, input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("should support step with multi edge")
    void test3() {
      var input = List.of(
          withIndicator(stepM1, MAIN, START), stepM2, withIndicator(stepM3, MAIN, END),
          withIndicator(stepE1, GENERAL_ERROR, START, END),
          withIndicator(stepF1, FINALLY, START), withIndicator(stepF2, FINALLY, END));
      var expected = HashMap.of(
          MAIN_VALUE, BlockRange.newBuilder().setType(MAIN).setStartIdx(0).setEndIdx(2).build(),
          GENERAL_ERROR_VALUE, BlockRange.newBuilder().setType(GENERAL_ERROR).setStartIdx(3).setEndIdx(3).build(),
          FINALLY_VALUE, BlockRange.newBuilder().setType(FINALLY).setStartIdx(4).setEndIdx(5).build());

      assertThat(blockRanges(0, input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("should handle index based in initial step idx provided")
    void test4() {
      var input = List.of(
          withIndicator(stepM1, MAIN, START), stepM2, withIndicator(stepM3, MAIN, END),
          withIndicator(stepE1, GENERAL_ERROR, START), withIndicator(stepE2, GENERAL_ERROR, END),
          withIndicator(stepF1, FINALLY, START), withIndicator(stepF2, FINALLY, END));
      var expected = HashMap.of(
          MAIN_VALUE, BlockRange.newBuilder().setType(MAIN).setStartIdx(6).setEndIdx(8).build(),
          GENERAL_ERROR_VALUE, BlockRange.newBuilder().setType(GENERAL_ERROR).setStartIdx(9).setEndIdx(10).build(),
          FINALLY_VALUE, BlockRange.newBuilder().setType(FINALLY).setStartIdx(11).setEndIdx(12).build());

      assertThat(blockRanges(6, input)).isEqualTo(expected);
    }

  }

  @Nested
  @DisplayName("nextStepOnSuccess")
  public class NextStepOnSuccessTest {

    @Test
    @DisplayName("should move to the next step inside the main block")
    void test0() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(1)
          .setCurrentBlock(BlockType.MAIN)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.GENERAL_ERROR, 5, 7))
          .putRanges(BlockType.FINALLY_VALUE, range(BlockType.FINALLY, 8, 9))
          .build();

      var nextTracker = nextStepOnSuccess(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.MAIN);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(2);
    }

    @Test
    @DisplayName("should skip to finally when finish successfuly main block")
    void test1() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(4)
          .setCurrentBlock(BlockType.MAIN)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.GENERAL_ERROR, 5, 7))
          .putRanges(BlockType.FINALLY_VALUE, range(BlockType.FINALLY, 8, 9))
          .build();

      var nextTracker = nextStepOnSuccess(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.FINALLY);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(8);
    }

    @Test
    @DisplayName("should continue to the next step when successfuly inside error block")
    void test2() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(6)
          .setCurrentBlock(BlockType.GENERAL_ERROR)
          .setErrorRaised(true)
          .setErrorRaisedStep(3)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.GENERAL_ERROR, 5, 7))
          .putRanges(BlockType.FINALLY_VALUE, range(BlockType.FINALLY, 8, 9))
          .build();

      var nextTracker = nextStepOnSuccess(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.GENERAL_ERROR);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(7);
    }

    @Test
    @DisplayName("should continue to finally when completed error block")
    void test3() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(7)
          .setCurrentBlock(BlockType.GENERAL_ERROR)
          .setErrorRaised(true)
          .setErrorRaisedStep(3)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.GENERAL_ERROR, 5, 7))
          .putRanges(BlockType.FINALLY_VALUE, range(BlockType.FINALLY, 8, 9))
          .build();

      var nextTracker = nextStepOnSuccess(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.FINALLY);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(8);
    }

    @Test
    @DisplayName("should mark as BLOCK_NA and termination step when finish MAIN with no other blocks")
    void test4() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(4)
          .setCurrentBlock(BlockType.MAIN)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .setTermination(5)
          .build();

      var nextTracker = nextStepOnSuccess(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.TYPE_NA);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(5);
    }

    @Test
    @DisplayName("should mark as BLOCK_NA and termination step when finish ERROR with no FINALLY block")
    void test5() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(7)
          .setCurrentBlock(BlockType.GENERAL_ERROR)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.MAIN, 5, 7))
          .setTermination(8)
          .build();

      var nextTracker = nextStepOnSuccess(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.TYPE_NA);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(8);
    }

    @Test
    @DisplayName("should mark as BLOCK_NA and termination step when finish FINALLY block")
    void test6() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(9)
          .setCurrentBlock(BlockType.FINALLY)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.GENERAL_ERROR, 5, 7))
          .putRanges(BlockType.FINALLY_VALUE, range(BlockType.FINALLY, 8, 9))
          .setTermination(10)
          .build();

      var nextTracker = nextStepOnSuccess(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.TYPE_NA);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(10);
    }

  }

  @Nested
  @DisplayName("nextStepOnFailure")
  public class NextStepOnFailureTest {

    @Test
    @DisplayName("should move to error block when fail in MAIN block")
    void test0() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(2)
          .setCurrentBlock(BlockType.MAIN)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.GENERAL_ERROR, 5, 7))
          .putRanges(BlockType.FINALLY_VALUE, range(BlockType.FINALLY, 8, 9))
          .setTermination(10)
          .build();

      var nextTracker = nextStepOnFailure(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.GENERAL_ERROR);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(5);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getErrorRaised()).isTrue();
      org.assertj.core.api.Assertions.assertThat(nextTracker.getErrorRaisedStep()).isEqualTo(2);
    }

    @Test
    @DisplayName("should terminate when error occured inside error block")
    void test1() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(5)
          .setCurrentBlock(BlockType.GENERAL_ERROR)
          .setErrorRaised(true)
          .setErrorRaisedStep(3)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.GENERAL_ERROR, 5, 7))
          .putRanges(BlockType.FINALLY_VALUE, range(BlockType.FINALLY, 8, 9))
          .setTermination(10)
          .build();

      var nextTracker = nextStepOnFailure(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.TYPE_NA);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(10);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getErrorRaised()).isTrue();
      org.assertj.core.api.Assertions.assertThat(nextTracker.getErrorRaisedStep()).isEqualTo(3);
    }

    @Test
    @DisplayName("should terminate when error occured inside FINALLY")
    void test2() {
      var tracker = Tracker.newBuilder()
          .setCurrentStep(8)
          .setCurrentBlock(BlockType.FINALLY)
          .setErrorRaised(true)
          .setErrorRaisedStep(3)
          .putRanges(BlockType.MAIN_VALUE, range(BlockType.MAIN, 0, 4))
          .putRanges(BlockType.GENERAL_ERROR_VALUE, range(BlockType.GENERAL_ERROR, 5, 7))
          .putRanges(BlockType.FINALLY_VALUE, range(BlockType.FINALLY, 8, 9))
          .setTermination(10)
          .build();

      var nextTracker = nextStepOnFailure(tracker).build();

      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentBlock()).isEqualTo(BlockType.TYPE_NA);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getCurrentStep()).isEqualTo(10);
      org.assertj.core.api.Assertions.assertThat(nextTracker.getErrorRaised()).isTrue();
      org.assertj.core.api.Assertions.assertThat(nextTracker.getErrorRaisedStep()).isEqualTo(3);
    }
  }

  private static final BlockRange range(BlockType type, Integer start, Integer end) {
    return BlockRange.newBuilder()
        .setType(type)
        .setStartIdx(start)
        .setEndIdx(end)
        .build();
  }
}
