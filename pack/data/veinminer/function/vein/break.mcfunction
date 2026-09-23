$execute if block ~ ~ ~ $(b) run particle minecraft:block{block_state:"$(b)"} ~0.5 ~0.5 ~0.5 0.25 0.25 0.25 0 12
$execute unless block ~ ~ ~ $(b) run particle minecraft:block{block_state:"$(a)"} ~0.5 ~0.5 ~0.5 0.25 0.25 0.25 0 12
$execute if score #loot veinminer.data matches 1 run loot spawn $(x) $(y) $(z) mine ~ ~ ~ mainhand
$execute if score #xp_on veinminer.data matches 1 store result score #roll veinminer.data run random value $(xp)
execute if score #xp_on veinminer.data matches 1 run scoreboard players operation #xp veinminer.data += #roll veinminer.data
setblock ~ ~ ~ air
