execute if score #budget veinminer.data matches ..0 run return 0
$execute if block ~1 ~ ~ #veinminer:veins/$(id) positioned ~1 ~ ~ run function veinminer:vein/visit
$execute if block ~-1 ~ ~ #veinminer:veins/$(id) positioned ~-1 ~ ~ run function veinminer:vein/visit
$execute if block ~ ~1 ~ #veinminer:veins/$(id) positioned ~ ~1 ~ run function veinminer:vein/visit
$execute if block ~ ~-1 ~ #veinminer:veins/$(id) positioned ~ ~-1 ~ run function veinminer:vein/visit
$execute if block ~ ~ ~1 #veinminer:veins/$(id) positioned ~ ~ ~1 run function veinminer:vein/visit
$execute if block ~ ~ ~-1 #veinminer:veins/$(id) positioned ~ ~ ~-1 run function veinminer:vein/visit
$execute if score #diagonal veinminer.config matches 1 run function veinminer:vein/diagonals {id:"$(id)"}
