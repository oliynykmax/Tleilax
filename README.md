# Tleilax 🌿

> A real-time ecosystem sandbox simulation for Android.

## What is it?

Tleilax is an Android application where you configure a living world and watch it evolve.
Set up a starting environment — species, population sizes, terrain — press play, and observe.
Predators hunt, prey flee, plants spread. No one programs the outcome; it emerges on its own.

## Team

- Maksym
- Maia
- Ejota

## Tech Stack

- Java, Android Studio
- Min SDK 28 / Target SDK 36
- Room (data persistence)
- AnyChart-Android (statistics visualization)

## Features

- Configurable ecosystem with multiple species
- Real-time grid-based simulation
- Live population statistics and charts
- Mid-simulation user intervention (add/remove entities, trigger events)
- Save and load simulation states
- Per-species behavior rules with unique traits

## Project Structure

```
app/
├── model/          Entity, Animal, Predator, Prey, Plant
├── simulation/     SimulationEngine, Grid, tick logic
├── storage/        Room database, save/load
├── ui/
│   ├── fragments/  SimulationFragment, StatsFragment, ConfigFragment
│   └── adapters/   RecyclerView adapters
└── utils/          StatTracker, helpers
```

## Status

🚧 In development — OOP course project, deadline April 20, 2026.
