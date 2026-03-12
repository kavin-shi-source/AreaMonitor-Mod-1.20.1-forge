# Quick Start Guide - AreaMonitor Mod

[![中文版本](https://img.shields.io/badge/中文版本-点击这里-red.svg)](QUICK_START.md)

## Table of Contents
- [Installation](#installation)
- [Creating Your First Area](#creating-your-first-area)
- [Command Reference](#command-reference)
- [Common Use Cases](#common-use-cases)
- [Troubleshooting](#troubleshooting)

## Installation

### Prerequisites
- Minecraft 1.20.1
- Forge installed
- Server operator (OP) permissions

### Steps
1. Download the AreaMonitor mod JAR file
2. Place it in your `mods` folder
3. Start the server

## Creating Your First Area

### Step 1: Get Selection Tool
```
/areamonitor visual tool
```

### Step 2: Select Area
1. Hold the wooden axe, right-click the first corner
2. Right-click the opposite corner
3. Review the area info to confirm

### Step 3: Create Area
```
/areamonitor selection create my_creative_zone
```

### Step 4: Configure Game Modes
```
/areamonitor area setEnterMode my_creative_zone creative
/areamonitor area setLeaveMode my_creative_zone survival
```

### Step 5: Test
Walk into area → Auto switch to creative mode
Walk out of area → Auto switch to survival mode

## Command Reference

### Basic Commands
| Command | Description |
|---------|-------------|
| `/areamonitor status` | Show monitoring status |
| `/areamonitor help` | Show all commands |
| `/areamonitor toggle` | Toggle monitoring |
| `/areamonitor reload` | Reload configuration |
| `/areamonitor save` | Save configuration |

### Area Management
| Command | Description |
|---------|-------------|
| `/areamonitor area list` | List all areas |
| `/areamonitor area info <name>` | Show area details |
| `/areamonitor area delete <name>` | Delete area |
| `/areamonitor area toggle <name>` | Toggle area |
| `/areamonitor area setEnterMode <area> <mode>` | Set enter mode |
| `/areamonitor area setLeaveMode <area> <mode>` | Set leave mode |

### Visual Tools
| Command | Description |
|---------|-------------|
| `/areamonitor visual tool` | Get selection tool |
| `/areamonitor visual show <area>` | Show area boundaries |
| `/areamonitor visual hide` | Hide area boundaries |

### Selection Tools
| Command | Description |
|---------|-------------|
| `/areamonitor selection create <name>` | Create area from selection |
| `/areamonitor selection cancel` | Cancel selection |
| `/areamonitor selection info` | Show selection info |
| `/areamonitor selection tutorial` | Show tutorial |

### Whitelist Management
| Command | Description |
|---------|-------------|
| `/areamonitor whitelist add <player>` | Add player |
| `/areamonitor whitelist remove <player>` | Remove player |
| `/areamonitor whitelist list` | List whitelist |
| `/areamonitor whitelist clear` | Clear whitelist |

### Blacklist Management
| Command | Description |
|---------|-------------|
| `/areamonitor blacklist info` | Show current restrictions |
| `/areamonitor blacklist area <area> add <item>` | Add blacklisted item |
| `/areamonitor blacklist area <area> remove <item>` | Remove blacklisted item |
| `/areamonitor blacklist area <area> list` | List blacklist |
| `/areamonitor blacklist area <area> toggle` | Toggle blacklist |

### Language Settings
| Command | Description |
|---------|-------------|
| `/areamonitor language` | Show current language |
| `/areamonitor language en` | Switch to English |
| `/areamonitor language zh` | Switch to Chinese |

## Common Use Cases

### Creative Building Zone
```
/areamonitor selection create creative_zone
/areamonitor area setEnterMode creative_zone creative
/areamonitor area setLeaveMode creative_zone survival
```

### PVP Arena
```
/areamonitor selection create pvp_arena
/areamonitor area setEnterMode pvp_arena adventure
/areamonitor area setLeaveMode pvp_arena survival
```

### Protected Area
```
/areamonitor selection create spawn_protection
/areamonitor area setEnterMode spawn_protection adventure
/areamonitor area setLeaveMode spawn_protection survival
```

## Troubleshooting

### Selection Tool Not Working
- Ensure you have OP permissions
- Make sure the tool is in your main hand
- Get a new tool: `/areamonitor visual tool`

### Area Not Triggering
- Check area size (≤1000x1000 blocks)
- Verify you're in the correct dimension
- Check if you're on the whitelist

### Language Issues
- Switch language: `/areamonitor language en`
- Check the lang folder

## Getting Help

- Use `/areamonitor help` for commands