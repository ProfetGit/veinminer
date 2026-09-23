execute unless entity @e[type=marker,tag=veinminer.ctl,limit=1] unless entity @e[type=block_display,tag=veinminer.fx,limit=1] unless entity @e[type=item_display,tag=veinminer.ghost,limit=1] unless entity @e[type=marker,tag=veinminer.pend,limit=1] run return 0
scoreboard players remove @e[type=marker,tag=veinminer.pend] veinminer.t 1
scoreboard players remove @e[type=block_display,tag=veinminer.fx] veinminer.t 1
scoreboard players remove @e[type=item_display,tag=veinminer.ghost] veinminer.t 1
execute as @e[type=block_display,tag=veinminer.fx,scores={veinminer.t=-1}] run data merge entity @s {start_interpolation:0,interpolation_duration:3,transformation:{translation:[-0.03f,0.05f,-0.03f],scale:[1.06f,0.9f,1.06f]}}
execute as @e[type=block_display,tag=veinminer.fx,scores={veinminer.t=-4}] run data merge entity @s {start_interpolation:0,interpolation_duration:2,transformation:{translation:[-0.1f,0.14f,-0.1f],scale:[1.2f,0.72f,1.2f]}}
execute as @e[type=block_display,tag=veinminer.fx,scores={veinminer.t=-8}] run data merge entity @s {start_interpolation:0,interpolation_duration:1,transformation:{translation:[0.1f,-0.2f,0.1f],scale:[0.8f,1.4f,0.8f]}}
execute as @e[type=block_display,tag=veinminer.fx,scores={veinminer.t=-9}] run data merge entity @s {start_interpolation:0,interpolation_duration:2,transformation:{translation:[0.5f,0.5f,0.5f],scale:[0f,0f,0f]}}
kill @e[type=block_display,tag=veinminer.fx,scores={veinminer.t=..-11}]
execute as @e[type=item_display,tag=veinminer.ghost,tag=!veinminer.hide,tag=!veinminer.lo,scores={veinminer.t=-5}] run data merge entity @s {start_interpolation:0,interpolation_duration:4,transformation:{translation:[0f,1.68f,0f],scale:[1.2f,1.2f,1.2f],left_rotation:{angle:4.189f,axis:[0f,1f,0f]}}}
execute as @e[type=item_display,tag=veinminer.ghost,tag=!veinminer.hide,tag=veinminer.lo,scores={veinminer.t=-5}] run data merge entity @s {start_interpolation:0,interpolation_duration:4,transformation:{translation:[0f,0.76f,0f],scale:[1.05f,1.05f,1.05f],left_rotation:{angle:4.189f,axis:[0f,1f,0f]}}}
execute as @e[type=item_display,tag=veinminer.ghost,tag=!veinminer.hide,scores={veinminer.t=-9}] run data merge entity @s {start_interpolation:0,interpolation_duration:3,transformation:{translation:[0f,0f,0f],scale:[1f,1f,1f],left_rotation:{angle:6.283f,axis:[0f,1f,0f]}}}
execute as @e[type=marker,tag=veinminer.ctl,tag=!veinminer.new] at @s run function veinminer:anim/ctl
execute as @e[type=item_display,tag=veinminer.ghost,scores={veinminer.t=..-30}] at @s run function veinminer:anim/drop {pd:10,hop:0.12}
kill @e[type=marker,tag=veinminer.pend,scores={veinminer.t=..-40}]
