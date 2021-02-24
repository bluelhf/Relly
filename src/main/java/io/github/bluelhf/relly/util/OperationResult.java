package io.github.bluelhf.relly.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class OperationResult {
    private final String message;
    private final Throwable thrown;
    private final State state;

    public OperationResult(State state) {
        this(state, null, null);
    }

    public OperationResult(State state, String message) {
        this(state, message, null);
    }

    public OperationResult(State state, String message, Throwable thrown) {
        this.state = state;
        this.message = message;
        this.thrown = thrown;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public Throwable getThrown() {
        return thrown;
    }

    @NotNull
    public State getState() {
        return state;
    }

    public static OperationResult fail() {
        return new OperationResult(State.FAIL);
    }

    public static OperationResult fail(String message) {
        return new OperationResult(State.FAIL, message);
    }

    public static OperationResult fail(String message, Throwable thrown) {
        return new OperationResult(State.FAIL, message, thrown);
    }

    public static OperationResult fail(Throwable thrown) {
        return new OperationResult(State.FAIL, null, thrown);
    }

    public static OperationResult succeed() {
        return new OperationResult(State.SUCCEED);
    }

    public static OperationResult succeed(String message) {
        return new OperationResult(State.SUCCEED, message);
    }

    @Override
    public String toString() {
        boolean hasInfo = message != null || thrown != null;
        return state + (!hasInfo ? "" : "{" +
            ((message != null) ? message + (thrown != null ? ", " : "") : "") +
            ((thrown != null) ? "  " + Arrays.stream(thrown.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n  ")) : "") +
            "}"
        );

    }

    public enum State {
        SUCCEED,
        PARTIAL,
        FAIL
    }

    public static OperationResult combine(ArrayList<OperationResult> results) {
        StringBuilder builder = new StringBuilder();
        int fails = 0;
        for (OperationResult result : results) {
            if (result.state == State.FAIL) fails++;
            builder.append(result.state).append(":\n");
            builder.append("  ").append(result.message.replace("\n", "\n  ")).append("\n");
            if (result.thrown != null) {
                builder.append("  Stack trace:\n");

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                result.thrown.printStackTrace(pw);

                builder.append("    ").append(sw.toString().replace("\n", "\n    ").replace("\u0009", "    ").replace("\r", ""));
            }
        }

        State s = State.SUCCEED;
        if (fails > 0) s = State.PARTIAL;
        if (fails == results.size()) s = State.FAIL;
        return new OperationResult(s, builder.toString());
    }
}
