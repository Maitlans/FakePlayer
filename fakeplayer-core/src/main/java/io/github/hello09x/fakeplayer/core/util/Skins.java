package io.github.hello09x.fakeplayer.core.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.hello09x.devtools.core.utils.SchedulerUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class Skins {

    /**
     *
     */
    public static boolean copySkin(@NotNull OfflinePlayer from, @NotNull Player to) {
        var profile = from.getPlayerProfile();
        if (profile.hasTextures()) {
            copyTexture(profile, to);
            return true;
        }
        return false;
    }

    /**
     *
     */
    public static CompletableFuture<Boolean> copySkinFromMojang(@NotNull JavaPlugin plugin, @NotNull OfflinePlayer from, @NotNull Player to) {
        if (copySkin(from, to)) {
            return CompletableFuture.completedFuture(true);
        }

        var profile = from.getPlayerProfile();
        return CompletableFuture
                .supplyAsync(profile::complete)
                .thenComposeAsync(completed -> SchedulerUtils.runTask(plugin, () -> {
                    if (!completed) {
                        return false;
                    }
                    try {
                        copyTexture(profile, to);
                        return true;
                    } catch (Throwable e) {
                        return false;
                    }
                }));
    }

    private static void copyTexture(@NotNull PlayerProfile from, @NotNull Player to) {
        var profile = to.getPlayerProfile();
        profile.setTextures(from.getTextures());
        from.getProperties().stream().filter(p -> p.getName().equals("textures")).findAny().ifPresent(profile::setProperty);
        to.setPlayerProfile(profile);
    }


}
