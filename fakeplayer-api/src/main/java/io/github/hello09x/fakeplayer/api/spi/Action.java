package io.github.hello09x.fakeplayer.api.spi;

public interface Action {

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
