$execute if entity @s[tag=veinminer.lo] at @a[tag=veinminer.o$(op),limit=1] facing entity @s feet rotated ~ 0 positioned ^ ^ ^0.9 run return run function veinminer:anim/rest
execute if entity @s[tag=!veinminer.hide] run scoreboard players add #dir veinminer.data 1
execute if score #dir veinminer.data matches 8.. run scoreboard players set #dir veinminer.data 0
$execute positioned $(x) $(y) $(z) facing entity @a[tag=veinminer.o$(op),limit=1] feet rotated ~ 0 run function veinminer:anim/spread
