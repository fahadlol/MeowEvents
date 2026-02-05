# MeowMCEvents

A feature-rich PvP event plugin for Minecraft servers running Paper/Spigot/Purpur 1.21.4+.

## Features

### Event System
- **Countdown-based joining**: Players can join during a configurable countdown period
- **Multiple game modes**: Solo (FFA) or Team modes (2v2, 3v3, 4v4, 5v5)
- **Automatic winner detection**: Detects when one player/team remains
- **Force start**: Admins can force start events early with minimum players
- **Mid-join spectating**: Players can join ongoing events as spectators

### Arena System
- **Multiple arenas**: Create and manage multiple event arenas
- **Arena boundaries**: Define arena bounds with pos1/pos2 selection
- **Damage zones**: Configurable damage near arena edges
- **Push-back system**: Optionally push players back instead of damage

### Team System
- **Random team assignment**: Players are randomly shuffled into teams
- **Team colors**: 5 distinct team colors (RED, BLUE, GREEN, YELLOW, AQUA)
- **Auto-balancing**: Teams are automatically rebalanced when players leave
- **Friendly fire toggle**: Enable/disable team damage
- **Team nametags**: Show team names above players

### Spectator Features
- **Multiple spectator modes**: Adventure mode (visible) or Spectator mode (invisible)
- **Spectator compass**: Track and teleport to alive players
- **Player cycling**: Right-click to cycle through players, Shift+Right for previous
- **GUI menu**: Left-click compass to open player selection GUI
- **Health display**: See player health in the spectator GUI
- **Spectator protection**: Grace period prevents damage when becoming spectator
- **Leave dye**: Quick exit button in hotbar

### Border System
- **Shrinking border**: Configurable shrink interval, amount, and minimum size
- **Border damage**: Configurable damage per second outside border
- **Warning system**: Players warned before border shrinks
- **Original border restoration**: Border resets when event ends

### Combat System
- **Kill feed**: Broadcasts kills to all participants
- **Kill streaks**: Track and announce kill streaks
- **Damage tracking**: Accurate kill attribution even for environmental deaths
- **Grace period**: Optional PvP grace period at event start
- **Combat tag**: Optional combat tagging system
- **Self-damage toggle**: Control ender pearl and own explosion damage

### Visual Feedback
- **Scoreboard**: Real-time event scoreboard with players alive, kills, etc.
- **Tab list**: Custom tab list during events
- **Boss bar**: Event status in boss bar
- **Action bar**: Kill/death messages in action bar
- **Titles**: Countdown, death, and winner titles
- **Particles**: Visual effects on kills and victories
- **Celebration effects**: Winner celebration with fireworks

### Kit Integration
- **External kit support**: Works with XyrisKits, EssentialsX, or custom kit plugins
- **Kit selection GUI**: Easy kit selection through GUI
- **Configurable command**: Customize the kit give command format
- **Automatic distribution**: Selected kit given to all players when event starts

### Admin Features
- **GUI-based management**: Full event control through intuitive GUI
- **Game rule toggles**: Building, breaking, regen, fall damage, hunger, etc.
- **Player elimination**: Manually eliminate players from GUI
- **Multiple spawn points**: Arena spawn, waiting area, and respawn locations
- **Command whitelist/blacklist**: Control which commands work during events
- **Debug mode**: Comprehensive logging for troubleshooting

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/meowevent` | Opens the event management GUI | `meowevent.use` |
| `/meowevent start` | Start event countdown | `meowevent.admin` |
| `/meowevent stop` | Stop current event/countdown | `meowevent.admin` |
| `/meowevent forcestart` | Force start with current players | `meowevent.admin` |
| `/meowevent setspawn` | Set event arena spawn | `meowevent.admin` |
| `/meowevent setevent` | Set waiting area spawn | `meowevent.admin` |
| `/meowevent setplayerspawn` | Set respawn location | `meowevent.admin` |
| `/meowevent team <1-5>` | Set game mode (1=Solo, 2-5=Teams) | `meowevent.admin` |
| `/meowevent border <seconds>` | Set border shrink interval | `meowevent.admin` |
| `/meowevent reload` | Reload configuration | `meowevent.admin` |
| `/meowevent debug` | Toggle debug mode | `meowevent.admin` |
| `/meowevents` | Player help and event info | `meowevent.help` |
| `/event` | Join an active event | `meowevent.join` |
| `/eventspectate` | Spectate an ongoing event | `meowevent.spectate` |
| `/eventleave` | Leave the event or stop spectating | `meowevent.leave` |
| `/kits` | Open kit selection GUI | `meowevent.admin` |
| `/arena create <name>` | Create a new arena | `meowevent.admin` |
| `/arena pos1` | Set arena corner 1 | `meowevent.admin` |
| `/arena pos2` | Set arena corner 2 | `meowevent.admin` |
| `/arena delete <name>` | Delete an arena | `meowevent.admin` |
| `/arena list` | List all arenas | `meowevent.admin` |
| `/arena set <name>` | Set active arena | `meowevent.admin` |
| `/arena info [name]` | Show arena information | `meowevent.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `meowevent.use` | Access to event GUI and basic commands | OP |
| `meowevent.join` | Ability to join events | true |
| `meowevent.leave` | Ability to leave events | true |
| `meowevent.spectate` | Ability to spectate events | true |
| `meowevent.help` | Access to help command | true |
| `meowevent.admin` | Full admin access | OP |

