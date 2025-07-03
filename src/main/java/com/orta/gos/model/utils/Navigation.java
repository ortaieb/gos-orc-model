package com.orta.gos.model.utils;

import com.orta.gos.model.PlatformMessage;
import com.orta.gos.model.PlatformWorkflow;

import io.vavr.control.Option;

public class Navigation {

  @Deprecated
  public static boolean hasMoreSteps(PlatformWorkflow workflow) {
    return !workflow.getStepsList().isEmpty();
  }

  @Deprecated
  public static PlatformWorkflow.Builder updateWorkflow(PlatformWorkflow orig) {

    var builder = PlatformWorkflow.newBuilder(orig);
    if (hasMoreSteps(orig)) {
      builder
          .addCompletedSteps(orig.getStepsList().getFirst().getName())
          .removeSteps(0);
    }
    return builder;
  }

  @Deprecated
  public static Option<String> maybeStepAddress(PlatformMessage message) {
    return message.getWorkflowLog().getStepsList().isEmpty() ? Option.none()
        : Option.of(message.getWorkflowLog().getStepsList().getFirst().getAddress());
  }

}
