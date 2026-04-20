# Tleilax

Real-time 2D ecosystem sandbox simulation for Android.

## Documentation

Project documentation, final report, and diagrams:

- [https://oliynykmax.github.io/Tleilax/](https://oliynykmax.github.io/Tleilax/)

## What is it?

Tleilax is an Android app where the user generates a living world and watches it evolve in real time.
Predators hunt prey, herbivores search for food, plants grow and die, and the player can interfere while the simulation is running.

The current implementation includes:

- a fixed `256x256` world
- layered tiles for terrain, grass, plants, and animals
- real-time tick-based simulation
- pan and zoom canvas rendering
- save/load through Room
- live population statistics
- runtime events such as disaster and predator frenzy

## Team

- Maksym Oliinyk
- Maia Salin
- Ejota Elezaj

## Tech Stack

- Java 21, Android Studio
- Min SDK 28 / Target SDK 36
- Room (data persistence)
- MPAndroidChart (statistics visualization)

## Features

- Layered 2D ecosystem with predators, prey, grass, berry bushes, and trees
- Real-time grid-based simulation
- Species picker with tap-to-place interaction during simulation
- Disaster and predator frenzy live events
- Live population statistics and charts
- Pan and zoom simulation canvas
- Save and load simulation states
- Per-species behavior rules with unique traits
- Settings for presentation and world generation defaults

## Build

```bash
./gradlew assembleDebug
```

Install on a connected device or emulator:

```bash
./gradlew installDebug
```

Run unit-test verification:

```bash
./gradlew testDebugUnitTest
```

## Project Structure

```text
app/
├── model/          Entity, Animal, Predator, Prey, Plant
├── simulation/     SimulationEngine, Grid, TickLogic, PlantState, snapshots
├── storage/        Room database, save/load, snapshot codec
├── ui/
│   ├── fragments/  SimulationFragment, StatsFragment, SaveLoadFragment, SettingsFragment
│   ├── adapters/   RecyclerView adapters
│   └── view/       SimulationCanvasView, TextureLibrary
└── utils/          AppSettings, StatTracker, helpers
```

## Status

Final course project submission.

## Asset Credits

- Trees and bushes used for `plant_bush_*` and `plant_tree_*` are derived from `Trees & Bushes` by ansimuz, licensed `CC0`:
  https://opengameart.org/content/trees-bushes
- Grass used for `ground_grass` is derived from `Overworld - Grass Biome` by Beast, licensed `CC0`:
  https://opengameart.org/content/overworld-grass-biome
- Animal sprites used for `animal_wolf`, `animal_rabbit`, `animal_mouse`, and `animal_deer` are derived from `Tiny Creatures` by Clint Bellanger, licensed `CC0`:
  https://opengameart.org/content/tiny-creatures
- Some in-game textures are edited from the source packs above, including recolors, transparency cleanup, dead-state variants, and crop/scale adjustments.
