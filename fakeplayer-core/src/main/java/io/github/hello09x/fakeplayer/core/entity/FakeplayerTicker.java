package io.github.hello09x.fakeplayer.core.entity;

import io.github.hello09x.fakeplayer.api.spi.NMSServerPlayer;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class FakeplayerTicker extends BukkitRunnable {

    public final static long NON_REMOVE_AT = -1;

    @NotNull
    private final Fakeplayer player;

    /**
     */
    private final long removeAt;

    /**
     */
    private boolean firstTick;

    public FakeplayerTicker(
            @NotNull Fakeplayer player,
            long lifespan
    ) {
        this.player = player;
        this.removeAt = lifespan > 0 ? System.currentTimeMillis() + lifespan : NON_REMOVE_AT;
        this.firstTick = true;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            super.cancel();
            return;
        }

        if (this.removeAt != NON_REMOVE_AT && this.player.getTickCount() % 20 == 0 && System.currentTimeMillis() > removeAt) {
            Main.getInjector().getInstance(FakeplayerManager.class).remove(player.getName(), "lifespan ends");
            super.cancel();
            return;
        }

        if (this.firstTick) {
            this.doFirstTick();
        } else {
            this.doTick();
        }
    }

    /**
     */
    private void doFirstTick() {
        var handle = this.player.getHandle();
        var player = this.player.getPlayer();
        var x = handle.getX();
        var y = handle.getY();
        var z = handle.getZ();

        handle.setXo(x);
        handle.setYo(y);
        handle.setZo(z);

        handle.doTick();

        player.teleport(new Location(player.getWorld(), x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch()));
        handle.absMoveTo(x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());
        this.firstTick = false;
    }

    private void doTick() {
        var handle = this.player.getHandle();
        handle.doTick();
    }

}
