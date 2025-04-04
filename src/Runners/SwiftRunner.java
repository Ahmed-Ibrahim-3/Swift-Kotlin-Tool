package Runners;

import java.io.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SwiftRunner implements ScriptRunner {

    private Process currentProcess;
    private ExecutorService executorService;
    private volatile boolean running = false;

    public SwiftRunner() {
        executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    public int runScript(String script, Consumer<String> outputConsumer, Consumer<String> errorConsumer) {
        if (running) {
            errorConsumer.accept("Script already running");
            return -1;
        }
        running = true;
        final int[] exitCode = {-1};

        try {
            File tempFile = File.createTempFile("swift_script_", ".swift");
            tempFile.deleteOnExit();

            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(script);
            }

            ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/env", "swift", tempFile.getAbsolutePath());
            processBuilder.redirectErrorStream(true);

            currentProcess = processBuilder.start();

            Future<?> outputFuture = executorService.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputConsumer.accept(line);
                    }
                } catch (IOException e) {
                    errorConsumer.accept("Error reading process output: " + e.getMessage());
                }
            });

            exitCode[0] = currentProcess.waitFor();
            outputFuture.get();

        } catch (IOException e) {
            errorConsumer.accept("I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            errorConsumer.accept("Script execution interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            errorConsumer.accept("Error in output processing: " + e.getMessage());
        } finally {
            running = false;
            currentProcess = null;
        }
        return exitCode[0];
    }

    @Override
    public void stopScript() {
        if (currentProcess != null){
            currentProcess.destroy();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)){
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Thread.currentThread().interrupt();
        }
    }
}
