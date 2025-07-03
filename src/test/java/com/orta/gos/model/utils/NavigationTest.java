package com.orta.gos.model.utils;

import com.orta.gos.model.PlatformMessage;
import com.orta.gos.model.PlatformWorkflow;
import com.orta.gos.model.rules.Step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.orta.gos.model.utils.Navigation.hasMoreSteps;
import static com.orta.gos.model.utils.Navigation.maybeStepAddress;
import static com.orta.gos.model.utils.Navigation.updateWorkflow;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Navigation")
public class NavigationTest {

  @Nested
  @DisplayName("hasMoreSteps")
  class HasMoreStepsTest {

    @Test
    @DisplayName("retur true if steps list has elements")
    void test0() {
      var input = PlatformWorkflow.newBuilder().addSteps(Step.newBuilder().setName("step1").setAddress("...")).build();
      assertThat(hasMoreSteps(input)).isTrue();
    }

    @Test
    @DisplayName("return fakse if steps list is empty")
    void test1() {
      var input = PlatformWorkflow.newBuilder().build();
      assertThat(hasMoreSteps(input)).isFalse();
    }

  }

  @Nested
  @DisplayName("updateWorkflow")
  class UpdateWorkflowTest {

    @Test
    @DisplayName("return same instance if no more steps avaiable")
    void test0() {
      var input = PlatformWorkflow.newBuilder().addCompletedSteps("step1").build();
      assertThat(updateWorkflow(input).build()).isEqualTo(input);
    }

    @Test
    @DisplayName("return fakse if steps list is empty")
    void test1() {
      var input = PlatformWorkflow.newBuilder().addSteps(Step.newBuilder().setName("step2").setAddress("addr2"))
          .addCompletedSteps("step1").build();
      var expected = PlatformWorkflow.newBuilder()
          .addCompletedSteps("step1")
          .addCompletedSteps("step2")
          .build();
      assertThat(updateWorkflow(input).build()).isEqualTo(expected);
    }

  }

  @Nested
  @DisplayName("maybeStepAddress")
  class MaybeStepAddressTest {

    @Test
    @DisplayName("return address of first step in the workflow")
    void test0() {
      var workflow = PlatformWorkflow.newBuilder()
          .addSteps(Step.newBuilder().setName("step1").setAddress("addr1"))
          .addSteps(Step.newBuilder().setName("step2").setAddress("addr2"))
          .addCompletedSteps("step0");
      var input = PlatformMessage.newBuilder().setWorkflowLog(workflow).build();

      assertThat(maybeStepAddress(input)).contains("addr1");
    }

    @Test
    @DisplayName("return empty() if no steps in the workflow")
    void test1() {
      var workflow = PlatformWorkflow.newBuilder()
          .addCompletedSteps("step0")
          .addCompletedSteps("step1")
          .addCompletedSteps("step2");
      var input = PlatformMessage.newBuilder().setWorkflowLog(workflow).build();

      assertThat(maybeStepAddress(input)).isEmpty();
    }

    @Test
    @DisplayName("return empty() if no steps in the workflow")
    void test2() {
      assertThat(maybeStepAddress(PlatformMessage.newBuilder().build())).isEmpty();
    }

  }

}
