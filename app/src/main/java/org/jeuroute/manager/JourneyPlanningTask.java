package org.jeuroute.manager;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public interface JourneyPlanningTask<Result> {
	int advance(int workBudget);

	boolean isComplete();

	Result result();

	static <Result> JourneyPlanningTask<Result> deferred(Supplier<Result> calculation) {
		return new Deferred<>(calculation);
	}

	final class Deferred<Result> implements JourneyPlanningTask<Result> {

		private final Supplier<Result> calculation;
		private Optional<Result> result = Optional.empty();
		private boolean complete;

		private Deferred(Supplier<Result> calculation) {
			this.calculation = Objects.requireNonNull(calculation);
		}

		@Override
		public int advance(int workBudget) {
			if (complete || workBudget <= 0) {
				return 0;
			}
			result = Optional.of(Objects.requireNonNull(calculation.get()));
			complete = true;
			return 1;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public Result result() {
			if (!complete) {
				throw new IllegalStateException("Planning task is not complete");
			}
			return result.orElseThrow();
		}
	}
}
