scoreboard players add #ray veinminer.data 1
execute if score #ray veinminer.data matches 121.. run return 0
execute unless block ~ ~ ~ #veinminer:hollow positioned ^ ^ ^-0.05 align xyz run return run function veinminer:vein/begin
execute positioned ^ ^ ^0.05 run function veinminer:vein/ray
