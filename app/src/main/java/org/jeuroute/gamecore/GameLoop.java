package org.jeuroute.gamecore;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

public class GameLoop {
    public void start(BooleanSupplier shouldClose, Runnable pollEvents, DoubleConsumer update, Runnable render) {
        long previousFrameNs = System.nanoTime();

        while (!shouldClose.getAsBoolean()) {
            pollEvents.run();

            if (shouldClose.getAsBoolean()) {
                break;
            }

            long nowNs = System.nanoTime();
            double deltaSeconds = (nowNs - previousFrameNs) / 1_000_000_000.0;
            previousFrameNs = nowNs;

            update.accept(deltaSeconds);
            render.run();
        }
    }
}
