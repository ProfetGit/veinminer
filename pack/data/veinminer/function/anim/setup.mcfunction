tag @s remove veinminer.new
scoreboard players add #opid veinminer.data 1
execute if score #opid veinminer.data matches 1000000.. run scoreboard players set #opid veinminer.data 1
$execute as @e[type=marker,tag=veinminer.pnew] at @s run function veinminer:anim/restore {a:"$(a)",b:"$(b)"}
scoreboard players operation @e[type=marker,tag=veinminer.pnew] veinminer.op = #opid veinminer.data
function veinminer:anim/rings
scoreboard players set @s veinminer.t 0
scoreboard players set @s veinminer.ring 0
scoreboard players set @s veinminer.xp 0
scoreboard players operation @s veinminer.end = #maxd veinminer.data
scoreboard players add @s veinminer.end 24
data modify storage veinminer:op c set from storage veinminer:op m
execute store result storage veinminer:op c.op int 1 run scoreboard players get #opid veinminer.data
data modify storage veinminer:op c.tid set from storage veinminer:op tool.id
data modify storage veinminer:op c.ench set value {}
data modify storage veinminer:op c.ench set from storage veinminer:op tool.components."minecraft:enchantments"
execute store result storage veinminer:op c.loot byte 1 run scoreboard players get #loot veinminer.data
execute store result storage veinminer:op c.xpon byte 1 run scoreboard players get #xp_on veinminer.data
execute store result storage veinminer:op c.own byte 1 run scoreboard players get #drops veinminer.config
data modify storage veinminer:op c.p set value 1.0f
execute unless score #drops veinminer.config matches 1 positioned ~0.5 ~0.5 ~0.5 facing entity @a[tag=veinminer.miner,limit=1] eyes positioned ^ ^ ^1.3 if block ~ ~ ~ #veinminer:hollow summon marker run function veinminer:anim/land_pos
function veinminer:anim/light_at with storage veinminer:op c
data modify entity @s data.m set from storage veinminer:op c
function veinminer:anim/owner with storage veinminer:op c
