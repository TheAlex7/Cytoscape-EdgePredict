package com.blant.edgepredict.internal.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class DockerUtil extends AbstractTask {
    private Boolean isWin;

    private static final String MAC_PATH =
        "/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin";

    @Override
    public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("Docker Environment Setup");

        String os = System.getProperty("os.name").toLowerCase();
        isWin = os.contains("win");

        // 1. Check Docker Daemon and attempt to start if not running
        monitor.setStatusMessage("Checking Docker daemon...");
        if (!executeCommand("docker info")) {
            monitor.setStatusMessage("Docker is not running. Attempting to start Docker...");
            tryToStartDocker();

            boolean isReady = false;
            int retries = 0;

            while (!isReady && retries < 10) {
                try {
                    Boolean check = executeCommand("docker info");
                    if (check) {
                        isReady = true;
                    } else {
                        monitor.setStatusMessage("Waiting for Docker to start... (" + retries + "/10)");
                        retries++;
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    retries++;
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }

            if (!isReady) {
                JOptionPane.showMessageDialog(
                    null,
                    "Docker failed to start. Please re-install Docker and try again.",
                    "Docker Not Running",
                    JOptionPane.INFORMATION_MESSAGE
                );
                openInstallationPage();
                throw new Exception("Docker failed to start.");
            }
        }

        // 2. Download image if not exists
        monitor.setStatusMessage("Checking for analysis image...");
        monitor.setProgress(0.3);
        boolean imageExists = executeCommand("docker image inspect thealex7/blant-predict");
        if (!imageExists) {
            monitor.setStatusMessage("Downloading analysis engine. This may take a few minutes ...");
            if (!pullImageWithProgress("thealex7/blant-predict", monitor)) {
                throw new Exception("Failed to download Docker image. Check your internet connection.");
            }
            monitor.setStatusMessage("Verifying downloaded image...");
            if (!executeCommand("docker image inspect thealex7/blant-predict")) {
                throw new Exception("Docker image was downloaded but could not be verified. Please try again.");
            }
        } else {
            monitor.setStatusMessage("Analysis image found, skipping build.");
        }
        monitor.setProgress(0.5);

        // 4. Run container (skip if service already up on the expected port)
        monitor.setStatusMessage("Starting Flask server...");
        monitor.setProgress(0.7);
        if (!isServiceAlreadyRunning()) {
            if (!executeCommand("docker start blant-svc")) {
                monitor.setStatusMessage("Container not found, creating new container...");
                monitor.setProgress(0.8);
                String runError = runContainer();
                if (runError != null) {
                    throw new Exception("Failed to start Docker container: " + runError);
                }
            }
        }

        // Wait for Flask to be ready before returning.
        // healthRetries is incremented for both connection failures (exception) and
        // non-200 responses so the 30-attempt bound is enforced in all cases.
        monitor.setStatusMessage("Waiting for server to be ready...");
        boolean serverReady = false;
        int healthRetries = 0;
        while (!serverReady && healthRetries < 30) {
            try {
                Thread.sleep(1000);
                HttpURLConnection healthConn = (HttpURLConnection) URI.create(BlantConfig.BLANT_URL_LOCAL).toURL().openConnection();
                healthConn.setConnectTimeout(1000);
                healthConn.setReadTimeout(1000);
                healthConn.setRequestMethod("GET");
                int code = healthConn.getResponseCode();
                healthConn.disconnect();
                if (code == 200) {
                    serverReady = true;
                } else {
                    healthRetries++;
                    monitor.setStatusMessage("Waiting for server (" + healthRetries + "/30)...");
                }
            } catch (Exception e) {
                healthRetries++;
                monitor.setStatusMessage("Waiting for server (" + healthRetries + "/30)...");
            }
        }
        if (!serverReady) {
            throw new Exception("Server did not start in time. Please try again.");
        }

        monitor.setStatusMessage("Setup completed successfully!");
        monitor.setProgress(1.0);
    }

    public void closeDocker() {
        // Stop container if exists
        String os = System.getProperty("os.name").toLowerCase();
        isWin = os.contains("win");
        executeCommand("docker stop blant-svc");
    }

    private boolean isServiceAlreadyRunning() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(BlantConfig.BLANT_URL_LOCAL).toURL().openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // Returns null on success, or the captured docker error output on failure.
    private String runContainer() {
        String cmd = "docker run -d --name blant-svc -p 49161:5000 --platform linux/amd64 thealex7/blant-predict";
        List<String> fullCmd = isWin
                ? List.of("cmd.exe", "/c", cmd)
                : List.of("sh", "-c", cmd);

        try {
            ProcessBuilder builder = new ProcessBuilder(fullCmd);
            if (!isWin) {
                builder.environment().put("PATH", MAC_PATH);
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Docker Run]: " + line);
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "timed out after 30 seconds";
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return output.toString().trim();
            }
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private boolean executeCommand(String command) {
        List<String> fullCmd = new ArrayList<>();
        if (isWin) {
            fullCmd.addAll(List.of("cmd.exe", "/c", command));
        } else {
            fullCmd.addAll(List.of("sh", "-c", command));
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(fullCmd);
            if (!isWin) {
                builder.environment().put("PATH", MAC_PATH);
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // Drain stdout/stderr on a daemon thread — prevents pipe-buffer deadlock
            // and avoids blocking the calling thread in readLine() when the process hangs
            Thread drainer = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Docker Log]: " + line);
                    }
                } catch (Exception ignored) {}
            });
            drainer.setDaemon(true);
            drainer.start();

            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("[ERROR] Command timed out: " + command);
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                System.err.println("[ERROR] Command failed with exit code " + exitCode);
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean pullImageWithProgress(String image, TaskMonitor monitor) {
        List<String> fullCmd = new ArrayList<>();
        String pullCmd = "docker pull " + image;
        if (isWin) {
            fullCmd.addAll(List.of("cmd.exe", "/c", pullCmd));
        } else {
            fullCmd.addAll(List.of("sh", "-c", pullCmd));
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(fullCmd);
            if (!isWin) {
                builder.environment().put("PATH", MAC_PATH);
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // Read output on a daemon thread so layer extraction (silent phase)
            // does not block the calling thread and freeze the Cytoscape task monitor
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("[Docker Pull]: " + line);
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            monitor.setStatusMessage("Downloading: " + trimmed);
                        }
                    }
                } catch (Exception ignored) {}
            });
            reader.setDaemon(true);
            reader.start();

            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("[ERROR] docker pull timed out after 10 minutes.");
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                System.err.println("[ERROR] docker pull failed with exit code " + exitCode);
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isDockerInstalled() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean win = os.contains("win");
        List<String> cmd = win
                ? List.of("cmd.exe", "/c", "where docker")
                : List.of("which", "docker");

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (!win) {
                pb.environment().put("PATH", MAC_PATH);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            Thread drainer = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    while (r.readLine() != null) {}
                } catch (Exception ignored) {}
            });
            drainer.setDaemon(true);
            drainer.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void tryToStartDocker() {
        if (isWin) {
            // Windows: Call Docker Desktop executable to start the daemon
            try {
                Process p = new ProcessBuilder("cmd.exe", "/c", "where docker").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();

                if (line != null && !line.isEmpty()) {
                    java.io.File dockerExe = new java.io.File(line);
                    java.io.File rootDir = dockerExe.getParentFile().getParentFile().getParentFile();
                    java.io.File desktopExe = new java.io.File(rootDir, "Docker Desktop.exe");

                    if (desktopExe.exists()) {
                        executeCommand("start \"\" \"" + desktopExe.getAbsolutePath() + "\"");
                    }
                }
            } catch (Exception e) {
                System.err.println("Dynamic path detection failed, trying default paths...");
            }
        } else {
            // macOS: open, Linux: systemctl
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac")) {
                executeCommand("open -a Docker");
            } else {
                executeCommand("sudo systemctl start docker");
            }
        }
    }

    public static void openInstallationPage() {
        String os = System.getProperty("os.name").toLowerCase();
        String url = "https://www.docker.com/products/docker-desktop";
        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TaskIterator createTaskIterator() {
        return new TaskIterator(this);
    }
}
