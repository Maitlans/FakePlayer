package io.github.hello09x.fakeplayer.v1_21_11.action;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;


public class AttackAction extends TraceAction {

    private final ServerPlayer player;

    public AttackAction(ServerPlayer player) {
        super(player);
        this.player = player;
    }


    @Override
    public boolean tick() {
        var hit = this.getTarget();
        if (hit == null) {
            player.swing(InteractionHand.MAIN_HAND);
            player.resetLastActionTime();
            return true;
        }

        if (hit.getType() != HitResult.Type.ENTITY) {
            player.swing(InteractionHand.MAIN_HAND);
            player.resetLastActionTime();
            return true;
        }

        var entityHit = (EntityHitResult) hit;
        player.attack(entityHit.getEntity());
        player.swing(InteractionHand.MAIN_HAND);
        player.resetAttackStrengthTicker();
        player.resetLastActionTime();
        return true;
    }

    @Override
    public void inactiveTick() {

    }

    @Override
    public void stop() {

    }


}
