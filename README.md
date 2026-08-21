# Ender Terg

Ender Terg binds a named living mob to a reusable tag and recalls it to you.
Its saved entity NBT lets recall work even when the original chunk is unloaded
or the mob is in another dimension.

## Requirements

- Minecraft 1.12.2
- Forge 14.23.5.2847 or compatible Forge for Minecraft 1.12.2

## Recipe

Craft an Ender Terg with a Name Tag and an Ender Pearl.

## Usage

1. Rename the Ender Terg in an anvil.
2. Right-click a non-player mob with the renamed Ender Terg to bind it.
3. Right-click while holding the Ender Terg in the air to recall that mob.

Recall reconstructs the bound entity from its stored NBT at your current
location and dimension. This preserves supported entity data such as its name,
health, equipment, inventory, AI state, and modded NBT data.

## Installation

1. Install Minecraft Forge for 1.12.2.
2. Download the Ender Terg jar from the release page.
3. Place the jar in your Minecraft instance's `mods` folder.
4. Launch Minecraft with the Forge profile.

## License

Ender Terg is licensed under the [MIT License](LICENSE).
