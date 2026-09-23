scoreboard players operation #total veinminer.data = #mined veinminer.data
scoreboard players add #total veinminer.data 1
title @s actionbar [{text:"⛏ ",color:"gold"},{text:"Veinminer: ",color:"gray"},{score:{name:"#total",objective:"veinminer.data"},color:"white"},{text:" blocks mined",color:"gray"}]
