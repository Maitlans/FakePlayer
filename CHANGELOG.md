# Changelog

## Unreleased

### Features

* add Minecraft/Paper/Spigot 1.21.11 build support for the fakeplayer runtime module
* add OP-only `/fp stress` command namespace for bulk fakeplayer stress testing
* add `/fp stress spawn <amount> [profile]` for batched fakeplayer spawning using the operator's name pattern
* add `/fp stress spread <radius> [profile]` to scatter all fakeplayers around the operator for chunk-load testing
* add `/fp stress randomwalk start|stop` with configurable movement, sprint, sneak, jump, use, attack, hotbar, and offhand-swap behavior
* add `/fp stress mirror start|stop` to mirror operator movement, look direction, actions, hotbar changes, offhand swaps, and commands to fakeplayers
* add bulk stress actions: use, attack, jump, swap, hotbar slot, hotbar random, and hotbar cycle
* add reusable `stress` config profiles for spawn batching, spread Y placement, random walking, and mirror behavior
* add randomwalk use/attack cooldowns and per-tick action budgets for realistic bounded interaction stress
* add `/fp stress status` for fakeplayer counts, active stress tasks, active use/attack actions, and profile visibility
* add mirror `commands-as-op`, `input-sync-window-ticks`, and `debug-inputs` config options
* improve mirror movement to copy jump input, cancelled input events, packet-style hotbar selection, arm swings, and F-key/offhand swaps
* add `auto-respawn-on-death` config option to automatically respawn fakeplayers after death
* add `fakeplayer.command.stress` permission and include it under `fakeplayer.*`

### Bug Fixes

* update CommandAPI integration for CommandAPI 11.2.0 compatibility
* fix custom fakeplayer command argument construction for newer CommandAPI versions
* fix one-shot use actions getting stuck when an interaction fires events but is not consumed
* harden fakeplayer network/channel cleanup during disconnect and plugin disable
* clean up fakeplayer lifecycle handling so fakeplayers shut down more safely when the plugin unloads

### Changes

* make bundled config, comments, plugin metadata, README, and language resources English-only
* change default config locale to `en`
* change default message prefix to `FakePlayer`
* change death defaults so fakeplayers are not kicked on death and auto-respawn is enabled

## [1.2.0](https://github.com/coderxi1/minecraft-fakeplayer/compare/v1.1.3...v1.2.0) (2026-03-24)


### Features

* remove fakeplayer immediately on creator quit ([d6041da](https://github.com/coderxi1/minecraft-fakeplayer/commit/d6041da6b983be844dbb51952bcb7834217bda79))


### Bug Fixes

* config after-quit-commands invalid ([d8ae21e](https://github.com/coderxi1/minecraft-fakeplayer/commit/d8ae21e66618d9164df57e54bdcb068dca88f785))
* skip fakeplayer removal on rapid reconnection ([fbf03ce](https://github.com/coderxi1/minecraft-fakeplayer/commit/fbf03cec8ab16574b6ec055f3db7aa4b7f70a80c))

## [1.1.3](https://github.com/coderxi1/minecraft-fakeplayer/compare/v1.1.2...v1.1.3) (2026-03-11)


### Bug Fixes

* add placeholder fakeplayer_actions_translated ([66f2af6](https://github.com/coderxi1/minecraft-fakeplayer/commit/66f2af68cf176e731cb56fe15804f6482cbbab3e))

## [1.1.2](https://github.com/coderxi1/minecraft-fakeplayer/compare/v1.1.1...v1.1.2) (2026-03-10)


### Bug Fixes

* cannot use openinv ([7162822](https://github.com/coderxi1/minecraft-fakeplayer/commit/716282294dfbdd097ec831c75482ea1a6e5e9fc2))

## [1.1.1](https://github.com/coderxi1/minecraft-fakeplayer/compare/v1.1.0...v1.1.1) (2026-03-10)


### Bug Fixes

* add message prefix ([b2aa685](https://github.com/coderxi1/minecraft-fakeplayer/commit/b2aa685adaea6ee17cd4d6bde91d3eee7f2e9f46))

## [1.1.0](https://github.com/coderxi1/minecraft-fakeplayer/compare/v1.0.1...v1.1.0) (2026-03-10)


### Features

* add message prefix ([3d78d4a](https://github.com/coderxi1/minecraft-fakeplayer/commit/3d78d4a5d3f6a683a0ddd7b244ca224abce87d42))
* apply dynamic fakeplayer limits to server lag ([1d3d2f0](https://github.com/coderxi1/minecraft-fakeplayer/commit/1d3d2f0121fde9dac7c315194e9d56c9c30055a1))


### Bug Fixes

* **typo:** incorrect closing brackets ([316825d](https://github.com/coderxi1/minecraft-fakeplayer/commit/316825d46932d03596ab38fc12e566917216b56f))

## [1.0.1](https://github.com/coderxi1/minecraft-fakeplayer/compare/v1.0.0...v1.0.1) (2026-03-10)


### Bug Fixes

* player invincible in 1.21.10 ([04f1dc3](https://github.com/coderxi1/minecraft-fakeplayer/commit/04f1dc35f5559c41562274475f417871655b6a29))

## 1.0.0 (2026-03-09)


### Bug Fixes

* inventory data not loading ([1f2d4fb](https://github.com/coderxi1/minecraft-fakeplayer/commit/1f2d4fbc7aa30abe8aa48ea4e5df27a72d39d11d))
* placeholder fakeplayer_actions invalid ([9d21b2c](https://github.com/coderxi1/minecraft-fakeplayer/commit/9d21b2c8f410729c5d2e30ff71d3bf972c34dcbe))
