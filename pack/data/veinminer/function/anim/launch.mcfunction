execute if score #vis veinminer.data matches 2.. run tag @s add veinminer.hide
execute if score #shown veinminer.data matches 10.. run tag @s add veinminer.hide
execute if entity @s[tag=!veinminer.hide] run scoreboard players add #vis veinminer.data 1
execute if entity @s[tag=!veinminer.hide] run scoreboard players add #shown veinminer.data 1
execute if data storage veinminer:anim r{own:1b} run tag @s add veinminer.lo
execute if entity @s[tag=!veinminer.hide,tag=!veinminer.lo] run data merge entity @s {start_interpolation:0,interpolation_duration:5,transformation:{translation:[0f,1.92f,0f],scale:[1.3f,1.3f,1.3f],left_rotation:{angle:2.094f,axis:[0f,1f,0f]}}}
execute if entity @s[tag=!veinminer.hide,tag=veinminer.lo] run data merge entity @s {start_interpolation:0,interpolation_duration:5,transformation:{translation:[0f,0.86f,0f],scale:[1.15f,1.15f,1.15f],left_rotation:{angle:2.094f,axis:[0f,1f,0f]}}}
$execute if data storage veinminer:anim r{own:1b} at @a[tag=veinminer.o$(op),limit=1] facing entity @s feet positioned ^ ^ ^0.9 run return run tp @s ~ ~0.1 ~
scoreboard players add #dir veinminer.data 1
execute if score #dir veinminer.data matches 8.. run scoreboard players set #dir veinminer.data 0
$execute positioned $(x) $(y) $(z) facing entity @a[tag=veinminer.o$(op),limit=1] feet run function veinminer:anim/spread
