package Runners;

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SwiftRunner implements ScriptRunner {

    private Process currentProcess;
    private ExecutorService executorService;
    private volatile boolean running = false;
    private volatile boolean outputLimitReached = false;

    private int maxOutputLines = 1000;

    private Queue<String> outputBuffer = new LinkedList<>();
    private long totalLines = 0;
    private long droppedLines = 0;

    public SwiftRunner() {
        executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    public int runScript(String scriptContent, Consumer<String> outputConsumer, Consumer<String> errorConsumer) {
        if (running) {
            errorConsumer.accept("A script is already running");
            return -1;
        }

        running = true;
        outputLimitReached = false;
        totalLines = 0;
        droppedLines = 0;
        outputBuffer.clear();

        final int[] exitCode = {-1};

        try {
            File tempFile = File.createTempFile("swift_script_", ".swift");
            tempFile.deleteOnExit();

            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(scriptContent);
            }

            ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/env", "swift", tempFile.getAbsolutePath());
            processBuilder.redirectErrorStream(true);

            currentProcess = processBuilder.start();

            Future<?> outputFuture = executorService.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(currentProcess.getInputStream()),
                        8192)) {

                    String line;
                    StringBuilder batch = new StringBuilder();
                    int batchSize = 0;
                    final int MAX_BATCH_SIZE = 50;

                    while ((line = reader.readLine()) != null) {
                        totalLines++;

                        if (outputBuffer.size() >= maxOutputLines && !outputLimitReached) {
                            outputLimitReached = true;
                            String warningMsg = "\n--- Output limit reached (" + maxOutputLines +
                                    " lines). Additional output will be limited. ---\n";
                            outputConsumer.accept(warningMsg);
                        }

                        if (outputBuffer.size() >= maxOutputLines) {
                            outputBuffer.poll();
                            droppedLines++;

                            if (totalLines % 1000 == 0) {
                                outputBuffer.add(line);
                                batch.append(line).append("\n");
                                batchSize++;
                            }
                        } else {
                            outputBuffer.add(line);
                            batch.append(line).append("\n");
                            batchSize++;
                        }

                        if (batchSize >= MAX_BATCH_SIZE || totalLines % 500 == 0) {
                            final String batchOutput = batch.toString();
                            if (!batchOutput.isEmpty()) {
                                outputConsumer.accept(batchOutput);
                                batch.setLength(0);
                                batchSize = 0;
                            }
                        }
                    }

                    if (batchSize > 0) {
                        outputConsumer.accept(batch.toString());
                    }

                    if (droppedLines > 0) {
                        outputConsumer.accept("\n--- Summary: " + totalLines + " total lines, " +
                                droppedLines + " lines not shown due to memory limits ---\n");
                    }

                } catch (IOException e) {
                    if (running) {
                        errorConsumer.accept("Error reading process output: " + e.getMessage());
                    }
                }
            });

            exitCode[0] = currentProcess.waitFor();

            try {
                outputFuture.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                outputFuture.cancel(true);
                outputConsumer.accept("\n--- Output processing timed out. Some output may be missing. ---\n");
            }

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

            outputBuffer.clear();
            System.gc();
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

    @Override
    public void setMaxOutputLines(int maxOutputLines) {
        this.maxOutputLines = maxOutputLines;
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
