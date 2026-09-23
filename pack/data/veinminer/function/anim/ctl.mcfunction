scoreboard players add @s veinminer.t 1
data modify storage veinminer:anim r set from entity @s data.m
execute store result score #cop veinminer.data run data get storage veinminer:anim r.op
scoreboard players set #vis veinminer.data 0
scoreboard players operation #shown veinminer.data = @s veinminer.xp
function veinminer:anim/op with storage veinminer:anim r
scoreboard players operation @s veinminer.xp = #shown veinminer.data
execute if score @s veinminer.t >= @s veinminer.end run function veinminer:anim/done with storage veinminer:anim r
