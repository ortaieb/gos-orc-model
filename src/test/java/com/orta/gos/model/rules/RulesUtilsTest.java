package com.orta.gos.model.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.vavr.collection.List;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

@DisplayName("RulesUtils")
class RulesUtilsTest {

  @Nested
  @DisplayName("fromRule")
  class FromRuleTest {

    @Test
    @DisplayName("should process all block types in correct order")
    void test0() {
      // Arrange
      Step step1 = Step.newBuilder().setName("step1").build();
      Step step2 = Step.newBuilder().setName("step2").build();
      Step step3 = Step.newBuilder().setName("step3").build();
      Step step4 = Step.newBuilder().setName("step4").build();
      Step step5 = Step.newBuilder().setName("step5").build();
      Step step6 = Step.newBuilder().setName("step6").build();
      Step step7 = Step.newBuilder().setName("step7").build();

      Map<BlockType, List<Step>> rulesSteps = HashMap.<BlockType, List<Step>>empty()
          .put(BlockType.MAIN, List.of(step1, step2, step3))
          .put(BlockType.FINALLY, List.of(step4, step5))
          .put(BlockType.GENERAL_ERROR, List.of(step6, step7));

      // Act
      List<Step> result = RulesUtils.fromRule(rulesSteps);

      // Assert
      assertThat(result.size()).isEqualTo(7);

      // Check MAIN block
      verifyBlockIndicator(result.get(0), "step1", BlockType.MAIN, BlockEdge.START);
      assertThat(result.get(1).getName()).isEqualTo("step2");
      verifyBlockIndicator(result.get(2), "step3", BlockType.MAIN, BlockEdge.END);

      // Check GENERAL_ERROR block
      verifyBlockIndicator(result.get(3), "step6", BlockType.GENERAL_ERROR, BlockEdge.START);
      verifyBlockIndicator(result.get(4), "step7", BlockType.GENERAL_ERROR, BlockEdge.END);

      // Check FINALLY block
      verifyBlockIndicator(result.get(5), "step4", BlockType.FINALLY, BlockEdge.START);
      verifyBlockIndicator(result.get(6), "step5", BlockType.FINALLY, BlockEdge.END);
    }

    @Test
    @DisplayName("should skip empty blocks")
    void test1() {
      // Arrange
      Step step1 = Step.newBuilder().setName("step1").build();
      Step step2 = Step.newBuilder().setName("step2").build();

      Map<BlockType, List<Step>> rulesSteps = HashMap.<BlockType, List<Step>>empty()
          .put(BlockType.MAIN, List.of(step1, step2))
          .put(BlockType.FINALLY, List.empty())
          .put(BlockType.GENERAL_ERROR, null);

      // Act
      List<Step> result = RulesUtils.fromRule(rulesSteps);

      // Assert
      assertThat(result.size()).isEqualTo(2);
      verifyBlockIndicator(result.get(0), "step1", BlockType.MAIN, BlockEdge.START);
      verifyBlockIndicator(result.get(1), "step2", BlockType.MAIN, BlockEdge.END);
    }

    @Test
    @DisplayName("should handle single step in block")
    void test2() {
      // Arrange
      Step step = Step.newBuilder().setName("step1").build();

      Map<BlockType, List<Step>> rulesSteps = HashMap.<BlockType, List<Step>>empty()
          .put(BlockType.MAIN, List.of(step));

      // Act
      List<Step> result = RulesUtils.fromRule(rulesSteps);

      // Assert
      assertThat(result.size()).isEqualTo(1);
      Step resultStep = result.get(0);
      verifyBlockIndicator(resultStep, "step1", BlockType.MAIN, BlockEdge.START, BlockEdge.END);
    }
  }

  @SafeVarargs
  private void verifyBlockIndicator(Step step, String expectedName, BlockType expectedType,
      BlockEdge... expectedEdges) {

    var indicators = List.ofAll(Stream.of(expectedEdges))
        .map(edge -> BlockIndicator.newBuilder().setType(expectedType).setEdge(edge).build());
    var expectedStep = Step.newBuilder()
        .setName(expectedName)
        .addAllIndicators(indicators.toJavaList())
        .build();

    assertThat(step).isEqualTo(expectedStep);
  }
}
