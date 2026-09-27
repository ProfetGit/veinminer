$execute if block ~ ~ ~ $(b) run summon block_display ~ ~ ~-1 {block_state:{Name:"$(b)",id:"$(b)"},Tags:["veinminer.fx","veinminer.fnew","veinminer.b","veinminer.at_zn"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.003f,-0.003f,0.997f],scale:[1.006f,1.006f,1.006f]}}
$execute unless block ~ ~ ~ $(b) run summon block_display ~ ~ ~-1 {block_state:{Name:"$(a)",id:"$(a)"},Tags:["veinminer.fx","veinminer.fnew","veinminer.at_zn"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.003f,-0.003f,0.997f],scale:[1.006f,1.006f,1.006f]}}
execute as @e[type=block_display,tag=veinminer.fnew] run function veinminer:anim/fx_init
tag @s add veinminer.prepped
