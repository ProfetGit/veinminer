scoreboard players operation #cur veinminer.data += #dmg veinminer.data
execute store result storage veinminer:op m.dmg int 1 run scoreboard players get #cur veinminer.data
function veinminer:tool/set_damage with storage veinminer:op m
