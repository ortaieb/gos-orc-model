package com.orta.gos.model.rules;

import io.vavr.control.Either;
import io.vavr.control.Option;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import io.vavr.Tuple;
import io.vavr.Tuple2;

public record TrackerDetails(TrackerOrBuilder tracker) {

  public static TrackerDetails instance(TrackerOrBuilder tracker) {
    return new TrackerDetails(tracker);
  }

  public Boolean lastStepInBlock() {
    return tracker.getCurrentStep() == tracker.getRangesMap().get(tracker.getCurrentBlockValue()).getEndIdx();
  }

  public Either<String, Tuple2<Integer, BlockType>> jumpToBlock(final BlockType blockType) {
    return Option.of(tracker.getRangesMap().get(blockType.getNumber()))
        .toEither(String.format("Block [%s] was not found", blockType))
        .map(BlockRange::getStartIdx)
        .flatMap(idx -> Integer.compare(idx, tracker.getCurrentStep()) < 0
            ? left(String.format("attempt to move backwards (from %d to idx %d)", tracker.getCurrentStep(), idx))
            : right(Tuple.of(idx, blockType)));
  }

}
