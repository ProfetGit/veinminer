setblock 9 -54 10 minecraft:air destroy
execute as @a[tag=vmscene.ready,limit=1] at @s run function veinminer:mined with storage vmscene:cfg