## Configuration

The plugin has extensive configuration options in `config.yml`:

### Event Settings
```yaml
event:
  min-players: 2              # Minimum players to start
  max-players: 0              # Maximum players (0 = unlimited)
  countdown-seconds: 60       # Countdown duration
  default-mode: 1             # Default team size (1=Solo)
  broadcast-to-server: true   # Broadcast events server-wide
  allow-mid-join-spectate: true
```

### Border Settings
```yaml
border:
  enabled: true
  start-size: 50
  shrink-to: 10
  interval-seconds: 30
  shrink-amount: 5
  damage-per-second: 1.0
  warning-time: 5
```

### PvP Settings
```yaml
pvp:
  grace-period-seconds: 0     # PvP grace period
  friendly-fire: false        # Team damage
  self-damage: true           # Ender pearl damage, etc.
  combat-tag-seconds: 0       # Combat tagging
```

### Game Rules
```yaml
game:
  allow-building: false
  allow-breaking: false
  allow-natural-regen: true
  disable-fall-damage: false
  keep-inventory: true
  clear-drops-on-death: true
  disable-hunger: false
```

### Spectator Settings
```yaml
spectator:
  death-delay-ticks: 0
  gamemode: ADVENTURE         # ADVENTURE or SPECTATOR
  compass-teleport: true
  flight-speed: 0.2
  grace-period-ticks: 60
```

### Kit System
```yaml
kits:
  - Warrior
  - Archer
  - Tank
selected-kit: Warrior
kit-command: "ekits give %player% %kit%"
```

## Installation

1. Download the latest release
2. Place `MeowMCEvents.jar` in your `plugins` folder
3. (Optional) Install a kit plugin (XyrisKits, EssentialsX, etc.)
4. Restart your server
5. Configure spawn locations:
   - `/meowevent setspawn` - Arena spawn point
   - `/meowevent setevent` - Waiting/queue area
   - `/meowevent setplayerspawn` - Return spawn after event
6. (Optional) Create arenas with `/arena create <name>`
7. Edit `config.yml` to customize settings

## Dependencies

- **Required**: Paper/Spigot/Purpur 1.21.4+
- **Optional**: XyrisKits or any kit plugin for kit distribution

## Event Flow

1. Admin starts countdown with `/meowevent start` or GUI
2. Players join using `/event` during countdown
3. When countdown ends:
   - Players teleported to arena
   - Kits distributed
   - Border initialized
   - PvP enabled (after optional grace period)
4. Border shrinks at configured intervals
5. Players fight until one player/team remains
6. Winner announced with statistics and celebration
7. All players teleported back to spawn

## Statistics Tracking

- **Placements**: 1st, 2nd, 3rd, etc. based on elimination order
- **Kill counts**: Track kills per player during event
- **Kill streaks**: Track consecutive kills
- **Most kills**: Special recognition for top killer
- **Team stats**: Stats tracked per team in team mode

## License

MIT License - Feel free to use and modify!

## Support

For issues and feature requests, please open an issue on GitHub.

**Server**: meowmc.fun
