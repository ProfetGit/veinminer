scoreboard players set #tier veinminer.data 0
function veinminer:tool/identify
execute if score #tier veinminer.data matches 0 run return fail
execute store result storage veinminer:op slot.s int 1 run data get entity @s SelectedItemSlot
function veinminer:tool/fetch with storage veinminer:op slot
execute store result score #cur veinminer.data run data get storage veinminer:op tool.components."minecraft:damage"
execute if data storage veinminer:op tool.components."minecraft:max_damage" store result score #maxdur veinminer.data run data get storage veinminer:op tool.components."minecraft:max_damage"
execute store result score #unb veinminer.data run data get storage veinminer:op tool.components."minecraft:enchantments"."minecraft:unbreaking"
scoreboard players operation #unb1 veinminer.data = #unb veinminer.data
scoreboard players add #unb1 veinminer.data 1
execute store success score #silk veinminer.data if data storage veinminer:op tool.components."minecraft:enchantments"."minecraft:silk_touch"
scoreboard players operation #wear veinminer.data = #durability veinminer.config
execute if data storage veinminer:op tool.components."minecraft:unbreakable" run scoreboard players set #wear veinminer.data 0
scoreboard players operation #dur_left veinminer.data = #maxdur veinminer.data
scoreboard players operation #dur_left veinminer.data -= #cur veinminer.data
scoreboard players remove #dur_left veinminer.data 1
