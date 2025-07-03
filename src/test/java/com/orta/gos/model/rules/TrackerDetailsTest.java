package com.orta.gos.model.rules;

import static org.assertj.vavr.api.VavrAssertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.vavr.Tuple;

@DisplayName("TrackerDetails")
public class TrackerDetailsTest {

  @Nested
  @DisplayName("lastStepInBlock()")
  public class LastStepInBlockTest {

    @Test
    @DisplayName("should identify when current-step is the end of block")
    void test0() {
      var tracker = Tracker.newBuilder()
          .setCurrentBlock(BlockType.MAIN)
          .setCurrentStep(3)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE,
              BlockRange.newBuilder().setType(BlockType.MAIN).setStartIdx(0).setEndIdx(3).build())
          .putRanges(BlockType.GENERAL_ERROR_VALUE,
              BlockRange.newBuilder().setType(BlockType.GENERAL_ERROR).setStartIdx(4).setEndIdx(5).build())
          .build();

      org.assertj.core.api.Assertions.assertThat(TrackerDetails.instance(tracker).lastStepInBlock()).isTrue();
    }

    @Test
    @DisplayName("should identify when current-step is not the end of block")
    void test1() {
      var tracker = Tracker.newBuilder()
          .setCurrentBlock(BlockType.MAIN)
          .setCurrentStep(2)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE,
              BlockRange.newBuilder().setType(BlockType.MAIN).setStartIdx(0).setEndIdx(3).build())
          .putRanges(BlockType.GENERAL_ERROR_VALUE,
              BlockRange.newBuilder().setType(BlockType.GENERAL_ERROR).setStartIdx(4).setEndIdx(5).build())
          .build();

      org.assertj.core.api.Assertions.assertThat(TrackerDetails.instance(tracker).lastStepInBlock()).isFalse();
    }

  }

  @Nested
  @DisplayName("jumpToBlock")
  public class JumpToBlockTest {

    @Test
    @DisplayName("should provide the index of the first step in the provided block")
    void test0() {
      var tracker = Tracker.newBuilder()
          .setCurrentBlock(BlockType.MAIN)
          .setCurrentStep(2)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE,
              BlockRange.newBuilder().setType(BlockType.MAIN).setStartIdx(0).setEndIdx(3).build())
          .putRanges(BlockType.GENERAL_ERROR_VALUE,
              BlockRange.newBuilder().setType(BlockType.GENERAL_ERROR).setStartIdx(4).setEndIdx(5).build())
          .build();
      assertThat(TrackerDetails.instance(tracker).jumpToBlock(BlockType.GENERAL_ERROR)).isRight()
          .containsOnRight(Tuple.of(4, BlockType.GENERAL_ERROR));
    }

    @Test
    @DisplayName("should present error message (left) when requesting missing BlockType")
    void test1() {
      var tracker = Tracker.newBuilder()
          .setCurrentBlock(BlockType.MAIN)
          .setCurrentStep(2)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE,
              BlockRange.newBuilder().setType(BlockType.MAIN).setStartIdx(0).setEndIdx(3).build())
          .putRanges(BlockType.GENERAL_ERROR_VALUE,
              BlockRange.newBuilder().setType(BlockType.GENERAL_ERROR).setStartIdx(4).setEndIdx(5).build())
          .build();
      assertThat(TrackerDetails.instance(tracker).jumpToBlock(BlockType.FINALLY)).isLeft()
          .containsOnLeft("Block [FINALLY] was not found");
    }

    @Test
    @DisplayName("should present error message (left) when requesting to jump to proir index")
    void test2() {
      var tracker = Tracker.newBuilder()
          .setCurrentBlock(BlockType.MAIN)
          .setCurrentStep(2)
          .setErrorRaised(false)
          .putRanges(BlockType.MAIN_VALUE,
              BlockRange.newBuilder().setType(BlockType.MAIN).setStartIdx(0).setEndIdx(3).build())
          .putRanges(BlockType.GENERAL_ERROR_VALUE,
              BlockRange.newBuilder().setType(BlockType.GENERAL_ERROR).setStartIdx(4).setEndIdx(5).build())
          .build();
      assertThat(TrackerDetails.instance(tracker).jumpToBlock(BlockType.MAIN)).isLeft()
          .containsOnLeft("attempt to move backwards (from 2 to idx 0)");
    }

  }

}
