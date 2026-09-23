tellraw @s ["",{text:"\n⛏ Veinminer settings ",color:"gold",bold:true},{text:"v1.0.0",color:"dark_gray"}]
data modify storage veinminer:menu row set value []
function veinminer:settings/max_button {v:16}
function veinminer:settings/max_button {v:32}
function veinminer:settings/max_button {v:64}
function veinminer:settings/max_button {v:128}
function veinminer:settings/max_button {v:256}
tellraw @s ["",{text:" Max blocks per vein: ",color:"gray"},{score:{name:"#max_blocks",objective:"veinminer.config"},color:"white"},{text:"  "},{storage:"veinminer:menu",nbt:"row[]",interpret:true,separator:" "}]
function veinminer:settings/row_bool {key:"require_sneak",label:"Require sneaking",hint:"Only veinmine while sneaking"}
data modify storage veinminer:menu row set value []
function veinminer:settings/choice {key:"drops",v:0,label:"At mined block",hint:"Drops and XP appear where you broke the first block"}
function veinminer:settings/choice {key:"drops",v:1,label:"At player",hint:"Drops and XP appear at your feet"}
tellraw @s ["",{text:" Drops: ",color:"gray"},{storage:"veinminer:menu",nbt:"row[]",interpret:true,separator:" "}]
function veinminer:settings/row_bool {key:"durability",label:"Use tool durability",hint:"Each extra block costs durability (Unbreaking applies); stops before the tool breaks"}
function veinminer:settings/row_bool {key:"diagonal",label:"Diagonal connections",hint:"Treat ores touching only at edges/corners as one vein"}
function veinminer:settings/row_bool {key:"feedback",label:"Action bar message",hint:"Show how many blocks were mined"}
function veinminer:settings/row_bool {key:"welcome",label:"Join hint",hint:"Explain Veinminer to each player once"}
data modify storage veinminer:menu row set value []
function veinminer:settings/ore {key:"coal",label:"Coal"}
function veinminer:settings/ore {key:"copper",label:"Copper"}
function veinminer:settings/ore {key:"iron",label:"Iron"}
function veinminer:settings/ore {key:"gold",label:"Gold"}
function veinminer:settings/ore {key:"redstone",label:"Redstone"}
function veinminer:settings/ore {key:"lapis",label:"Lapis"}
function veinminer:settings/ore {key:"diamond",label:"Diamond"}
function veinminer:settings/ore {key:"emerald",label:"Emerald"}
function veinminer:settings/ore {key:"nether_gold",label:"Nether Gold"}
function veinminer:settings/ore {key:"quartz",label:"Quartz"}
function veinminer:settings/ore {key:"ancient_debris",label:"Ancient Debris"}
tellraw @s ["",{text:" Ores: ",color:"gray"},{storage:"veinminer:menu",nbt:"row[]",interpret:true,separator:" "}]
tellraw @s ["",{text:" [Reset to defaults]",color:"yellow",hover_event:{action:"show_text",value:"Restore every setting to its default"},click_event:{action:"run_command",command:"/function veinminer:settings/reset"}},{text:"  "},{text:"[Uninstall]",color:"dark_red",hover_event:{action:"show_text",value:"Remove all Veinminer scoreboards and data"},click_event:{action:"suggest_command",command:"/function veinminer:uninstall"}}]
