scoreboard objectives add vmtest dummy
scoreboard players add #loaded vmtest 1
execute store result score #version_id vmtest run data get storage veinminer:meta version_id
data modify storage veinminer:meta requires append value {text:"Test requirement. ",color:"gray"}
