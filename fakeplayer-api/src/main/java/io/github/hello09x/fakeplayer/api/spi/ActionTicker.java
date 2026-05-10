package io.github.hello09x.fakeplayer.api.spi;

import org.jetbrains.annotations.NotNull;

public interface ActionTicker {

    @NotNull
    ActionSetting getSetting();

    /**
     *
     */
    boolean tick();

    /**
     */
    void inactiveTick();

    /**
     */
    void stop();

}
