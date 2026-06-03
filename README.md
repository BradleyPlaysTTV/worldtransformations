
In-World Transformations
=======

This mod allows you to create recipes for in-world transformations
either with a data pack or using mods such as KubeJS.

<span color="#FF0000"><b>This mod does not come with any recipes by default as it is intended
for modpacks to use their own.</b></span>

Recipe Examples:
======

This recipe is a random output based on percentage, for example.
You have 5% chance of getting Diamonds when throwing Gravel into Water
```
{
  "type": "worldtransform:world_transform",
  "item": {
    "item": "minecraft:gravel"
  },
  "transformTime": 5,
  "fluid": "minecraft:water",
  "results": [
    { "id": "minecraft:diamond", "chance": 5 },
    { "id": "minecraft:raw_iron", "chance": 60 },
    { "id": "minecraft:lapis_lazuli", "chance": 70 },
    { "id": "minecraft:redstone", "chance": 20 },
    { "id": "minecraft:raw_gold", "chance": 20 },
    { "id": "minecraft:raw_copper", "chance": 20 },
    { "id": "minecraft:ancient_debris", "chance": 20 }
  ],
  "replaceFluid": "minecraft:air"
}
```

