function veinminer:player/reset_stats
$execute unless score #ore.$(id) veinminer.config matches 1 run return fail
execute if score @s veinminer.off matches 1 run return fail
execute if entity @s[gamemode=!survival] run return fail
execute if score #require_sneak veinminer.config matches 1 unless predicate veinminer:is_sneaking run return fail
execute unless items entity @s weapon.mainhand #veinminer:tools run return fail
execute if function #veinminer:api/cancel run return fail
$data modify storage veinminer:op m set from storage veinminer:groups $(id)
tag @s add veinminer.miner
function veinminer:vein/start
tag @s remove veinminer.miner
