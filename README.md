# ModernWaystones (originally forked)
A plugin that adds Waystones for traveling. This plugin was originally forked from [a fork](https://github.com/SkyboundLab/QuickWaystones) of a [QuickWaystones plugin](https://github.com/Pozzoo/QuickWaystones)
that I used as the base for this, since I rewrote a lot of its original code, I decided to make this repository standalone and not a fork.

The plugin was tested with 1.21.7 version, but should work with 1.21.6 (the version when dialogs were introduced)

## Features:
- Custom craftable waystone.
- Renamable waystones with a nametag.
- Very configurable, player and server-wise! To access your waystone (as a player) settings, shift and right-click on any waystone.
- BEDROCK SUPPORT! It uses floodgate API, so be sure to have that plugin installed.
  - Floodgate is not really required, Geyser already translates Java dialogs to Bedrock forms, but they are not going to look pretty and some actions are a little buggy.
- Some redstone activations are implemented (open up your waystone menu on wooden/stone pressure plate press).
- Uses minecraft's new dialog system.
  - For bedrock, forms are used but since they are limited, features like manual sorting are cut down.
- Customizable sounds when waystone is placed, broken or when a player is teleporting to it.
- Sort waystones listing by either name, when it was made, player's name or manually (by moving them yourself!).
- Player waystone settings — change waystones numbering, hide coordinates, attributes visibility, waystone button width or columns.
- Waystone's visibility — public (default) or private, can be changed by special item (echo shard).
  - If your waystone is private, you can add players to it so they could teleport to it (works by placing a special block below waystone, default is target block), configurable via right-clicking special block.
- Server's waystones — players with operator permissions can make the waystone global, meaning it would behave as /warp.
- Delays before teleporting and between waystone uses.
- Limited waystones — limit how many waystones a player can have and what min distance should be between their other waystones.
- Teleportation sickness — give players effects (based on chances) as if teleportation could have an impact on player's mentality.
- Expirable waystones — if another player destroys your waystone, it is still shown in your waystones list but is deleted after some time.

### Features I'm considering adding or fixing some things
- Add a setting to switch back to inventory's GUI (this would allow support for older minecraft versions, <1.21.6).
- Do not use player's name in waystone configuration (I hate working with offline players, like getting their usernames).

## Screenshots
soon.

## Default configurations
soon.

## Notes
- Be careful editing configuration, proper error handling is not implemented (yet?)

<details>
  <summary>Original stuff before becoming standalone</summary>

### Changes over original (by SkyboundLab)
- **More Saving:** Instead of saving once on server shutdown, it will also save on adding and removing a waystone.
- **Bigger Menus:** Instead of a single chest, it now uses a double chest so you can see more per page. (removed feature as dialogs are used)
- **UUIDs for IDs:** Allows multiple waystones to have the same name and not override each other.
- **World's specific icons:** The 3 dimensions in Minecraft have their own block as the icon in the menu. (removed feature as dialogs are used)

## Below is the original readme text from forked plugin.

### Features:
- Right-click a lodestone to activate it as a waypoint
- Renamable waypoints
- Unlimited paginated Waystone GUI
- New crafting for lodestone, making it more accessible:

![image](https://github.com/Pozzoo/QuickWaystones/assets/73541474/003effe1-ae79-4061-89d9-90a1d5fcb4a6)

## Right-click with a nametag to rename!
![2024-05-01-21-07-00_2-min-ezgif com-optimize](https://github.com/Pozzoo/QuickWaystones/assets/73541474/955b7f93-f440-461e-93fb-2a2d4c547636)

## Quick travel!
![2024-05-01-21-07-00_1-min-optimize](https://github.com/Pozzoo/QuickWaystones/assets/73541474/b5f62abe-7e4e-446a-8362-80ff5fc40151)


</details>