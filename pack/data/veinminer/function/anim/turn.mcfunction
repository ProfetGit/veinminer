$execute unless block ~ ~ ~ #veinminer:veins/$(id) run return run kill @s
$execute if block ~ ~ ~ $(b) run summon block_display ~ ~ ~ {block_state:{Name:"$(b)",id:"$(b)"},Tags:["veinminer.fx","veinminer.fnew","veinminer.b"]}
$execute unless block ~ ~ ~ $(b) run summon block_display ~ ~ ~ {block_state:{Name:"$(a)",id:"$(a)"},Tags:["veinminer.fx","veinminer.fnew"]}
execute as @e[type=block_display,tag=veinminer.fnew] run function veinminer:anim/fx_init
scoreboard players set #roll veinminer.data 0
$scoreboard players set #bl veinminer.data $(bl)
$execute if data storage veinminer:anim r{xpon:1b} store result score #roll veinminer.data run random value $(xp)
$execute if data storage veinminer:anim r{loot:1b} run loot spawn ~0.5 ~0.5 ~0.5 mine ~ ~ ~ $(tid)[minecraft:enchantments=$(ench)]
execute positioned ~0.5 ~0.5 ~0.5 as @e[type=item,distance=..0.01] at @s run function veinminer:anim/ghost
execute if score #roll veinminer.data matches 1.. positioned ~0.5 ~0.5 ~0.5 summon item_display run function veinminer:anim/ghost_init
setblock ~ ~ ~ air
kill @s
