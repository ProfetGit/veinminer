tp @s ~ ~ ~ ~ ~
execute store result score #yaw veinminer.data run data get entity @s Rotation[0]
execute store result score #pit veinminer.data run data get entity @s Rotation[1]
kill @s
execute if score #pit veinminer.data matches ..-45 run return run data modify storage veinminer:op c merge value {nd:"yp",ny:1,my:-1}
execute if score #pit veinminer.data matches 45.. run return run data modify storage veinminer:op c merge value {nd:"yn",ny:-1,my:1}
execute if score #yaw veinminer.data matches -45..45 run return run data modify storage veinminer:op c merge value {nd:"zp",nz:1,mz:-1}
execute if score #yaw veinminer.data matches 46..135 run return run data modify storage veinminer:op c merge value {nd:"xn",nx:-1,mx:1}
execute if score #yaw veinminer.data matches -135..-46 run return run data modify storage veinminer:op c merge value {nd:"xp",nx:1,mx:-1}
data modify storage veinminer:op c merge value {nd:"zn",nz:-1,mz:1}
