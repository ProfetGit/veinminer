tag @s add vmscene.ready
kill @e[type=item]
tp @s 5.5 -55 10.5 -90 6
gamemode survival @s
item replace entity @s weapon.mainhand with minecraft:iron_pickaxe
scoreboard players set #require_sneak veinminer.config 0
scoreboard players set #welcome veinminer.config 0
scoreboard players set #t vmscene 0
