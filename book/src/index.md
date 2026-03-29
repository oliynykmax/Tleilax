# Project Plan — Tleilax
**OOP Course — CT60A4001**
**Team:** Maksym Oliinyk, Maia Salin, Ejota Elezaj

---

## 1. Topic & Overview

Tleilax is a real-time 2D ecosystem sandbox simulation for Android. The user configures a
starting world — choosing species and their initial populations — then observes the simulation
evolve autonomously on a top-down grid canvas. Predators hunt, prey flee, plants spread and
are consumed. No outcome is scripted; complex population dynamics emerge naturally from
simple per-entity behavioral rules.

The user can intervene during a running simulation by tapping the canvas to place new
entities, adjust simulation speed, or pause at any point. Population statistics are tracked live
and visualized as graphs on a separate screen. Simulations can be saved and reloaded.

---

## 2. Features

### Core (mandatory)
- Real-time tick-based simulation on a 2D grid
- Three species categories: Predator, Prey, Plant — up to 7 species total
- Per-entity behavior: movement, feeding, reproduction, energy/death
- Pre-simulation configuration (species counts, grid size)
- Tap-to-place entities during a live simulation
- Simulation speed control (pause, slow, normal, fast)
- Pinch-to-pan and pinch-to-zoom on the canvas

### Bonus features targeted
| Feature | Implementation | Points |
|---|---|---|
| RecyclerView | Save/load screen lists saved simulations | +1 |
| Species Images | Unique sprite per species drawn on canvas | +1 |
| Simulation Visualization | Animated canvas with live grid updates | +2 |
| Tactical Combat | User intervenes with tap-to-place and environmental events during live sim | +2 |
| Statistics | Live population count per species per tick | +1 |
| Statistics Visualization | AnyChart line graph, updates in real time | +2 |
| Specialization Bonuses | Each species has unique speed, vision, energy rules | +2 |
| Randomness | Movement direction, reproduction chance, mutation | +1 |
| Fragments | Simulation / Stats / Save-Load / Settings screens | +2 |
| Data Storage & Loading | Room database, save and restore full simulation state | +2 |
| Custom Feature X | Environmental Events — drought, food bloom, predator frenzy | +2 |
| **Total available** | | **+18** |

---

## 3. Species & Inheritance Hierarchy

```mermaid
graph BT
    %% Concrete Species
    Wolf["<b>Wolf</b>"]
    Rabbit["<b>Rabbit</b>"]
    Mouse["<b>Mouse</b>"]
    Deer["<b>Deer</b>"]
    Grass["<b>Grass</b>"]
    BerryBush["<b>BerryBush</b>"]
    Tree["<b>Tree</b>"]

    %% Abstract Classes
    Entity["«abstract»<br/><b>Entity</b>"]
    Organism["«abstract»<br/><b>Organism</b>"]
    Animal["«abstract»<br/><b>Animal</b>"]
    Predator["«abstract»<br/><b>Predator</b>"]
    Prey["«abstract»<br/><b>Prey</b>"]
    Plant["«abstract»<br/><b>Plant</b>"]

    %% Inheritance (Pointing up to parent)
    Organism --> Entity
    Animal --> Organism
    Plant --> Organism
    
    Predator --> Animal
    Prey --> Animal
    
    Wolf --> Predator
    
    Rabbit --> Prey
    Mouse --> Prey
    Deer --> Prey
    
    Grass --> Plant
    BerryBush --> Plant
    Tree --> Plant

    %% Uniform Styling
    classDef default fill:none,stroke:#666,stroke-width:2px
    classDef abstract stroke-dasharray: 5 5
    
    class Entity,Organism,Animal,Predator,Prey,Plant abstract
```

**Key behavioral differences per class:**

| Species | Speed | Vision | Energy | Special Rule |
|---|---|---|---|---|
| Wolf | High | High | High | Hunts Deer, Rabbit, Mouse |
| Rabbit | Medium | Medium | Low | Eats Grass, BerryBush |
| Mouse | High | Low | Low | Eats Grass, BerryBush |
| Deer | Low | Medium | Medium | Eats Grass, BerryBush, Tree leaves |
| Grass | — | — | — | Spreads to adjacent empty tiles |
| BerryBush | — | — | — | Slower spread, higher energy yield |
| Tree | — | — | — | Does not spread, blocks movement |

---

## 4. Preliminary UML Class Diagram

### Classes and relationships

```mermaid
classDiagram
    class Entity {
        <<abstract>>
        +int x
        +int y
        +int energy
        +update(Grid grid)*
        +isAlive() boolean
    }
    class Organism {
        <<abstract>>
        +int maxEnergy
        +int reproductionThreshold
        +reproduce()* Organism
    }
    class Animal {
        <<abstract>>
        +int speed
        +int visionRange
        +move(Grid grid)
        +eat(Organism target)
        +findTarget(Grid grid)* Organism
    }
    class Predator {
        <<abstract>>
        +findTarget(Grid grid) Organism
    }
    note for Predator "scans for Prey within visionRange"

    note for Wolf "highest speed and vision, hunts all Prey subtypes"

    class Prey {
        <<abstract>>
        +findTarget(Grid grid) Organism
    }
    note for Prey "scans for Plant within visionRange, flees Predator"

    class Plant {
        <<abstract>>
        +spread(Grid grid)*
        +reproduce() Organism
    }
    note for Plant "reproduce() calls spread"

    note for Grass "spreads fast, low energy yield"
    note for BerryBush "spreads slow, high energy yield"
    note for Tree "does not spread, blocks animal movement"

    Entity <|-- Organism
    Organism <|-- Animal
    Animal <|-- Predator
    Predator <|-- Wolf
    Animal <|-- Prey
    Prey <|-- Rabbit
    Prey <|-- Mouse
    Prey <|-- Deer
    Organism <|-- Plant
    Plant <|-- Grass
    Plant <|-- BerryBush
    Plant <|-- Tree

    class Grid {
        +Entity[][] cells
        +int width
        +int height
        +getNeighbours(int x, int y) List~Entity~
        +place(Entity e)
        +remove(Entity e)
        +tick()
    }
    class SimulationEngine {
        +Grid grid
        +int tickCount
        +SimulationState state
        +start()
        +pause()
        +setSpeed(int speed)
        +tick()
        +getStats() Map~String, Integer~
    }
    class StatTracker {
        +Map~String, List_Integer~ populationHistory
        +record(Map~String, Integer~ snapshot)
        +getHistory() Map~String, List_Integer~
    }
    class Storage {
        <<Room-based>>
        +save(SimulationState state)
        +load(int id) SimulationState
        +listSaves() List~SimulationSummary~
    }
    class SimulationState {
        +List~Entity~ entities
        +int gridWidth
        +int gridHeight
        +int tickCount
        +long timestamp
    }

    SimulationEngine o-- Grid
    SimulationEngine o-- SimulationState
    Grid o-- Entity
```

