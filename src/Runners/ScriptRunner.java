package Runners;

import java.util.function.Consumer;

public interface ScriptRunner {
    int runScript(String script, Consumer<String> outputConsumer, Consumer<String> errorConsumer);

    void stopScript();

    boolean isRunning();

}
