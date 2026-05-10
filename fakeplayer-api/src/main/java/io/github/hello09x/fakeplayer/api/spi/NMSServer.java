package io.github.hello09x.fakeplayer.api.spi;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface NMSServer {

    /**
     *
     * @param uuid UUID
     */
    @NotNull NMSServerPlayer newPlayer(@NotNull UUID uuid, @NotNull String name);


}
