package ai.nextgpu.agent.config;

import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.agent.util.OSUtil;
import ai.nextgpu.common.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalPropertyConfig {

    private static final Logger log = LoggerFactory.getLogger(GlobalPropertyConfig.class);

    // Contains the token used during the session
    public static final String JWT_TOKEN = "JWT_TOKEN";

    // Contains the UUID of the computer saved in the local DB
    public static final String COMPUTER_UUID = "COMPUTER_UUID";

    // Contains the Ethereum wallet address of the user currently logged in
    public static final String LOGIN_WALLET = "LOGIN_WALLET";

    // Contains the JRE installed in the PPC
    public static final String INSTALLED_JRE = "INSTALLED_JRE";

    // Contains Linux distro on PPC or the WSL (for Windows)
    public static final String LINUX_DISTRO = "LINUX_DISTRO";

    // Contains version of Nvidia CUDA library
    public static final String CUDA_VERSION = "CUDA_VERSION";

    // Unlock code is required to unlock the UI once the PPC starts serving
    public static final String UNLOCK_CODE = "UNLOCK_CODE";

    // Username of the operating environment (Linux/WSL Distro)
    public static final String OS_USERNAME = "OS_USERNAME";

    // Password for the username of the operating environment (Linux/WSL Distro)
    public static final String OS_PASSWORD = "OS_PASSWORD";

    // Indicates whether the EULA has been accepted or not
    public static final String IS_EULA_ACCEPTED = "IS_EULA_ACCEPTED";

    // Indicates whether the client machine setup has been completed or not
    public static final String IS_SETUP_COMPLETED = "IS_SETUP_COMPLETED";

    public static final String SETUP_VERSION = "SETUP_VERSION";


    // Comma-separated list of values Indicates the AI usage preferences of the user
    public static final String USAGE_PREFERENCES = "USAGE_PREFERENCES";

    // Defines the maximum number of tokens to be kept in the chat history
    public static final String TOKEN_HISTORY_LIMIT = "TOKEN_HISTORY_LIMIT";

    public static final String MAX_PINNED_MESSAGES = "MAX_PINNED_MESSAGES";

    // Application settings
    public static final String APP_THEME = "APP_THEME";

    public static final String IS_PRIVATE_MODE = "IS_PRIVATE_MODE";

    public static final String IS_ADVANCED_MODE = "IS_ADVANCED_MODE";

    // Indicates whether the OpenClaw setup has been completed or not
    public static final String IS_OPENCLAW_SETUP_COMPLETED = "IS_OPENCLAW_SETUP_COMPLETED";

    // Indicates if the OpenClaw shortcut should be pinned to the app's main navigation
    public static final String SHOW_OPENCLAW_SHORTCUT = "SHOW_OPENCLAW_SHORTCUT";

    // Indicates the selected installation path: 'provider' or 'ai_hub'
    public static final String INSTALL_PROFILE = "INSTALL_PROFILE";

    // Tracks the last active screen to restore the session upon restart
    public static final String LAST_ACTIVE_SCREEN = "LAST_ACTIVE_SCREEN";

    public static final String HAS_NVIDIA_GPU = "HAS_NVIDIA_GPU";

    // IP address of the WSL distribution (cached to avoid repeated WSL queries)
    public static final String LOCAL_IP = "WSL_IP";

    // Application uptime timestamp
    public static Long APPLICATION_UP_TIMESTAMP;

    private final GlobalPropertyRepository globalPropertyRepository;

    @Autowired
    public GlobalPropertyConfig(GlobalPropertyRepository globalPropertyRepository) {
        this.globalPropertyRepository = globalPropertyRepository;
    }

    public void InitializeGlobalProperties() {

        if (globalPropertyRepository.findByName(UNLOCK_CODE).isEmpty()) {
            updateUnlockCodeProperty(StringUtil.generateRandomHexString(6));
        }
        if (globalPropertyRepository.findByName(INSTALLED_JRE).isEmpty()) {
            updateInstalledJreProperty();
        }
        if (globalPropertyRepository.findByName(CUDA_VERSION).isEmpty()) {
            updateCudaVersionProperty();
        }
        if (globalPropertyRepository.findByName(LINUX_DISTRO).isEmpty()) {
            updateLinuxDistroProperty("nextgpu");
        }
        if (globalPropertyRepository.findByName(OS_USERNAME).isEmpty()) {
            updateOsUsernameProperty("nextgpu");
        }
        if (globalPropertyRepository.findByName(OS_PASSWORD).isEmpty()) {
            updateOsPasswordProperty("");
        }
        if (globalPropertyRepository.findByName(IS_EULA_ACCEPTED).isEmpty()) {
            updateIsEulaAcceptedProperty(false);
        }
        if (globalPropertyRepository.findByName(IS_SETUP_COMPLETED).isEmpty()) {
            updateIsSetupCompleteProperty(false);
        }
        if (globalPropertyRepository.findByName(SETUP_VERSION).isEmpty()) {
            updateSetupVersionProperty(0);
        }
        if (globalPropertyRepository.findByName(USAGE_PREFERENCES).isEmpty()) {
            updateUsagePreferencesProperty("");
        }
        if  (globalPropertyRepository.findByName(TOKEN_HISTORY_LIMIT).isEmpty()) {
            updateTokenHistoryLimitProperty(128_000);
        }
        if (globalPropertyRepository.findByName(MAX_PINNED_MESSAGES).isEmpty()) {
            createMaxPinnedMessagesProperty();
        }
        if (globalPropertyRepository.findByName(APP_THEME).isEmpty()) {
            createAppThemeProperty();
        }
        if (globalPropertyRepository.findByName(IS_PRIVATE_MODE).isEmpty()) {
            createPrivateModeProperty();
        }
        if (globalPropertyRepository.findByName(IS_ADVANCED_MODE).isEmpty()) {
            createAdvancedModeProperty();
        }
        if (globalPropertyRepository.findByName(IS_OPENCLAW_SETUP_COMPLETED).isEmpty()) {
            updateIsOpenclawSetupCompleteProperty(false);
        }
        if (globalPropertyRepository.findByName(SHOW_OPENCLAW_SHORTCUT).isEmpty()) {
            createOpenclawShortcutProperty();
        }
        if (globalPropertyRepository.findByName(INSTALL_PROFILE).isEmpty()) {
            updateInstallProfileProperty(""); // Default empty until user selects a path
        }
        if (globalPropertyRepository.findByName(LAST_ACTIVE_SCREEN).isEmpty()) {
            updateLastActiveScreenProperty(""); // Default to empty
        }
        if (globalPropertyRepository.findByName(HAS_NVIDIA_GPU).isEmpty()) {
            updateHasNvidiaGpuProperty(false);
        }
        if (globalPropertyRepository.findByName(LOCAL_IP).isEmpty()) {
            updateLocalIpProperty("127.0.0.1");
        }
    }

    public void updateUnlockCodeProperty(String unlockCode) {
        try {
            globalPropertyRepository.findByName(UNLOCK_CODE).ifPresentOrElse(unlock -> {
                    unlock.setValueReference(unlockCode);
                        globalPropertyRepository.save(unlock);
                    }, () -> {
                        GlobalProperty unlock = new GlobalProperty();
                        unlock.setName(UNLOCK_CODE);
                        unlock.setDatatype("java.lang.String");
                        unlock.setDescription("The unlock code used to unlock the UI once the PPC starts serving");
                        unlock.setVersion(1);
                        unlock.setRetired(false);
                        unlock.setValueReference(unlockCode);
                        globalPropertyRepository.save(unlock);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateJwtProperty(String jwtToken) {
        try {
            globalPropertyRepository.findByName(JWT_TOKEN).ifPresentOrElse(jwtProperty -> {
                jwtProperty.setValueReference(jwtToken);
                globalPropertyRepository.save(jwtProperty);
            }, () -> {
                GlobalProperty jwtProperty = new GlobalProperty();
                jwtProperty.setName(JWT_TOKEN);
                jwtProperty.setDatatype("java.lang.String");
                jwtProperty.setDescription("The Json Web Token used for authentication with the server");
                jwtProperty.setVersion(1);
                jwtProperty.setRetired(false);
                jwtProperty.setValueReference(jwtToken);
                globalPropertyRepository.save(jwtProperty);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateInstalledJreProperty() {
        try {
            String jreVersion = OSUtil.executeCommand("java -version");
            assert jreVersion != null;
            jreVersion = jreVersion.split("\"")[1]; // Extract semantic version from raw output using quotes
            GlobalProperty jreProperty = new GlobalProperty();
            jreProperty.setName(INSTALLED_JRE);
            jreProperty.setDatatype("java.lang.String");
            jreProperty.setDescription("The installed Java Runtime Environment version");
            jreProperty.setVersion(1);
            jreProperty.setRetired(false);
            jreProperty.setValueReference(jreVersion);
            globalPropertyRepository.save(jreProperty);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateCudaVersionProperty() {
        try {
            String rawOutput = OSUtil.executeCommand("nvcc --version");
            // Match line like: "Cuda compilation tools, release 12.2, V12.2.91"
            assert rawOutput != null;
            rawOutput = rawOutput.substring(rawOutput.indexOf("release") + 7);
            String[] parts = rawOutput.split(",");
            String version = parts[0].trim();
            GlobalProperty cudaVersionProperty = new GlobalProperty();
            cudaVersionProperty.setName(CUDA_VERSION);
            cudaVersionProperty.setDatatype("java.lang.String");
            cudaVersionProperty.setDescription("The installed CUDA version on the system");
            cudaVersionProperty.setVersion(1);
            cudaVersionProperty.setRetired(false);
            cudaVersionProperty.setValueReference(version);
            globalPropertyRepository.save(cudaVersionProperty);
        } catch (Exception e) {
            log.error("Error detecting CUDA version: {}", e.getMessage());
        }
    }

    public void updateLinuxDistroProperty(String instanceName) {
        try {
            globalPropertyRepository.findByName(LINUX_DISTRO).ifPresentOrElse(distro -> {
                        distro.setValueReference(instanceName);
                        globalPropertyRepository.save(distro);
                    },
                    () -> {
                        GlobalProperty distro = new GlobalProperty();
                        distro.setName(LINUX_DISTRO);
                        distro.setDatatype("java.lang.String");
                        distro.setDescription("The installed Distribution of Linux");
                        distro.setVersion(1);
                        distro.setRetired(false);
                        distro.setValueReference(instanceName);
                        globalPropertyRepository.save(distro);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateOsUsernameProperty(String username) {
        try {
            globalPropertyRepository.findByName(OS_USERNAME).ifPresentOrElse(osUsername -> {
                        osUsername.setValueReference(username);
                        globalPropertyRepository.save(osUsername);
                    },
                    () -> {
                        GlobalProperty osUsername = new GlobalProperty();
                        osUsername.setName(OS_USERNAME);
                        osUsername.setDatatype("java.lang.String");
                        osUsername.setDescription("The Username of the Operating System");
                        osUsername.setVersion(1);
                        osUsername.setRetired(false);
                        osUsername.setValueReference(username);
                        globalPropertyRepository.save(osUsername);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateOsPasswordProperty(String password) {
        try {
            globalPropertyRepository.findByName(OS_PASSWORD).ifPresentOrElse(osPassword -> {
                        osPassword.setValueReference(password);
                        globalPropertyRepository.save(osPassword);
                    }, () -> {
                        GlobalProperty osPassword = new GlobalProperty();
                        osPassword.setName(OS_PASSWORD);
                        osPassword.setDatatype("java.lang.String");
                        osPassword.setDescription("The Password of the Operating System");
                        osPassword.setVersion(1);
                        osPassword.setRetired(false);
                        osPassword.setValueReference(password);
                        globalPropertyRepository.save(osPassword);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateIsEulaAcceptedProperty(boolean eulaAccepted) {
        try {
            globalPropertyRepository.findByName(IS_EULA_ACCEPTED).ifPresentOrElse(eula -> {
                        eula.setValueReference(eulaAccepted ? "true" : "false");
                        globalPropertyRepository.save(eula);
                    }, () -> {
                        GlobalProperty eula = new GlobalProperty();
                        eula.setName(IS_EULA_ACCEPTED);
                        eula.setDatatype("java.lang.Boolean");
                        eula.setDescription("Indicates whether the EULA has been accepted or not");
                        eula.setVersion(1);
                        eula.setRetired(false);
                        eula.setValueReference(eulaAccepted ? "true" : "false");
                        globalPropertyRepository.save(eula);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateIsSetupCompleteProperty(boolean setupComplete) {
        try {
            globalPropertyRepository.findByName(IS_SETUP_COMPLETED).ifPresentOrElse(setup -> {
                        setup.setValueReference(Boolean.toString(setupComplete));
                        globalPropertyRepository.save(setup);
                    }, () -> {
                        GlobalProperty setup = new GlobalProperty();
                        setup.setName(IS_SETUP_COMPLETED);
                        setup.setDatatype("java.lang.Boolean");
                        setup.setDescription("Indicates whether the client machine setup has been completed or not");
                        setup.setVersion(1);
                        setup.setRetired(false);
                        setup.setValueReference(setupComplete ? "true" : "false");
                        globalPropertyRepository.save(setup);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateSetupVersionProperty(int version) {
        try {
            globalPropertyRepository.findByName(SETUP_VERSION).ifPresentOrElse(setup -> {
                        setup.setValueReference(Integer.toString(version));
                        globalPropertyRepository.save(setup);
                    }, () -> {
                        GlobalProperty setup = new GlobalProperty();
                        setup.setName(SETUP_VERSION);
                        setup.setDatatype("java.lang.Integer");
                        setup.setDescription("Indicates the version of the setup used for installation of prerequisites.");
                        setup.setVersion(1);
                        setup.setRetired(false);
                        setup.setValueReference(Integer.toString(version));
                        globalPropertyRepository.save(setup);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateUsagePreferencesProperty(String preferences) {
        try {
            globalPropertyRepository.findByName(USAGE_PREFERENCES).ifPresentOrElse(preference -> {
                        preference.setValueReference(preferences);
                        globalPropertyRepository.save(preference);
                    }, () -> {
                        GlobalProperty preference = new GlobalProperty();
                        preference.setName(USAGE_PREFERENCES);
                        preference.setDatatype("java.lang.String");
                        preference.setDescription("Comma-separated list of values indicating the AI usage preferences of the user");
                        preference.setVersion(1);
                        preference.setRetired(false);
                        preference.setValueReference(preferences);
                        globalPropertyRepository.save(preference);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateTokenHistoryLimitProperty(int tokenHistoryLimit) {
        try {
            globalPropertyRepository.findByName(TOKEN_HISTORY_LIMIT).ifPresentOrElse(preference -> {
                        preference.setValueReference(String.valueOf(tokenHistoryLimit));
                        globalPropertyRepository.save(preference);
                    }, () -> {
                        GlobalProperty preference = new GlobalProperty();
                        preference.setName(TOKEN_HISTORY_LIMIT);
                        preference.setDatatype("java.lang.Integer");
                        preference.setDescription("Maximum number of tokens to be kept in the chat history");
                        preference.setVersion(1);
                        preference.setRetired(false);
                        preference.setValueReference(String.valueOf(tokenHistoryLimit));
                        globalPropertyRepository.save(preference);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateIsOpenclawSetupCompleteProperty(boolean setupComplete) {
        try {
            globalPropertyRepository.findByName(IS_OPENCLAW_SETUP_COMPLETED).ifPresentOrElse(setup -> {
                        setup.setValueReference(setupComplete ? "true" : "false");
                        globalPropertyRepository.save(setup);
                    }, () -> {
                        GlobalProperty setup = new GlobalProperty();
                        setup.setName(IS_OPENCLAW_SETUP_COMPLETED);
                        setup.setDatatype("java.lang.Boolean");
                        setup.setDescription("Indicates whether the OpenClaw integration setup has been completed or not");
                        setup.setVersion(1);
                        setup.setRetired(false);
                        setup.setValueReference(setupComplete ? "true" : "false");
                        globalPropertyRepository.save(setup);
                    }
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateInstallProfileProperty(String profile) {
        try {
            globalPropertyRepository.findByName(INSTALL_PROFILE).ifPresentOrElse(p -> {
                p.setValueReference(profile);
                globalPropertyRepository.save(p);
            }, () -> {
                GlobalProperty p = new GlobalProperty();
                p.setName(INSTALL_PROFILE);
                p.setDatatype("java.lang.String");
                p.setDescription("The selected installation profile: 'provider' or 'ai_hub'");
                p.setVersion(1);
                p.setRetired(false);
                p.setValueReference(profile);
                globalPropertyRepository.save(p);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateLastActiveScreenProperty(String screen) {
        try {
            globalPropertyRepository.findByName(LAST_ACTIVE_SCREEN).ifPresentOrElse(p -> {
                p.setValueReference(screen);
                globalPropertyRepository.save(p);
            }, () -> {
                GlobalProperty p = new GlobalProperty();
                p.setName(LAST_ACTIVE_SCREEN);
                p.setDatatype("java.lang.String");
                p.setDescription("The last active screen before the application was closed");
                p.setVersion(1);
                p.setRetired(false);
                p.setValueReference(screen);
                globalPropertyRepository.save(p);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateHasNvidiaGpuProperty(boolean hasGpu) {
        try {
            globalPropertyRepository.findByName(HAS_NVIDIA_GPU).ifPresentOrElse(p -> {
                p.setValueReference(hasGpu ? "true" : "false");
                globalPropertyRepository.save(p);
            }, () -> {
                GlobalProperty p = new GlobalProperty();
                p.setName(HAS_NVIDIA_GPU);
                p.setDatatype("java.lang.Boolean");
                p.setDescription("Indicates if a compatible Nvidia GPU was detected");
                p.setVersion(1);
                p.setRetired(false);
                p.setValueReference(hasGpu ? "true" : "false");
                globalPropertyRepository.save(p);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }


    private void createMaxPinnedMessagesProperty() {
        GlobalProperty maxPinnedProperty = new GlobalProperty();
        maxPinnedProperty.setName(MAX_PINNED_MESSAGES);
        maxPinnedProperty.setDatatype("java.lang.Integer");
        maxPinnedProperty.setDescription("The maximum number of messages allowed to be pinned per session");
        maxPinnedProperty.setVersion(1);
        maxPinnedProperty.setRetired(false);
        maxPinnedProperty.setValueReference("10"); // Default limit
        globalPropertyRepository.save(maxPinnedProperty);
    }

    private void createAppThemeProperty() {
        GlobalProperty themeProp = new GlobalProperty();
        themeProp.setName(APP_THEME);
        themeProp.setDatatype("java.lang.String");
        themeProp.setDescription("The UI theme preference: Light, Dark, or System");
        themeProp.setVersion(1);
        themeProp.setRetired(false);
        themeProp.setValueReference("Light");
        globalPropertyRepository.save(themeProp);
    }

    private void createPrivateModeProperty() {
        GlobalProperty privateModeProp = new GlobalProperty();
        privateModeProp.setName(IS_PRIVATE_MODE);
        privateModeProp.setDatatype("java.lang.Boolean");
        privateModeProp.setDescription("Toggle to enable or disable private mode");
        privateModeProp.setVersion(1);
        privateModeProp.setRetired(false);
        privateModeProp.setValueReference("false"); // Default
        globalPropertyRepository.save(privateModeProp);
    }

    private void createAdvancedModeProperty() {
        GlobalProperty advancedModeProp = new GlobalProperty();
        advancedModeProp.setName(IS_ADVANCED_MODE);
        advancedModeProp.setDatatype("java.lang.Boolean");
        advancedModeProp.setDescription("Toggle to expose advanced configuration options");
        advancedModeProp.setVersion(1);
        advancedModeProp.setRetired(false);
        advancedModeProp.setValueReference("false"); // Default
        globalPropertyRepository.save(advancedModeProp);
    }

    private void createOpenclawShortcutProperty() {
        GlobalProperty shortcutProp = new GlobalProperty();
        shortcutProp.setName(SHOW_OPENCLAW_SHORTCUT);
        shortcutProp.setDatatype("java.lang.Boolean");
        shortcutProp.setDescription("Toggle to show OpenClaw shortcut in the main app navigation");
        shortcutProp.setVersion(1);
        shortcutProp.setRetired(false);
        shortcutProp.setValueReference("false"); // Default to false
        globalPropertyRepository.save(shortcutProp);
    }

    public void updateLocalIpProperty(String localIp) {
        try {
            globalPropertyRepository.findByName(LOCAL_IP).ifPresentOrElse(p -> {
                p.setValueReference(localIp);
                globalPropertyRepository.save(p);
            }, () -> {
                GlobalProperty p = new GlobalProperty();
                p.setName(LOCAL_IP);
                p.setDatatype("java.lang.String");
                p.setDescription("The IP address of the WSL distribution for connecting to services like Ollama");
                p.setVersion(1);
                p.setRetired(false);
                p.setValueReference(localIp);
                globalPropertyRepository.save(p);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
