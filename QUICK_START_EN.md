# Quick Start Guide - AreaMonitor Mod

[![中文版本](https://img.shields.io/badge/中文版本-点击这里-red.svg)](QUICK_START.md)

## Table of Contents
- [Installation](#installation)
- [Creating Your First Area](#creating-your-first-area)
- [Advanced Usage](#advanced-usage)
- [Troubleshooting](#troubleshooting)

## Installation

### Prerequisites
- Minecraft 1.20.1
- Forge installed and working
- Server operator (OP) permissions

### Steps
1. Download the AreaMonitor mod JAR file
2. Place it in your Minecraft `mods` folder
3. Start your Minecraft client or server
4. Verify the mod is loaded by checking the logs for "AreaMonitor" entries

## Creating Your First Area

### Step 1: Get the Selection Tool
```
/areamonitor visual tool
```
This gives you a wooden axe that you'll use to select your area.

### Step 2: Select the First Corner
1. Hold the wooden axe in your main hand
2. Right-click on the first corner block of your desired area
3. You'll see a green message confirming the first point

### Step 3: Select the Second Corner
1. Continue holding the wooden axe
2. Right-click on the opposite corner block
3. You'll see detailed area information including:
   - Coordinates of both points
   - Area size in blocks
   - Current dimension

### Step 4: Create the Area
```
/areamonitor selection create my_creative_zone
```
Replace `my_creative_zone` with your desired area name.

### Step 5: Configure Game Modes
Set the game mode for when players enter the area:
```
/areamonitor area setEnterMode my_creative_zone creative
```

Set the game mode for when players leave the area:
```
/areamonitor area setLeaveMode my_creative_zone survival
```

### Step 6: Test Your Area
1. Walk into the area - you should automatically switch to creative mode
2. Walk out of the area - you should automatically switch back to survival mode
3. You'll see on-screen messages confirming the transitions

## Advanced Usage

### Adding Players to Whitelist
Players on the whitelist won't be affected by area monitoring:
```
/areamonitor whitelist add PlayerName
```

### Viewing Area Information
```
/areamonitor area info my_creative_zone
```

### Visualizing Area Boundaries
```
/areamonitor visual show my_creative_zone
```

### Managing Multiple Areas
You can create multiple areas with different settings:
```
/areamonitor selection create pvp_arena
/areamonitor area setEnterMode pvp_arena adventure
/areamonitor area setLeaveMode pvp_arena survival
```

### Language Switching
```
/areamonitor language en  # Switch to English
/areamonitor language zh  # Switch to Chinese
```

## Common Use Cases

### Creative Building Zone
```
/areamonitor selection create creative_plot
/areamonitor area setEnterMode creative_plot creative
/areamonitor area setLeaveMode creative_plot survival
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
- Make sure you're holding the tool in your main hand
- Try getting a new tool: `/areamonitor visual tool`

### Area Not Triggering
- Check area size (must be ≤1000x1000 blocks)
- Verify you're in the correct dimension
- Check if you're on the whitelist
- Look at server logs for error messages

### Language Issues
- Switch language and restart if text appears incorrectly
- Check that both language files are present in the lang folder

### Performance Issues
- Reduce the number of active areas
- Increase the check interval in configuration
- Remove very large areas (>100000 blocks)

## Tips and Best Practices

1. **Start Small**: Begin with small areas to test functionality
2. **Use Descriptive Names**: Choose clear, memorable area names
3. **Test Thoroughly**: Always test areas with different game modes
4. **Monitor Performance**: Watch server TPS when using many areas
5. **Backup Configs**: Regularly backup your area configurations
6. **Document Areas**: Keep notes on what each area is for

## Getting Help

- Use `/areamonitor help` for command reference
- Check server logs for error messages
- Create an issue on the GitHub repository
- Review the full documentation