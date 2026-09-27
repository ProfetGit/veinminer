$execute if entity @s[tag=veinminer.lo] at @a[tag=veinminer.o$(op),limit=1] run return run function veinminer:anim/drop {pd:0,hop:0.0,dy:0}
execute if entity @s[tag=veinminer.cube] run return run function veinminer:anim/drop {pd:10,hop:0.12,dy:0}
function veinminer:anim/drop {pd:10,hop:0.12,dy:-0.1875}
