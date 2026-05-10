package io.github.hello09x.fakeplayer.core.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.ComponentUtils;
import io.github.hello09x.devtools.core.utils.Exceptions;
import io.github.hello09x.devtools.core.utils.MetadataUtils;
import io.github.hello09x.fakeplayer.api.spi.NMSBridge;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.config.FakeplayerConfig;
import io.github.hello09x.fakeplayer.core.constant.MetadataKeys;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import io.github.hello09x.fakeplayer.core.repository.FakeplayerProfileRepository;
import io.github.hello09x.fakeplayer.core.repository.UsedIdRepository;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.*;
import static com.coderxi.plugin.utils.translation.MessageUtils.translatableWithPrefix;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.UNDERLINED;

@Singleton
public class FakeplayerListener implements Listener {

    private final static Logger log = Main.getInstance().getLogger();

    private final FakeplayerManager manager;
    private final UsedIdRepository usedIdRepository;
    private final FakeplayerProfileRepository profileRepository;
    private final FakeplayerConfig config;
    private final NMSBridge bridge;

    @Inject
    public FakeplayerListener(FakeplayerManager manager, UsedIdRepository usedIdRepository, FakeplayerProfileRepository profileRepository, FakeplayerConfig config, NMSBridge bridge) {
        this.manager = manager;
        this.usedIdRepository = usedIdRepository;
        this.profileRepository = profileRepository;
        this.config = config;
        this.bridge = bridge;
    }

    /**
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void disallowUsedUUIDLogin(@NotNull PlayerLoginEvent event) {
        var player = event.getPlayer();
        if (player.hasMetadata(MetadataKeys.SPAWNED_AT)) {
            return;
        }

        if (usedIdRepository.contains(player.getUniqueId()) || profileRepository.existsByUUID(player.getUniqueId())) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, textOfChildren(
                    translatable("fakeplayer.listener.login.deny-used-uuid", RED),
                    newline(),
                    newline(),
                    text("<<---- fakeplayer ---->>", GRAY)
            ));
            log.info("%s(%s) was disallowed to login because his UUID was used by [Fakeplayer]".formatted(
                    player.getName(),
                    player.getUniqueId()
            ));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void preventKicking(@NotNull PlayerKickEvent event) {
        var player = event.getPlayer();

        if (manager.isNotFake(event.getPlayer())) {
            return;
        }

        if (ComponentUtils.toString(event.reason()).startsWith(FakeplayerManager.REMOVAL_REASON_PREFIX)) {
            return;
        }

        switch (config.getPreventKicking()) {
            case ON_SPAWNING -> {
                var spawnAt = MetadataUtils
                        .find(Main.getInstance(), player, MetadataKeys.SPAWNED_AT, Integer.class)
                        .map(MetadataValue::asInt)
                        .orElse(null);
                if (spawnAt != null && Bukkit.getCurrentTick() - spawnAt < 20) {
                    event.setCancelled(true);
                    log.warning(String.format(
                            "Canceled kicking fake player '%s' on spawning due to your configuration",
                            player.getName()
                    ));
                }
            }
            case ALWAYS -> {
                event.setCancelled(true);
                log.warning(String.format(
                        "Canceled kicking fake player '%s' due to your configuration",
                        player.getName()
                ));
            }
        }
    }

    /**
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void kickOrNotifyOnDead(@NotNull PlayerDeathEvent event) {
        var player = event.getPlayer();
        if (manager.isNotFake(player)) {
            return;
        }
        if (config.isAutoRespawnOnDeath()) {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (player.isOnline() && player.isDead()) {
                    bridge.fromPlayer(player).respawn();
                }
            });
            return;
        }
        if (!config.isKickOnDead()) {
            var creator = manager.getCreator(player);
            if (creator != null) {
                creator.sendMessage(translatableWithPrefix(
                        "fakeplayer.listener.death.notify",
                        text(player.getName(), GOLD),
                        text("/fp respawn", DARK_GREEN, UNDERLINED).clickEvent(runCommand("/fp respawn " + player.getName()))
                ).color(RED));
            }
            return;
        }

        Optional.ofNullable(player.getAttribute(Attribute.MAX_HEALTH))
                .map(AttributeInstance::getValue)
                .ifPresent(player::setHealth);
        event.setCancelled(true);
        manager.remove(event.getPlayer().getName(), event.deathMessage());
    }

    /**
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void cleanup(@NotNull PlayerQuitEvent event) {
        var target = event.getPlayer();
        if (manager.isNotFake(target)) {
            return;
        }

        try {
            if (manager.getCreator(target) instanceof Player creator && manager.countByCreator(creator) == 1) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), creator::updateCommands, 1);
            }
        } finally {
            manager.cleanup(target);
        }
    }

    @EventHandler
    public void onPluginDisable(@NotNull PluginDisableEvent event) {
        if (event.getPlugin() == Main.getInstance()) {
            Exceptions.suppress(Main.getInstance(), manager::onDisable);
            Exceptions.suppress(Main.getInstance(), usedIdRepository::onDisable);
        }
    }

}
