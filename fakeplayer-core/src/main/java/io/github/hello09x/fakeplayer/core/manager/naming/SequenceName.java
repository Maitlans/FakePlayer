package io.github.hello09x.fakeplayer.core.manager.naming;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 *
 * @param uuid     UUID
 */
public record SequenceName(

        @NotNull
        String group,

        int sequence,

        @NotNull
        UUID uuid,

        @NotNull
        String name

) {
}
