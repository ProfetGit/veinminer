execute if score #budget veinminer.data matches ..0 run return 0
execute if score #tier veinminer.data matches 1 if block ~ ~ ~ #minecraft:incorrect_for_wooden_tool run return 0
execute if score #tier veinminer.data matches 2 if block ~ ~ ~ #minecraft:incorrect_for_gold_tool run return 0
execute if score #tier veinminer.data matches 3 if block ~ ~ ~ #minecraft:incorrect_for_stone_tool run return 0
execute if score #tier veinminer.data matches 4 if block ~ ~ ~ #minecraft:incorrect_for_copper_tool run return 0
execute if score #tier veinminer.data matches 5 if block ~ ~ ~ #minecraft:incorrect_for_iron_tool run return 0
execute if score #tier veinminer.data matches 6 if block ~ ~ ~ #minecraft:incorrect_for_diamond_tool run return 0
execute if score #tier veinminer.data matches 7 if block ~ ~ ~ #minecraft:incorrect_for_netherite_tool run return 0
execute if entity @e[type=marker,tag=veinminer.pend,distance=..0.1] run return 0
execute if score #wear veinminer.data matches 1 unless function veinminer:vein/wear run return 0
execute if score #anim veinminer.data matches 0 run function veinminer:vein/break with storage veinminer:op m
execute if score #anim veinminer.data matches 1 run function veinminer:anim/mark with storage veinminer:op m
scoreboard players remove #budget veinminer.data 1
scoreboard players add #mined veinminer.data 1
function veinminer:vein/neighbors with storage veinminer:op m
