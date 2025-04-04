package Runners;

import java.util.function.Consumer;

public interface ScriptRunner {
    int runScript(String script, Consumer<String> outputConsumer, Consumer<String> errorConsumer);

    boolean sendInput(String input);

    void setInputRequiredCallback(Runnable inputRequiredCallback);

    void stopScript();

    boolean isRunning();

    void setMaxOutputLines(int maxOutputLines);
}