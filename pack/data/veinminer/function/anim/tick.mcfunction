execute unless entity @e[type=marker,tag=veinminer.ctl,limit=1] unless entity @e[type=block_display,tag=veinminer.fx,limit=1] unless entity @e[type=item_display,tag=veinminer.ghost,limit=1] unless entity @e[type=marker,tag=veinminer.pend,limit=1] run return 0
scoreboard players remove @e[type=marker,tag=veinminer.pend] veinminer.t 1
scoreboard players remove @e[type=block_display,tag=veinminer.fx] veinminer.t 1
scoreboard players remove @e[type=item_display,tag=veinminer.ghost] veinminer.t 1
function veinminer:anim/keys
kill @e[type=block_display,tag=veinminer.fx,scores={veinminer.t=..-12}]
execute as @e[type=item_display,tag=veinminer.ghost,tag=!veinminer.hide,scores={veinminer.t=-12..-1}] at @s run function veinminer:anim/fly
execute as @e[type=marker,tag=veinminer.ctl,tag=!veinminer.new] at @s run function veinminer:anim/ctl
execute as @e[type=item_display,tag=veinminer.ghost,scores={veinminer.t=..-30}] at @s run function veinminer:anim/drop {pd:10,hop:0.12,dy:0}
kill @e[type=marker,tag=veinminer.pend,scores={veinminer.t=..-40}]
