execute if score #drops veinminer.config matches 1 at @s summon marker run function veinminer:util/drop_pos
execute unless score #drops veinminer.config matches 1 positioned ~0.5 ~0.5 ~0.5 summon marker run function veinminer:util/drop_pos
function veinminer:vein/neighbors with storage veinminer:op m
