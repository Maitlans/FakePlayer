package io.github.hello09x.fakeplayer.core.manager;

import com.google.inject.Singleton;
import io.github.hello09x.fakeplayer.core.entity.Fakeplayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Stream;

@Singleton
public class FakeplayerList {

    private final Map<String, Fakeplayer> playersByName = new HashMap<>();
    private final Map<UUID, Fakeplayer> playersByUUID = new HashMap<>();
    private final Map<String, List<Fakeplayer>> playersByCreator = new HashMap<>();

    /**
     *
     */
    public void add(@NotNull Fakeplayer player) {
        this.playersByName.put(player.getName(), player);
        this.playersByUUID.put(player.getUUID(), player);
        this.playersByCreator.computeIfAbsent(player.getCreator().getName(), key -> new LinkedList<>()).add(player);
    }

    /**
     *
     */
    public @Nullable Fakeplayer getByName(@NotNull String name) {
        return Optional.ofNullable(this.playersByName.get(name)).map(this::checkOnline).orElse(null);
    }

    /**
     *
     * @param uuid UUID
     */
    public @Nullable Fakeplayer getByUUID(@NotNull UUID uuid) {
        return Optional.ofNullable(this.playersByUUID.get(uuid)).map(this::checkOnline).orElse(null);
    }

    /**
     *
     */
    public @NotNull @Unmodifiable List<Fakeplayer> getByCreator(@NotNull String creator) {
        return Optional.ofNullable(this.playersByCreator.get(creator)).map(Collections::unmodifiableList).orElse(Collections.emptyList());
    }

    /**
     *
     */
    public void remove(@NotNull Fakeplayer player) {
        this.playersByName.remove(player.getName());
        this.playersByUUID.remove(player.getUUID());
        Optional.ofNullable(this.playersByCreator.get(player.getCreator().getName())).map(players -> players.remove(player));
    }

    /**
     *
     * @param uuid UUID
     */
    public @Nullable Fakeplayer removeByUUID(@NotNull UUID uuid) {
        var player = getByUUID(uuid);
        if (player == null) {
            return null;
        }
        this.remove(player);
        return player;
    }

    /**
     *
     */
    public int countByCreator(@NotNull String creator) {
        return Optional
                .ofNullable(this.playersByCreator.get(creator))
                .map(List::size)
                .orElse(0);
    }

    /**
     *
     */
    public @NotNull @Unmodifiable List<Fakeplayer> getAll() {
        return List.copyOf(this.playersByUUID.values());
    }

    /**
     *
     */
    private @Nullable Fakeplayer checkOnline(@NotNull Fakeplayer player) {
        if (!player.isOnline()) {
            this.remove(player);
            return null;
        }

        return player;
    }

    public @NotNull Stream<Fakeplayer> stream() {
        return this.playersByUUID.values().stream();
    }

    public int getSize() {
        return this.playersByUUID.size();
    }

}
