data modify storage veinminer:op c.x set from entity @s Pos[0]
data modify storage veinminer:op c.y set from entity @s Pos[1]
data modify storage veinminer:op c.z set from entity @s Pos[2]
scoreboard players set #fd veinminer.data 0
function veinminer:anim/floor
execute at @s unless block ~ ~-0.26 ~ #veinminer:hollow run data modify storage veinminer:op c.y set from entity @s Pos[1]
kill @s
