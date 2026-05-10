package io.github.hello09x.fakeplayer.core.manager;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.fakeplayer.api.spi.ActionSetting;
import io.github.hello09x.fakeplayer.api.spi.ActionType;
import io.github.hello09x.fakeplayer.api.spi.NMSBridge;
import io.github.hello09x.fakeplayer.api.spi.NMSServerPlayer;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.config.FakeplayerConfig;
import io.github.hello09x.fakeplayer.core.config.StressConfig;
import io.github.hello09x.fakeplayer.core.entity.FakeplayerTicker;
import io.github.hello09x.fakeplayer.core.manager.action.ActionManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;

@Singleton
public class StressManager implements Listener {

    private static final Logger log = Main.getInstance().getLogger();

    private final FakeplayerManager fakeplayerManager;
    private final FakeplayerConfig config;
    private final ActionManager actionManager;
    private final NMSBridge bridge;
    private final Random random = new Random();
    private final Map<UUID, Integer> nextRandomWalkTurn = new HashMap<>();
    private final Map<UUID, Integer> nextStressUseAction = new HashMap<>();
    private final Map<UUID, Integer> nextStressAttackAction = new HashMap<>();
    private final Set<UUID> stressUseActions = new HashSet<>();
    private final Set<UUID> stressAttackActions = new HashSet<>();
    private final Map<UUID, MirrorSession> mirrorSessions = new HashMap<>();
    private final Map<UUID, Integer> mirrorInputSyncUntil = new HashMap<>();

    private @Nullable BukkitTask randomWalkTask;
    private @Nullable String randomWalkProfileName;

    @Inject
    public StressManager(
            @NotNull FakeplayerManager fakeplayerManager,
            @NotNull FakeplayerConfig config,
            @NotNull ActionManager actionManager,
            @NotNull NMSBridge bridge
    ) {
        this.fakeplayerManager = fakeplayerManager;
        this.config = config;
        this.actionManager = actionManager;
        this.bridge = bridge;
    }

    public void spawn(@NotNull Player sender, int amount, @NotNull StressConfig.StressProfile profile) {
        var spawnAt = sender.getLocation();
        var lifespan = Optional.ofNullable(config.getLifespan()).map(Duration::toMillis).orElse(FakeplayerTicker.NON_REMOVE_AT);
        new BukkitRunnable() {
            private int spawned;

            @Override
            public void run() {
                var batch = Math.min(profile.spawn().batchSize(), amount - spawned);
                for (int i = 0; i < batch; i++) {
                    fakeplayerManager.spawnAsync(sender, null, spawnAt, lifespan)
                            .exceptionally(e -> {
                                log.warning("Failed to spawn stress fakeplayer: " + Throwables.getRootCause(e).getMessage());
                                return null;
                            });
                    spawned++;
                }

                if (spawned >= amount) {
                    sender.sendMessage(text("Queued " + amount + " stress fakeplayers.", NamedTextColor.GRAY));
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, profile.spawn().batchIntervalTicks());
    }

    public int spread(@NotNull Player sender, double radius, @NotNull StressConfig.StressProfile profile) {
        var center = sender.getLocation();
        var world = center.getWorld();
        if (world == null) {
            return 0;
        }

        var count = 0;
        for (var fake : fakeplayers()) {
            var angle = random.nextDouble() * Math.PI * 2;
            var distance = Math.sqrt(random.nextDouble()) * radius;
            var x = center.getX() + Math.cos(angle) * distance;
            var z = center.getZ() + Math.sin(angle) * distance;
            var y = switch (profile.spread().yMode()) {
                case SAME_Y -> center.getY();
                case SURFACE -> world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;
            };

            fake.teleport(new Location(world, x, y, z, fake.getLocation().getYaw(), fake.getLocation().getPitch()));
            count++;
        }
        return count;
    }

    public int startRandomWalk(@NotNull String profileName, @NotNull StressConfig.StressProfile profile) {
        stopRandomWalk();
        nextRandomWalkTurn.clear();
        nextStressUseAction.clear();
        nextStressAttackAction.clear();
        randomWalkProfileName = profileName;
        randomWalkTask = Bukkit.getScheduler().runTaskTimer(
                Main.getInstance(),
                () -> tickRandomWalk(profile.randomWalk()),
                0,
                profile.randomWalk().tickInterval()
        );
        return fakeplayers().size();
    }

    public int stopRandomWalk() {
        if (randomWalkTask != null) {
            randomWalkTask.cancel();
            randomWalkTask = null;
        }
        nextRandomWalkTurn.clear();
        nextStressUseAction.clear();
        nextStressAttackAction.clear();
        randomWalkProfileName = null;
        stopStressActions();
        return stopMovement();
    }

    public void startMirror(@NotNull Player controller, @NotNull String profileName, @NotNull StressConfig.StressProfile profile) {
        mirrorSessions.put(controller.getUniqueId(), new MirrorSession(controller.getUniqueId(), profileName, profile));
    }

    public int stopMirror() {
        var size = mirrorSessions.size();
        mirrorSessions.clear();
        mirrorInputSyncUntil.clear();
        stopMovement();
        return size;
    }

    public int stopAll() {
        stopRandomWalk();
        var count = stopMirror();
        stopStressActions();
        return count;
    }

    public int action(@NotNull String action) {
        var count = 0;
        for (var fake : fakeplayers()) {
            switch (action.toLowerCase(Locale.ROOT)) {
                case "use" -> startStressAction(fake, ActionType.USE);
                case "attack" -> startStressAction(fake, ActionType.ATTACK);
                case "jump" -> actionManager.setAction(fake, ActionType.JUMP, ActionSetting.once());
                case "swap" -> bridge.fromPlayer(fake).swapItemWithOffhand();
                default -> {
                    continue;
                }
            }
            count++;
        }
        return count;
    }

    public @NotNull StressStatus status() {
        return new StressStatus(
                fakeplayers().size(),
                randomWalkTask != null,
                randomWalkProfileName,
                mirrorSessions.size(),
                mirrorSessions.values().stream()
                        .map(MirrorSession::profileName)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", ")),
                actionManager.countActiveAction(ActionType.USE),
                actionManager.countActiveAction(ActionType.ATTACK),
                countTrackedActive(stressUseActions, ActionType.USE),
                countTrackedActive(stressAttackActions, ActionType.ATTACK)
        );
    }

