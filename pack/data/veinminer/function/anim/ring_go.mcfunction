scoreboard players operation #p veinminer.data = @s veinminer.ring
scoreboard players operation #p veinminer.data *= #7 veinminer.data
scoreboard players add #p veinminer.data 85
execute if score #p veinminer.data matches 161.. run scoreboard players set #p veinminer.data 160
execute store result storage veinminer:anim r.p float 0.01 run scoreboard players get #p veinminer.data
scoreboard players add #p veinminer.data 50
execute store result storage veinminer:anim r.q float 0.01 run scoreboard players get #p veinminer.data
function veinminer:anim/ring with storage veinminer:anim r
scoreboard players add @s veinminer.ring 1
