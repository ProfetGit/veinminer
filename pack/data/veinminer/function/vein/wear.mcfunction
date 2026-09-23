execute if score #unb veinminer.data matches 1.. store result score #roll veinminer.data run random value 0..999999
execute if score #unb veinminer.data matches 1.. run scoreboard players operation #roll veinminer.data %= #unb1 veinminer.data
execute if score #unb veinminer.data matches 1.. unless score #roll veinminer.data matches 0 run return 1
execute if score #dur_left veinminer.data matches ..0 run scoreboard players set #worn veinminer.data 1
execute if score #dur_left veinminer.data matches ..0 run scoreboard players set #budget veinminer.data 0
execute if score #dur_left veinminer.data matches ..0 run return fail
scoreboard players remove #dur_left veinminer.data 1
scoreboard players add #dmg veinminer.data 1
return 1
