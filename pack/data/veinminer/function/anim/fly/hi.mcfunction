execute if score @s veinminer.t matches -1 run data merge entity @s {start_interpolation:0,interpolation_duration:11,transformation:{translation:[0f,0f,0f],scale:[1f,1f,1f],left_rotation:{angle:3.1416f,axis:[0f,1f,0f]}}}
execute if score @s veinminer.t matches -1 run return run tp @s ~ ~0.4 ~
execute if score @s veinminer.t matches -2 run return run tp @s ~ ~0.3111 ~
execute if score @s veinminer.t matches -3 run return run tp @s ~ ~0.2222 ~
execute if score @s veinminer.t matches -4 run return run tp @s ~ ~0.1333 ~
execute if score @s veinminer.t matches -5 run return run tp @s ~ ~0.0444 ~
execute if score @s veinminer.t matches -6 run return run tp @s ~ ~-0.0444 ~
execute if score @s veinminer.t matches -7 run return run tp @s ~ ~-0.1333 ~
execute if score @s veinminer.t matches -8 run return run tp @s ~ ~-0.2222 ~
execute if score @s veinminer.t matches -9 run return run tp @s ~ ~-0.3111 ~
execute if score @s veinminer.t matches -10 run return run tp @s ~ ~-0.4 ~
execute if score @s veinminer.t matches -11 run return run tp @s ~ ~-0.4889 ~
execute if score @s veinminer.t matches -12 if entity @s[tag=!veinminer.cube] run return run data merge entity @s {start_interpolation:0,interpolation_duration:1,transformation:{translation:[0f,-0.0375f,0f],scale:[1.25f,0.7f,1.25f]}}
execute if score @s veinminer.t matches -12 if entity @s[tag=veinminer.cube] run return run data merge entity @s {start_interpolation:0,interpolation_duration:1,transformation:{translation:[0f,0.0188f,0f],scale:[1.25f,0.7f,1.25f]}}
