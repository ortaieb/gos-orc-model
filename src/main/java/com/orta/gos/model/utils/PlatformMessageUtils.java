package com.orta.gos.model.utils;

import com.orta.gos.model.Payload;
import com.orta.gos.model.PlatformMessage;
import com.orta.gos.model.PlatformWorkflow;
import com.orta.gos.model.ProcessInput;
import com.orta.gos.model.ProcessOutcome;
import com.orta.gos.model.ProcessOutcomeSucces;
import com.orta.gos.model.rules.Step;

import io.quarkus.logging.Log;
import io.vavr.Function1;
import io.vavr.collection.List;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

public record PlatformMessageUtils(PlatformMessage message) {

  public static PlatformMessageUtils withMessage(PlatformMessage message) {
    return new PlatformMessageUtils(message);
  }

  public ProcessInput processInput() {
    return ProcessInput.newBuilder()
        .addPayloads(message.getPayloadsList().getLast())
        .putAllAttributes(message.getWorkflowLog().getStepsList().getFirst().getAttributesMap())
        .build();
  }

  /**
   * Appends the process outcome to the platform message.
   * Updates the workflow tracker, payloads, and headers based on the outcome.
   *
   * @param outcome the process outcome to append
   * @return a new PlatformMessage with updated state
   */
  public PlatformMessage appendOutcome(ProcessOutcome outcome) {
    // Update tracker in workflow log
    var newTracker = PlatformWorkflowUtils.updateTracker(message.getWorkflowLog(), outcome);
    var newWorkflow = PlatformWorkflow.newBuilder(message.getWorkflowLog())
        .setTracker(newTracker)
        .build();

    // Start building new message with updated workflow
    var builder = PlatformMessage.newBuilder(message)
        .setWorkflowLog(newWorkflow);

    // Handle payloads and headers based on outcome type
    switch (outcome.getOutcomeCase()) {
      case SUCCESS:
        return handleSuccess(builder, outcome.getSuccess());
      case FAILURE:
        Log.errorf("ProcessOutcome returned with error: %s", outcome.getFailure().getErrorMessage());
        return builder.build(); // No changes to payloads or headers
      case OUTCOME_NOT_SET:
      default:
        throw new IllegalStateException("Outcome was not set");
    }
  }

  private static final Function1<ProcessOutcomeSucces, List<Payload>> successPayloads = po -> po.hasOutputPayload()
      ? List.of(po.getOutputPayload())
      : List.empty();

  private static final Function1<ProcessOutcomeSucces, Map<String, String>> successHeaders = po -> HashMap
      .ofAll(po.getHeadersMap());

  /**
   * Handles successful outcome by updating the message with new payloads and
   * headers.
   *
   * @param builder the message builder with updated workflow
   * @param success the success outcome to process
   * @return updated platform message
   */
  private static PlatformMessage handleSuccess(PlatformMessage.Builder builder, ProcessOutcomeSucces success) {
    // Add payloads if present
    successPayloads.apply(success)
        .forEach(builder::addPayloads);

    // Add only new headers that don't exist in the message
    var existingHeaders = builder.getHeadersMap();
    successHeaders.apply(success)
        .filterKeys(key -> !existingHeaders.containsKey(key))
        .forEach(builder::putHeaders);

    return builder.build();
  }

  public Option<String> maybeCurrentAddress() {
    var workflow = message.getWorkflowLog();
    var currStepIdx = workflow.getTracker().getCurrentStep();
    var steps = workflow.getStepsList();

    return Try.of(() -> steps.get(currStepIdx))
        .map(Step::getAddress)
        .recoverWith(error -> {
          Log.errorf("Could not get step (currStepIdx: %d, steps.size = %d)", currStepIdx,
              steps == null ? -1 : steps.size());
          return Try.failure(error);
        })
        .toOption();
  }

  public Either<String, Step> currentStep() {
    return Try.of(() -> {
      PlatformWorkflow workflow = message.getWorkflowLog();
      int currStepIdx = workflow.getTracker().getCurrentStep();

      return workflow.getStepsList().get(currStepIdx);
    }).toEither()
        .mapLeft(Throwable::getMessage);
  }

}
