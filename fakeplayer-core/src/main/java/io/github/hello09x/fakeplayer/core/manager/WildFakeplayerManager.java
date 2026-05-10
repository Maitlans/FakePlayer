package io.github.hello09x.fakeplayer.core.manager;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.config.FakeplayerConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class WildFakeplayerManager implements PluginMessageListener, Listener {

    private final static Logger log = Main.getInstance().getLogger();
    private final static boolean IS_BUNGEECORD = Bukkit
            .getServer()
            .spigot()
            .getSpigotConfig()
            .getBoolean("settings.bungeecord", false);
    private final static String CHANNEL = "BungeeCord";
    private final static String SUB_CHANNEL = "PlayerList";

    /**
     * <br>
     */
    private final static int CLEANUP_THRESHOLD = 2;
    private final static int CLEANUP_PERIOD = 6000;

    private final FakeplayerManager manager;
    private final FakeplayerConfig config;
    private final Map<String, AtomicInteger> offline = new HashMap<>();

    @Inject
    public WildFakeplayerManager(FakeplayerManager manager, FakeplayerConfig config) {
        this.manager = manager;
        this.config = config;
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::cleanup, 0, CLEANUP_PERIOD);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void handleFollowQuitingForce(PlayerQuitEvent event) {
        if (!config.isFollowQuiting() || !config.isFollowQuitingForce()) return;
        Bukkit.getScheduler().runTaskLater(Main.getInstance(),()->{
            if (Bukkit.getPlayer(event.getPlayer().getUniqueId()) != null) return;
            List<Player> targets = manager.getAll(event.getPlayer());
            if(targets.isEmpty()) return;
            for (var target : targets) {
                manager.remove(target.getName(), "Creator offline");
            }
        },config.getFollowQuitingForceDelay()*20);
    }

    @Override
    public void onPluginMessageReceived(
            @NotNull String channel,
            @NotNull Player player,
            byte @NotNull [] message
    ) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        @SuppressWarnings("UnstableApiUsage")
        var in = ByteStreams.newDataInput(message);
        if (!in.readUTF().equals(SUB_CHANNEL)) {
            return;
        }

        if (!in.readUTF().equals("ALL")) {
            return;
        }

        var players = new HashSet<String>();
        players.addAll(Arrays.asList(in.readUTF().split(", ")));
        players.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        this.cleanup0(players);
    }

    /**
     *
     */
    public void cleanup0(@NotNull Set<String> online) {
        @SuppressWarnings("all")
        var group = manager.getAll()
                           .stream()
                           .collect(Collectors.groupingBy(manager::getCreatorName));

        for (var entry : group.entrySet()) {
            var creator = entry.getKey();
            if (creator.equals("CONSOLE")) {
                continue;
            }

            var targets = entry.getValue();
            if (targets.isEmpty() || online.contains(creator)) {
                continue;
            }

            if (offline.computeIfAbsent(creator, x -> new AtomicInteger()).incrementAndGet() < CLEANUP_THRESHOLD) {
                continue;
            }

            for (var target : targets) {
                manager.remove(target.getName(), "Creator offline");
            }
            log.info("%s is offline more than %d ticks, removing %d fake players".formatted(
                    creator,
                    CLEANUP_PERIOD * CLEANUP_THRESHOLD,
                    targets.size())
            );
        }

        for (var player : online) {
            offline.remove(player);
        }
    }

    /**
     */
    public void cleanup() {
        if (!config.isFollowQuiting()) {
            return;
        }

        if (!IS_BUNGEECORD) {
            this.cleanup0(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toSet()));
            return;
        }

        var recipient = Bukkit
                .getServer()
                .getOnlinePlayers()
                .stream()
                .filter(manager::isNotFake)
                .findAny()
                .orElse(null);

        if (recipient == null) {
            return;
        }

        @SuppressWarnings("UnstableApiUsage")
        var out = ByteStreams.newDataOutput();
        out.writeUTF(SUB_CHANNEL);
        out.writeUTF("ALL");
        recipient.sendPluginMessage(
                Main.getInstance(),
                CHANNEL,
                out.toByteArray()
        );
    }

}
