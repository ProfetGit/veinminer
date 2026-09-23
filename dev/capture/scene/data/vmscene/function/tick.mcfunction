execute as @a[tag=!vmscene.ready,limit=1] at @s run function vmscene:setup
execute if score #t vmscene matches 0.. run scoreboard players add #t vmscene 1
execute if score #t vmscene matches 50 run function vmscene:trigger
