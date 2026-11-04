---
navigation:
  title: "Embedded materials and removal"
  icon: "minecraft:paper"
---

# Embedded materials and removal

Hold a material in the offhand and strike a successful hit to embed it; a failed hit consumes nothing.
In automation, put one material in the tank next to the target.

Slots: weapons 3, tools 3, armour 1, amulets 1. Paper adds 3 (unique, durability -50%), a transcendium nugget adds 1.
A full item reports "slots full" instead of silently refusing.

Dual-role items (axes, dragon rods, resonators) take the union of the tool and weapon material sets, apply both
sides once each, and show the de-duplicated union of their descriptions.

Removal uses a sponge gemmule on a grindstone (manual) or in a grindstone-based tank (automatic): it always takes
back the most recently embedded material, consumes one gemmule, and needs no success roll.
