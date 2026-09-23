scoreboard players enable @a veinminer
execute as @a[scores={veinminer=1..}] run function veinminer:player/toggle
execute as @a[tag=!veinminer.welcomed] run function veinminer:player/welcome
execute as @a[scores={veinminer.mined.coal_ore=1..}] at @s run function veinminer:mined {id:"coal"}
execute as @a[scores={veinminer.mined.deepslate_coal_ore=1..}] at @s run function veinminer:mined {id:"coal"}
execute as @a[scores={veinminer.mined.copper_ore=1..}] at @s run function veinminer:mined {id:"copper"}
execute as @a[scores={veinminer.mined.deepslate_copper_ore=1..}] at @s run function veinminer:mined {id:"copper"}
execute as @a[scores={veinminer.mined.iron_ore=1..}] at @s run function veinminer:mined {id:"iron"}
execute as @a[scores={veinminer.mined.deepslate_iron_ore=1..}] at @s run function veinminer:mined {id:"iron"}
execute as @a[scores={veinminer.mined.gold_ore=1..}] at @s run function veinminer:mined {id:"gold"}
execute as @a[scores={veinminer.mined.deepslate_gold_ore=1..}] at @s run function veinminer:mined {id:"gold"}
execute as @a[scores={veinminer.mined.nether_gold_ore=1..}] at @s run function veinminer:mined {id:"nether_gold"}
execute as @a[scores={veinminer.mined.redstone_ore=1..}] at @s run function veinminer:mined {id:"redstone"}
execute as @a[scores={veinminer.mined.deepslate_redstone_ore=1..}] at @s run function veinminer:mined {id:"redstone"}
execute as @a[scores={veinminer.mined.lapis_ore=1..}] at @s run function veinminer:mined {id:"lapis"}
execute as @a[scores={veinminer.mined.deepslate_lapis_ore=1..}] at @s run function veinminer:mined {id:"lapis"}
execute as @a[scores={veinminer.mined.diamond_ore=1..}] at @s run function veinminer:mined {id:"diamond"}
execute as @a[scores={veinminer.mined.deepslate_diamond_ore=1..}] at @s run function veinminer:mined {id:"diamond"}
execute as @a[scores={veinminer.mined.emerald_ore=1..}] at @s run function veinminer:mined {id:"emerald"}
execute as @a[scores={veinminer.mined.deepslate_emerald_ore=1..}] at @s run function veinminer:mined {id:"emerald"}
execute as @a[scores={veinminer.mined.nether_quartz_ore=1..}] at @s run function veinminer:mined {id:"quartz"}
execute as @a[scores={veinminer.mined.ancient_debris=1..}] at @s run function veinminer:mined {id:"ancient_debris"}
