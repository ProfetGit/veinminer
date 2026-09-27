data modify storage veinminer:anim g set value {start_interpolation:0,interpolation_duration:1,transformation:{translation:[0f,0f,0f],scale:[1.2f,1.2f,1.2f],left_rotation:{angle:0.2618f,axis:[0f,1f,0f]}}}
execute store result storage veinminer:anim g.transformation.translation[0] float 0.000916667 run scoreboard players get @s veinminer.dx
execute store result storage veinminer:anim g.transformation.translation[1] float 0.000916667 run scoreboard players get @s veinminer.dy
execute store result storage veinminer:anim g.transformation.translation[2] float 0.000916667 run scoreboard players get @s veinminer.dz
data modify entity @s {} merge from storage veinminer:anim g
execute if entity @s[tag=veinminer.lo] run return run tp @s ~ ~0.1681 ~
tp @s ~ ~0.4889 ~
