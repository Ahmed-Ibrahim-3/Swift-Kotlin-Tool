package Runners;

import javax.swing.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class KotlinRunner implements ScriptRunner {

    private Process currentProcess;
    private ExecutorService executorService;
    private volatile boolean running = false;
    private volatile boolean outputLimitReached = false;
    private volatile boolean waitingForInput = false;

    private OutputStreamWriter processInput;
    private Runnable inputRequiredCallback;

    private int maxOutputLines = 1000;

    private Queue<String> outputBuffer = new LinkedList<>();
    private long totalLines = 0;
    private long droppedLines = 0;

    // Flag to check if readLine() is used in the script
    private boolean containsReadLine = false;

    public KotlinRunner() {
        executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    public void setInputRequiredCallback(Runnable callback) {
        this.inputRequiredCallback = callback;
    }

    @Override
    public boolean sendInput(String input) {
        // Only send input if the process is alive.
        if (!running || currentProcess == null || !currentProcess.isAlive() || processInput == null) {
            System.out.println("[DEBUG] KotlinRunner.sendInput: Process not alive, ignoring input");
            return false;
        }
        try {
            processInput.write(input + "\n");
            processInput.flush();
            waitingForInput = false;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int runScript(String scriptContent, Consumer<String> outputConsumer, Consumer<String> errorConsumer) {
        // Check if the script contains readLine() calls.
        containsReadLine = scriptContent.contains("readLine()");
        if (running) {
            errorConsumer.accept("A script is already running");
            return -1;
        }

        running = true;
        waitingForInput = false;
        outputLimitReached = false;
        totalLines = 0;
        droppedLines = 0;
        outputBuffer.clear();

        final int[] exitCode = {-1};
        File tempFile = null;
        try {
            // 1) Write the script to a temporary .kts file.
            tempFile = File.createTempFile("kotlin_script_", ".kts");
            tempFile.deleteOnExit();
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(scriptContent);
            }

            // 2) Start the process using kotlinc in script mode.
            ProcessBuilder processBuilder = new ProcessBuilder("kotlinc", "-script", tempFile.getAbsolutePath());
            processBuilder.redirectErrorStream(true);
            currentProcess = processBuilder.start();
            processInput = new OutputStreamWriter(currentProcess.getOutputStream());

            if (containsReadLine) {
                outputConsumer.accept("Note: Script contains readLine() calls. You'll be prompted for input when needed.");
            }

            // 3) Start reading process output on a background thread.
            Future<?> outputFuture = executorService.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(currentProcess.getInputStream()), 8192)) {

                    StringBuilder batch = new StringBuilder();
                    int batchSize = 0;
                    final int MAX_BATCH_SIZE = 50;

                    int ch;
                    StringBuilder lineBuffer = new StringBuilder();
                    long lastCharTime = System.currentTimeMillis();
                    int inactivityCount = 0;

                    // Loop until the stream is fully drained.
                    while (true) {
                        if (reader.ready()) {
                            ch = reader.read();
                            if (ch == -1) break;
                            char c = (char) ch;
                            lineBuffer.append(c);

                            if (c == '\n') {
                                String currentLine = lineBuffer.toString();
                                totalLines++;

                                if (outputBuffer.size() >= maxOutputLines && !outputLimitReached) {
                                    outputLimitReached = true;
                                    outputConsumer.accept("\n--- Output limit reached ---\n");
                                }

                                if (outputBuffer.size() >= maxOutputLines) {
                                    outputBuffer.poll();
                                    droppedLines++;
                                    if (totalLines % 1000 == 0) {
                                        outputBuffer.add(currentLine.trim());
                                        batch.append(currentLine);
                                        batchSize++;
                                    }
                                } else {
                                    outputBuffer.add(currentLine.trim());
                                    batch.append(currentLine);
                                    batchSize++;
                                }

                                if (batchSize >= MAX_BATCH_SIZE) {
                                    String batchOutput = batch.toString();
                                    if (!batchOutput.isEmpty()) {
                                        outputConsumer.accept(batchOutput);
                                        batch.setLength(0);
                                        batchSize = 0;
                                    }
                                }
                                lastCharTime = System.currentTimeMillis();
                                inactivityCount = 0;
                                lineBuffer.setLength(0);
                            } else {
                                lastCharTime = System.currentTimeMillis();
                                inactivityCount = 0;
                            }
                        } else {
                            if (currentProcess.isAlive()) {
                                // Process is alive; check for inactivity to trigger input.
                                long currentTime = System.currentTimeMillis();
                                if (containsReadLine && !waitingForInput && (currentTime - lastCharTime > 500)) {
                                    inactivityCount++;
                                    if (inactivityCount > 3) {
                                        if (lineBuffer.length() > 0) {
                                            outputConsumer.accept(lineBuffer.toString());
                                            lineBuffer.setLength(0);
                                        }
                                        System.out.println("[DEBUG] KotlinRunner: Input required detected");
                                        waitingForInput = true;
                                        if (inputRequiredCallback != null) {
                                            SwingUtilities.invokeLater(inputRequiredCallback);
                                        }
                                        inactivityCount = 0;
                                    }
                                }
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            } else {
                                // Process has ended; drain any remaining output.
                                int chDrain = reader.read();
                                if (chDrain == -1) break;
                                else {
                                    char c = (char) chDrain;
                                    lineBuffer.append(c);
                                    if (c == '\n') {
                                        String currentLine = lineBuffer.toString();
                                        totalLines++;
                                        outputBuffer.add(currentLine.trim());
                                        batch.append(currentLine);
                                        batchSize++;
                                        lineBuffer.setLength(0);
                                    }
                                }
                            }
                        }
                    }

                    // Final flush of any buffered output.
                    if (lineBuffer.length() > 0) {
                        outputConsumer.accept(lineBuffer.toString());
                    }
                    if (batch.length() > 0) {
                        outputConsumer.accept(batch.toString());
                    }
                } catch (IOException e) {
                    if (running) {
                        errorConsumer.accept("Error reading process output: " + e.getMessage());
                    }
                }
            });

            // 4) Wait for the process to complete.
            exitCode[0] = currentProcess.waitFor();

            // Wait a bit more to capture any remaining output.
            try {
                outputFuture.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                outputFuture.cancel(true);
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
            waitingForInput = false;
            currentProcess = null;
            processInput = null;
            outputBuffer.clear();
            System.gc();

            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return exitCode[0];
    }

    @Override
    public void stopScript() {
        if (currentProcess != null) {
            running = false;
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
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
