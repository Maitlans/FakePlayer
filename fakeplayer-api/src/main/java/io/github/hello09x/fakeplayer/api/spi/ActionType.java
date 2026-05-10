package io.github.hello09x.fakeplayer.api.spi;

import lombok.AllArgsConstructor;
import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

/**
 * @author tanyaofei
 * @since 2024/8/9
 **/
@AllArgsConstructor
public
enum ActionType implements Translatable {

    /**
     */
    ATTACK("fakeplayer.action.attack"),

    /**
     */
    MINE("fakeplayer.action.mine"),

    /**
     */
    USE("fakeplayer.action.use"),

    /**
     */
    JUMP("fakeplayer.action.jump"),

    /**
     */
    LOOK_AT_NEAREST_ENTITY("fakeplayer.action.look-at-entity"),

    /**
     */
    DROP_ITEM("fakeplayer.action.drop-item"),

    /**
     */
    DROP_STACK("fakeplayer.action.drop-stack"),

    /**
     */
    DROP_INVENTORY("fakeplayer.action.drop-inventory");

    final String translationKey;


    @Override
    public @NotNull String translationKey() {
        return this.translationKey;
    }
}
