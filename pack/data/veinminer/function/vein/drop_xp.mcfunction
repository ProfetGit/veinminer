execute if score #xp veinminer.data matches 32768.. run scoreboard players set #xp veinminer.data 32767
execute store result storage veinminer:op m.xpv int 1 run scoreboard players get #xp veinminer.data
function veinminer:vein/summon_xp with storage veinminer:op m
