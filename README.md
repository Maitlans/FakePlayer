# FakePlayer

FakePlayer is a Minecraft server plugin for spawning and controlling server-side fake players. It is useful for testing gameplay systems, chunk loading, permissions, automation, events, and plugin behavior without needing real players online.

This fork is customized for modern Paper/Spigot testing on Minecraft `1.21.11`.

## Features

- Spawn fake players that behave like online players.
- Use owner-based names such as `YourName_1`, `YourName_2`, and so on.
- Control fake players with movement, rotation, attack, use, mining, jumping, sneaking, sprinting, sleep, riding, hotbar, and offhand actions.
- View and manage fake player inventories.
- Optional fake player persistence and inventory dropping.
- Auto-respawn fake players after death.
- OP-only stress-test tools for bulk testing.
- Mirror mode that copies player movement, look direction, clicks, arm swings, F-key/offhand swaps, hotbar changes, commands, and jump-like movement.
- Configurable random walking and bulk action simulation.
- Basic protection against fake players being kicked by other plugins.

## Requirements

- Minecraft server: Paper or Spigot `1.21.11`
- Java `21`
- [CommandAPI](https://commandapi.jorel.dev/) `11.2.0`

Optional integrations:

- OpenInv for improved inventory viewing
- PlaceholderAPI support if installed

## Installation

1. Build or download `fakeplayer-0.3.19.jar`.
2. Install CommandAPI on the server.
3. Put the FakePlayer jar in the server `plugins` folder.
4. Start the server once to generate the config.
5. Review `plugins/fakeplayer/config.yml`.

If you are upgrading from an older config, compare your config with the bundled defaults. Newer options may not appear until you regenerate or manually add them.

## Basic Commands

The main command is:

```text
/fp
```

Common commands:

```text
/fp spawn
/fp kill
/fp list
/fp select
/fp status
/fp respawn
/fp tp
/fp tphere
/fp tps
/fp invsee
/fp config
/fp set
```

Action commands:

```text
/fp attack
/fp mine
/fp use
/fp jump
/fp sneak
/fp sprint
/fp look
/fp turn
/fp move
/fp ride
/fp swap
/fp hold
/fp stop
```

Use `/fp ?` in game for command help and syntax.

## Stress Testing

Stress commands are under:

```text
/fp stress
```

These commands are OP-only and are intended for controlled test environments.

Examples:

```text
/fp stress spawn 100
/fp stress spread 500
/fp stress randomwalk start
/fp stress randomwalk stop
/fp stress mirror start
/fp stress mirror stop
/fp stress status
/fp stress stop
```

Bulk action helpers:

```text
/fp stress action use
/fp stress action attack
/fp stress action jump
/fp stress action swap
/fp stress hotbar slot 1
/fp stress hotbar random
/fp stress hotbar cycle
```

Stress profiles are configured in `config.yml`:

```yaml
stress:
  default-profile: default
  profiles:
    default:
      spawn:
        batch-size: 5
        batch-interval-ticks: 1
      spread:
        y-mode: SAME_Y
      random-walk:
        tick-interval: 5
        direction-change-min-ticks: 20
        direction-change-max-ticks: 80
        speed: 1.0
        use-chance: 0.02
        attack-chance: 0.02
        max-use-actions-per-tick: 10
        max-attack-actions-per-tick: 10
      mirror:
        movement: true
        look: true
        actions: true
        commands: true
        commands-as-op: true
        input-sync-window-ticks: 100
        debug-inputs: false
```

## Mirror Mode

Mirror mode lets an operator control many fake players by copying their own inputs.

It can mirror:

- Movement and look direction
- Sprinting and sneaking
- Jump-like movement
- Hotbar changes
- F-key/offhand swaps
- Right-click use actions
- Left-click arm swings and attack actions
- Commands

Mirrored commands ignore `/fp` and `/fakeplayer` by default to prevent control loops.

For debugging mirrored inputs, set:

```yaml
debug-inputs: true
```

Then reload the plugin and restart mirror mode.

## Important Config Options

Death behavior:

```yaml
kick-on-dead: false
auto-respawn-on-death: true
```

Limits:

```yaml
server-limit: 1000
player-limit: 1
detect-ip: true
```

Kick protection:

```yaml
prevent-kicking: ON_SPAWNING
```

Name settings:

```yaml
name-template: ''
name-prefix: ''
name-pattern: '^[a-zA-Z0-9_]+$'
```

## Permissions

Useful permission groups:

```text
fakeplayer.spawn
fakeplayer.tp
fakeplayer.exp
fakeplayer.action
fakeplayer.*
```

Stress testing:

```text
fakeplayer.command.stress
```

Stress commands also require the sender to be OP.

## Building

Build with Maven:

```bash
mvn clean package -DskipTests
```

The final shaded plugin jar is created at:

```text
target/fakeplayer-0.3.19.jar
```

## Notes

- Bulk fake players can create real server load. Start small and increase gradually.
- Spread and random walking can load many chunks.
- Use `/fp stress stop` if a stress test needs to be stopped quickly.
- Some inputs only exist as raw client packets. Without a packet-listening dependency, Bukkit/NMS mirror mode can only mirror inputs that the server exposes through events or server handlers.

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for detailed update history.