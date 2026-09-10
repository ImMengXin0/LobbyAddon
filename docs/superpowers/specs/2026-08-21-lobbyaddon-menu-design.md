# LobbyAddon Personal Menu Design

## Goal

Upgrade `/lobbyaddon menu` from a preset-only shortcut into a polished personal-settings menu that exposes nickname, custom prefix, suffix, and chat-color features without removing the existing `/cosmetics` preset menu.

## Interaction

- `/lobbyaddon menu` opens a 27-slot main menu with a player summary and four feature entries.
- Nickname, custom prefix, and suffix entries open focused 27-slot menus. Players can choose permanent, 30-minute, 1-hour, or 1-day input, reset the value, visit the configured profile presets, or return.
- Free-form values are collected from the player's next chat message. `取消` or `cancel` aborts, and requests expire after 60 seconds.
- Chat color opens a 54-slot menu with the 16 configured legacy colors, configured presets, Hex and gradient chat-input actions, format toggles, preview, reset, and return.
- Menu items show current state, permission or disabled state, and concise click instructions. Decorative panes frame the actionable items.

## Architecture

- Add `PersonalMenuService` to build menus and start or cancel pending chat input.
- Add `PersonalMenuHolder` to identify each menu page and map slots to typed actions.
- Add `PersonalMenuListener` to cancel inventory movement, route clicks, intercept pending chat input, handle quit cleanup, and dispatch validated existing commands on the main thread.
- Keep `PresetMenuService` and `/cosmetics` intact. Add a preset-menu back target so profile submenus can open existing configured presets and return cleanly.
- `LobbyCommand` opens the new main menu while preserving its current player and permission checks.

## Data And Safety

- The listener dispatches `nick`, `custom`, `suffix`, and `chatcolor` through Bukkit as the player. Existing command implementations remain the source of truth for permissions, validation, cooldowns, approval workflow, storage, previews, and messages.
- Pending input is keyed by UUID, has a scheduler-backed timeout, and is removed on completion, cancellation, quit, reload, or plugin shutdown.
- Input is passed as a command argument only after rejecting control characters and leading slash input. Bukkit receives the complete command line directly.

## Compatibility

- Use only APIs available from the project's Spigot 1.8.8 compile target.
- Use legacy materials and data values for colored panes and wool.
- Hex and gradient actions remain visible only when enabled; the existing chat-color command handles server-version fallback.

## Testing

- Add pure unit tests for action command construction, duration choices, cancellation words, and unsafe chat input.
- Run the full Gradle test suite and build the shaded plugin JAR.
- Final live-server checks cover all menu pages, permissions, chat cancellation and timeout, reset paths, presets, reload behavior, and item-movement prevention.
