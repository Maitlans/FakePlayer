package io.github.hello09x.fakeplayer.core.entity;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 */
public record SpawnOption(

        @NotNull
        Location spawnAt,

        boolean invulnerable,

        boolean collidable,

        boolean lookAtEntity,

        boolean pickupItems,

        boolean skin,

        boolean replenish,

        boolean autofish,

        boolean wolverine

) {
}
