$execute at @e[type=block_display,tag=!veinminer.b,scores={veinminer.op=$(op),veinminer.t=-9}] run particle minecraft:block{block_state:"$(a)"} ~0.5 ~0.5 ~0.5 0.25 0.25 0.25 0 12
$execute at @e[type=block_display,tag=veinminer.b,scores={veinminer.op=$(op),veinminer.t=-9}] run particle minecraft:block{block_state:"$(b)"} ~0.5 ~0.5 ~0.5 0.25 0.25 0.25 0 12
$execute at @e[type=block_display,scores={veinminer.op=$(op),veinminer.t=-9}] run particle minecraft:small_gust ~0.5 ~0.5 ~0.5 0.15 0.15 0.15 0 1
$execute at @e[type=block_display,scores={veinminer.op=$(op),veinminer.t=-9},limit=1] run playsound minecraft:entity.chicken.egg block @a ~0.5 ~0.5 ~0.5 0.45 $(q)
$execute at @e[type=block_display,tag=veinminer.b,scores={veinminer.op=$(op),veinminer.t=-9},limit=1] run return run playsound $(sb) block @a ~0.5 ~0.5 ~0.5 0.8 $(p)
$execute at @e[type=block_display,scores={veinminer.op=$(op),veinminer.t=-9},limit=1] run playsound $(sa) block @a ~0.5 ~0.5 ~0.5 0.8 $(p)
