execute as @e[type=marker,tag=veinminer.ctl,tag=!veinminer.new] at @s run function veinminer:anim/flush_one
execute as @e[type=item_display,tag=veinminer.ghost] at @s run function veinminer:anim/drop {pd:10,hop:0.12}
kill @e[type=block_display,tag=veinminer.fx]
kill @e[type=marker,tag=veinminer.pend]
kill @e[type=marker,tag=veinminer.ctl]
