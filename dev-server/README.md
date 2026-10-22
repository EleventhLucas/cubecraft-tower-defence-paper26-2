Local VS Code playtest
=====================

Select **Launch CubeCraft TD (Minecraft 26.2)** in VS Code Run and Debug and press F5.
The launch builds the shaded plugin, prepares a separate server under `server/`,
and finds Java 25 in `JAVA25_HOME`, `JAVA_HOME`, PATH, Gradle's JDK cache, or `/usr/lib/jvm`.
A Gradle 9.7.1 wrapper is included; Python 3 and Linux `flock` are also required.

The first launch asks you to accept the Minecraft EULA in the terminal. It then
boots once to run `ctdinstall create 1`, stops cleanly, and starts the playable
server. The plugin builds its approximate full Farm scene and bundled tower
models automatically. Wait for **Managed Farm READY** before visiting/queueing.
Initial setup logs are in `server/deployment-boot-local-install.log`.

- Client: **Minecraft Java 26.2**, vanilla is sufficient. Use a separate client
  profile; mods from a 26.1.1 profile are not required by this server.
- Address: **127.0.0.1:25566**. It is bound to loopback for local testing.
- Paper: **26.2 build 134**, pinned to the upstream network harness's checksum.
- Memory: **1 GB initial / 4 GB maximum heap**.
- Server console: `op YourMinecraftName` once to grant yourself admin commands.
- In-game preview: `/ctdinstall visit`.
- Solo testing: `/ctdfreeplay` as an operator. Build on Red's side and send enemies
  down your own lane using the Summoner. Troop levels/potions are unlocked, with
  100 million coins/EXP, protected castles, no send cooldown, no automatic
  Armageddon and no match deadline. `/ctdleave` cleans up and restores your state.
- Preflight: `/ctdpreflight`. To exercise player-state capture and restoration,
  run `/ctdsnapshotcheck` as an operator outside a match.
- Match: two online players each use `/ctdjoin`; `/ctdvote` controls pregame votes.
  This is **1v1**, not a solo/bot opponent. One person can preview the map.
- Leave: `/ctdleave`. Inspect load: `/ctdperf`.
- Stop: type `stop` in the server terminal and wait for it to finish before F5 again.

For a second person on another computer, explicitly change `server-ip` in
`server/server.properties` to the desired LAN binding and connect using that
machine's LAN IP and port 25566. Online account authentication stays enabled.

Terminal equivalents:

```sh
bash dev-server/prepare.sh
bash dev-server/start.sh
bash dev-server/gradle.sh test
```

Preparation preserves existing server settings and worlds. Server state,
downloaded maps, credentials and player data remain ignored under `server/`.
The original `playtest/` packaging scripts and TD-Minecraft instance are independent.
No live-test certification is manufactured by this setup.
`/ctdready` may still report `PAPER_LIVE_GATE_NOT_CERTIFIED`: it tracks the
upstream's broader live-test evidence. The engineering queue explicitly allows
that outstanding certification, while still enforcing its other readiness checks.

Local verification on 2026-10-21: the plugin built, Farm completed its initial
build, composition preflight passed, and a subsequent launch reused the same
owned READY world without another paste. Both playable-server boots shut down
with `clean=true`. Duplicate launch protection and preservation of local settings
were checked. Human gameplay and multiplayer matches remain to be tested.
