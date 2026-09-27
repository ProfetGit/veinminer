data modify storage veinminer:anim r set from storage veinminer:op c
scoreboard players operation #cop veinminer.data = #opid veinminer.data
scoreboard players set #ft veinminer.data 1
$execute as @e[type=marker,tag=veinminer.pend,scores={veinminer.op=$(op),veinminer.t=1}] at @s run function veinminer:anim/prep with storage veinminer:anim r
