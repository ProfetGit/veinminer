$execute unless block ~ ~ ~ #veinminer:veins/$(id) run return 0
execute if data storage veinminer:anim r{nd:"yp"} if block ~ ~1 ~ #veinminer:hollow run return run function veinminer:anim/stand/yp with storage veinminer:anim r
execute if data storage veinminer:anim r{nd:"xp"} if block ~1 ~ ~ #veinminer:hollow run return run function veinminer:anim/stand/xp with storage veinminer:anim r
execute if data storage veinminer:anim r{nd:"xn"} if block ~-1 ~ ~ #veinminer:hollow run return run function veinminer:anim/stand/xn with storage veinminer:anim r
execute if data storage veinminer:anim r{nd:"zp"} if block ~ ~ ~1 #veinminer:hollow run return run function veinminer:anim/stand/zp with storage veinminer:anim r
execute if data storage veinminer:anim r{nd:"zn"} if block ~ ~ ~-1 #veinminer:hollow run return run function veinminer:anim/stand/zn with storage veinminer:anim r
execute if data storage veinminer:anim r{nd:"yn"} if block ~ ~-1 ~ #veinminer:hollow run return run function veinminer:anim/stand/yn with storage veinminer:anim r
execute if block ~ ~1 ~ #veinminer:hollow run return run function veinminer:anim/stand/yp with storage veinminer:anim r
execute if block ~1 ~ ~ #veinminer:hollow run return run function veinminer:anim/stand/xp with storage veinminer:anim r
execute if block ~-1 ~ ~ #veinminer:hollow run return run function veinminer:anim/stand/xn with storage veinminer:anim r
execute if block ~ ~ ~1 #veinminer:hollow run return run function veinminer:anim/stand/zp with storage veinminer:anim r
execute if block ~ ~ ~-1 #veinminer:hollow run return run function veinminer:anim/stand/zn with storage veinminer:anim r
execute if block ~ ~-1 ~ #veinminer:hollow run return run function veinminer:anim/stand/yn with storage veinminer:anim r
function veinminer:anim/stand/0 with storage veinminer:anim r
