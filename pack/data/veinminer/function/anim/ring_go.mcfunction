scoreboard players set #ps veinminer.data 70
execute if data storage veinminer:anim r.ps store result score #ps veinminer.data run data get storage veinminer:anim r.ps
scoreboard players operation #p veinminer.data = @s veinminer.ring
scoreboard players operation #p veinminer.data *= #ps veinminer.data
scoreboard players add #p veinminer.data 850
execute if score #p veinminer.data matches 1501.. run scoreboard players set #p veinminer.data 1500
execute store result storage veinminer:anim r.p float 0.001 run scoreboard players get #p veinminer.data
scoreboard players add #p veinminer.data 500
execute store result storage veinminer:anim r.q float 0.001 run scoreboard players get #p veinminer.data
function veinminer:anim/ring with storage veinminer:anim r
scoreboard players add @s veinminer.ring 1
