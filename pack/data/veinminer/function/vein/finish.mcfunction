execute if score #dmg veinminer.data matches 1.. run function veinminer:tool/apply_damage
execute if score #xp veinminer.data matches 1.. run function veinminer:vein/drop_xp
execute if score #worn veinminer.data matches 1 run return run title @s actionbar {text:"Veinminer stopped: your pickaxe is almost broken",color:"red"}
execute if score #feedback veinminer.config matches 1 run function veinminer:vein/feedback
