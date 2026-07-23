package ai.nextgpu.agent.ui.component.provider.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.exception.ApiException
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.service.NextGpuWebService
import ai.nextgpu.common.exception.ErrorCode
import ai.nextgpu.common.model.PpcAuditStatus
import ai.nextgpu.common.model.PpcRegistrationStatus
import com.fasterxml.jackson.core.JsonParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.function.Consumer

enum class ProviderSetupState {
    CHECKING_HARDWARE,
    INCOMPATIBLE_HARDWARE,
    AWAITING_OTP,
    READY_TO_APPLY,
    PROCESSING_APPLICATION,
    APPLICATION_FAILED,
    ACTIVE_PROVIDER
}

class ProviderViewModel(
    private val agentService: NextGpuAgentService,
    private val webService: NextGpuWebService
) {
    private val log = LoggerFactory.getLogger(ProviderViewModel::class.java)
    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    var uiState by mutableStateOf(ProviderSetupState.CHECKING_HARDWARE)
        private set
    var securityCode by mutableStateOf("")
        private set
    var statusMessage by mutableStateOf("")
        private set

    var auditStatus by mutableStateOf("UNAUDITED")
        private set
    var registrationStatus by mutableStateOf("UNREGISTERED")
        private set

    // New state to hold the progress value (0.0f to 1.0f)
    var auditProgress by mutableStateOf(0f)
        private set

    // Helper function to map text to a progress float
    private fun calculateProgress(message: String): Float {
        return when {
            message.contains("Initializing", ignoreCase = true) -> 0.1f
            message.contains("Detecting", ignoreCase = true) -> 0.3f
            message.contains("benchmark", ignoreCase = true) -> 0.6f
            message.contains("Auditing", ignoreCase = true) -> 0.9f
            else -> auditProgress // Keep current progress if message is unrecognized
        }
    }

    fun performColdHardwareCheck() {
        log.info("--- STARTING performColdHardwareCheck ---")
        uiState = ProviderSetupState.CHECKING_HARDWARE
        statusMessage = ""

        viewModelScope.launch {
            try {
                log.debug("Calling agentService.checkAndSaveNvidiaGpuPresence()...")
                val detected = agentService.checkAndSaveNvidiaGpuPresence()
                log.info("Hardware scan complete. GPU Detected: $detected")

                withContext(Dispatchers.Main) {
                    if (detected) {
                        log.info("Hardware is compatible. Handing off to refreshData() for routing.")
                        refreshData()
                    } else {
                        log.warn("Hardware incompatible. Routing to INCOMPATIBLE_HARDWARE.")
                        uiState = ProviderSetupState.INCOMPATIBLE_HARDWARE
                    }
                }
            } catch (e: Exception) {
                log.error("Hardware scan crashed with exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    uiState = ProviderSetupState.INCOMPATIBLE_HARDWARE
                }
            }
        }
    }

    fun refreshData() {
        log.info("--- STARTING refreshData ---")
        viewModelScope.launch(Dispatchers.IO) {

            // 1. Core Authentication & Identity Checks
            val isTokenValid = agentService.hasValidJwt()
            val loginWallet = agentService.getGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET)?.valueReference
            val computerUuid = agentService.getGlobalProperty(GlobalPropertyConfig.COMPUTER_UUID)?.valueReference

            log.info("Identity Check -> isTokenValid: $isTokenValid | loginWallet: ${loginWallet ?: "NULL"} | computerUuid: ${computerUuid ?: "NULL"}")

            // 2. Fetch Local Entities
            val provider = if (isTokenValid) agentService.localProvider else null
            val computer = if (isTokenValid) agentService.localComputer else null

            var localAuditStatus = "UNAUDITED"
            var localRegistrationStatus = PpcRegistrationStatus.UNREGISTERED.name

            // 3. Extract Attributes safely
            val hasMinimumStake = provider?.providerAttributes?.entries?.find {
                it.key.name.equals("minimum_stake_status", ignoreCase = true)
            }?.value?.toBoolean() ?: false

            if (computer?.computerAttributes != null) {

                // --- DIAGNOSTIC LOGGING ---
                log.info("Computer attributes found. Total count: ${computer.computerAttributes.size}")
                computer.computerAttributes.entries.forEach { entry ->
                    log.info("  -> Found Key: '${entry.key.name}' | Value: '${entry.value}'")
                }
                // --------------------------

                localAuditStatus = computer.computerAttributes.entries.find {
                    it.key.name.equals("last_audit_status", ignoreCase = true)
                }?.value ?: "UNAUDITED"

                localRegistrationStatus = computer.computerAttributes.entries.find {
                    it.key.name.equals("registration_status", ignoreCase = true)
                }?.value ?: PpcRegistrationStatus.UNREGISTERED.name

            } else {
                // Helps confirm if the attributes map itself is entirely null
                log.warn("Computer entity exists, but computerAttributes is null.")
            }

            log.info("Extracted States -> Minimum Stake: $hasMinimumStake | Audit: $localAuditStatus | Registration: $localRegistrationStatus")

            // 4. State Routing Logic (Pushed to Main Thread)
            withContext(Dispatchers.Main) {
                auditStatus = localAuditStatus
                registrationStatus = localRegistrationStatus

                when {
                    // Gateway 1: Unauthenticated or missing wallet
                    !isTokenValid || loginWallet.isNullOrBlank() -> {
                        log.info("Routing logic: No valid JWT or missing login wallet. Routing to AWAITING_OTP.")
                        uiState = ProviderSetupState.AWAITING_OTP
                    }

                    // Gateway 2: Authenticated, but insufficient stake
                    !hasMinimumStake -> {
                        log.warn("Routing logic: Provider does not meet minimum stake. Routing to AWAITING_OTP.")
                        uiState = ProviderSetupState.AWAITING_OTP
                        statusMessage = "Insufficient stake detected. Please ensure your wallet meets the minimum requirements."
                    }

                    // Gateway 3: Authenticated and Staked, but no computer registered at all
                    computerUuid.isNullOrBlank() -> {
                        log.info("Routing logic: No computer found. Normal flow routing to READY_TO_APPLY.")
                        uiState = ProviderSetupState.READY_TO_APPLY
                        statusMessage = "" // Clear any lingering errors
                    }

                    // Gateway 4: Computer exists, Audit Failed
                    localRegistrationStatus.equals(PpcRegistrationStatus.UNREGISTERED.name, ignoreCase = true) -> {
                        log.warn("Routing logic: Audit Failed. Routing to APPLICATION_FAILED.")
                        uiState = ProviderSetupState.APPLICATION_FAILED
                        statusMessage = "" // Let the UI default handle the message, or inject a specific one here
                    }

                    // Gateway 5: Computer exists, Audit Successful AND Registered
                    localRegistrationStatus.equals(PpcRegistrationStatus.REGISTERED.name, ignoreCase = true) -> {
                        log.info("Routing logic: Audit Successful and Registered. Routing to ACTIVE_PROVIDER.")
                        uiState = ProviderSetupState.ACTIVE_PROVIDER
                    }

                    // Gateway 6: The Fallback (Audit passed but unregistered, or unaudited)
                    else -> {
                        log.info("Routing logic: Incomplete registration or unaudited state. Routing to READY_TO_APPLY.")
                        uiState = ProviderSetupState.READY_TO_APPLY
                        statusMessage = ""
                    }
                }

                log.info("--- END refreshData. Final UI State: $uiState ---")
            }
        }
    }

    fun updateSecurityCode(code: String) {
        // Only logging the length to prevent saving plain-text OTPs in system logs
        log.debug("Security code updated. Current length: ${code.length}")
        securityCode = code
        statusMessage = ""
    }

    fun submitOtp() {
        log.info("--- STARTING submitOtp ---")
        if (securityCode.isBlank()) {
            log.warn("submitOtp called but security code is blank. Aborting.")
            return
        }

        uiState = ProviderSetupState.CHECKING_HARDWARE
        statusMessage = ""

        viewModelScope.launch {
            try {
                log.debug("Calling agentService.authenticate()...")
                val authSuccessful = agentService.authenticate(securityCode)
                log.info("Authentication result: $authSuccessful")

                if (authSuccessful) {
                    log.info("Auth successful. Triggering refreshData() to hydrate state.")
                    refreshData()
                } else {
                    log.warn("Auth failed via boolean return. Routing back to AWAITING_OTP.")
                    withContext(Dispatchers.Main) {
                        uiState = ProviderSetupState.AWAITING_OTP
                        statusMessage = "Access denied. Node onboarding requires a validated PROVIDER account profile."
                    }
                }
            } catch (e: JsonParseException) {
                log.error("Backend validation failed, unable to parse response JSON. (Bad OTP?)", e)
                withContext(Dispatchers.Main) {
                    uiState = ProviderSetupState.AWAITING_OTP
                    statusMessage = "Invalid security code. Please check your dashboard and try again."
                }
            } catch (e: Exception) {
                log.error("OTP Verification crashed", e)

                withContext(Dispatchers.Main) {
                    uiState = ProviderSetupState.AWAITING_OTP
                    // Show a clean, generalized fallback message to the USER
                    statusMessage = "Authentication failed. Please check your connection or try again later."
                }
            }
        }
    }

    /**
     * Walks the exception's cause chain looking for an ApiException.
     * Returns null if none is found. Guards against cyclic cause chains.
     */
    private fun findApiException(throwable: Throwable, maxDepth: Int = 10): ApiException? {
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < maxDepth) {
            if (current is ApiException) return current
            current = current.cause
            depth++
        }
        return null
    }

    fun executeRegistrationPipeline() {
        log.info("--- STARTING executeRegistrationPipeline ---")
        uiState = ProviderSetupState.PROCESSING_APPLICATION
        statusMessage = "Initializing provider application pipeline"
        auditProgress = 0.1f
        viewModelScope.launch {
            try {
                log.debug("Calling agentService.applyAsAProvider()...")

                val auditPassed = agentService.applyAsAProvider { update ->
                    log.debug("Audit status received: {}", update)

                    // Dispatch to Main thread to safely update the Compose state
                    viewModelScope.launch(Dispatchers.Main) {
                        statusMessage = update
                        auditProgress = calculateProgress(update)
                    }
                }

                log.info("Application complete. Audit Passed: $auditPassed")

                // Handle the clean boolean outcome
                withContext(Dispatchers.Main) {
                    if (auditPassed) {
                        log.info("Audit successful. Triggering refreshData() to finalize routing.")
                        auditProgress = 1.0f
                        refreshData()
                    } else {
                        log.warn("Audit completed but hardware failed eligibility. Routing to APPLICATION_FAILED.")
                        uiState = ProviderSetupState.APPLICATION_FAILED
                        statusMessage = "Hardware audit completed, but your system does not meet the minimum requirements."
                    }
                }

            } catch (e: Exception) {
                val apiException = findApiException(e)
                var errorMessage: String
                var errorCode: String? = null

                if (apiException != null) {
                    errorMessage = apiException.errorResponse.message
                    errorCode = apiException.errorResponse.errorCode.name
                    log.error("Provider registration pipeline crashed: $errorMessage with error code $errorCode")

                } else {
                    errorMessage = e.message ?: e.localizedMessage ?: ""
                    log.error("Provider registration pipeline crashed: $errorMessage", e)
                }

                if (errorMessage.contains("Exception:")) {
                    errorMessage = errorMessage.substringAfterLast("Exception:").trim()
                }

                withContext(Dispatchers.Main) {
                    // Auth Failure Check
                    if (errorMessage.contains("not logged-in", ignoreCase = true) ||
                        errorMessage.contains("401", ignoreCase = true)) {

                        log.warn("Auth failed inferred from exception. Routing to AWAITING_OTP.")
                        uiState = ProviderSetupState.AWAITING_OTP
                        statusMessage = "Session expired or invalid. Please reconnect."

                        // General Network / Server Error Check
                    } else if(errorCode != null && errorCode.equals(ErrorCode.AUDIT_FAILED.name, ignoreCase = true)) {
                        log.warn("Audit completed but hardware failed eligibility. Routing to APPLICATION_FAILED.")
                        uiState = ProviderSetupState.APPLICATION_FAILED
                        statusMessage = "Hardware audit completed, but your system does not meet the minimum requirements."
                    }
                    else {
                        log.error("Network or server error inferred. Staying on READY_TO_APPLY.")
                        uiState = ProviderSetupState.READY_TO_APPLY

                        // Surface a generalized error text to the Apply screen
                        statusMessage = errorMessage.ifBlank {
                            "Something went wrong. Please check your connection and try again."
                        }
                    }
                }
            }
        }
    }
}