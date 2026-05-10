package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.config.StressConfig;
import io.github.hello09x.fakeplayer.core.manager.StressManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.kyori.adventure.text.Component.text;

@Singleton
public class StressCommand extends AbstractCommand {

    private final StressManager stressManager;

    @Inject
    public StressCommand(@NotNull StressManager stressManager) {
        this.stressManager = stressManager;
    }

    public void spawn(@NotNull Player sender, @NotNull CommandArguments args) {
        var amount = (int) Objects.requireNonNull(args.get("amount"));
        stressManager.spawn(sender, amount, profile(args));
    }

    public void spread(@NotNull Player sender, @NotNull CommandArguments args) {
        var radius = (double) Objects.requireNonNull(args.get("radius"));
        var count = stressManager.spread(sender, radius, profile(args));
        sender.sendMessage(text("Spread " + count + " fakeplayers.", NamedTextColor.GRAY));
    }

    public void randomWalkStart(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var profileName = profileName(args);
        var count = stressManager.startRandomWalk(profileName, profile(args));
        sender.sendMessage(text("Started random walk for " + count + " fakeplayers using profile " + profileName + ".", NamedTextColor.GRAY));
    }

    public void randomWalkStop(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var count = stressManager.stopRandomWalk();
        sender.sendMessage(text("Stopped random walk for " + count + " fakeplayers.", NamedTextColor.GRAY));
    }

    public void mirrorStart(@NotNull Player sender, @NotNull CommandArguments args) {
        var profileName = profileName(args);
        stressManager.startMirror(sender, profileName, profile(args));
        sender.sendMessage(text("Started mirroring to fakeplayers using profile " + profileName + ".", NamedTextColor.GRAY));
    }

    public void mirrorStop(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var count = stressManager.stopMirror();
        sender.sendMessage(text("Stopped " + count + " mirror session(s).", NamedTextColor.GRAY));
    }

    public void action(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var action = (String) Objects.requireNonNull(args.get("action"));
        var count = stressManager.action(action);
        sender.sendMessage(text("Ran " + action + " for " + count + " fakeplayers.", NamedTextColor.GRAY));
    }

    public void hotbarSlot(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var slot = (int) Objects.requireNonNull(args.get("slot"));
        var count = stressManager.hotbarSlot(slot);
        sender.sendMessage(text("Set hotbar slot " + slot + " for " + count + " fakeplayers.", NamedTextColor.GRAY));
    }

    public void hotbarRandom(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var count = stressManager.hotbarRandom();
        sender.sendMessage(text("Randomized hotbar slots for " + count + " fakeplayers.", NamedTextColor.GRAY));
    }

    public void hotbarCycle(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var count = stressManager.hotbarCycle();
        sender.sendMessage(text("Cycled hotbar slots for " + count + " fakeplayers.", NamedTextColor.GRAY));
    }

    public void stop(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        stressManager.stopAll();
        sender.sendMessage(text("Stopped stress tasks.", NamedTextColor.GRAY));
    }

    public void status(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var status = stressManager.status();
        var randomWalk = status.randomWalkRunning()
                ? "running (" + status.randomWalkProfile() + ")"
                : "stopped";
        var mirrorProfiles = status.mirrorProfiles().isBlank() ? "none" : status.mirrorProfiles();

        sender.sendMessage(text("Stress status:", NamedTextColor.GRAY));
        sender.sendMessage(text("- Fakeplayers: " + status.fakeplayers(), NamedTextColor.GRAY));
        sender.sendMessage(text("- Random walk: " + randomWalk, NamedTextColor.GRAY));
        sender.sendMessage(text("- Mirror sessions: " + status.mirrorSessions() + " (" + mirrorProfiles + ")", NamedTextColor.GRAY));
        sender.sendMessage(text("- Active USE actions: " + status.activeUseActions() + " (" + status.stressUseActions() + " stress-created)", NamedTextColor.GRAY));
        sender.sendMessage(text("- Active ATTACK actions: " + status.activeAttackActions() + " (" + status.stressAttackActions() + " stress-created)", NamedTextColor.GRAY));
    }

    private @NotNull StressConfig.StressProfile profile(@NotNull CommandArguments args) {
        return config.getStress().getProfile(profileName(args));
    }

    private @NotNull String profileName(@NotNull CommandArguments args) {
        return args.getOptional("profile")
                .map(String.class::cast)
                .filter(profile -> !profile.isBlank())
                .orElse(config.getStress().getDefaultProfile());
    }

}
