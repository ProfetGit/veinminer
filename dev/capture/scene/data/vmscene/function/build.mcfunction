fill -2 -60 0 16 -48 20 minecraft:stone
fill 0 -55 8 8 -52 12 minecraft:air
setblock 2 -53 8 minecraft:wall_torch[facing=south]
setblock 7 -53 8 minecraft:wall_torch[facing=south]
setblock 7 -53 12 minecraft:wall_torch[facing=north]
setblock 7 -52 9 minecraft:light[level=15]
setblock 7 -52 11 minecraft:light[level=15]
setblock 9 -54 10 minecraft:iron_ore
setblock 9 -54 9 minecraft:iron_ore
setblock 9 -54 11 minecraft:iron_ore
setblock 9 -53 10 minecraft:iron_ore
setblock 9 -55 10 minecraft:iron_ore
setblock 9 -53 11 minecraft:iron_ore
setblock 9 -55 9 minecraft:iron_ore
setblock 9 -52 11 minecraft:iron_ore
setblock 10 -54 10 minecraft:iron_ore
setblock 10 -53 10 minecraft:iron_ore
setblock 10 -54 11 minecraft:iron_ore
setblock 11 -54 10 minecraft:iron_ore
setblock 10 -55 9 minecraft:iron_ore
kill @e[type=item]
data modify storage vmscene:cfg id set value "iron"