    public int hotbarSlot(int slot) {
        var count = 0;
        for (var fake : fakeplayers()) {
            bridge.fromPlayer(fake).setCarriedItem(slot - 1);
            count++;
        }
        return count;
    }

    public int hotbarRandom() {
        var count = 0;
        for (var fake : fakeplayers()) {
            bridge.fromPlayer(fake).setCarriedItem(random.nextInt(9));
            count++;
        }
        return count;
    }

    public int hotbarCycle() {
        var count = 0;
        for (var fake : fakeplayers()) {
            bridge.fromPlayer(fake).setCarriedItem((fake.getInventory().getHeldItemSlot() + 1) % 9);
            count++;
        }
        return count;
    }

    @EventHandler(ignoreCancelled = true)
    public void mirrorMovement(@NotNull PlayerMoveEvent event) {
        var session = mirrorSessions.get(event.getPlayer().getUniqueId());
        if (session == null || event.getTo() == null) {
            return;
        }

        var profile = session.profile().mirror();
        if (!profile.look() && !profile.movement()) {
            return;
        }

        for (var fake : fakeplayers()) {
            var handle = bridge.fromPlayer(fake);
            if (profile.look()) {
                handle.setYRot(event.getTo().getYaw());
                handle.setXRot(event.getTo().getPitch());
            }
            if (profile.movement()) {
                applyMirroredMovement(event.getPlayer(), fake, event.getFrom(), event.getTo());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void mirrorSneak(@NotNull PlayerToggleSneakEvent event) {
        var session = mirrorSessions.get(event.getPlayer().getUniqueId());
        if (session == null || !session.profile().mirror().movement()) {
            return;
        }
        fakeplayers().forEach(fake -> fake.setSneaking(event.isSneaking()));
    }

    @EventHandler(ignoreCancelled = true)
    public void mirrorSprint(@NotNull PlayerToggleSprintEvent event) {
        var session = mirrorSessions.get(event.getPlayer().getUniqueId());
        if (session == null || !session.profile().mirror().movement()) {
            return;
        }
        fakeplayers().forEach(fake -> fake.setSprinting(event.isSprinting()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mirrorHotbar(@NotNull PlayerItemHeldEvent event) {
        var session = mirrorSessions.get(event.getPlayer().getUniqueId());
        var mirror = session == null ? null : session.profile().mirror();
        if (mirror == null || !mirror.actions()) {
            return;
        }
        var fakes = fakeplayers();
        fakes.forEach(fake -> bridge.fromPlayer(fake).setCarriedItem(event.getNewSlot()));
        debugMirrorInput(mirror, "hotbar", event.getPlayer(), fakes.size(), event.getNewSlot(), event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mirrorSwap(@NotNull PlayerSwapHandItemsEvent event) {
        var session = mirrorSessions.get(event.getPlayer().getUniqueId());
        var mirror = session == null ? null : session.profile().mirror();
        if (mirror == null || !mirror.actions()) {
            return;
        }
        activateInputSyncWindow(event.getPlayer(), mirror);
        var fakes = fakeplayers();
        fakes.forEach(fake -> {
            bridge.fromPlayer(fake).swapItemWithOffhand();
            bridge.fromPlayer(fake).setCarriedItem(event.getPlayer().getInventory().getHeldItemSlot());
        });
        debugMirrorInput(mirror, "swap", event.getPlayer(), fakes.size(), event.getPlayer().getInventory().getHeldItemSlot(), event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mirrorInteract(@NotNull PlayerInteractEvent event) {
        var session = mirrorSessions.get(event.getPlayer().getUniqueId());
        var mirror = session == null ? null : session.profile().mirror();
        if (mirror == null || !mirror.actions()) {
            return;
        }

        syncCurrentHotbarIfInWindow(event.getPlayer(), mirror);
        debugMirrorInput(mirror, "interact-" + event.getAction().name().toLowerCase(Locale.ROOT), event.getPlayer(), fakeplayers().size(), event.getPlayer().getInventory().getHeldItemSlot(), event.isCancelled());
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            action("use");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mirrorAnimation(@NotNull PlayerAnimationEvent event) {
        var session = mirrorSessions.get(event.getPlayer().getUniqueId());
        var mirror = session == null ? null : session.profile().mirror();
        if (mirror == null || !mirror.actions() || event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        syncCurrentHotbarIfInWindow(event.getPlayer(), mirror);
        var fakes = fakeplayers();
        fakes.forEach(fake -> bridge.fromPlayer(fake).swingMainHand());
        debugMirrorInput(mirror, "arm-swing", event.getPlayer(), fakes.size(), event.getPlayer().getInventory().getHeldItemSlot(), event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mirrorAttack(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        var session = mirrorSessions.get(player.getUniqueId());
        var mirror = session == null ? null : session.profile().mirror();
        if (mirror == null || !mirror.actions()) {
            return;
        }
        syncCurrentHotbarIfInWindow(player, mirror);
        debugMirrorInput(mirror, "attack", player, fakeplayers().size(), player.getInventory().getHeldItemSlot(), event.isCancelled());
        action("attack");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mirrorCommand(@NotNull PlayerCommandPreprocessEvent event) {
        var session = mirrorSessions.get(event.getPlayer().getUniqueId());
        var mirror = session == null ? null : session.profile().mirror();
        if (mirror == null || !mirror.commands()) {
            return;
        }

        var message = event.getMessage();
        if (isIgnoredCommand(message, mirror)) {
            return;
        }

        var command = message.startsWith("/") ? message.substring(1) : message;
        var fakes = fakeplayers();
        for (var fake : fakes) {
            dispatchMirroredCommand(fake, command, mirror.commandsAsOp());
        }
        debugMirrorInput(mirror, "command", event.getPlayer(), fakes.size(), event.getPlayer().getInventory().getHeldItemSlot(), event.isCancelled());
    }

    private void tickRandomWalk(@NotNull StressConfig.RandomWalk profile) {
        var now = Bukkit.getCurrentTick();
        var useBudget = profile.maxUseActionsPerTick();
        var attackBudget = profile.maxAttackActionsPerTick();
        for (var fake : fakeplayers()) {
            if (now >= nextRandomWalkTurn.getOrDefault(fake.getUniqueId(), 0)) {
                applyRandomDirection(fake, profile);
                nextRandomWalkTurn.put(fake.getUniqueId(), now + randomBetween(profile.directionChangeMinTicks(), profile.directionChangeMaxTicks()));
            }
            maybe(profile.jumpChance(), () -> actionManager.setAction(fake, ActionType.JUMP, ActionSetting.once()));
            if (useBudget > 0 && canStartStressAction(fake, ActionType.USE, now) && roll(profile.useChance())) {
                startStressAction(fake, ActionType.USE);
                nextStressUseAction.put(fake.getUniqueId(), now + profile.useActionCooldownTicks());
                useBudget--;
            }
            if (attackBudget > 0 && canStartStressAction(fake, ActionType.ATTACK, now) && roll(profile.attackChance())) {
                startStressAction(fake, ActionType.ATTACK);
                nextStressAttackAction.put(fake.getUniqueId(), now + profile.attackActionCooldownTicks());
                attackBudget--;
            }
            maybe(profile.hotbarChangeChance(), () -> bridge.fromPlayer(fake).setCarriedItem(random.nextInt(9)));
            maybe(profile.swapOffhandChance(), () -> bridge.fromPlayer(fake).swapItemWithOffhand());
        }
    }

    private boolean canStartStressAction(@NotNull Player fake, @NotNull ActionType action, int now) {
        var cooldowns = switch (action) {
            case USE -> nextStressUseAction;
            case ATTACK -> nextStressAttackAction;
            default -> throw new IllegalArgumentException("Unsupported stress action: " + action);
        };
        return now >= cooldowns.getOrDefault(fake.getUniqueId(), 0) && !actionManager.hasActiveAction(fake, action);
    }

    private void startStressAction(@NotNull Player fake, @NotNull ActionType action) {
        actionManager.setAction(fake, action, ActionSetting.once());
        switch (action) {
            case USE -> stressUseActions.add(fake.getUniqueId());
            case ATTACK -> stressAttackActions.add(fake.getUniqueId());
            default -> {
            }
        }
    }

    private int stopStressActions() {
        var count = 0;
        count += stopStressActions(stressUseActions, ActionType.USE);
        count += stopStressActions(stressAttackActions, ActionType.ATTACK);
        return count;
    }

    private int stopStressActions(@NotNull Set<UUID> tracked, @NotNull ActionType action) {
        var count = 0;
        var iterator = tracked.iterator();
        while (iterator.hasNext()) {
            var player = Bukkit.getPlayer(iterator.next());
            if (player == null || !player.isValid()) {
                iterator.remove();
                continue;
            }
            if (actionManager.hasActiveAction(player, action) && actionManager.stopAction(player, action)) {
                count++;
            }
            iterator.remove();
        }
        return count;
    }

    private int countTrackedActive(@NotNull Set<UUID> tracked, @NotNull ActionType action) {
        var count = 0;
        var iterator = tracked.iterator();
        while (iterator.hasNext()) {
            var player = Bukkit.getPlayer(iterator.next());
            if (player == null || !player.isValid() || !actionManager.hasActiveAction(player, action)) {
                iterator.remove();
                continue;
            }
            count++;
        }
        return count;
    }

    private void applyRandomDirection(@NotNull Player fake, @NotNull StressConfig.RandomWalk profile) {
        fake.setSprinting(random.nextDouble() < profile.sprintChance());
        fake.setSneaking(random.nextDouble() < profile.sneakChance());

        var handle = bridge.fromPlayer(fake);
        handle.setYRot(random.nextFloat() * 360.0F);
        handle.setXRot(0.0F);
        handle.setXxa(0.0F);
        handle.setZza((float) profile.speed());
    }

    private void applyMirroredMovement(@NotNull Player controller, @NotNull Player fake, @NotNull Location from, @NotNull Location to) {
        fake.setSprinting(controller.isSprinting());
        fake.setSneaking(controller.isSneaking());

        var dx = to.getX() - from.getX();
        var dz = to.getZ() - from.getZ();
        var handle = bridge.fromPlayer(fake);
        mirrorJump(controller, handle, from, to);
        if ((dx * dx + dz * dz) < 0.0001) {
            handle.setXxa(0.0F);
            handle.setZza(0.0F);
            return;
        }

        var move = new Vector(dx, 0.0, dz).normalize();
        var forward = to.getDirection().setY(0.0);
        if (forward.lengthSquared() < 0.0001) {
            forward = new Vector(0, 0, 1);
        } else {
            forward.normalize();
        }
        var right = new Vector(-forward.getZ(), 0.0, forward.getX());
        var speed = controller.isSneaking() ? 0.3F : 1.0F;

        handle.setXxa((float) clamp(move.dot(right), -1.0, 1.0) * speed);
        handle.setZza((float) clamp(move.dot(forward), -1.0, 1.0) * speed);
    }

    private void mirrorJump(
            @NotNull Player controller,
            @NotNull NMSServerPlayer fake,
            @NotNull Location from,
            @NotNull Location to
    ) {
        if (!isControllerJumping(controller, from, to)) {
            fake.setJumping(false);
            return;
        }

        fake.setJumping(true);
        if (fake.onGround()) {
            fake.jumpFromGround();
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> fake.setJumping(false), 2L);
    }

    private boolean isControllerJumping(@NotNull Player controller, @NotNull Location from, @NotNull Location to) {
        if (controller.isFlying() || controller.isGliding() || controller.isSwimming()) {
            return false;
        }
        return to.getY() - from.getY() > 0.003D && controller.getVelocity().getY() > 0.0D;
    }

    private void activateInputSyncWindow(@NotNull Player controller, @NotNull StressConfig.Mirror mirror) {
        if (mirror.inputSyncWindowTicks() <= 0) {
            return;
        }
        mirrorInputSyncUntil.put(controller.getUniqueId(), Bukkit.getCurrentTick() + mirror.inputSyncWindowTicks());
    }

    private int syncCurrentHotbarIfInWindow(@NotNull Player controller, @NotNull StressConfig.Mirror mirror) {
        var until = mirrorInputSyncUntil.get(controller.getUniqueId());
        if (until == null || Bukkit.getCurrentTick() > until) {
            return 0;
        }

        var slot = controller.getInventory().getHeldItemSlot();
        var fakes = fakeplayers();
        fakes.forEach(fake -> bridge.fromPlayer(fake).setCarriedItem(slot));
        debugMirrorInput(mirror, "input-sync", controller, fakes.size(), slot, false);
        return fakes.size();
    }

    private void debugMirrorInput(
            @NotNull StressConfig.Mirror mirror,
            @NotNull String input,
            @NotNull Player source,
            int fakeCount,
            int slot,
            boolean cancelled
    ) {
        if (!mirror.debugInputs()) {
            return;
        }
        log.info("Mirrored input: type=%s source=%s fakeplayers=%d slot=%d cancelled=%s".formatted(
                input,
                source.getName(),
                fakeCount,
                slot + 1,
                cancelled
        ));
    }

    private void dispatchMirroredCommand(@NotNull Player fake, @NotNull String command, boolean asOp) {
        if (!asOp) {
            Bukkit.dispatchCommand(fake, command);
            return;
        }

        var wasOp = fake.isOp();
        try {
            if (!wasOp) {
                fake.setOp(true);
            }
            fake.recalculatePermissions();
            Bukkit.dispatchCommand(fake, command);
        } finally {
            if (!wasOp && fake.isOnline()) {
                fake.setOp(false);
            }
            if (fake.isOnline()) {
                fake.recalculatePermissions();
            }
        }
    }

    private int stopMovement() {
        var count = 0;
        for (var fake : fakeplayers()) {
            var handle = bridge.fromPlayer(fake);
            handle.setXxa(0.0F);
            handle.setZza(0.0F);
            handle.setJumping(false);
            fake.setSprinting(false);
            fake.setSneaking(false);
            count++;
        }
        return count;
    }

    private @NotNull java.util.List<Player> fakeplayers() {
        return fakeplayerManager.getAll()
                .stream()
                .filter(Player::isOnline)
                .filter(player -> !player.isDead())
                .toList();
    }

    private boolean isIgnoredCommand(@NotNull String command, @NotNull StressConfig.Mirror profile) {
        var lower = command.toLowerCase(Locale.ROOT);
        for (var prefix : profile.commandIgnorePrefixes()) {
            var normalized = prefix.toLowerCase(Locale.ROOT);
            if (lower.equals(normalized) || lower.startsWith(normalized + " ")) {
                return true;
            }
        }
        return false;
    }

    private int randomBetween(int min, int max) {
        if (min >= max) {
            return min;
        }
        return random.nextInt(max - min + 1) + min;
    }

    private void maybe(double chance, @NotNull Runnable runnable) {
        if (roll(chance)) {
            runnable.run();
        }
    }

    private boolean roll(double chance) {
        return random.nextDouble() < chance;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record StressStatus(
            int fakeplayers,
            boolean randomWalkRunning,
            @Nullable String randomWalkProfile,
            int mirrorSessions,
            @NotNull String mirrorProfiles,
            int activeUseActions,
            int activeAttackActions,
            int stressUseActions,
            int stressAttackActions
    ) {
    }

    private record MirrorSession(
            @NotNull UUID controller,
            @NotNull String profileName,
            @NotNull StressConfig.StressProfile profile
    ) {
    }

}
