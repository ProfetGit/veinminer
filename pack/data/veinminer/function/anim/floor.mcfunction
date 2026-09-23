execute at @s unless block ~ ~-0.26 ~ #minecraft:air run return 0
scoreboard players add #fd veinminer.data 1
execute if score #fd veinminer.data matches 17.. run return 0
execute at @s run tp @s ~ ~-0.25 ~
function veinminer:anim/floor
