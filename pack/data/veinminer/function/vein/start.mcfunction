function veinminer:tool/read
execute if score #tier veinminer.data matches 0 run return fail
scoreboard players operation #budget veinminer.data = #max_blocks veinminer.config
scoreboard players remove #budget veinminer.data 1
execute if score #budget veinminer.data matches ..0 run return fail
scoreboard players set #mined veinminer.data 0
scoreboard players set #xp veinminer.data 0
scoreboard players set #dmg veinminer.data 0
scoreboard players set #worn veinminer.data 0
scoreboard players operation #anim veinminer.data = #animation veinminer.config
execute store result score #xp_on veinminer.data run data get storage veinminer:op m.hasxp
execute store result score #loot veinminer.data run gamerule block_drops
execute if score #loot veinminer.data matches 0 run scoreboard players set #xp_on veinminer.data 0
execute if score #silk veinminer.data matches 1 run scoreboard players set #xp_on veinminer.data 0
execute anchored eyes positioned ^ ^ ^ as @e[type=item,distance=..8,nbt={Age:0s},sort=nearest] at @s align xyz if block ~ ~ ~ #veinminer:hollow run function veinminer:vein/candidate
scoreboard players set #ray veinminer.data 0
execute if score #mined veinminer.data matches 0 anchored eyes positioned ^ ^ ^ run function veinminer:vein/ray
execute if score #mined veinminer.data matches 1.. run function veinminer:vein/finish
execute if score #anim veinminer.data matches 1 run kill @e[type=marker,tag=veinminer.new]
