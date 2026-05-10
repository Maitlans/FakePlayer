package io.github.hello09x.fakeplayer.core.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.command.exception.CommandException;
import io.github.hello09x.devtools.core.utils.Exceptions;
import io.github.hello09x.devtools.core.utils.MetadataUtils;
import io.github.hello09x.fakeplayer.api.spi.ActionSetting;
import io.github.hello09x.fakeplayer.api.spi.ActionType;
import io.github.hello09x.fakeplayer.api.spi.NMSBridge;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.config.FakeplayerConfig;
import io.github.hello09x.fakeplayer.core.constant.MetadataKeys;
import io.github.hello09x.fakeplayer.core.entity.Fakeplayer;
import io.github.hello09x.fakeplayer.core.entity.SpawnOption;
import io.github.hello09x.fakeplayer.core.manager.feature.FakeplayerFeatureManager;
import io.github.hello09x.fakeplayer.core.manager.naming.NameManager;
import io.github.hello09x.fakeplayer.core.repository.model.Feature;
import io.github.hello09x.fakeplayer.core.util.AddressUtils;
import io.github.hello09x.fakeplayer.core.util.Commands;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Singleton
public class FakeplayerManager {

    public final static String REMOVAL_REASON_PREFIX = "[fakeplayer] ";

    private final static Logger log = Main.getInstance().getLogger();

    private final NameManager nameManager;
    private final FakeplayerList playerList;
    private final FakeplayerFeatureManager featureManager;
    private final NMSBridge nms;
    private final FakeplayerConfig config;
    private final ScheduledExecutorService lagMonitor;
    private int laglevel=0;

