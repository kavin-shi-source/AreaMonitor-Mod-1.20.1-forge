# AreaMonitor Mod 1.20.1

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.4.0-blue.svg)](https://files.minecraftforge.net)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE.txt)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-brightgreen.svg)](https://modrinth.com/mod/areamonitor)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange.svg)](https://www.curseforge.com/minecraft/mc-mods/areamonitor)

[中文版本](README.md)

A feature-rich Minecraft Forge server-side mod for **monitoring designated zones** and **auto-switching player game modes**. Supports rectangle/circle/polygon boundaries, nine protection rules, programmable triggers, GUI management, and more.

---

## Features

| Module | Description |
|------|------|
| **Zone Management** | Create, delete, list, toggle, inspect, export/import zones |
| **Game Mode Switching** | Auto-switch game mode on zone enter/leave |
| **Zone Boundaries** | Rectangle, circle, and polygon (3–32 vertices) |
| **Protection System** | 9 protection types: block break/place/interact, PVP, explosion, entity damage, container access, fluid placement, item drop |
| **Protection Whitelist** | Per-zone exception list for protection rules |
| **Trigger System** | Independent enter/leave triggers: commands, sound, title, action bar, potion effects, teleport, cooldown, debounce |
| **Trigger Conditions** | AND-combined conditions: held item, time range, weather, minimum players |
| **Item Blacklist** | Block specific items within zones, with smart tab-completion |
| **Whitelist System** | Global + per-zone whitelists to bypass game mode switching |
| **Time Scheduling** | Auto-enable/disable zones by in-game time (cross-midnight supported) |
| **Conditional Activation** | Activate zones based on online player count or specific player presence |
| **Zone Chaining** | Enter zone A → auto-teleport to B → C → … |
| **Selection Tool** | Wooden axe right-click + polygon vertex collection mode |
| **Boundary Visualization** | Real-time particle outline of zone borders |
| **GUI Management** | Glass Morphism floating panel for complete visual management |
| **Configuration** | JSON format with hot-reload and auto-generation |
| **Backup & Restore** | One-click timestamped config folder backup |
| **Statistics Tracking** | Entry count, last visitor, last visit time per zone |
| **Performance Monitor** | `/areamonitor performance` for runtime metrics |
| **Spatial Partitioning** | Grid-based spatial index, O(k) lookup |
| **Audit Logging** | Auto-log zone create/delete/toggle operations |
| **Localization** | Full Chinese (zh_cn) and English (en_us) support |

---

## Quick Start

### Installation

1. Install Minecraft 1.20.1 + Forge 47.4.0+
2. Place `areamonitor-2.0.4.jar` in your server's `mods/` folder
3. Start the server — config files are auto-generated in `config/areamonitor/`

### Creating Your First Zone

```mcfunction
# Get the selection tool (wooden axe)
/areamonitor visual tool

# Right-click two corners, then create
/areamonitor selection create spawn

# Set game modes
/areamonitor area setEnterMode spawn creative
/areamonitor area setLeaveMode spawn survival
```

---

## Command Overview

All commands use `/areamonitor` as the root and require OP level 2. **14 subcommand groups, 60+ instructions.**

### Zone (`area`)
```
create <name>  delete <name>  list  toggle <name>  info <name>
setEnterMode <area> <mode>  setLeaveMode <area> <mode>
export <name>  import <name> <json>
```

### Protection (`protect`)
```
<area> all on|off          — Toggle all protections
<area> <type> on|off       — Toggle a single protection type
<area> info                — View protection status
```

Protection types: `blockBreak` `blockPlace` `blockInteract` `pvp` `explosion` `entityDamage` `containerInteract` `fluidPlace` `itemDrop`

### Trigger (`trigger`)
```
<area> enter|leave cmd add|remove|list|clear
<area> enter|leave sound <soundId>|clear
<area> enter|leave title <main> [<sub>]|clear
<area> enter|leave tp <dim> <x> <y> <z>|clear
<area> enter|leave info
```

### Blacklist (`blacklist`)
```
info  reload
area <area> add <item>|remove <item>|list|toggle
```

### Selection (`selection`)
```
create <name>  cancel  info  tutorial
polygon start|finish
```

### Visual (`visual`)
```
tool  show <area>  hide
```

### Whitelist (`whitelist`)
```
add <player>  remove <player>  list  clear
```

### Other Commands
```
/areamonitor toggle         — Global monitoring toggle
/areamonitor config reload  — Hot-reload configuration
/areamonitor config generate— Generate default config files
/areamonitor stats          — Zone statistics
/areamonitor backup         — Backup configuration
/areamonitor performance    — Performance metrics
/areamonitor gui            — Open management GUI
/areamonitor help           — Command help
```

---

## Configuration Files

Located at `config/areamonitor/`:

| File | Description |
|------|------|
| `areas.json` | All zone definitions (bounds, dimension, game modes, protections, triggers, schedule, conditions, chaining) |
| `blacklist.json` | Item and command blacklists |
| `backups/` | Backup directory (timestamped subfolders created by `/areamonitor backup`) |

---

## Game Modes

| Mode | Command Argument |
|------|----------|
| Survival | `survival` |
| Creative | `creative` |
| Adventure | `adventure` |
| Spectator | `spectator` |

---

## Trigger Conditions

Triggers support AND-combined activation conditions (enter and leave configured independently):

| Condition | Description |
|------|------|
| `playerHasItem` | Player must hold a specific item |
| `timeMin` / `timeMax` | In-game time window (0–24000 ticks) |
| `weather` | Weather: `clear` / `rain` / `thunder` |
| `minPlayers` | Minimum online players ≥ N |
| `requirePlayer` | A specific player must be online |

Trigger actions execute in order: command → sound → title → action bar → potion → teleport. Each action supports cooldown and debounce.

---

## Links

- :inbox_tray: [Download on Modrinth](https://modrinth.com/mod/areamonitor)
- :inbox_tray: [Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/areamonitor)
- :book: [Changelog](CHANGELOG.md)
- :bug: [Report an Issue](https://github.com/kavin-shi-source/AreaMonitor-Mod-1.20.1-forge/issues)

## License

This project is licensed under GPL-3.0 — see [LICENSE](LICENSE.txt).
