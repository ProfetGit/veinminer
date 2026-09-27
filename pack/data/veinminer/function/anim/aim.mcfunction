execute store result score #v veinminer.data run data get entity @s Pos[0] 1000
scoreboard players operation @s veinminer.dx -= #v veinminer.data
execute store result score #v veinminer.data run data get entity @s Pos[1] 1000
scoreboard players operation @s veinminer.dy -= #v veinminer.data
execute store result score #v veinminer.data run data get entity @s Pos[2] 1000
scoreboard players operation @s veinminer.dz -= #v veinminer.data
data modify storage veinminer:anim g set value {start_interpolation:0,interpolation_duration:0,transformation:{translation:[0f,0f,0f]}}
execute store result storage veinminer:anim g.transformation.translation[0] float 0.001 run scoreboard players get @s veinminer.dx
execute store result storage veinminer:anim g.transformation.translation[1] float 0.001 run scoreboard players get @s veinminer.dy
execute store result storage veinminer:anim g.transformation.translation[2] float 0.001 run scoreboard players get @s veinminer.dz
data modify entity @s {} merge from storage veinminer:anim g
