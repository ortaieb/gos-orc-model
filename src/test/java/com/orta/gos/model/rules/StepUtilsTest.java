package com.orta.gos.model.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.vavr.collection.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StepUtils")
class StepUtilsTest {

  @Nested
  @DisplayName("markEdges")
  class MarkEdgesTest {

    @Test
    @DisplayName("should return empty list for empty input")
    void test0() {
      // Act
      List<Step> result = StepUtils.markEdges(BlockType.MAIN, List.empty());

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should mark single step with both START and END edges")
    void test1() {
      // Arrange
      Step step = Step.newBuilder().setName("step1").build();

      // Act
      List<Step> result = StepUtils.markEdges(BlockType.MAIN, List.of(step));

      // Assert
      assertThat(result).hasSize(1);
      Step resultStep = result.get(0);
      assertThat(resultStep.getName()).isEqualTo("step1");
      assertThat(resultStep.getIndicatorsList()).hasSize(2);
      verifyBlockIndicators(resultStep, "step1", BlockType.MAIN, BlockEdge.START, BlockEdge.END);
    }

    @Test
    @DisplayName("should mark first with START and last with END for two steps")
    void test2() {
      // Arrange
      Step step1 = Step.newBuilder().setName("step1").build();
      Step step2 = Step.newBuilder().setName("step2").build();

      // Act
      List<Step> result = StepUtils.markEdges(BlockType.MAIN, List.of(step1, step2));

      // Assert
      assertThat(result).hasSize(2);
      verifyBlockIndicators(result.get(0), "step1", BlockType.MAIN, BlockEdge.START);
      verifyBlockIndicators(result.get(1), "step2", BlockType.MAIN, BlockEdge.END);
    }

    @Test
    @DisplayName("should preserve middle steps unchanged")
    void test3() {
      // Arrange
      Step step1 = Step.newBuilder().setName("step1").build();
      Step step2 = Step.newBuilder().setName("step2")
          .addIndicators(BlockIndicator.newBuilder()
              .setType(BlockType.MAIN)
              .setEdge(BlockEdge.START)
              .build())
          .build();
      Step step3 = Step.newBuilder().setName("step3").build();

      // Act
      List<Step> result = StepUtils.markEdges(BlockType.MAIN, List.of(step1, step2, step3));

      // Assert
      assertThat(result).hasSize(3);
      verifyBlockIndicators(result.get(0), "step1", BlockType.MAIN, BlockEdge.START);
      // Middle step should preserve its original block indicators
      assertThat(result.get(1).getName()).isEqualTo("step2");
      assertThat(result.get(1).getIndicatorsList())
          .containsExactlyElementsOf(step2.getIndicatorsList());
      verifyBlockIndicators(result.get(2), "step3", BlockType.MAIN, BlockEdge.END);
    }

    @Test
    @DisplayName("should work with different block types")
    void test4() {
      // Arrange
      Step step1 = Step.newBuilder().setName("step1").build();
      Step step2 = Step.newBuilder().setName("step2").build();

      // Act
      List<Step> result = StepUtils.markEdges(BlockType.FINALLY, List.of(step1, step2));

      // Assert
      assertThat(result).hasSize(2);
      verifyBlockIndicators(result.get(0), "step1", BlockType.FINALLY, BlockEdge.START);
      verifyBlockIndicators(result.get(1), "step2", BlockType.FINALLY, BlockEdge.END);
    }
  }

  private void verifyBlockIndicators(Step step, String expectedName, BlockType expectedType,
      BlockEdge... expectedEdges) {
    assertThat(step.getName()).isEqualTo(expectedName);
    assertThat(step.getIndicatorsList()).hasSize(expectedEdges.length);

    for (int i = 0; i < expectedEdges.length; i++) {
      BlockIndicator indicator = step.getIndicators(i);
      assertThat(indicator.getType()).isEqualTo(expectedType);
      assertThat(indicator.getEdge()).isEqualTo(expectedEdges[i]);
    }
  }
}
