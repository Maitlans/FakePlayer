package io.github.hello09x.fakeplayer.core.config;


import com.google.common.annotations.Beta;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.config.ConfigUtils;
import io.github.hello09x.devtools.core.config.PluginConfig;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.repository.model.Feature;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.translatable;
import static com.coderxi.plugin.utils.translation.MessageUtils.translatableWithPrefix;

@Getter
@ToString
@Singleton
public class FakeplayerConfig extends PluginConfig {

    private final static Logger log = Main.getInstance().getLogger();

    private final static String defaultNameChars = "^[a-zA-Z0-9_]+$";

    /**
     */
    private int playerLimit;

    /**
     */
    private int serverLimit;

    /**
     */
    private String nameTemplate;

    /**
     */
    private String namePrefix;

    /**
     */
    private NamedTextColor nameStyleColor;

    /**
     */
    private List<TextDecoration> nameStyleDecorations;

    /**
     */
    private boolean followQuiting;
    private boolean followQuitingForce;
    private Integer followQuitingForceDelay;

    /**
     */
    private boolean detectIp;

    /**
     */
    private int kaleTps;

    /**
     */
    private List<String> preSpawnCommands;

    /**
     */
    private List<String> postSpawnCommands;

    /**
     */
    private List<String> afterSpawnCommands;

    /**
     */
    private List<String> postQuitCommands;

    /**
     */
    private List<String> afterQuitCommands;

    /**
     */
    private List<String> selfCommands;

    /**
     */
    private boolean dropInventoryOnQuiting;

    /**
     */
    private boolean persistData;

    /**
     */
    private boolean kickOnDead;

    private boolean autoRespawnOnDeath;

    /**
     */
    private Pattern namePattern;

    /**
     */
    private boolean checkForUpdates;

    /**
     */
    @Deprecated
    private Set<String> allowCommands;

    /**
     */
    @Nullable
    private Duration lifespan;

    /**
     */
    private boolean debug;

    /**
     */
    private PreventKicking preventKicking;

    /**
     */
    private InvseeImplement invseeImplement;

    /**
     */
    @Beta
    private boolean defaultOnlineSkin;

    private StressConfig stress;

    private Map<Feature, String> defaultFeatures;

    @Inject
    public FakeplayerConfig() {
        super(Main.getInstance());
    }

    private static int maxIfZero(int value) {
        return value <= 0 ? Integer.MAX_VALUE : value;
    }

