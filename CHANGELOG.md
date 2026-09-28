# Changelog

## [1.0.1] - 2026-09-28

### Fixed
- Prevented duplicate bound mobs when a previously unloaded instance is loaded after recall.
- Kept the previous live mob intact if reconstructing or spawning the recalled replacement fails.

## [1.0.0] - 2026-08-21

### Added
- Ender Terg item with a shapeless Name Tag and Ender Pearl recipe.
- Anvil naming and binding to non-player living entities.
- NBT-backed entity recall across unloaded chunks and dimensions.
- Duplicate-instance prevention during recall.
- Enderman teleport sound on successful recall.
