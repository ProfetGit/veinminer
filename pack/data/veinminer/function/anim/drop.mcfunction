data modify storage veinminer:anim it set from entity @s item
$execute if data storage veinminer:anim it.id run summon item ~ ~ ~ {Item:{id:"minecraft:stone"},Age:1s,PickupDelay:$(pd)s,Tags:["veinminer.pop"],Motion:[0d,$(hop)d,0d]}
execute as @n[type=item,tag=veinminer.pop] run function veinminer:anim/fill
execute store result storage veinminer:anim xo int 1 run scoreboard players get @s veinminer.xp
execute if score @s veinminer.xp matches 1.. run function veinminer:anim/orb with storage veinminer:anim
kill @s
