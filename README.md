# Tleilax

Real-time 2D ecosystem sandbox simulation for Android.

### 📖 Documentation
The full project plan, architectural diagrams, and UI wireframes are available at:
**[https://oliynykmax.github.io/Tleilax/](https://oliynykmax.github.io/Tleilax/)**

---
 🌿

> A real-time ecosystem sandbox simulation for Android.
> The name is inspired by the Bene Tleilax from Frank Herbert's *Dune* — a faction known for biological engineering and creating new life forms.

## What is it?

Tleilax is an Android application where you configure a living world and watch it evolve.
Generate a world, press play, and observe.
Predators hunt, prey flee, plants spread. No one programs the outcome; it emerges on its own.

## Team

- Maksym
- Maia
- Ejota

## Tech Stack

- Java, Android Studio
- Min SDK 28 / Target SDK 36
- Room (data persistence)
- MPAndroidChart (statistics visualization)

## Features

- Layered 2D ecosystem with predators, prey, grass, berry bushes, and trees
- Real-time grid-based simulation
- Species picker with tap-to-place interaction during simulation
- Live population statistics and charts
- Pan and zoom simulation canvas
- Save and load simulation states
- Per-species behavior rules with unique traits
- Settings for presentation and world generation

## Project Structure

```
app/
├── model/          Entity, Animal, Predator, Prey, Plant
├── simulation/     SimulationEngine, Grid, tick logic
├── storage/        Room database, save/load
├── ui/
│   ├── fragments/  SimulationFragment, StatsFragment, SaveLoadFragment, SettingsFragment
│   └── adapters/   RecyclerView adapters
└── utils/          StatTracker, helpers
```

## Status

🚧 In development — OOP course project, deadline April 20, 2026.

## Asset Credits

- Trees and bushes used for `plant_bush_*` and `plant_tree_*` are derived from `Trees & Bushes` by ansimuz, licensed `CC0`:
  https://opengameart.org/content/trees-bushes
- Grass used for `ground_grass` is derived from `Overworld - Grass Biome` by Beast, licensed `CC0`:
  https://opengameart.org/content/overworld-grass-biome
- Animal sprites used for `animal_wolf`, `animal_rabbit`, `animal_mouse`, and `animal_deer` are derived from `Tiny Creatures` by Clint Bellanger, licensed `CC0`:
  https://opengameart.org/content/tiny-creatures
- Some in-game textures are edited from the source packs above, including recolors, transparency cleanup, dead-state variants, and crop/scale adjustments.
