# MeowMCEvents

A feature-rich PvP event plugin for Minecraft servers running Paper/Spigot/Purpur 1.21.4+.

## Features

### Event System
- **Countdown-based joining**: Players can join during a configurable countdown period
- **Multiple game modes**: Solo (FFA) or Team modes (2v2, 3v3, 4v4)
- **Automatic winner detection**: Detects when one player/team remains
- **Spectator system**: Dead players become spectators with a tracking compass

### Team System
- **Random team assignment**: Players are randomly shuffled into teams
- **Team colors**: 8 distinct team colors for easy identification
- **Auto-balancing**: Teams are automatically rebalanced when players leave
- **Team-based win conditions**: Last team with alive members wins

### Spectator Features
- **Spectator compass**: Track and teleport to alive players
- **Player cycling**: Right-click to cycle through players, Shift+Right for previous
- **GUI menu**: Left-click compass to open player selection GUI
- **Health display**: See player health in the spectator GUI

### Border System
- **Shrinking border**: Configurable shrink interval and minimum size
- **Original border restoration**: Border resets to original settings when event ends
- **Broadcast messages**: Players are notified when border shrinks

### Kit Integration
- **XyrisKits integration**: Seamlessly works with XyrisKits plugin
- **Kit selection GUI**: Easy kit selection through GUI
- **Automatic distribution**: Selected kit given to all players when event starts

### Admin Features
- **GUI-based management**: Full event control through intuitive GUI
- **Toggle settings**: Building, breaking, and natural regen toggles
- **Player elimination**: Manually eliminate players from GUI
- **Multiple spawn points**: Separate spawns for event arena, waiting area, and respawn

### Security Features
- **Command injection protection**: Kit names are sanitized before execution
- **Race condition prevention**: Thread-safe winner detection
- **Item duplication prevention**: Inventories cleared on death/quit
- **Permission system**: Fine-grained permission control

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/meowevent` | Opens the event management GUI | `meowevent.use` |
| `/meowevent start` | Start event countdown | `meowevent.admin` |
| `/meowevent stop` | Stop current event/countdown | `meowevent.admin` |
| `/meowevent setspawn` | Set event arena spawn | `meowevent.admin` |
| `/meowevent setevent` | Set waiting area spawn | `meowevent.admin` |
| `/meowevent setplayerspawn` | Set respawn location | `meowevent.admin` |
| `/meowevent team <1-5>` | Set game mode (1=Solo, 2-5=Teams) | `meowevent.admin` |
| `/meowevent border <seconds>` | Set border shrink interval | `meowevent.admin` |
| `/meowevent reload` | Reload configuration | `meowevent.admin` |
| `/meowevent debug` | Toggle debug mode | `meowevent.admin` |
| `/event` | Join an active event or spectate | `meowevent.join` |
| `/leave` | Leave the event or stop spectating | - |
| `/kits` | Open kit selection GUI | `meowevent.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `meowevent.use` | Access to basic event commands | OP |
| `meowevent.join` | Ability to join events | true |
| `meowevent.admin` | Full admin access | OP |

## Configuration

```yaml
# Border settings
border:
  start-size: 50        # Starting border size
  shrink-to: 10         # Minimum border size
  interval-seconds: 30  # Shrink interval in seconds

# Game settings
game:
  allow-building: false      # Allow block placement
  allow-breaking: false      # Allow block breaking
  allow-natural-regen: true  # Allow natural health regen

# Kit system (requires XyrisKits)
kits:
  - Warrior
  - Archer
  - Tank
  - Mage
  - Assassin
selected-kit: Warrior

# Advanced settings
advanced:
  default-mode: 1           # Default game mode (1=Solo)
  min-players: 2            # Minimum players to start
  countdown-seconds: 60     # Countdown duration

# Debug settings
debug:
  enabled: false
  log-events: true
  log-players: true
  log-teams: true
  log-border: true
  log-kits: true
  log-gui: true
```

## Installation

1. Download the latest release
2. Place `MeowMCEvents-1.0.jar` in your `plugins` folder
3. (Optional) Install XyrisKits for kit functionality
4. Restart your server
5. Configure spawn locations using `/meowevent setspawn`, `/meowevent setevent`, `/meowevent setplayerspawn`
6. Edit `config.yml` to customize settings

## Dependencies

- **Required**: Paper/Spigot/Purpur 1.21.4+
- **Optional**: XyrisKits (for kit distribution)

## Event Flow

1. Admin starts countdown with `/meowevent start` or GUI
2. Players join using `/event` during countdown
3. When countdown ends, players are teleported to arena and given kits
4. Border begins shrinking at configured intervals
5. Players fight until one player/team remains
6. Winner is announced with statistics
7. All players teleported back to spawn

## Statistics Tracking

- **Placements**: 1st, 2nd, 3rd, etc. based on elimination order
- **Kill counts**: Track kills per player
- **Most kills**: Special recognition for top killer
- **Team-based stats**: Stats tracked per team in team mode

## Bug Fixes (v1.0.1)

- Fixed spectator compass not being cleared when event ends
- Fixed spectator inventory not cleared when using /leave
- Fixed potential crash when configured world doesn't exist
- Fixed missing permission check for /event command
- Fixed spectators potentially able to place/break blocks
- Fixed memory leak in rank tracking
- Fixed potential crash with empty world list

## License

MIT License - Feel free to use and modify!

## Support

For issues and feature requests, please open an issue on GitHub.
