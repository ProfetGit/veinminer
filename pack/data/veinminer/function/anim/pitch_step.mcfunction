scoreboard players remove #rings veinminer.data 1
scoreboard players set #ps veinminer.data 650
scoreboard players operation #ps veinminer.data /= #rings veinminer.data
execute if score #ps veinminer.data matches 71.. run scoreboard players set #ps veinminer.data 70
