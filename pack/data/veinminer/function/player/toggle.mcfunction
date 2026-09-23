execute if score @s veinminer matches 1 store success score @s veinminer.off unless score @s veinminer.off matches 1
execute if score @s veinminer matches 2 run scoreboard players set @s veinminer.off 0
execute if score @s veinminer matches 3 run scoreboard players set @s veinminer.off 1
scoreboard players set @s veinminer 0
execute if score @s veinminer.off matches 1 run return run tellraw @s ["",{text:"⛏ Veinminer ",color:"gold"},{text:"disabled",color:"red"},{text:" for you. ",color:"gray"},{text:"[Turn on]",color:"green",hover_event:{action:"show_text",value:"/trigger veinminer"},click_event:{action:"run_command",command:"/trigger veinminer"}}]
tellraw @s ["",{text:"⛏ Veinminer ",color:"gold"},{text:"enabled",color:"green"},{text:" for you. ",color:"gray"},{text:"[Turn off]",color:"red",hover_event:{action:"show_text",value:"/trigger veinminer"},click_event:{action:"run_command",command:"/trigger veinminer"}}]
