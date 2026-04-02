# Tleilax — Agent Guidelines

## Project Overview
Real-time 2D ecosystem sandbox simulation for Android. Java, Min SDK 28, Target SDK 36, Java 21.
Full docs: https://oliynykmax.github.io/Tleilax/

## Build / Test Commands

```bash
# Build
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK

# Tests
./gradlew test                   # Run all unit tests
./gradlew testDebugUnitTest      # Run unit tests (debug variant)
./gradlew test --tests "com.example.tleilax.ExampleUnitTest"  # Run single unit test
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)

# Clean / Install
./gradlew clean                  # Clean build outputs
./gradlew installDebug           # Install on connected device
./gradlew lint                   # Run Android lint
```

## Code Style

### Package Structure
```
com.example.tleilax/
├── model/          Entity, Animal, Predator, Prey, Plant
├── simulation/     SimulationEngine, Grid, TickLogic
├── storage/        SaveLoad interface, Room persistence
├── ui/
│   ├── fragments/  SimulationFragment, StatsFragment, ConfigFragment
│   └── adapters/   RecyclerView adapters
└── utils/          StatTracker, Helpers
```

### Imports
- Group imports: `android.*`, `androidx.*`, `java.*`, `javax.*`, third-party, then same-package
- No wildcard imports — always use fully qualified imports
- Remove unused imports

### Naming Conventions
- **Classes**: PascalCase — `SimulationEngine`, `EntityAdapter`
- **Interfaces**: PascalCase, adjective/noun — `SaveLoad`
- **Methods/Variables**: camelCase — `tickLogic`, `clamp()`
- **Constants**: UPPER_SNAKE_CASE — `MAX_POPULATION`
- **Test methods**: snake_case with underscores — `addition_isCorrect()`
- **Resources**: snake_case — `activity_main.xml`, `btn_start_simulation`

### Formatting
- 4-space indentation (no tabs)
- Opening brace on same line for methods/classes
- Max line length: 120 characters
- Blank line between method declarations
- One blank line between logical blocks within methods

### Types & Nullability
- Prefer primitive types (`int`, `float`, `boolean`) over boxed types when null is not meaningful
- Use `@Nullable` and `@NonNull` annotations from `androidx.annotation` on method parameters and return types
- Avoid raw types — always parameterize generics (e.g., `List<Entity>`, not `List`)
- Use `@Override` on all overridden methods

### Class Design
- Model classes follow inheritance: `Entity` → `Animal` (abstract) → `Predator`/`Prey`; `Entity` → `Plant`
- Keep constructors simple; use builder pattern for complex object creation
- Make fields `private` with getters/setters as needed
- Use `abstract` classes for shared behavior (e.g., `Animal`), interfaces for contracts (e.g., `SaveLoad`)

### Error Handling
- Never swallow exceptions — log or rethrow
- Use `Log.e(TAG, "message", exception)` for Android logging
- Validate inputs at method boundaries; throw `IllegalArgumentException` for invalid args
- Handle edge cases in simulation (empty grid, zero population, boundary conditions)

### Android-Specific
- Use `EdgeToEdge.enable()` in activities (already set in `MainActivity`)
- Fragments extend `androidx.fragment.app.Fragment`
- Use `ViewBinding` or `findViewById` consistently (project currently uses `findViewById`)
- Keep UI logic in Fragments, business logic out of UI layer
- Use `RecyclerView` with `ViewHolder` pattern for lists

### Testing
- **Unit tests** (`src/test/`): JUnit 4, run on JVM, no Android dependencies
- **Instrumented tests** (`src/androidTest/`): JUnit 4 + AndroidJUnit4, run on device/emulator
- Test class names: `<ClassName>Test` (unit) or `<ClassName>InstrumentedTest` (instrumented)
- Use `assertEquals(expected, actual)` — expected first
- Name test methods descriptively: `methodUnderTest_condition_expectedResult`

### Dependencies
Managed via Gradle version catalog (`gradle/libs.versions.toml`). Use `libs.*` aliases, never hardcode versions in `build.gradle`.

### Git
- Descriptive commit messages: imperative mood ("Add simulation tick logic", not "Added")
- One logical change per commit
- Do not commit generated files (`build/`, `.gradle/`, `local.properties`)
