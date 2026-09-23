scoreboard players set #bl veinminer.data 1
execute if predicate veinminer:light_ge4 run scoreboard players set #bl veinminer.data 2
execute if predicate veinminer:light_ge8 run scoreboard players set #bl veinminer.data 3
execute if predicate veinminer:light_ge12 run scoreboard players set #bl veinminer.data 4
execute if predicate veinminer:sees_sky run scoreboard players set #bl veinminer.data 5
execute store result storage veinminer:op c.bl int 1 run scoreboard players get #bl veinminer.data
