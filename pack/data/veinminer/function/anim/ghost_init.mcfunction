data modify entity @s item set from entity @n[type=item,distance=..0.01] Item
data merge entity @s {Tags:["veinminer.ghost"],item_display:"ground",teleport_duration:12,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0f,0f,0f]}}
function veinminer:anim/bright
scoreboard players operation @s veinminer.op = #cop veinminer.data
scoreboard players set @s veinminer.t 9
scoreboard players operation @s veinminer.xp = #roll veinminer.data
scoreboard players set #roll veinminer.data 0
