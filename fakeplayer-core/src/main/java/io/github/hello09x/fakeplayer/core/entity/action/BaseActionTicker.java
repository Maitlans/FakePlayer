package io.github.hello09x.fakeplayer.core.entity.action;

import io.github.hello09x.fakeplayer.api.spi.*;
import io.github.hello09x.fakeplayer.core.entity.action.impl.*;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public abstract class BaseActionTicker implements ActionTicker {

    protected final NMSBridge bridge;

    /**
     */
    @UnknownNullability
    protected Action action;

    @NotNull
    @Getter
    protected ActionSetting setting;

    public BaseActionTicker(NMSBridge nms, @NotNull Player player, @NotNull ActionType action, @NotNull ActionSetting setting) {
        this.bridge = nms;
        this.setting = setting;
        this.action = switch (action) {
            case JUMP -> new JumpAction(nms.fromPlayer(player));
            case LOOK_AT_NEAREST_ENTITY -> new LookAtEntityAction(nms.fromPlayer(player));
            case DROP_ITEM -> new DropItemAction(nms.fromPlayer(player));
            case DROP_STACK -> new DropStackAction(nms.fromPlayer(player));
            case DROP_INVENTORY -> new DropInventoryAction(nms.fromPlayer(player));
            default -> null;
        };
    }


    @Override
    public boolean tick() {
        if (this.setting.equals(ActionSetting.stop())) {
            this.action.stop();
            return true;
        }

        if (setting.wait > 0) {
            this.setting.wait--;
            this.inactiveTick();
            return false;
        }

        if (this.setting.remains == 0) {
            this.inactiveTick();
            return true;
        }

        try {
            if (this.action.tick()) {
                if (this.setting.remains > 0) {
                    this.setting.remains--;
                }
            }
        } finally {
            this.setting.wait = this.setting.interval;
        }

        return false;
    }

    @Override
    public void inactiveTick() {
        action.inactiveTick();
    }

    @Override
    public void stop() {
        action.stop();
    }


}
