data modify storage veinminer:meta version set value "1.0.0"
scoreboard objectives add veinminer trigger {text:"Veinminer"}
scoreboard objectives add veinminer.off dummy
scoreboard objectives add veinminer.config dummy
scoreboard objectives add veinminer.data dummy
scoreboard objectives add veinminer.mined.coal_ore minecraft.mined:minecraft.coal_ore
scoreboard objectives add veinminer.mined.deepslate_coal_ore minecraft.mined:minecraft.deepslate_coal_ore
scoreboard objectives add veinminer.mined.copper_ore minecraft.mined:minecraft.copper_ore
scoreboard objectives add veinminer.mined.deepslate_copper_ore minecraft.mined:minecraft.deepslate_copper_ore
scoreboard objectives add veinminer.mined.iron_ore minecraft.mined:minecraft.iron_ore
scoreboard objectives add veinminer.mined.deepslate_iron_ore minecraft.mined:minecraft.deepslate_iron_ore
scoreboard objectives add veinminer.mined.gold_ore minecraft.mined:minecraft.gold_ore
scoreboard objectives add veinminer.mined.deepslate_gold_ore minecraft.mined:minecraft.deepslate_gold_ore
scoreboard objectives add veinminer.mined.nether_gold_ore minecraft.mined:minecraft.nether_gold_ore
scoreboard objectives add veinminer.mined.redstone_ore minecraft.mined:minecraft.redstone_ore
scoreboard objectives add veinminer.mined.deepslate_redstone_ore minecraft.mined:minecraft.deepslate_redstone_ore
scoreboard objectives add veinminer.mined.lapis_ore minecraft.mined:minecraft.lapis_ore
scoreboard objectives add veinminer.mined.deepslate_lapis_ore minecraft.mined:minecraft.deepslate_lapis_ore
scoreboard objectives add veinminer.mined.diamond_ore minecraft.mined:minecraft.diamond_ore
scoreboard objectives add veinminer.mined.deepslate_diamond_ore minecraft.mined:minecraft.deepslate_diamond_ore
scoreboard objectives add veinminer.mined.emerald_ore minecraft.mined:minecraft.emerald_ore
scoreboard objectives add veinminer.mined.deepslate_emerald_ore minecraft.mined:minecraft.deepslate_emerald_ore
scoreboard objectives add veinminer.mined.nether_quartz_ore minecraft.mined:minecraft.nether_quartz_ore
scoreboard objectives add veinminer.mined.ancient_debris minecraft.mined:minecraft.ancient_debris
function veinminer:config/defaults
data modify storage veinminer:groups coal set value {id:"coal",a:"minecraft:coal_ore",b:"minecraft:deepslate_coal_ore",xp:"0..2",hasxp:1b}
data modify storage veinminer:groups copper set value {id:"copper",a:"minecraft:copper_ore",b:"minecraft:deepslate_copper_ore",xp:"0..1",hasxp:0b}
data modify storage veinminer:groups iron set value {id:"iron",a:"minecraft:iron_ore",b:"minecraft:deepslate_iron_ore",xp:"0..1",hasxp:0b}
data modify storage veinminer:groups gold set value {id:"gold",a:"minecraft:gold_ore",b:"minecraft:deepslate_gold_ore",xp:"0..1",hasxp:0b}
data modify storage veinminer:groups nether_gold set value {id:"nether_gold",a:"minecraft:nether_gold_ore",b:"minecraft:nether_gold_ore",xp:"0..1",hasxp:1b}
data modify storage veinminer:groups redstone set value {id:"redstone",a:"minecraft:redstone_ore",b:"minecraft:deepslate_redstone_ore",xp:"1..5",hasxp:1b}
data modify storage veinminer:groups lapis set value {id:"lapis",a:"minecraft:lapis_ore",b:"minecraft:deepslate_lapis_ore",xp:"2..5",hasxp:1b}
data modify storage veinminer:groups diamond set value {id:"diamond",a:"minecraft:diamond_ore",b:"minecraft:deepslate_diamond_ore",xp:"3..7",hasxp:1b}
data modify storage veinminer:groups emerald set value {id:"emerald",a:"minecraft:emerald_ore",b:"minecraft:deepslate_emerald_ore",xp:"3..7",hasxp:1b}
data modify storage veinminer:groups quartz set value {id:"quartz",a:"minecraft:nether_quartz_ore",b:"minecraft:nether_quartz_ore",xp:"2..5",hasxp:1b}
data modify storage veinminer:groups ancient_debris set value {id:"ancient_debris",a:"minecraft:ancient_debris",b:"minecraft:ancient_debris",xp:"0..1",hasxp:0b}
