$execute if entity @e[type=marker,tag=veinminer.pend,scores={veinminer.op=$(op)},limit=1] if score @s veinminer.t matches ..400 run return 0
$execute as @e[type=item_display,tag=veinminer.ghost,scores={veinminer.op=$(op)}] at @s run function veinminer:anim/land with storage veinminer:anim r
$kill @e[type=block_display,tag=veinminer.fx,scores={veinminer.op=$(op)}]
$kill @e[type=marker,tag=veinminer.pend,scores={veinminer.op=$(op)}]
$tag @a remove veinminer.o$(op)
kill @s
