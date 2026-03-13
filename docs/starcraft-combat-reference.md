# StarCraft Combat Reference

This file stores the combat-stat references used to tune unit stats in this project.

## Primary sources

- BWAPI unit type reference: https://bwapi.github.io/namespace_b_w_a_p_i_1_1_unit_types.html
- BWAPI weapon type reference: https://bwapi.github.io/namespace_b_w_a_p_i_1_1_weapon_types.html

## Confirmed reference values

### Units

- Terran Marine
  - hp: `40`
  - armor: `0`
  - damage: `6`
  - attack cooldown: `15`
  - range: `128`
- Terran Firebat
  - hp: `50`
  - armor: `1`
  - damage: `8`
  - attack cooldown: `22`
  - range: `32`
  - splash: `Enemy Splash (15, 20, 25)`
- Terran SCV
  - hp: `60`
  - armor: `0`
  - damage: `5`
  - attack cooldown: `15`
  - range: `10`
- Zerg Hydralisk
  - hp: `80`
  - armor: `0`
  - damage: `10`
  - attack cooldown: `15`
  - range: `128`
- Zerg Zergling
  - hp: `35`
  - armor: `0`
  - damage: `5`
  - attack cooldown: `8`
  - range: `15`

## Project note

This project does not reproduce Brood War movement and pathing exactly, so movement speed may remain project-tuned. Use this file first for combat-facing stats such as hp, armor, damage, attack cooldown, and range.
