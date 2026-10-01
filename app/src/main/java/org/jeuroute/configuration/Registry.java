package org.jeuroute.configuration;

public interface Registry<T> {
	Registry<T> register(String indicatorId, T indicator);
	T get(String indicatorId);
}
