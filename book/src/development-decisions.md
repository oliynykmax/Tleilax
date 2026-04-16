# Decisions Done During Development

This page records the design decisions agreed while implementation was already underway.

## World Model

- The world uses a layered tile model with exactly four slots:
  - `terrain`
  - `ground`
  - `plant`
  - `animal`
- `terrain` is currently immutable sand/soil.
- `ground` currently hosts grass.
- `plant` currently hosts berry bushes or trees.
- `animal` hosts at most one animal per tile.

## Grass

- Grass is not the default cell state.
- Every tile starts with sand/soil terrain.
- Grass is a resource on the `ground` layer.
- Grass is removed immediately when eaten.
- Grass only spreads from neighboring grass tiles.
- Grass spread uses orthogonal neighbors only.
- Grass may coexist with berry bushes.
- Grass is blocked by trees.

## Berry Bushes

- Berry bushes live in the `plant` layer and may coexist with grass.
- Berry bushes own a finite berry resource value.
- Berries regrow over time.
- Animals must stop on the bush tile for a tick to harvest berries.
- Movement through a bush may damage it depending on species rules.
- Bush trampling reduces durability instead of destroying the bush instantly.
- Destroyed bushes enter a dead state before clearing.
- Dead bushes are passable.

## Trees

- Trees live in the `plant` layer and replace grass on placement.
- Trees block other plants from occupying the tile.
- Tree traversal depends on obstacle height versus animal clearance.
- Tree height is numeric in the model, but content currently uses three authored variants:
  - `LOW`
  - `MEDIUM`
  - `TALL`
- Trees have the lifecycle:
  - `SAPLING`
  - `MATURE`
  - `OLD`
  - `DEAD`
- Only mature trees spread.
- Dead trees remain for a while and still block grass until they clear.

## Animals

- Animals move orthogonally.
- Animals use one main action per tick:
  - move
  - feed
  - attack
  - reproduce
  - idle
- Feeding on the current tile happens before movement.
- Feeding consumes the full tick.
- Reproduction should consume the full tick.
- Attacking should consume the full tick.
- Predators attack from adjacent range.
- Animals use separate `energy` and `health`.
- Diet and movement rules are explicit per species.

## Generation And Rendering

- Initial world generation should create organic grass patches instead of uniform coverage.
- Grass patches should prefer contiguous clusters.
- Trees and berry bushes should be tied to grass-rich areas.
- Trees should prefer the edges of grass clusters.
- Berry bushes are fixed in place for now and only regrow berries.
- The simulation view should support panning and zooming.
- The simulation view should only render the visible viewport instead of the whole world every frame.
- The renderer should load textures when available and fall back to generated placeholder art.
- World size is currently fixed at `256x256`.
- The startup state should be paused, with the play button showing `Play`.
- World generation/loading should appear inside the simulation frame rather than as a full-screen blocker.
- Loading UI should last at least 3 seconds and use a determinate progress bar.
- Loading copy should use one silly randomly chosen status word per generation pass.

## Save And Settings UX

- Save/Load should use a real persisted archive, not mock rows.
- Users should name a world before saving it.
- Saved worlds should be tappable to load.
- Saved worlds should be removed with a one-direction swipe delete gesture.
- Delete should provide both visual and haptic feedback.
- Settings changes for world generation apply on the next explicit reset, not on tab switch.
- Settings should include a destructive cleanup action that clears all saves and restores defaults.
- Destructive reset actions should use custom dialogs matching the app style rather than platform-default alerts.

## Delivery

- CI should build the unsigned debug APK and publish it as a GitHub Actions artifact.
- Signed release publishing is deferred until a signing strategy exists.

## Implementation Scope

- The layered tile model is treated as the stable architecture.
- Animal decision-making stays intentionally minimal until teammate work lands.
