package ai.nextgpu.agent.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.nextgpu.agent.aop.Loggable;
import ai.nextgpu.agent.service.NextGpuAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Component
public class OSUtil {

    private static final Logger log = LoggerFactory.getLogger(OSUtil.class);

    public static final String OS_NAME;

    public static Boolean IS_WINDOWS;

    public static Boolean IS_LINUX;

    public static Boolean IS_MACOS;

    static {
        OS_NAME = System.getProperty("os.name").toLowerCase();
        IS_LINUX = OS_NAME.contains("linux");
        IS_WINDOWS = OS_NAME.contains("win");
        IS_MACOS = OS_NAME.contains("mac");
    }

    public static class InstallState {
        public boolean initialized = false;
        public int progressPercentage = 0;
        public String currentStepName = "Awaiting Start...";
        public String status = "PENDING";
        public String error = null;
    }

    public static void deleteInstallCredentials() {
        if (!IS_WINDOWS) return;

        Path nextGpuDir = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU");
        Path credFile = nextGpuDir.resolve("wsl_credentials.txt");

        try {
            if (Files.exists(credFile)) {
                Files.delete(credFile);
                log.info("Securely deleted temporary WSL credentials from local app data.");
            }
        } catch (IOException e) {
            log.warn("Failed to delete temporary WSL credentials: {}", e.getMessage());
        }
    }

