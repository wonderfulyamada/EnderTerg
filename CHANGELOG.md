# Changelog

## [1.1.0] - 2026-09-28

### Added
- Configurable `entityBlacklist` for entity registry IDs that cannot be bound.

### Fixed
- Prevent duplicate bound entities when the original entity's chunk was unloaded during recall.
- Preserve the previous bound entity if replacement spawning fails.

## [1.0.0] - 2026-08-21

### Added
- Ender Terg item with a shapeless Name Tag and Ender Pearl recipe.
- Anvil naming and binding to non-player living entities.
- NBT-backed entity recall across unloaded chunks and dimensions.
- Duplicate-instance prevention during recall.
- Enderman teleport sound on successful recall.
