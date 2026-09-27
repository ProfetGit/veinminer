$execute unless block ~ ~ ~ #veinminer:veins/$(id) run return run function veinminer:anim/skip with storage veinminer:anim r
scoreboard players set #ft veinminer.data 0
execute if entity @s[tag=!veinminer.prepped] run function veinminer:anim/prep with storage veinminer:anim r
scoreboard players set #roll veinminer.data 0
$execute if data storage veinminer:anim r{xpon:1b} store result score #roll veinminer.data run random value $(xp)
$execute if data storage veinminer:anim r{loot:1b} run loot spawn ~0.5 ~0.5 ~0.5 mine ~ ~ ~ $(tid)[minecraft:enchantments=$(ench)]
execute positioned ~0.5 ~0.5 ~0.5 as @e[type=item,distance=..0.01] at @s run function veinminer:anim/ghost
execute if score #roll veinminer.data matches 1.. positioned ~0.5 ~0.5 ~0.5 summon item_display run function veinminer:anim/ghost_init
setblock ~ ~ ~ air
kill @s
