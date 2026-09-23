tag @s add veinminer.welcomed
execute unless score #welcome veinminer.config matches 1 run return 0
execute if score #require_sneak veinminer.config matches 1 run return run tellraw @s ["",{text:"⛏ Veinminer: ",color:"gold"},{text:"sneak while mining an ore with a pickaxe to break the whole vein. ",color:"gray"},{text:"[Toggle]",color:"aqua",hover_event:{action:"show_text",value:"/trigger veinminer"},click_event:{action:"run_command",command:"/trigger veinminer"}}]
tellraw @s ["",{text:"⛏ Veinminer: ",color:"gold"},{text:"mine an ore with a pickaxe to break the whole vein. ",color:"gray"},{text:"[Toggle]",color:"aqua",hover_event:{action:"show_text",value:"/trigger veinminer"},click_event:{action:"run_command",command:"/trigger veinminer"}}]
