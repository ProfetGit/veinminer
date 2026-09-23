execute if entity @e[type=marker,tag=veinminer.pend,distance=..0.1] run return 0
execute if score #anim veinminer.data matches 1 run kill @e[type=marker,tag=veinminer.new]
execute if score #anim veinminer.data matches 1 run summon marker ~ ~ ~ {Tags:["veinminer.ctl","veinminer.new"]}
execute if score #drops veinminer.config matches 1 at @s summon marker run function veinminer:util/drop_pos
execute unless score #drops veinminer.config matches 1 positioned ~0.5 ~0.5 ~0.5 summon marker run function veinminer:util/drop_pos
function veinminer:vein/neighbors with storage veinminer:op m
