package me.oblueberrey.meowMcEvents.utils;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LicenseManager {

    private final MeowMCEvents plugin;
    private boolean validated = false;
    private final HttpClient httpClient;

    // License API configuration (hardcoded for security)
    private static final String LICENSE_API_URL = "https://npafkatdrfchhoxnptfr.supabase.co/functions/v1/verify";
    private static final String LICENSE_API_KEY = "Ehahnrbjeiudbeit287$@";

    public LicenseManager(MeowMCEvents plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Validate the license key from config using a real API call
     * @return true if license is valid (immediate check of cached state)
     */
    public boolean validate() {
        String key = plugin.getConfig().getString("license-key", "");
        
        if (key == null || key.isEmpty() || key.equals("YOUR-LICENSE-HERE")) {
            this.validated = false;
            showFailureMessage("KEY_MISSING");
            return false;
        }

        plugin.getLogger().info("Verifying license with License Guardian Backend...");
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String ip = Bukkit.getIp().isEmpty() ? "127.0.0.1" : Bukkit.getIp();
                String jsonBody = String.format("{\"license_key\":\"%s\", \"server_ip\":\"%s\"}", key, ip);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(LICENSE_API_URL))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", LICENSE_API_KEY)
                        .header("User-Agent", "MeowMCEvents-Plugin")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (response.statusCode() == 200) {
                        // Assuming the API returns a JSON with success: true
                        if (response.body().contains("\"success\":true")) {
                            this.validated = true;
                            plugin.getLogger().info("License Status: ACTIVE");
                        } else {
                            this.validated = false;
                            showFailureMessage("INVALID_KEY");
                            plugin.getLogger().warning("API Response: " + response.body());
                        }
                    } else {
                        this.validated = false;
                        showFailureMessage("SERVER_ERROR (" + response.statusCode() + ")");
                        plugin.getLogger().warning("API Debug: " + response.body());
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    this.validated = false;
                    showFailureMessage("CONNECTION_FAILED: " + e.getMessage());
                });
            }
        });

        return validated;
    }

    private void showFailureMessage(String reason) {
        plugin.getLogger().severe("========================================");
        plugin.getLogger().severe("   LICENSE VERIFICATION FAILED!");
        plugin.getLogger().severe("   Plugin functionality is restricted.");
        plugin.getLogger().severe("   Reason: " + reason);
        plugin.getLogger().severe("========================================");
    }

    /**
     * Check if the plugin is currently validated
     */
    public boolean isValidated() {
        return validated;
    }

    /**
     * Check if plugin can run an event
     */
    public boolean canStartEvent() {
        if (!validated) {
            Bukkit.broadcast(ChatColor.RED + "[MeowEvents] Cannot start event: Invalid License!", "meowevents.admin");
            return false;
        }
        return true;
    }
}
