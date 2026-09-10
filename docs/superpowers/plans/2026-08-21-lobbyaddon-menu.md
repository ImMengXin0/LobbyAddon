# LobbyAddon Personal Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a polished `/lobbyaddon menu` workflow for nickname, custom prefix, suffix, and chat color features.

**Architecture:** A menu service owns inventory construction and pending chat input, typed holders describe pages and slot actions, and one listener routes inventory/chat events. Existing commands remain the behavior boundary so all current validation, permissions, cooldowns, approval, and persistence continue to apply.

**Tech Stack:** Java 8, Spigot API 1.8.8, JUnit Jupiter 5, Gradle

---

### Task 1: Input Rules

**Files:**
- Create: `src/main/java/cn/mx/lobbyaddon/menu/PersonalMenuInput.java`
- Test: `src/test/java/cn/mx/lobbyaddon/menu/PersonalMenuInputTest.java`

- [ ] Write tests proving cancellation aliases, unsafe input rejection, profile command construction, Hex command construction, and gradient parsing.
- [ ] Run `./gradlew test --tests cn.mx.lobbyaddon.menu.PersonalMenuInputTest` and verify failure because the type is missing.
- [ ] Implement the minimal pure input helper.
- [ ] Re-run the focused test and verify it passes.

### Task 2: Menu Model And Rendering

**Files:**
- Create: `src/main/java/cn/mx/lobbyaddon/menu/PersonalMenuHolder.java`
- Create: `src/main/java/cn/mx/lobbyaddon/menu/PersonalMenuService.java`
- Modify: `src/main/java/cn/mx/lobbyaddon/menu/PresetMenuHolder.java`
- Modify: `src/main/java/cn/mx/lobbyaddon/menu/PresetMenuService.java`

- [ ] Add typed pages and actions for the main, profile, and chat-color menus.
- [ ] Build stable 27-slot and 54-slot layouts with legacy-compatible materials and data values.
- [ ] Render current state, feature availability, permissions, reset, return, profile presets, chat colors, configured chat presets, formats, Hex, gradient, and preview controls.
- [ ] Add pending-input lifecycle with a 60-second timeout.
- [ ] Add an overload allowing the existing preset menu to return to the originating profile page.

### Task 3: Events And Command Routing

**Files:**
- Create: `src/main/java/cn/mx/lobbyaddon/listener/PersonalMenuListener.java`
- Modify: `src/main/java/cn/mx/lobbyaddon/listener/PresetMenuListener.java`

- [ ] Cancel all click and drag movement in owned menus.
- [ ] Route menu actions, preserving existing permission and enabled-state checks.
- [ ] Capture pending chat input, handle cancel and invalid input, and dispatch accepted commands on the server thread.
- [ ] Clear pending input on player quit and provide preset-menu back navigation.

### Task 4: Plugin Wiring And Configuration

**Files:**
- Modify: `src/main/java/cn/mx/lobbyaddon/LobbyAddon.java`
- Modify: `src/main/java/cn/mx/lobbyaddon/command/LobbyCommand.java`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/resources/plugin.yml`
- Modify: `README.md`

- [ ] Construct and expose the personal menu service during load.
- [ ] Register and unregister the personal menu listener across reloads.
- [ ] Open the new main menu from `/lobbyaddon menu` while retaining `/cosmetics` for direct preset access.
- [ ] Add configurable title and input-timeout defaults and update command descriptions.

### Task 5: Verification

**Files:**
- Verify: all modified source and resource files

- [ ] Run `./gradlew test` and resolve all failures.
- [ ] Run `./gradlew clean build` and verify the plugin JAR is produced.
- [ ] Inspect the built JAR for the new classes and updated resources.
- [ ] Record live-server acceptance checks for the user.
