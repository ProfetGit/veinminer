$execute if block ~ ~ ~ $(b) run summon marker ~ ~ ~ {Tags:["veinminer.pend","veinminer.pnew","veinminer.b"]}
$execute unless block ~ ~ ~ $(b) run summon marker ~ ~ ~ {Tags:["veinminer.pend","veinminer.pnew"]}
setblock ~ ~ ~ minecraft:bedrock strict