    @Inject
    public FakeplayerManager(NameManager nameManager, FakeplayerList playerList, FakeplayerFeatureManager featureManager, NMSBridge nms, FakeplayerConfig config) {
        this.nameManager = nameManager;
        this.playerList = playerList;
        this.featureManager = featureManager;
        this.nms = nms;
        this.config = config;

        this.lagMonitor = Executors.newSingleThreadScheduledExecutor();
        this.lagMonitor.scheduleWithFixedDelay(() -> {
                                                //Detects TPS performance from the past 1 minute only
                                                   if (Bukkit.getServer().getTPS()[0] < config.getKaleTps()) {
                                                       Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                                           laglevel=min(laglevel+1,this.config.getPlayerLimit());
                                                           Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
                                                           for (Player player : onlinePlayers) {
                                                               if(isFake(player))continue;
                                                               List<Player> fakeplayerlist= getAll(player);
                                                               if(fakeplayerlist.size()>this.config.getPlayerLimit()-laglevel){
                                                                   for (int i = fakeplayerlist.size() - 1; i >= this.config.getPlayerLimit()-laglevel; i--) {
                                                                       //Remove fakeplayers in reverse order of summoning
                                                                       remove(fakeplayerlist.get(i).getName(),"Server lag");
                                                                   }
                                                               }
                                                           }
                                                           //Lacking translation key for now
                                                           Bukkit.broadcast(Component.text("Server lag! Current fakeplayer limits: ").color(GOLD).append(Component.text(this.config.getPlayerLimit()-laglevel).color(RED)));
                                                       });
                                                   }
                                                   else {
                                                       //Restore fakeplayer limits, one at a time
                                                       if(laglevel>0)Bukkit.broadcast(Component.text("Fakeplayer restrictions removed! Current limits: ").color(GREEN).append(Component.text(this.config.getPlayerLimit()-laglevel+1).color(AQUA)));
                                                       laglevel=max(laglevel-1,0);
                                                   }
                                               }, 0, 60, TimeUnit.SECONDS
        );
    }

    /**
     *
     */
    public @NotNull CompletableFuture<Player> spawnAsync(
            @NotNull CommandSender creator,
            @Nullable String name,
            @NotNull Location spawnAt,
            long lifespan
    ) {
        this.checkLimit(creator);

        var sn = name == null ? nameManager.getRegularName(creator) : nameManager.getSpecifiedName(name);
        log.info("UUID of fake player %s is %s".formatted(sn.name(), sn.uuid()));

        var fp = new Fakeplayer(
                creator,
                AddressUtils.getAddress(creator),
                sn,
                lifespan
        );

        var target = fp.getPlayer();
        this.playerList.add(fp);

        this.dispatchCommandsEarly(fp, this.config.getPreSpawnCommands());
        return CompletableFuture
                .supplyAsync(() -> {
                    var configs = featureManager.getFeatures(creator);
                    return new SpawnOption(
                            spawnAt,
                            configs.get(Feature.invulnerable).asBoolean(),
                            configs.get(Feature.collidable).asBoolean(),
                            configs.get(Feature.look_at_entity).asBoolean(),
                            configs.get(Feature.pickup_items).asBoolean(),
                            configs.get(Feature.skin).asBoolean(),
                            configs.get(Feature.replenish).asBoolean(),
                            configs.get(Feature.autofish).asBoolean(),
                            configs.get(Feature.wolverine).asBoolean()
                    );
                })
                .thenComposeAsync(fp::spawnAsync)
                .thenApply(ignored -> target);
    }

    /**
     *
     */
    public @Nullable Player get(@NotNull CommandSender creator, @NotNull String name) {
        return Optional
                .ofNullable(this.playerList.getByName(name))
                .filter(p -> p.isCreatedBy(creator))
                .map(Fakeplayer::getPlayer)
                .orElse(null);
    }

    /**
     *
     */
    public @Nullable Player get(@NotNull String name) {
        return Optional
                .ofNullable(this.playerList.getByName(name))
                .map(Fakeplayer::getPlayer)
                .orElse(null);
    }

    /**
     *
     */
    public @Nullable String getCreatorName(@NotNull Player target) {
        return Optional
                .ofNullable(this.playerList.getByUUID(target.getUniqueId()))
                .map(Fakeplayer::getCreator)
                .map(CommandSender::getName)
                .orElse(null);
    }

    /**
     *
     */
    public @Nullable CommandSender getCreator(@NotNull Player target) {
        return Optional.ofNullable(this.playerList.getByUUID(target.getUniqueId()))
                       .map(Fakeplayer::getCreator)
                       .map(creator -> {
                           if (creator instanceof Player p) {
                               return Bukkit.getPlayer(p.getUniqueId());
                           } else {
                               return creator;
                           }
                       })
                       .orElse(null);
    }

    /**
     *
     */
    public boolean remove(@NotNull String name, @Nullable String reason) {
        return this.remove(name, reason == null ? null : text(reason));
    }

    /**
     *
     */
    public boolean remove(@NotNull String name, @Nullable Component reason) {
        var target = this.get(name);
        if (target == null) {
            return false;
        }

        target.kick(textOfChildren(
                text("[fakeplayer] "),
                reason == null ? text("removed") : reason
        ));
        return true;
    }

    /**
     *
     */
    public int removeAll(@Nullable String reason) {
        var targets = getAll();
        for (var target : targets) {
            target.kick(text(REMOVAL_REASON_PREFIX + (reason == null ? "removed" : reason)));
        }
        return targets.size();
    }

    /**
     */
    public @NotNull List<Player> getAll() {
        return this.getAll((Predicate<Player>) null);
    }

    /**
     */
    public @NotNull List<Player> getAll(@Nullable Predicate<Player> predicate) {
        var stream = this.playerList.getAll().stream().map(Fakeplayer::getPlayer);
        if (predicate != null) {
            stream = stream.filter(predicate);
        }
        return stream.toList();
    }

    /**
     *
     */
    public void cleanup(@NotNull Player target) {
        var fakeplayer = this.playerList.removeByUUID(target.getUniqueId());
        if (fakeplayer == null) {
            return;
        }
        this.nameManager.unregister(fakeplayer.getSequenceName());
        if (config.isDropInventoryOnQuiting()) {
            this.nms.createAction(
                    fakeplayer.getPlayer(),
                    ActionType.DROP_INVENTORY,
                    ActionSetting.once()
            ).tick();
        }
    }

    /**
     *
     */
    public @NotNull List<Player> getAll(@NotNull CommandSender creator) {
        return this.getAll(creator, null);
    }

    /**
     *
     */
    public @NotNull List<Player> getAll(@NotNull CommandSender creator, @Nullable Predicate<Player> predicate) {
        var stream = this.playerList.getByCreator(creator.getName()).stream().map(Fakeplayer::getPlayer);
        if (predicate != null) {
            stream = stream.filter(predicate);
        }
        return stream.toList();
    }

    public int getSize() {
        return this.playerList.getSize();
    }

    /**
     *
     */
    public boolean isFake(@NotNull Player target) {
        return this.playerList.getByUUID(target.getUniqueId()) != null;
    }

    /**
     *
     */
    public boolean isNotFake(@NotNull Player target) {
        return this.playerList.getByUUID(target.getUniqueId()) == null;
    }

    /**
     *
     */
    public long countByAddress(@NotNull String address) {
        return this.playerList
                .stream()
                .filter(p -> p.getCreatorIp().equals(address))
                .count();
    }

    /**
     *
     */
    public int countByCreator(@NotNull CommandSender creator) {
        return this.playerList.countByCreator(creator.getName());
    }

    /**
     *
     */
    public void setSelection(@NotNull Player creator, @Nullable Player target) {
        if (target == null) {
            creator.removeMetadata(MetadataKeys.SELECTION, Main.getInstance());
            return;
        }

        if (!this.isFake(target)) {
            return;
        }

        creator.setMetadata(MetadataKeys.SELECTION, new FixedMetadataValue(Main.getInstance(), target.getUniqueId()));
    }

    /**
     *
     */
    public @Nullable Player getSelection(@NotNull CommandSender creator) {
        if (!(creator instanceof Player p)) {
            return null;
        }
        if (!p.hasMetadata(MetadataKeys.SELECTION)) {
            return null;
        }

        var uuid = MetadataUtils
                .find(Main.getInstance(), p, MetadataKeys.SELECTION, UUID.class)
                .map(MetadataValue::value)
                .map(UUID.class::cast)
                .orElse(null);

        if (uuid == null) {
            return null;
        }

        var target = Optional.ofNullable(this.playerList.getByUUID(uuid)).map(Fakeplayer::getPlayer).orElse(null);
        if (target == null) {
            this.setSelection(p, null);
        }
        return target;
    }

    /**
     *
     */
    public void issueCommands(@NotNull Player target, @NotNull List<String> commands) {
        if (commands.isEmpty()) {
            return;
        }
        if (this.isNotFake(target)) {
            return;
        }

        var p = target.getName();
        var u = target.getUniqueId().toString();
        var c = Objects.requireNonNull(this.getCreatorName(target));
        for (var cmd : Commands.formatCommands(commands, "%p", p, "%u", u, "%c", c)) {
            if (!target.performCommand(cmd)) {
                log.warning(target.getName() + " failed to execute command: " + cmd);
            } else {
                log.info(target.getName() + " issued command: " + cmd);
            }
        }
    }

    public void dispatchCommandsEarly(@NotNull Fakeplayer fp, @NotNull List<String> commands) {
        if (commands.isEmpty()) {
            return;
        }

        var server = Bukkit.getServer();
        var sender = Bukkit.getConsoleSender();
        var p = fp.getName();
        var u = fp.getUUID().toString();
        var c = fp.getCreator().getName();
        for (var cmd : Commands.formatCommands(commands, "%p", p, "%u", u, "%c", c)) {
            if (!server.dispatchCommand(sender, cmd)) {
                log.warning("Failed to execute command for %s: ".formatted(p) + cmd);
            } else {
                log.info("Dispatched command: " + cmd);
            }
        }
    }

    /**
     *
     */
    public void dispatchCommands(@NotNull DispatchCommandArgs args, @NotNull List<String> commands) {
        if (commands.isEmpty()) {
            return;
        }

        var server = Bukkit.getServer();
        var sender = Bukkit.getConsoleSender();

        var p = args.fakeplayerName;
        var u = args.fakeplayerUUID;
        var c = args.creatorName;
        for (var cmd : Commands.formatCommands(commands, "%p", p, "%u", u, "%c", c)) {
            if (!server.dispatchCommand(sender, cmd)) {
                log.warning("Failed to execute command for %s: ".formatted(p) + cmd);
            } else {
                log.info("Dispatched command: " + cmd);
            }
        }
    }

    /**
     *
     */
    public void dispatchCommands(@NotNull Player player, @NotNull List<String> commands) {
        this.dispatchCommands(new DispatchCommandArgs(player.getName(),player.getUniqueId().toString(),Objects.requireNonNull(this.getCreatorName(player))),commands);
    }

    @AllArgsConstructor
    public static class DispatchCommandArgs {
        public String fakeplayerName , fakeplayerUUID, creatorName;
    }

    /**
     *
     */
    private void checkLimit(@NotNull CommandSender creator) throws CommandException {
        if (creator.isOp()) {
            return;
        }

        if (this.playerList.getSize() >= this.config.getServerLimit()) {
            throw new CommandException(translatable("fakeplayer.command.spawn.error.server-limit"));
        }

        //Apply dynamic limits to fakeplayers
        if (this.playerList.getByCreator(creator.getName()).size() >= this.config.getPlayerLimit()-laglevel) {
            throw new CommandException(translatable("fakeplayer.command.spawn.error.player-limit"));
        }

        if (this.config.isDetectIp() && this.countByAddress(AddressUtils.getAddress(creator)) >= this.config.getPlayerLimit()-laglevel) {
            throw new CommandException(translatable("fakeplayer.command.spawn.error.ip-limit"));
        }
    }

    public void onDisable() {
        Exceptions.suppress(Main.getInstance(), () -> this.removeAll("Plugin disabled"));
        Exceptions.suppress(Main.getInstance(), this.lagMonitor::shutdownNow);
    }

}
