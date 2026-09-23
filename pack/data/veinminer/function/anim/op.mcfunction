$execute as @e[type=marker,tag=veinminer.pend,scores={veinminer.op=$(op),veinminer.t=..0}] at @s run function veinminer:anim/turn with storage veinminer:anim r
$execute if entity @e[type=block_display,tag=veinminer.fx,scores={veinminer.op=$(op),veinminer.t=-9},limit=1] run function veinminer:anim/ring_go
$execute as @e[type=item_display,tag=veinminer.ghost,scores={veinminer.op=$(op),veinminer.t=0}] run function veinminer:anim/launch with storage veinminer:anim r
$execute as @e[type=item_display,tag=veinminer.ghost,scores={veinminer.op=$(op),veinminer.t=..-12}] at @s run function veinminer:anim/land with storage veinminer:anim r
