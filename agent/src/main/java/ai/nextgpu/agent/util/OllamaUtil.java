package ai.nextgpu.agent.util;

import ai.nextgpu.agent.aop.Loggable;
import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.common.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Encapsulates all Ollama connectivity concerns so the rest of the app doesn't have to:
 * <ul>
 *     <li>resolving the base URL ({@code localhost} primary, WSL-IP-substituted fallback);</li>
 *     <li>running a call against those candidates with connection-failure fallback
 *     ({@link #withFallback(Function)});</li>
 *     <li>recovering when Ollama is unreachable &mdash; restart the service, force a
 *     {@code 0.0.0.0}-bound {@code ollama serve}, refresh the WSL portproxy, and persist the WSL IP.</li>
 * </ul>
 *
 * <p>On Windows a {@code netsh} portproxy bridges {@code localhost:11434} to WSL and is refreshed on
 * every WSL start, so {@code localhost} is the reliable primary. The WSL-IP URL only covers the case
 * where that portproxy isn't in place (e.g. the app isn't elevated).</p>
 */
@Component
public class OllamaUtil {

    private static final Logger log = LoggerFactory.getLogger(OllamaUtil.class);

    /** Max time to wait for Ollama to answer after a force-start before giving up and letting the caller retry. */
    private static final long FORCE_START_READY_TIMEOUT_MS = 40_000;

    /** Delay between reachability polls while waiting for a freshly (re)started Ollama to come up. */
    private static final long READY_POLL_INTERVAL_MS = 2_000;

    private final GlobalPropertyRepository globalPropertyRepository;

    /** Default Ollama base URL (e.g. http://localhost:11434) as configured. */
    private final String defaultOllamaUrl;

    /** Short-timeout client used only by the reachability probe during recovery. */
    private final RestTemplate probeRestTemplate;

    /** Cached WSL-IP-substituted fallback URL. */
    private volatile String cachedWslIpUrl;

    /** Throttle so repeated connection failures don't trigger a recovery storm. */
    private long lastSelfRecoveryAt = 0;

    private static final int THROTTLE_MS = 15_000;

    public OllamaUtil(GlobalPropertyRepository globalPropertyRepository,
                      @Value("${ollama.api.url:http://localhost:11434}") String defaultOllamaUrl) {
        this.globalPropertyRepository = globalPropertyRepository;
        this.defaultOllamaUrl = defaultOllamaUrl;

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.probeRestTemplate = new RestTemplate(rf);
    }

    /**
     * The primary base URL (localhost) used to reach Ollama. Mainly for diagnostics and error messages.
     */
    public String getBaseUrl() {
        return defaultOllamaUrl;
    }

    /**
     * Runs an Ollama HTTP call against each candidate base URL in turn, falling back to the next
     * candidate only on connectivity failures ({@link ResourceAccessException}). Any other exception
     * (e.g. an HTTP error carrying a response) propagates immediately without a retry. When every
     * candidate fails to connect, a single throttled recovery is attempted and localhost retried once.
     *
     * @param call a function that performs the HTTP request given a base URL and returns its result
     * @param <T>  the result type of the call
     * @return the result of the first candidate that connects successfully
     */
    public <T> T withFallback(Function<String, T> call) {
        List<String> candidates = candidates();
        ResourceAccessException last = null;
        for (int i = 0; i < candidates.size(); i++) {
            String base = candidates.get(i);
            try {
                T result = call.apply(base);
                if (i > 0) {
                    log.info("Ollama reachable via fallback URL {} (primary URL was unreachable).", base);
                }
                return result;
            } catch (ResourceAccessException e) {
                last = e;
                boolean hasNext = i < candidates.size() - 1;
                log.warn("Ollama call failed against {}: {}.{}", base, e.getMessage(),
                        hasNext ? " Trying fallback URL." : "");
            }
        }
        // Every candidate failed to connect. On Windows, try to recover once (restart Ollama +
        // re-point localhost -> current WSL IP), then retry localhost before giving up.
        if (attemptSelfRecovery()) {
            try {
                T result = call.apply(defaultOllamaUrl);
                log.info("Ollama reachable via localhost after recovery.");
                return result;
            } catch (ResourceAccessException e) {
                last = e;
                log.warn("Ollama still unreachable via localhost after recovery: {}", e.getMessage());
            }
        }
        throw last;
    }

    /**
     * Ordered list of base URLs to attempt: {@code localhost} first (the portproxy path), then the
     * WSL-IP-substituted URL as a fallback.
     */
    private List<String> candidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add(defaultOllamaUrl);
        String wslIpUrl = resolveWslIpUrl();
        if (!wslIpUrl.equals(defaultOllamaUrl)) {
            candidates.add(wslIpUrl);
        }
        return candidates;
    }

    /**
     * Resolves the WSL-IP fallback base URL by substituting the current WSL IP (from the {@code WSL_IP}
     * global property) for {@code localhost}/{@code 127.0.0.1} when it is a valid IP address. Returns the
     * default localhost URL unchanged when no valid IP is stored. Cached to avoid a DB read on every call;
     * the cache is cleared by {@link #attemptSelfRecovery()} after the IP is refreshed.
     */
    private String resolveWslIpUrl() {
        String cached = cachedWslIpUrl;
        if (cached != null) {
            return cached;
        }
        String localIp;
        try {
            localIp = globalPropertyRepository.findByName(GlobalPropertyConfig.LOCAL_IP)
                    .map(GlobalProperty::getValueReference).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to read '{}' global property, using default Ollama URL '{}': {}",
                    GlobalPropertyConfig.LOCAL_IP, defaultOllamaUrl, e.getMessage());
            return defaultOllamaUrl;
        }
        if (localIp != null && !StringUtil.isValidIPAddress(localIp)) {
            log.warn("GlobalProperty '{}' holds an invalid IP address '{}'. Falling back to default Ollama URL '{}'.",
                    GlobalPropertyConfig.LOCAL_IP, localIp, defaultOllamaUrl);
            localIp = null;
        }
        String resolved = (localIp != null)
                ? defaultOllamaUrl.replaceAll("localhost|127\\.0\\.0\\.1", localIp)
                : defaultOllamaUrl;
        cachedWslIpUrl = resolved;
        return resolved;
    }

    /**
     * Last-resort recovery invoked when every candidate URL is unreachable. On Windows it, in order:
     * restarts the Ollama service in WSL to recover a dead or wedged service; re-points the netsh
     * portproxy ({@code localhost -> current WSL IP}); persists the freshly detected IP to the
     * {@code WSL_IP} property; and, if the service still isn't answering (e.g. it's bound to loopback via
     * a bad {@code OLLAMA_HOST} override that a restart just re-reads), force-starts a detached
     * {@code ollama serve} bound to {@code 0.0.0.0}. Throttled ({@link #THROTTLE_MS}) so a burst
     * of failures doesn't hammer WSL. No-op (returns {@code false}) off Windows or while throttled.
     *
     * @return {@code true} if a recovery was actually attempted
     */
    private boolean attemptSelfRecovery() {
        if (!OSUtil.IS_WINDOWS) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastSelfRecoveryAt < THROTTLE_MS) {
            return false;
        }
        lastSelfRecoveryAt = now;
        try {
            String distro = globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO)
                    .map(GlobalProperty::getValueReference).orElse("nextgpu");
            String user = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_USERNAME)
                    .map(GlobalProperty::getValueReference).orElse("nextgpu");
            String password = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_PASSWORD)
                    .map(GlobalProperty::getValueReference).orElse("");
            log.warn("Ollama unreachable on all candidate URLs; restarting Ollama service and refreshing WSL portproxy to recover.");
            restartOllamaService(distro, user, password);
            String wslIp = OSUtil.refreshWslPortProxy(distro, user, password);
            if (StringUtil.isValidIPAddress(wslIp)) {
                globalPropertyRepository.findByName(GlobalPropertyConfig.LOCAL_IP).ifPresent(p -> {
                    p.setValueReference(wslIp);
                    globalPropertyRepository.save(p);
                });
            }
            cachedWslIpUrl = null; // force re-resolve of the WSL-IP fallback from the fresh IP

            // If a plain service restart didn't restore reachability, fall back to the dirty approach:
            // stop the unit and launch a detached 'ollama serve' bound to 0.0.0.0.
            if (!isReachable(defaultOllamaUrl)) {
                log.warn("Ollama still unreachable after service restart; force-starting 'ollama serve' bound to 0.0.0.0.");
                forceStartOllamaBound(distro, user, password);
            }
            return true;
        } catch (Exception e) {
            log.warn("Ollama recovery failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Quick connectivity probe: returns true if Ollama answers {@code /api/tags} at the given base URL,
     * false on any error.
     */
    private boolean isReachable(String baseUrl) {
        try {
            probeRestTemplate.getForEntity(baseUrl + "/api/tags", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Restarts the Ollama systemd service inside WSL. Used to recovery when Ollama is unreachable
     * because the service has died or wedged (as opposed to a stale portproxy). Falls back to
     * {@code start} if the unit wasn't running.
     */
    @Loggable
    private void restartOllamaService(String distro, String username, String password) {
        if (!OSUtil.IS_WINDOWS) {
            return;
        }
        try {
            String cmd = "sudo systemctl restart ollama || sudo systemctl start ollama";
            String output = (password == null || password.isEmpty())
                    ? OSUtil.executeCommandInWsl(cmd, distro, username)
                    : OSUtil.executeCommandInWsl(cmd, distro, username, password);
            log.info("Requested Ollama service restart in WSL '{}'.{}", distro,
                    output.isBlank() ? "" : " Output: " + output.trim());
        } catch (Exception e) {
            log.warn("Failed to restart Ollama service in WSL '{}': {}", distro, e.getMessage());
        }
    }

    /**
     * Dirty last-resort recovery: stops the managed Ollama systemd unit and launches a detached
     * {@code ollama serve} explicitly bound to {@code 0.0.0.0}. This mirrors a manual
     * {@code OLLAMA_HOST=0.0.0.0 ollama serve} and is used when a normal service restart can't restore
     * reachability -- e.g. the unit is bound to loopback via a bad {@code OLLAMA_HOST} override that
     * {@code systemctl restart} just re-reads. The server is detached (setsid + nohup, streams redirected)
     * so it survives this one-shot WSL command as long as the distro stays up.
     */
    @Loggable
    private void forceStartOllamaBound(String distro, String username, String password) {
        if (!OSUtil.IS_WINDOWS) {
            return;
        }
        try {
            String cmd =
                    "sudo systemctl stop ollama 2>/dev/null; sudo service ollama stop 2>/dev/null; " +
                            "sudo pkill -x ollama 2>/dev/null; sleep 1; " +
                            "OLLAMA_BIN=\"$(command -v ollama || echo /usr/local/bin/ollama)\"; " +
                            "setsid env OLLAMA_HOST=0.0.0.0:" + OSUtil.OLLAMA_PORT + " nohup \"$OLLAMA_BIN\" serve " +
                            ">/tmp/nextgpu-ollama.log 2>&1 </dev/null &";
            String output = (password == null || password.isEmpty())
                    ? OSUtil.executeCommandInWsl(cmd, distro, username)
                    : OSUtil.executeCommandInWsl(cmd, distro, username, password);
            log.info("Force-started 'ollama serve' bound to 0.0.0.0:{} in WSL '{}'.{}", OSUtil.OLLAMA_PORT, distro,
                    output.isBlank() ? "" : " Output: " + output.trim());
            awaitReachable(defaultOllamaUrl);
        } catch (Exception e) {
            log.warn("Failed to force-start Ollama in WSL '{}': {}", distro, e.getMessage());
        }
    }

    /**
     * Polls {@link #isReachable(String)} until it succeeds or {@code timeoutMs} elapses. A fixed sleep after
     * launching {@code ollama serve} is unreliable: process fork + socket bind + (on the very first request)
     * model load into VRAM can each take anywhere from under a second to tens of seconds depending on the
     * machine, so a short fixed wait can let callers retry against a server that accepts the TCP connection but
     * then resets it because it isn't actually ready yet. Polling lets us return as soon as it's genuinely ready,
     * and wait longer than a fixed sleep would when the machine is slow.
     */
    private void awaitReachable(String baseUrl) {
        long deadline = System.currentTimeMillis() + OllamaUtil.FORCE_START_READY_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (isReachable(baseUrl)) {
                return;
            }
            try {
                Thread.sleep(OllamaUtil.READY_POLL_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Ollama at {} did not become reachable within {} ms after force-start.", baseUrl, OllamaUtil.FORCE_START_READY_TIMEOUT_MS);
    }
}