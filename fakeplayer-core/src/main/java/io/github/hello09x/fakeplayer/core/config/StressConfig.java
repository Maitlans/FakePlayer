package io.github.hello09x.fakeplayer.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StressConfig {

    public enum SpreadYMode {
        SAME_Y,
        SURFACE
    }

    @NotNull
    private final String defaultProfile;

    @NotNull
    private final Map<String, StressProfile> profiles;

    public StressConfig(@NotNull String defaultProfile, @NotNull Map<String, StressProfile> profiles) {
        this.defaultProfile = defaultProfile;
        this.profiles = Map.copyOf(profiles);
    }

    public static @NotNull StressConfig load(@Nullable ConfigurationSection section) {
        var defaultProfile = section == null ? "default" : section.getString("default-profile", "default");
        var profiles = new HashMap<String, StressProfile>();

        if (section != null) {
            var profilesSection = section.getConfigurationSection("profiles");
            if (profilesSection != null) {
                for (var key : profilesSection.getKeys(false)) {
                    profiles.put(key.toLowerCase(Locale.ROOT), StressProfile.load(profilesSection.getConfigurationSection(key)));
                }
            }
        }

        profiles.putIfAbsent("default", StressProfile.load(null));
        profiles.putIfAbsent(defaultProfile.toLowerCase(Locale.ROOT), StressProfile.load(null));
        return new StressConfig(defaultProfile, profiles);
    }

    public @NotNull StressProfile getProfile(@Nullable String name) {
        var key = name == null || name.isBlank() ? defaultProfile : name;
        return profiles.getOrDefault(key.toLowerCase(Locale.ROOT), profiles.get("default"));
    }

    public @NotNull String getDefaultProfile() {
        return defaultProfile;
    }

    public @NotNull Set<String> getProfileNames() {
        return profiles.keySet();
    }

    public record StressProfile(
            @NotNull Spawn spawn,
            @NotNull Spread spread,
            @NotNull RandomWalk randomWalk,
            @NotNull Mirror mirror
    ) {
        public static @NotNull StressProfile load(@Nullable ConfigurationSection section) {
            return new StressProfile(
                    Spawn.load(child(section, "spawn")),
                    Spread.load(child(section, "spread")),
                    RandomWalk.load(child(section, "random-walk")),
                    Mirror.load(child(section, "mirror"))
            );
        }
    }

    public record Spawn(int batchSize, int batchIntervalTicks) {
        public static @NotNull Spawn load(@Nullable ConfigurationSection section) {
            return new Spawn(
                    Math.max(1, getInt(section, "batch-size", 5)),
                    Math.max(1, getInt(section, "batch-interval-ticks", 1))
            );
        }
    }

    public record Spread(@NotNull SpreadYMode yMode) {
        public static @NotNull Spread load(@Nullable ConfigurationSection section) {
            return new Spread(getEnum(section, "y-mode", SpreadYMode.SAME_Y));
        }
    }

    public record RandomWalk(
            int tickInterval,
            int directionChangeMinTicks,
            int directionChangeMaxTicks,
            double speed,
            double sprintChance,
            double sneakChance,
            double jumpChance,
            double useChance,
            double attackChance,
            int useActionCooldownTicks,
            int attackActionCooldownTicks,
            int maxUseActionsPerTick,
            int maxAttackActionsPerTick,
            double hotbarChangeChance,
            double swapOffhandChance
    ) {
        public static @NotNull RandomWalk load(@Nullable ConfigurationSection section) {
            var min = Math.max(1, getInt(section, "direction-change-min-ticks", 20));
            var max = Math.max(min, getInt(section, "direction-change-max-ticks", 80));
            return new RandomWalk(
                    Math.max(1, getInt(section, "tick-interval", 5)),
                    min,
                    max,
                    clamp(getDouble(section, "speed", 1.0), 0.0, 1.0),
                    chance(section, "sprint-chance", 0.25),
                    chance(section, "sneak-chance", 0.05),
                    chance(section, "jump-chance", 0.03),
                    chance(section, "use-chance", 0.02),
                    chance(section, "attack-chance", 0.02),
                    Math.max(0, getInt(section, "use-action-cooldown-ticks", 20)),
                    Math.max(0, getInt(section, "attack-action-cooldown-ticks", 20)),
                    Math.max(0, getInt(section, "max-use-actions-per-tick", 10)),
                    Math.max(0, getInt(section, "max-attack-actions-per-tick", 10)),
                    chance(section, "hotbar-change-chance", 0.05),
                    chance(section, "swap-offhand-chance", 0.01)
            );
        }
    }

    public record Mirror(
            boolean movement,
            boolean look,
            boolean actions,
            boolean commands,
            boolean commandsAsOp,
            int inputSyncWindowTicks,
            boolean debugInputs,
            @NotNull List<String> commandIgnorePrefixes
    ) {
        public static @NotNull Mirror load(@Nullable ConfigurationSection section) {
            var prefixes = section == null ? List.<String>of() : section.getStringList("command-ignore-prefixes");
            if (prefixes.isEmpty()) {
                prefixes = List.of("/fp", "/fakeplayer");
            }
            return new Mirror(
                    getBoolean(section, "movement", true),
                    getBoolean(section, "look", true),
                    getBoolean(section, "actions", true),
                    getBoolean(section, "commands", true),
                    getBoolean(section, "commands-as-op", true),
                    Math.max(0, getInt(section, "input-sync-window-ticks", 100)),
                    getBoolean(section, "debug-inputs", false),
                    prefixes
            );
        }
    }

    private static @Nullable ConfigurationSection child(@Nullable ConfigurationSection section, @NotNull String path) {
        return section == null ? null : section.getConfigurationSection(path);
    }

    private static int getInt(@Nullable ConfigurationSection section, @NotNull String path, int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }

    private static double getDouble(@Nullable ConfigurationSection section, @NotNull String path, double fallback) {
        return section == null ? fallback : section.getDouble(path, fallback);
    }

    private static boolean getBoolean(@Nullable ConfigurationSection section, @NotNull String path, boolean fallback) {
        return section == null ? fallback : section.getBoolean(path, fallback);
    }

    private static double chance(@Nullable ConfigurationSection section, @NotNull String path, double fallback) {
        return clamp(getDouble(section, path, fallback), 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static <T extends Enum<T>> @NotNull T getEnum(@Nullable ConfigurationSection section, @NotNull String path, @NotNull T fallback) {
        if (section == null) {
            return fallback;
        }

        var raw = section.getString(path, fallback.name());
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