    @Override
    protected void reload(@NotNull FileConfiguration file) {
        this.playerLimit = maxIfZero(file.getInt("player-limit", 1));
        this.serverLimit = maxIfZero(file.getInt("server-limit", 1000));
        this.followQuiting = file.getBoolean("follow-quiting", true);
        this.followQuitingForce = file.getBoolean("follow-quiting-force", false);
        this.followQuitingForceDelay = file.getInt("follow-quiting-delay", 3);
        this.detectIp = file.getBoolean("detect-ip", false);
        this.kaleTps = file.getInt("kale-tps", 0);
        this.selfCommands = file.getStringList("self-commands");
        this.preSpawnCommands = file.getStringList("pre-spawn-commands");
        this.postSpawnCommands = file.getStringList("post-spawn-commands");
        this.afterSpawnCommands = file.getStringList("after-spawn-commands");
        this.postQuitCommands = file.getStringList("post-quit-commands");
        this.afterQuitCommands = file.getStringList("after-quit-commands");
        this.nameTemplate = file.getString("name-template", "");
        this.dropInventoryOnQuiting = file.getBoolean("drop-inventory-on-quiting", true);
        this.persistData = file.getBoolean("persist-data", true);
        this.kickOnDead = file.getBoolean("kick-on-dead", false);
        this.autoRespawnOnDeath = file.getBoolean("auto-respawn-on-death", true);
        this.checkForUpdates = file.getBoolean("check-for-updates", true);
        this.namePattern = getNamePattern(file);
        this.preventKicking = this.getPreventKicking(file);
        this.nameTemplate = getNameTemplate(file);
        this.namePrefix = file.getString("name-prefix", "");
        this.lifespan = getLifespan(file);
        this.allowCommands = file.getStringList("allow-commands")
                                 .stream()
                                 .map(c -> c.startsWith("/") ? c.substring(1) : c)
                                 .filter(c -> !c.isBlank())
                                 .collect(Collectors.toSet());

        this.defaultOnlineSkin = file.getBoolean("default-online-skin", false);
        this.stress = StressConfig.load(file.getConfigurationSection("stress"));
        this.defaultFeatures = Arrays.stream(Feature.values())
                                     .collect(Collectors.toMap(Function.identity(), key -> file.getString("default-features." + key.name(), key.getDefaultOption())));
        this.invseeImplement = ConfigUtils.getEnum(file, "invsee-implement", InvseeImplement.class, InvseeImplement.AUTO);
        this.debug = file.getBoolean("debug", false);
        this.nameStyleColor = this.getNameStyleColor(file);
        this.nameStyleDecorations = this.getNameStyleDecorations(file);

        if (this.isConfigFileOutOfDate()) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                if (Main.getInstance().isEnabled()) {
                    Main.getInstance().getComponentLogger().warn(translatable("fakeplayer.configuration.out-of-date"));
                }
            }, 1);
        }

        if (!this.allowCommands.isEmpty()) {
            log.warning("allow-commands is deprecated which will be removed at 0.4.0, you should use Permissions Plugin to assign permission groups to fake players.");
        }

        var preparingCommands = file.getStringList("preparing-commands");
        if (!preparingCommands.isEmpty()) {
            log.warning("preparing-commands is deprecated, use post-spawn-commands instead.");
            this.postSpawnCommands.addAll(preparingCommands);
        }

        var destroyCommands = file.getStringList("destroy-commands");
        if (!destroyCommands.isEmpty()) {
            log.warning("destroy-commands is deprecated, use post-quit-commands instead.");
            this.postQuitCommands.addAll(destroyCommands);
        }

    }

    private @Nullable Duration getLifespan(@NotNull FileConfiguration file) {
        var minutes = file.getLong("lifespan");
        if (minutes <= 0) {
            return null;
        }
        return Duration.ofMinutes(minutes);
    }


    private @NotNull Pattern getNamePattern(@NotNull FileConfiguration file) {
        try {
            return Pattern.compile(file.getString("name-pattern", defaultNameChars));
        } catch (PatternSyntaxException e) {
            log.warning("Invalid name-pattern: " + file.getString("name-chars"));
            return Pattern.compile(defaultNameChars);
        }
    }

    private @NotNull String getNameTemplate(@NotNull FileConfiguration file) {
        var tmpl = file.getString("name-template", "");
        if (tmpl.startsWith("-") || tmpl.startsWith("@")) {
            log.warning("Invalid name template: " + this.nameTemplate);
            return "";
        }
        return tmpl;
    }

    private @NotNull PreventKicking getPreventKicking(@NotNull FileConfiguration file) {
        if (file.getBoolean("prevent-kicked-on-spawning", false)) {
            log.warning("prevent-kicked-on-spawning is deprecated which will be removed at 0.4.0, use prevent-kick instead");
            return PreventKicking.ON_SPAWNING;
        }

        return ConfigUtils.getEnum(file, "prevent-kicking", PreventKicking.class, PreventKicking.ON_SPAWNING);
    }

    private @NotNull NamedTextColor getNameStyleColor(@NotNull FileConfiguration file) {
        var styles = Objects.requireNonNullElse(file.getString("name-style"), "").split(",\\s*");
        var color = NamedTextColor.WHITE;
        for (var style : styles) {
            var c = NamedTextColor.NAMES.value(style);
            if (c != null) {
                color = c;
            }
        }
        return color;
    }

    private @NotNull List<TextDecoration> getNameStyleDecorations(@NotNull FileConfiguration file) {
        var styles = Objects.requireNonNullElse(file.getString("name-style"), "").split(",\\s*");
        var decorations = new ArrayList<TextDecoration>();
        for (var style : styles) {
            var decoration = TextDecoration.NAMES.value(style);
            if (decoration != null) {
                decorations.add(decoration);
            }
        }
        return decorations;
    }

}
