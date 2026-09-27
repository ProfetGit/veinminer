data modify entity @s item set from entity @n[type=item,distance=..0.01] Item
data merge entity @s {Tags:["veinminer.ghost"],item_display:"ground",teleport_duration:1,Rotation:[0f,0f],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0f,0f,0f]}}
execute if items entity @s contents #veinminer:cube_loot run tag @s add veinminer.cube
execute unless items entity @s contents * run tag @s add veinminer.hide
execute if score #vis veinminer.data matches 3.. run tag @s add veinminer.hide
execute if score #shown veinminer.data matches 12.. run tag @s add veinminer.hide
execute if entity @s[tag=!veinminer.hide] run scoreboard players add #vis veinminer.data 1
execute if entity @s[tag=!veinminer.hide] run scoreboard players add #shown veinminer.data 1
execute if data storage veinminer:anim r{own:1b} run tag @s add veinminer.lo
scoreboard players operation @s veinminer.op = #cop veinminer.data
scoreboard players set @s veinminer.t 10
scoreboard players operation @s veinminer.xp = #roll veinminer.data
scoreboard players set #roll veinminer.data 0
execute if entity @s[tag=!veinminer.hide] run function veinminer:anim/start
function veinminer:anim/place with storage veinminer:anim r
execute if entity @s[tag=!veinminer.hide] run function veinminer:anim/aim
