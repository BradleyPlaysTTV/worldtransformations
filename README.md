
In-World Transformations
=======

This mod allows you to create recipes for in-world transformations
either with a data pack or using mods such as KubeJS.

**This mod does not come with any recipes by default as it is intended
for modpacks to use their own.**

Recipe Examples:
======

<ins>Example of throwing Gravel into Water.</ins>

This recipe is a random output based on percentage, for example.
**You have 5% chance of getting Diamonds when throwing Gravel into Water**
```
{
  "type": "worldtransformations:world_transform",
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

<ins>Example of throwing logs onto a magma block.</ins>

This recipe requires you to throw any log on top of a Magma Block and it transforms into Charcoal after 30 seconds.
```
{
  "type": "worldtransformations:world_transform",
  "item": {
    "tag": "minecraft:logs"
  },
  "transformTime": 30,
  "block": "minecraft:magma_block",
  "result": {
    "count": 1,
    "id": "minecraft:charcoal"
  }
}
```

<ins>This is every option you can have</ins>
```
{
  "type": "worldtransformations:world_transform",
  "item": {
    "tag": "minecraft:logs" // The item you throw. either "tag" or "item"
  },
  "transformTime": 30, // How long it takes in seconds.
  "block": "minecraft:magma_block", // The block it must be on top of.
  "fluid: "minecraft:water", // The fluid it must be in.

  // Singular result.
  "result": {
    "count": 1, // The amount of items you want returned per item thrown.
    "id": "minecraft:charcoal" // The ganrenteed drop.
  }

  // Multiple results.
  "results": [
    { "id": "minecraft:diamond", "chance": 5 }, // id is the item you get and chance is the percentage. 5% is the chance of getting diamonds.
    { "id": "minecraft:raw_iron", "chance": 60 },
    { "id": "minecraft:lapis_lazuli", "chance": 70 },
    { "id": "minecraft:redstone", "chance": 20 },
    { "id": "minecraft:raw_gold", "chance": 20 },
    { "id": "minecraft:raw_copper", "chance": 20 },
    { "id": "minecraft:ancient_debris", "chance": 20 }
  ],
  "replaceFluid": "minecraft:air" // Replaces fluid with something else. (This can also be used for Witch Water from exdeorum)
}
```
