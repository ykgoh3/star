# StarCraft Size Reference

This file stores the external size references used to tune unit, mineral, and building scale in this project.

## Primary sources

- Liquipedia: https://liquipedia.net/starcraft/List_of_Unit_and_Building_Sizes
- BWAPI unit type reference: https://bwapi.github.io/namespace_b_w_a_p_i_1_1_unit_types.html

## Confirmed reference values

### Units

- Terran Marine: `17x20`
- Terran Firebat: `23x22`
- Terran SCV: `23x23`
- Zerg Hydralisk: `21x23`
- Zerg Zergling: `16x16`

### Terran buildings

- Terran Barracks:
  - box size: `128x96`
  - real size: `105x73`
  - tiles: `4x3`
- Terran Command Center:
  - box size: `128x96`
  - real size: `117x83`
  - tiles: `4x3`

## Useful ratios

- Barracks width vs Command Center width: `105 / 117 = 0.897`
- Barracks height vs Command Center height: `73 / 83 = 0.880`
- Barracks is smaller than Command Center, but not drastically smaller.
- SCV vs Marine width ratio: `23 / 17 = 1.353`
- SCV vs Marine height ratio: `23 / 20 = 1.150`

## Project note

When adjusting this project's visuals, prefer preserving these relative proportions over copying absolute pixel values directly, because this project does not reproduce original Brood War rendering, tile metrics, and collision exactly.