---

## 5. UI Description

### 5.1 View Architecture

```mermaid
classDiagram
    class MainActivity {
        <<Activity>>
        +BottomNavigationView bottomNav
        +FragmentContainerView fragmentContainer
        +onCreate(Bundle savedInstanceState)
        +switchFragment(Fragment target)
    }
    class SimulationFragment {
        <<Fragment>>
        +SpeciesPickerRow picker
        +SurfaceView canvas
        +onTap(float x, float y)
        +onPinchZoom(float scale)
    }
    class StatisticsFragment {
        <<Fragment>>
        +AnyChartView lineGraph
        +updateStats(Map data)
    }
    class SaveLoadFragment {
        <<Fragment>>
        +RecyclerView list
        +saveCurrentState()
        +loadState(int id)
    }
    class SettingsFragment {
        <<Fragment>>
        +Slider gridSize
        +applySettings()
    }

    MainActivity *-- SimulationFragment : Nav Item 1
    MainActivity *-- StatisticsFragment : Nav Item 2
    MainActivity *-- SaveLoadFragment : Nav Item 3
    MainActivity *-- SettingsFragment : Nav Item 4
```

### 5.2 Visual Wireframes

```mermaid
graph TD
    subgraph "Screen 1: Simulation"
        direction TB
        S1_H[Species Picker: 🐺 🐰 🐭 🌲]
        S1_C[Canvas: 2D Ecosystem Grid]
        S1_B[Controls: ⏸️  ⏪ ⏩ 🔄]
        S1_H --- S1_C --- S1_B
    end

    subgraph "Screen 2: Statistics"
        direction TB
        S2_G["AnyChart Line Graph<br/>(Population vs Time)"]
        S2_S["Summary: Ticks: 452 | Status: Stable"]
        S2_G --- S2_S
    end

    subgraph "Screen 3: Save / Load"
        direction TB
        S3_B[Button: Save Current Simulation]
        S3_L["RecyclerView:<br/>- Forest_World_1 (2026-03-29)<br/>- Desert_Test (2026-03-28)<br/>- Empty_Grid (2026-03-25)"]
        S3_B --- S3_L
    end

    subgraph "Screen 4: Settings"
        direction TB
        S4_G[Slider: Grid Size 10x10 - 40x40]
        S4_S[Selector: Default Speed]
        S4_P[Sliders: Initial Population per Species]
        S4_R[Button: Reset to Defaults]
        S4_G --- S4_S --- S4_P --- S4_R
    end
```

The app uses a single `MainActivity` hosting a `FragmentContainerView`. Navigation is
handled via a bottom navigation bar with four destinations.

---

### Screen 1 — Simulation (Main Screen)

**Layout:**
- Top: Horizontal species picker (button row) — one button per species, tap to select active species
- Center: Full-width `SurfaceView` / `Canvas` — renders the 2D grid, entities drawn as sprites
  - Tap on canvas → places selected species at that tile
  - Two-finger pinch → zoom in/out
  - Two-finger drag → pan the camera across the grid
- Bottom bar: Pause / Play, Speed Down, Speed Up, Reset buttons

---

### Screen 2 — Statistics

**Layout:**
- AnyChart line graph — one line per species, X axis = ticks, Y axis = population count
- Updates in real time while simulation runs in background
- Summary card below: current tick, species alive/extinct

---

### Screen 3 — Save / Load

**Layout:**
- RecyclerView listing saved simulations (name, date, tick count, species alive)
- Swipe to delete, tap to load
- Save current simulation button at top

---

### Screen 4 — Settings

**Layout:**
- Grid size slider (10×10 to 40×40)
- Default simulation speed selector
- Initial population sliders per species (used when starting a new simulation)
- Reset to defaults button

---

## 6. Technology Stack

| Item | Choice |
|---|---|
| Language | Java 21 |
| Build | Gradle (Groovy DSL) |
| Min SDK | 28 |
| Target SDK | 36 |
| Persistence | Room |
| Charts | AnyChart-Android |
| Architecture | Single Activity + Fragments, no MVVM |

---

## 7. Planned Bonus Features Summary

RecyclerView, Species Images, Simulation Visualization, Tactical Combat (user intervention),
Statistics, Statistics Visualization, Specialization Bonuses, Randomness, Fragments,
Data Storage & Loading, Custom Feature X (Environmental Events).

Total targeted bonus: **+18 points** (minimum needed for grade 5: +12, buffer: +6)