    /**
     * Checks if a WSL distribution named 'nextgpu' already exists.
     * This is used by the UI to determine if an overwrite prompt is needed.
     */
    public static boolean checkIfNextGpuExists() {
        if (!IS_WINDOWS) return false;
        try {
            String output = executeCommand("wsl --list --quiet");
            // WSL output often contains null characters, so we clean it
            return output != null && output.replace("\0", "").contains("nextgpu");
        } catch (Exception e) {
            log.warn("Failed to check existing WSL instances: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Reads the state.json file to provide real-time installation progress to the UI.
     */
    public static InstallState getInstallState() {
        InstallState state = new InstallState();
        if (!IS_WINDOWS) return state;

        Path stateFile = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU", "state.json");
        if (Files.exists(stateFile)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(stateFile.toFile());
                state.initialized = root.path("initialized").asBoolean(false);
                state.progressPercentage = root.path("progressPercentage").asInt(0);
                state.currentStepName = root.path("currentStepName").asText("Loading...");
                state.status = root.path("status").asText("RUNNING");

                JsonNode errorNode = root.path("error");
                state.error = errorNode.isNull() ? null : errorNode.asText();
            } catch (IOException e) {
                log.warn("Could not read state.json: {}", e.getMessage());
            }
        }
        return state;
    }

    /**
     * Reads the tail of the installation debug log for the UI console.
     * Returns the last 50 lines to ensure UI performance remains smooth.
     */
    public static String getInstallLogs() {
        if (!IS_WINDOWS) return "Logs only available on Windows.";

        Path logFile = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU", "install_debug.log");
        if (!Files.exists(logFile)) {
            return "Initializing log stream...";
        }

        try {
            // For a production app, consider using a RandomAccessFile for massive logs,
            // but for this installation script, readAllLines is perfectly safe.
            List<String> allLines = Files.readAllLines(logFile, StandardCharsets.UTF_8);

            // Filter out empty lines to keep the console clean
            List<String> cleanLines = allLines.stream()
                    .filter(line -> !line.trim().isEmpty())
                    .toList();

            int start = Math.max(0, cleanLines.size() - 50); // Get last 50 lines
            return String.join("\n", cleanLines.subList(start, cleanLines.size()));
        } catch (IOException e) {
            return "Error reading logs: " + e.getMessage();
        }
    }

    public static String generateSecureWslPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";

        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        return upper.charAt((int) (Math.random() * upper.length())) + "" +
                lower.charAt((int) (Math.random() * lower.length())) + "" +
                digits.charAt((int) (Math.random() * digits.length())) + "" +
                special.charAt((int) (Math.random() * special.length())) + "" +
                randomPart;
    }

    public static void prepareInstallCredentials() throws IOException {
        Path nextGpuDir = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU");
        Files.createDirectories(nextGpuDir);

        Path credFile = nextGpuDir.resolve("wsl_credentials.txt");
        if (!Files.exists(credFile)) {
            String newPassword = generateSecureWslPassword();
            Files.writeString(credFile, newPassword, StandardCharsets.UTF_8);
            log.info("Secure WSL credentials generated and saved to local app data.");
        }
    }

    /**
     * Executes a given shell command based on the underlying operating system.
     *
     * @param command the shell command to execute
     * @return the output of the executed command as a String, or null if the operating system is unsupported
     * @throws IOException          if an I/O error occurs while executing the command
     * @throws InterruptedException if the current thread is interrupted while waiting for the command to complete
     */
    public static String executeCommand(String command) throws IOException, InterruptedException {
        if (IS_WINDOWS) {
            String[] commands = {
                    "powershell",
                    "-Command",
                    command,
            };
            return executeCommands(commands);
        }
        if (IS_LINUX || IS_MACOS) {
            String[] commands = {"/bin/sh", "-c", command};
            return executeCommands(commands);
        }
        return null;
    }

    /**
     * Checks if WSL2 (Microsoft-Windows-Subsystem-Linux) is enabled.
     *
     * @return true if enabled, false otherwise.
     */
    public static boolean isWslEnabled() {
        try {
            // Run your exact command
            String[] commands = {"wsl.exe", "--status"};

            String output = executeCommands(commands);

            // If the output tells the user to "enable" it, then it is currently DISABLED.
            return !output.contains("Please enable") && !output.contains("optional component to use WSL");
        } catch (Exception e) {
            // Fallback: If wsl.exe doesn't exist at all, it throws an exception, meaning it's disabled.
            return false;
        }
    }

    /**
     * Enables WSL2. Requires Administrator privileges.
     */
    public static boolean enableWsl() {
        try {
            String[] commands = {
                    "powershell.exe",
                    "-Command",
                    "Start-Process powershell -Verb RunAs -Wait -ArgumentList 'Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -NoRestart'"
            };
            executeCommands(commands);
            return true;
        } catch (Exception e) {
            System.err.println("Error enabling WSL: " + e.getMessage());
            return false;
        }
    }

    /**
     * Restarts the Windows operating system after a short delay.
     *
     * @return true if the restart command was successfully issued.
     */
    public static boolean restartSystem() {
        try {
            System.out.println("Scheduling a system restart in 60 seconds...");

            // /r = restart
            // /t 60 = time delay of 60 seconds
            // /c = custom comment displayed to the user
            String[] commands = {
                    "shutdown.exe",
                    "/r", "/t", "60", "/c",
                    "WSL2 installation requires a system restart. Saving your work is highly recommended. Open NextGPU after the restart to continue installation."
            };
            executeCommands(commands);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to issue restart command: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a system command and captures the output as a String.
     * The method internally uses a {@link ProcessBuilder} to handle command execution.
     *
     * @param command an array of strings representing the command and its arguments to be executed
     * @return the output of the executed command as a String
     * @throws IOException          if an I/O error occurs when creating the process or reading its output
     * @throws InterruptedException if the current thread is interrupted while waiting for the process to complete
     */
    @Loggable
    private static String executeCommands(String[] command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        return renderCommandOutput(process);
    }

    /**
     * Executes a Windows PowerShell script from the specified file path.
     * This method uses PowerShell with the "-NoProfile" and "-ExecutionPolicy Bypass" flags.
     * If the script fails with a non-zero exit code, an {@link IOException} is thrown.
     *
     * @param scriptPath the absolute or relative path to the PowerShell script file to be executed
     * @throws IOException          if an I/O error occurs, or the script execution fails with a non-zero exit code
     * @throws InterruptedException if the thread executing the script is interrupted while waiting for the process to complete
     */
    private static void executeWindowsScript(String scriptPath) throws IOException, InterruptedException {
        // Start elevated cmd, wait for it, and return its ExitCode via stdout
        String psCommand = String.format(
                "$p = Start-Process -FilePath 'cmd.exe' -Verb RunAs -Wait -PassThru -ArgumentList '/c \"%s\"'; " +
                        "exit $p.ExitCode",
                scriptPath.replace("\"", "\\\"")
        );
        log.info(String.format("Executing script: %s", psCommand));
        // Launch script and ignore output
        Process process = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", psCommand
        ).redirectErrorStream(true).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String output;
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            throw new IOException("Script failed with exit code: " + exitCode + (output.isBlank() ? "" : (". Output: " + output)));
        }
    }

    /**
     * Executes a Linux shell script located at the specified file path.
     * The script will be executed with "sudo" privileges and using the shell interpreter.
     *
     * @param scriptPath the full path to the Linux shell script to be executed
     * @throws IOException          if an I/O error occurs during the script execution process
     * @throws InterruptedException if the current thread is interrupted while waiting for the process to complete
     */
    public static void executeLinuxScript(String scriptPath) throws IOException, InterruptedException {
        new ProcessBuilder("sudo", "sh", scriptPath)
                .inheritIO()
                .start();
    }

    // Check if the file already exists
    public static boolean hasInstallCredentials() {
        Path nextGpuDir = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU");
        Path credFile = nextGpuDir.resolve("wsl_credentials.txt");
        return Files.exists(credFile);
    }

    // Save user-provided password
    public static void saveInstallCredentials(String password) throws IOException {
        Path nextGpuDir = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU");
        Files.createDirectories(nextGpuDir);

        Path credFile = nextGpuDir.resolve("wsl_credentials.txt");
        Files.writeString(credFile, password, StandardCharsets.UTF_8);
        log.info("User WSL credentials saved to local app data.");
    }

    /**
     * Initiates the asynchronous installation of prerequisites by extracting necessary installation
     * scripts, preparing credentials, and invoking PowerShell commands. This method is specifically
     * designed to run on Windows operating systems and uses administrative privileges to execute.
     *
     * @param overwriteExisting A boolean flag indicating whether to overwrite an existing WSL
     * installation if detected. When set to true, the installation
     * forcibly overwrites the current WSL configuration.
     * @param installProfile    The selected installation path (e.g., "provider" or "ai_hub") to
     * dictate which components the PowerShell script should install.
     * @throws IOException If required resources, such as scripts or credentials, cannot be found,
     * extracted, or executed. This includes issues like missing credential files
     * or errors during script extraction from the application resources.
     */
    public static void startPrerequisitesInstallAsync(boolean overwriteExisting, String installProfile) throws IOException {
        if (!IS_WINDOWS) {
            log.error("This installation method is currently only supported on Windows.");
            return;
        }

        // Guarantee credentials exist before script runs
        if (!hasInstallCredentials()) {
            throw new FileNotFoundException("wsl_credentials.txt not found. Credentials must be provided before installation.");
        }
        prepareInstallCredentials();

        // Extract the PS1 script from the Spring Boot JAR to the OS Temp directory
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path tempScriptPath = Paths.get(tmpDir, "install_prerequisites.ps1");

        try (InputStream is = new ClassPathResource("scripts/install_prerequisites.ps1").getInputStream()) {
            // This copies the file from the JAR to the Temp folder, overwriting any older version
            Files.copy(is, tempScriptPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully extracted installation script to: {}", tempScriptPath);
        } catch (IOException e) {
            log.error("Failed to extract script from classpath", e);
            throw new FileNotFoundException("Cannot find or extract installation script from classpath (resources/scripts/install_prerequisites.ps1).");
        }

        // Construct the async, elevated, hidden PowerShell command pointing to the Temp directory
        String args = "-ExecutionPolicy Bypass -File \\\"" + tempScriptPath.toString() + "\\\"";

        if (overwriteExisting) {
            args += " -OverwriteExistingWsl";
        }

        // Append the installation profile parameter if provided
        if (installProfile != null && !installProfile.isBlank()) {
            args += " -InstallProfile " + installProfile;
        }

        String psCommand = "Start-Process powershell.exe -ArgumentList '" + args + "' -Verb RunAs -WindowStyle Hidden";

        log.info("Starting background installation for profile [{}]: {}", installProfile, psCommand);

        // Start the process and immediately return (do not use .waitFor())
        new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCommand).start();
    }

    /**
     * Checks if the prerequisites are installed by reading the state file.
     * Uses the new JSON structure tracking setup completion.
     */
    public static boolean isPrerequisitesInstalled() {
        if (IS_WINDOWS) {
            return getInstallState().initialized;
        }

        // MacOS/Linux fallback
        Path stateDir = Paths.get(System.getProperty("user.home"), "NextGPU");
        Path stateFile = stateDir.resolve("state.json");
        if (!Files.exists(stateFile)) {
            return false;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(stateFile.toFile());
            return root != null && root.path("initialized").asBoolean();
        } catch (IOException e) {
            log.warn("Could not read state.json for MacOS/Linux: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Authenticates a user in a specified WSL (Windows Subsystem for Linux) distribution.
     * This method attempts to use the provided username and password to execute
     * a command within the specified WSL distribution, verifying if the credentials
     * are valid.
     *
     * @param distro   The name of the WSL distribution to authenticate against.
     * @param username The username to authenticate within the specified WSL distribution.
     * @param password The password associated with the provided username.
     * @return true if authentication is successful, false otherwise.
     */
    public static boolean authenticateWsl(String distro, String username, String password) {
        try {
            // Try a simple command that doesn't require sudo first to check if distro/user exists
            Process p = new ProcessBuilder(
                    "wsl", "-u", username, "-d", distro, "true"
            ).start();
            if (p.waitFor() != 0) {
                log.warn("WSL distribution '{}' or user '{}' not found or inaccessible.", distro, username);
                return false;
            }

            // Now check sudo authentication if password is provided
            if (password != null && !password.isBlank()) {
                Process sudoP = new ProcessBuilder(
                        "wsl", "-u", username, "-d", distro,
                        "sudo", "-k", "-S", "true"
                ).start();
                try (OutputStream os = sudoP.getOutputStream()) {
                    os.write((password + "\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                return sudoP.waitFor() == 0;
            }
            return true;
        } catch (IOException | InterruptedException e) {
            log.error("Error during WSL authentication for distro '{}': {}", distro, e.getMessage());
            return false;
        }
    }

    /**
     * Executes a command in a specified WSL (Windows Subsystem for Linux) distribution using a specific user.
     * The method constructs a command string to be executed in the WSL environment and sends the sudo password
     * (if necessary) as input to the process. The command executes in the context of the specified Linux user
     * within the given WSL distribution.
     *
     * @param command      the command to be executed within WSL
     * @param distro       the name of the WSL distribution where the command will be executed
     * @param username     the username under which the command will be executed
     * @param sudoPassword the password for the user, used for commands requiring sudo privileges
     * @return the standard output of the executed command as a string
     * @throws IOException if an I/O error occurs during the process creation or communication
     */
    @Loggable
    public static String executeCommandInWsl(String command, String distro, String username, String sudoPassword)
            throws IOException, InterruptedException {
        List<String> cmd = List.of(
                "wsl",
                "-u", username,
                "-d", distro,
                "--",
                "bash", "-c",
                "sudo -S " + command
        );
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()))) {
            w.write(sudoPassword);
            w.newLine();
            w.flush();
        }
        return renderCommandOutput(process);
    }

    /**
     * Executes a specified command within a particular Windows Subsystem for Linux (WSL) distribution
     * and under a given user. The method constructs and runs the command using the WSL environment.
     * The command output is captured and returned as a string.
     *
     * @param command  the command to be executed in the WSL environment
     * @param distro   the name of the WSL distribution where the command will be executed
     * @param username the username under which the command will be executed
     * @return the standard output of the executed command as a string
     * @throws IOException          if an I/O error occurs during the process creation or during command execution
     * @throws InterruptedException if the current thread is interrupted while waiting for the command to complete
     */
    @Loggable
    public static String executeCommandInWsl(String command, String distro, String username)
            throws IOException, InterruptedException {
        List<String> cmd = List.of(
                "wsl",
                "-u", username,
                "-d", distro,
                "--",
                "bash", "-c",
                command
        );
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        return renderCommandOutput(process);
    }

    /**
     * Reads and processes the output of a given {@link Process} object, capturing its
     * standard output stream, and waits for the process to complete.
     *
     * @param process the {@link Process} instance whose output is to be captured
     * @return the standard output of the process as a string, with null characters removed
     * @throws IOException          if an I/O error occurs while reading the process's output stream
     * @throws InterruptedException if the current thread is interrupted while waiting for the process to complete
     */
    @NotNull
    private static String renderCommandOutput(Process process) throws IOException, InterruptedException {
        // For binary output that might include UTF-16 encoded text
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        try (InputStream is = process.getInputStream()) {
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
        }
        process.waitFor();
        byte[] outputBytes = baos.toByteArray();
        // Check if output looks like UTF-16 (alternating values followed by 0s)
        boolean looksLikeUtf16 = outputBytes.length > 4 &&
                outputBytes.length % 2 == 0 &&
                (outputBytes[1] == 0 || outputBytes[3] == 0);
        String result;
        if (looksLikeUtf16) {
            result = new String(outputBytes, StandardCharsets.UTF_16LE);
        } else {
            result = new String(outputBytes, StandardCharsets.UTF_8);
        }
        // Clean up ANSI escape sequences which are common in WSL output
        result = result.replaceAll("\u001B\\[[\\d;]*[a-zA-Z]", "");
        return result.trim();
    }

    /**
     * Installs Docker in the specified WSL distribution.
     *
     * @param distro the name of the WSL distribution
     * @param username the username under which the command will be executed
     * @param password the password for the user, used for commands requiring sudo privileges
     * @return the output of the installation command
     * @throws IOException if an I/O error occurs during the process creation or communication
     * @throws InterruptedException if the current thread is interrupted while waiting for the command to complete
     */
    /**
     * @deprecated Use native installation methods instead.
     */
    @Deprecated
    @Loggable
    public static String installDocker(String distro, String username, String password)
            throws IOException, InterruptedException {
        return "Docker is no longer used. Skipping installation.";
    }

    /**
     * Sets up the Ollama container in the specified WSL distribution.
     *
     * @param distro the name of the WSL distribution
     * @param username the username under which the command will be executed
     * @param password the password for the user, used for commands requiring sudo privileges
     * @return true if the Ollama container was set up successfully, false otherwise
     * @throws IOException if an I/O error occurs during the process creation or communication
     * @throws InterruptedException if the current thread is interrupted while waiting for the command to complete
     */
    /**
     * @deprecated Use native systemd services instead.
     */
    @Deprecated
    @Loggable
    public static boolean setupOllamaContainer(String distro, String username, String password)
            throws IOException, InterruptedException {
        // Native setup uses systemd. We can ensure the service is running.
        executeCommandInWsl("sudo systemctl start ollama", distro, username, password);
        return true;
    }

    /**
     * Retrieves a list of models from the Ollama environment inside a container.
     *
     * @param distro the distribution name to execute the command in WSL
     * @param username the username to access the specified WSL distribution
     * @param password the password corresponding to the username
     * @return a string containing the list of models from the Ollama environment
     * @throws IOException if an I/O error occurs during the execution of the command
     * @throws InterruptedException if the execution of the command is interrupted
     */
    /**
     * @deprecated Use {@link NextGpuAiService#listDownloadedModels()} instead.
     */
    @Deprecated
    public static String getModelList(String distro, String username, String password) throws IOException, InterruptedException {
        // Native check using the installed ollama binary
        return executeCommandInWsl(
                "ollama list", distro, username, password
        );
    }

    /**
     * Deploys the DeepSeek model in the specified WSL distribution.
     *
     * @param distro the name of the WSL distribution
     * @param username the username under which the command will be executed
     * @param password the password for the user, used for commands requiring sudo privileges
     * @return the output of the deployment command, or a message indicating it was already deployed
     * @throws IOException if an I/O error occurs during the process creation or communication
     * @throws InterruptedException if the current thread is interrupted while waiting for the command to complete
     */
    /**
     * @deprecated Use native ollama pull command instead.
     */
    @Deprecated
    @Loggable
    public static String deployDeepSeekModel(String distro, String username, String password)
            throws IOException, InterruptedException {
        return deployModel("deepseek-r1:1.5b", distro, username, password);
    }

    /**
     * Deploys a specified model in the given WSL distribution.
     *
     * @param modelName the name of the model to deploy
     * @param distro    the name of the WSL distribution
     * @param username  the username under which the command will be executed
     * @param password  the password for the user, used for commands requiring sudo privileges
     * @return the output of the deployment command, or a message indicating it was already deployed
     * @throws IOException          if an I/O error occurs during the process creation or communication
     * @throws InterruptedException if the current thread is interrupted while waiting for the command to complete
     */
    /**
     * @deprecated Use {@link NextGpuAiService#pullOllamaModel(String)} instead.
     */
    @Deprecated
    @Loggable
    public static String deployModel(String modelName, String distro, String username, String password)
            throws IOException, InterruptedException {
        // Ensure the Ollama service is running
        setupOllamaContainer(distro, username, password);

        String modelList = getModelList(distro, username, password);
        // If the model name is found, don't trigger a new download / run
        if (modelList.contains(modelName)) {
            return "Model '" + modelName + "' is already available in Ollama.";
        }
        // Otherwise, run it - Ollama will download it if necessary
        return executeCommandInWsl(
                "ollama run " + modelName, distro, username, password
        );
    }

    public static Process keepAliveProcess;

    /**
     * Starts the specified WSL distribution and makes sure the services used by the app are running.
     *
     * <p>A one-shot no-op command can wake WSL, but the distro may shut down again after the command exits.
     * The keepalive process prevents that idle shutdown while the app is expecting localhost-forwarded
     * services to remain reachable.</p>
     *
     * @param distro   the WSL distribution name
     * @param username the WSL username
     * @param password the password for WSL username
     * @return true when the WSL startup command is executed successfully
     */
    public static boolean ensureWslStarted(String distro, String username, String password) {
        if (!IS_WINDOWS) {
            return true;
        }
        if (distro == null || distro.isBlank() || username == null || username.isBlank()) {
            log.error("WSL distribution or username is missing. Distro: {}, Username: {}", distro, username);
            return false;
        }
        try {
            log.info("Starting WSL distribution '{}' for user '{}'...", distro, username);

            // Check if keep-alive is already running
            if (keepAliveProcess != null && keepAliveProcess.isAlive()) {
                log.info("WSL keep-alive process is already running.");
            } else {
                log.info("Starting persistent keep-alive process in WSL...");
                // We run sleep infinity WITHOUT & and keep the Process object alive in Java.
                // This ensures WSL distribution stays 'Running'.
                keepAliveProcess = new ProcessBuilder("wsl", "-u", username, "-d", distro, "--", "sleep", "infinity").start();

                // Give it a moment to start
                Thread.sleep(1000);
            }

            String startupCommand = String.join(" ",
                    "set -e;",
                    "if command -v systemctl >/dev/null 2>&1; then",
                    "sudo -n systemctl start ollama >/dev/null 2>&1 || true;",
                    "sudo -n systemctl start comfyui >/dev/null 2>&1 || true;",
                    "else", "sudo -n service ollama start >/dev/null 2>&1 || true;",
                    "sudo -n service comfyui start >/dev/null 2>&1 || true;",
                    "fi"
            );
            executeCommandInWsl(startupCommand, distro, username, password);
            log.info("WSL distribution '{}' startup command executed successfully.", distro);
            refreshWslPortProxy(distro, username, password);
            return true;
        } catch (Exception e) {
            log.error("Unable to start WSL distribution '{}': {}", distro, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Refreshes Windows portproxy mappings to current WSL IP.
     * <p>
     * Ensures localhost:<port> routes correctly to WSL services even after IP changes.
     */
    @Loggable
    public static void refreshWslPortProxy(String distro, String username, String password)
            throws IOException, InterruptedException {
        if (!IS_WINDOWS) {
            return;
        }
        // Get current WSL IP
        String rawIp = executeCommandInWsl("hostname -I | awk '{print $1}'", distro, username, password);
        if (rawIp.isBlank()) {
            throw new RuntimeException("Failed to retrieve WSL IP.");
        }
        String wslIp = rawIp.trim();
        log.info("Detected WSL IP: {}", wslIp);

        // TODO: Externalize these
        int ollamaPort = 11434;
        int comfyPort = 8188;
        int sttToolPort = 8177;

        // Reset proxies
        executeCommand("netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=" + ollamaPort);
        executeCommand("netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=" + comfyPort);
        executeCommand("netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=" + sttToolPort);

        // Add fresh proxies
        executeCommand("netsh interface portproxy add v4tov4 listenaddress=127.0.0.1 listenport=" + ollamaPort +
                " connectaddress=" + wslIp + " connectport=" + ollamaPort);
        executeCommand("netsh interface portproxy add v4tov4 listenaddress=127.0.0.1 listenport=" + comfyPort +
                " connectaddress=" + wslIp + " connectport=" + comfyPort);
        executeCommand("netsh interface portproxy add v4tov4 listenaddress=127.0.0.1 listenport=" + sttToolPort +
                " connectaddress=" + wslIp + " connectport=" + sttToolPort);

        // Optional: wait a bit for Windows to apply portproxy changes
        Thread.sleep(3000);

        if (isTcpPortBusy("127.0.0.1", ollamaPort)) {
            log.warn("Port {} not reachable after proxy refresh.", ollamaPort);
        }
        if (isTcpPortBusy("127.0.0.1", comfyPort)) {
            log.warn("Port {} not reachable after proxy refresh.", comfyPort);
        }
        if (isTcpPortBusy("127.0.0.1", sttToolPort)) {
            log.warn("Port {} not reachable after proxy refresh.", sttToolPort);
        }
        log.info("Portproxy refreshed -> localhost:{}, localhost:{} and localhost:{} now point to {}", ollamaPort, comfyPort, sttToolPort, wslIp);
    }

    /**
     * Reads the openclaw_state.json file to provide real-time installation progress to the UI.
     */
    public static InstallState getOpenclawInstallState() {
        InstallState state = new InstallState();
        if (!IS_WINDOWS) return state;

        Path stateFile = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU", "openclaw_state.json");
        if (Files.exists(stateFile)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(stateFile.toFile());
                state.initialized = root.path("initialized").asBoolean(false);
                state.progressPercentage = root.path("progressPercentage").asInt(0);
                state.currentStepName = root.path("currentStepName").asText("Loading...");
                state.status = root.path("status").asText("RUNNING");

                JsonNode errorNode = root.path("error");
                state.error = errorNode.isNull() ? null : errorNode.asText();
            } catch (IOException e) {
                log.warn("Could not read openclaw_state.json: {}", e.getMessage());
            }
        }
        return state;
    }

    /**
     * Reads the tail of the Openclaw installation debug log for the UI console.
     * Returns the last 50 lines with all terminal ANSI codes stripped out.
     */
    public static String getOpenclawLogs() {
        if (!IS_WINDOWS) return "Logs only available on Windows.";

        Path logFile = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU", "openclaw_debug.log");
        if (!Files.exists(logFile)) {
            return "Initializing Openclaw log stream...";
        }

        try {
            List<String> allLines = Files.readAllLines(logFile, StandardCharsets.UTF_8);

            // Filter out empty lines and completely strip ANSI escape codes
            List<String> cleanLines = allLines.stream()
                    .map(line -> line
                            // Strip standard CSI escape sequences (e.g., colors, cursor show/hide like \u001B[38;5;203m or \u001B[?25h)
                            .replaceAll("\u001B\\[[\\d;?]*[A-Za-z]", "")
                            // Strip OSC terminal query sequences (e.g., \u001B]11;?\u001B\)
                            .replaceAll("\u001B\\][^\u001B\u0007]*(\u0007|\u001B\\\\)", "")
                            .trim())
                    .filter(line -> !line.isEmpty())
                    .toList();

            int start = Math.max(0, cleanLines.size() - 50); // Get last 50 lines
            return String.join("\n", cleanLines.subList(start, cleanLines.size()));
        } catch (IOException e) {
            return "Error reading Openclaw logs: " + e.getMessage();
        }
    }

    /**
     * Initiates the asynchronous installation of Openclaw by extracting the installation
     * script and invoking it via an elevated PowerShell command.
     */
    public static void startOpenclawInstallAsync() throws IOException {
        if (!IS_WINDOWS) {
            log.error("This installation method is currently only supported on Windows.");
            return;
        }

        // Extract the PS1 script from the Spring Boot JAR to the OS Temp directory
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path tempScriptPath = Paths.get(tmpDir, "install_openclaw.ps1");

        try (InputStream is = new ClassPathResource("scripts/install_openclaw.ps1").getInputStream()) {
            Files.copy(is, tempScriptPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully extracted Openclaw installation script to: {}", tempScriptPath);
        } catch (IOException e) {
            log.error("Failed to extract script from classpath", e);
            throw new FileNotFoundException("Cannot find or extract installation script from classpath (resources/scripts/install_openclaw.ps1).");
        }

        // Construct the async, elevated, hidden PowerShell command pointing to the Temp directory
        String args = "-ExecutionPolicy Bypass -File \\\"" + tempScriptPath.toString() + "\\\"";
        String psCommand = "Start-Process powershell.exe -ArgumentList '" + args + "' -Verb RunAs -WindowStyle Hidden";

        log.info("Starting background Openclaw installation: {}", psCommand);

        // Start the process and immediately return (do not use .waitFor())
        new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCommand).start();
    }

    /**
     * Executes the OpenClaw uninstallation script synchronously and silently.
     * Blocks until the script completes, returning true if successful.
     */
    public static boolean uninstallOpenclaw() {
        if (!IS_WINDOWS) {
            log.error("This uninstallation method is currently only supported on Windows.");
            return false;
        }

        String tmpDir = System.getProperty("java.io.tmpdir");
        Path tempScriptPath = Paths.get(tmpDir, "uninstall_openclaw.ps1");

        try (InputStream is = new ClassPathResource("scripts/uninstall_openclaw.ps1").getInputStream()) {
            Files.copy(is, tempScriptPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully extracted OpenClaw uninstallation script to: {}", tempScriptPath);
        } catch (IOException e) {
            log.error("Failed to extract uninstall script from classpath", e);
            return false;
        }

        try {
            log.info("Starting OpenClaw uninstallation (Hidden & Synchronous)...");

            // Format args to explicitly run powershell silently, bypassing cmd.exe entirely
            String args = "-ExecutionPolicy Bypass -WindowStyle Hidden -File \\\"" + tempScriptPath.toString() + "\\\"";
            String psCommand = "Start-Process powershell.exe -ArgumentList '" + args + "' -Verb RunAs -WindowStyle Hidden -Wait";

            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCommand).start();
            process.waitFor(); // Blocks the background thread until uninstallation finishes

            log.info("OpenClaw uninstallation completed successfully.");

            // Clean up the openclaw_state.json file so reinstall starts fresh
            Path stateFile = Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU", "openclaw_state.json");
            Files.deleteIfExists(stateFile);

            return true;
        } catch (Exception e) {
            log.error("Failed to uninstall OpenClaw: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Reads the OpenClaw configuration file from WSL to extract the Gateway authentication token.
     *
     * @param distro   the WSL distribution name
     * @param username the WSL username
     * @return the authentication token, or null if not found
     */
    public static String getOpenclawGatewayToken(String distro, String username) {
        if (!IS_WINDOWS) return null;
        try {
            // Read the config file securely from the user's home directory
            String jsonContent = executeCommandInWsl("cat ~/.openclaw/openclaw.json", distro, username);

            if (jsonContent != null && !jsonContent.isBlank()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(jsonContent);
                // Navigate to gateway -> token
                String token = root.path("gateway").path("auth").path("token").asText(null);

                log.info("Extracted OpenClaw Gateway Token: {}", token);

                return token;
            }
        } catch (Exception e) {
            log.warn("Failed to extract OpenClaw gateway token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the IP address of the WSL distribution for use with network services like Ollama.
     * This method is used to construct URLs for accessing services running inside WSL from the Windows host.
     *
     * @param distro the WSL distribution name
     * @param username the WSL username
     * @return the IP address of the distro, or null if unable to retrieve
     */
    public static String getLocalIpAddress(String distro, String username) {
        if (IS_WINDOWS) {
            try {
                String rawIp = executeCommandInWsl("hostname -I | awk '{print $1}'", distro, username);
                if (rawIp.isBlank()) {
                    log.warn("Failed to retrieve local IP for distro '{}': empty response", distro);
                    return null;
                }
                log.info("Retrieved local IP for distro '{}': {}", distro, rawIp.trim());
                return rawIp.trim();
            } catch (IOException | InterruptedException e) {
                log.warn("Failed to retrieve local IP for distro '{}': {}", distro, e.getMessage());
            }
        }
        return null;
    }

    private static boolean isTcpPortBusy(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }
}
