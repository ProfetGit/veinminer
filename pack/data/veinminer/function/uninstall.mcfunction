scoreboard objectives remove veinminer
scoreboard objectives remove veinminer.off
scoreboard objectives remove veinminer.config
scoreboard objectives remove veinminer.data
scoreboard objectives remove veinminer.mined.coal_ore
scoreboard objectives remove veinminer.mined.deepslate_coal_ore
scoreboard objectives remove veinminer.mined.copper_ore
scoreboard objectives remove veinminer.mined.deepslate_copper_ore
scoreboard objectives remove veinminer.mined.iron_ore
scoreboard objectives remove veinminer.mined.deepslate_iron_ore
scoreboard objectives remove veinminer.mined.gold_ore
scoreboard objectives remove veinminer.mined.deepslate_gold_ore
scoreboard objectives remove veinminer.mined.nether_gold_ore
scoreboard objectives remove veinminer.mined.redstone_ore
scoreboard objectives remove veinminer.mined.deepslate_redstone_ore
scoreboard objectives remove veinminer.mined.lapis_ore
scoreboard objectives remove veinminer.mined.deepslate_lapis_ore
scoreboard objectives remove veinminer.mined.diamond_ore
scoreboard objectives remove veinminer.mined.deepslate_diamond_ore
scoreboard objectives remove veinminer.mined.emerald_ore
scoreboard objectives remove veinminer.mined.deepslate_emerald_ore
scoreboard objectives remove veinminer.mined.nether_quartz_ore
scoreboard objectives remove veinminer.mined.ancient_debris
tag @a remove veinminer.welcomed
tag @a remove veinminer.miner
data remove storage veinminer:op m
data remove storage veinminer:op tool
data remove storage veinminer:op slot
data remove storage veinminer:groups coal
data remove storage veinminer:groups copper
data remove storage veinminer:groups iron
data remove storage veinminer:groups gold
data remove storage veinminer:groups nether_gold
data remove storage veinminer:groups redstone
data remove storage veinminer:groups lapis
data remove storage veinminer:groups diamond
data remove storage veinminer:groups emerald
data remove storage veinminer:groups quartz
data remove storage veinminer:groups ancient_debris
data remove storage veinminer:menu row
data remove storage veinminer:meta version
tellraw @s ["",{text:"⛏ Veinminer data removed. ",color:"gold"},{text:"Now delete or disable the datapack (e.g. /datapack disable \"file/Veinminer-1.0.0.zip\") so it does not reinstall on the next /reload.",color:"gray"}]
