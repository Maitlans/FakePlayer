package io.github.hello09x.fakeplayer.api.spi;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public interface NMSServerPlayer {

    /**
     */
    @NotNull
    Player getPlayer();

    /**
     */
    double getX();

    /**
     */
    double getY();

    /**
     */
    double getZ();

    /**
     *
     */
    void setXo(double xo);

    /**
     *
     */
    void setYo(double yo);

    /**
     *
     */
    void setZo(double zo);

    /**
     */
    void doTick();

    /**
     *
     */
    void absMoveTo(double x, double y, double z, float yRot, float xRot);

    /**
     *
     */
    float getYRot();

    /**
     *
     */
    void setYRot(float yRot);

    /**
     *
     */
    float getXRot();

    /**
     *
     */
    void setXRot(float xRot);

    /**
     */
    float getZza();

    /**
     *
     */
    void setZza(float zza);

    /**
     */
    float getXxa();

    /**
     *
     */
    void setXxa(float xxa);

    /**
     */
    void setDeltaMovement(@NotNull Vector vector);

    /**
     *
     */
    boolean startRiding(@NotNull Entity entity, boolean force);

    /**
     */
    void stopRiding();

    /**
     *
     */
    void disableAdvancements(@NotNull Plugin plugin);

    /**
     *
     */
    int getTickCount();

    /**
     *
     * @param flag
     * @param flag1
     */
    void drop(int slot, boolean flag, boolean flag1);

    /**
     *
     */
    void drop(boolean allStack);

    /**
     */
    void resetLastActionTime();

    /**
     *
     */
    boolean onGround();

    /**
     */
    void jumpFromGround();

    /**
     *
     */
    void setJumping(boolean jumping);

    /**
     *
     */
    boolean isUsingItem();

    /**
     */
    void setPlayBefore();

    /**
     */
    void setupClientOptions();

    /**
     */
    void respawn();

    /**
     */
    void swapItemWithOffhand();

    /**
     */
    void setCarriedItem(int slot);

    /**
     */
    void swingMainHand();

}
