<# Executes exactly one resumable TEST 1 or TEST 2 phase per invocation. #>
[CmdletBinding()]
param([Parameter(Mandatory)][ValidateSet('1','2')][string]$Test,[switch]$Resume)
Set-StrictMode -Version Latest; $ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path; $commit=(git -C $repo rev-parse HEAD).Trim()
$world='New World-';$origin='0 6 0';$remote='1024 6 0';$root=Join-Path $repo '.e2e\evidence';$prism=Join-Path $repo '.e2e\tooling\prism';$mc=Join-Path $prism 'instances\1.12.2\minecraft'
$node=Join-Path $repo '.e2e\tooling\node\node.exe';$mct=Join-Path $repo '.e2e\tooling\mc-pilot-source\node_modules\@kzheart_\mc-pilot\bin\mct';$env:MCT_WS_URL='ws://127.0.0.1:25560'
$old=@(Get-ChildItem $root -Directory -ErrorAction SilentlyContinue|Sort-Object LastWriteTime -Descending|ForEach-Object {$p=Join-Path $_ 'state.json';if(Test-Path $p){Get-Content $p -Raw|ConvertFrom-Json}})|Where-Object {$_.testId -eq $Test -and $_.testedCommit -eq $commit -and $_.world -eq $world -and $_.status -eq 'RUNNING'}|Select-Object -First 1
if($old){$state=$old;$evidence=$state.evidence}else{$evidence=Join-Path $root ("enderterg-1.1.0-"+(Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ'));New-Item -ItemType Directory -Force $evidence|Out-Null;$state=[ordered]@{runId=(Split-Path $evidence -Leaf);testId=$Test;phase='INIT';status='RUNNING';testedCommit=$commit;world=$world;origin=$origin;remote=$remote;bindingId=$null;originalEntityUuid=$null;recalledEntityUuid=$null;timestamps=@();lastError=$null;evidence=$evidence}}
function Save-State{$state.timestamps+=@{utc=(Get-Date).ToUniversalTime().ToString('o');phase=$state.phase};$state|ConvertTo-Json -Depth 8|Set-Content (Join-Path $evidence 'state.json') -Encoding utf8;$state|ConvertTo-Json -Depth 8|Set-Content (Join-Path $evidence 'result.json') -Encoding utf8}
function Mct($name,[string[]]$mctArgs){$out=& $node $mct @mctArgs 2>&1;$text=$out -join "`n";Set-Content (Join-Path $evidence "$name.json") $text -Encoding utf8;if($LASTEXITCODE -ne 0){throw "MCT $name failed"};$r=$text|ConvertFrom-Json;if(-not $r.success -or -not $r.data.success){throw "MCT $name rejected"};$r.data.data}
function Cmd($name,$text){Mct $name @('chat','command',$text,'--via','client')|Out-Null}
function Snap($name){$x=Mct $name @('entity','list','--radius','32');$x|ConvertTo-Json -Depth 8|Set-Content (Join-Path $evidence "$name.snapshot.json");$x}
try{Save-State;switch($state.phase){
'INIT'{Copy-Item (Join-Path $repo 'build\libs\endertag-1.1.0.jar') (Join-Path $mc 'mods\endertag-1.1.0.jar') -Force;$state.phase='INSTANCE_READY'}
'INSTANCE_READY'{$s=$null;try{$s=Mct 'instance-status' @('status','all')}catch{};if(-not $s){Start-Process (Join-Path $prism 'prismlauncher.exe') -ArgumentList '--launch','1.12.2' -WorkingDirectory $prism -WindowStyle Hidden|Out-Null;throw 'INSTANCE_STARTING'};$state.phase='WORLD_READY'}
'WORLD_READY'{$s=Mct 'world-status' @('status','all');if(-not $s.inWorld){Mct 'world-join' @('client','singleplayer','join',$world,'--timeout','30')|Out-Null};$state.phase='ENTITY_CREATED'}
'ENTITY_CREATED'{Cmd 'creative' '/gamemode creative @p';Cmd 'cleanup' '/kill @e[type=minecraft:cow,r=32]';Cmd 'origin' "/tp @p $origin";Cmd 'give' '/give @p endertag:ender_tag 1 0 {display:{Name:"E2E Terg"}}';Cmd 'summon' '/summon cow 2 6 0 {CustomName:"E2E-Cow",CustomNameVisible:1b,Health:10.0f}';$c=@((Snap 'original').entities|Where-Object {$_.type -eq 'minecraft:cow' -and $_.name -eq 'E2E-Cow'})|Select-Object -First 1;if(-not $c){throw 'Original cow absent'};$state.originalEntityUuid=$c.uuid;$state.phase='BOUND'}
'BOUND'{Mct 'bind-hotbar' @('inventory','hotbar','0')|Out-Null;Mct 'bind' @('entity','interact','--nearest')|Out-Null;$state.phase=if($Test -eq '1'){'RECALLED'}else{'ORIGIN_UNLOADED'}}
'RECALLED'{Cmd 'move' '/tp @p 32 6 0';Mct 'recall' @('inventory','use')|Out-Null;$state.phase='VERIFIED'}
'VERIFIED'{$c=@((Snap 'recalled').entities|Where-Object {$_.type -eq 'minecraft:cow' -and $_.name -eq 'E2E Terg'});if($c.Count -ne 1 -or -not $c[0].alive -or $c[0].maxHealth -ne 10){throw 'TEST1 verification failed'};$state.recalledEntityUuid=$c[0].uuid;$state.phase='COMPLETE'}
'ORIGIN_UNLOADED'{Cmd 'remote' "/tp @p $remote";$state.phase='RECALLED_REMOTE'}
'RECALLED_REMOTE'{Mct 'remote-recall' @('inventory','use')|Out-Null;$state.phase='REMOTE_VERIFIED'}
'REMOTE_VERIFIED'{$c=@((Snap 'remote').entities|Where-Object {$_.type -eq 'minecraft:cow' -and $_.name -eq 'E2E Terg'});if($c.Count -ne 1 -or -not $c[0].alive){throw 'TEST2 remote verification failed'};$state.recalledEntityUuid=$c[0].uuid;$state.phase='ORIGIN_RELOADED'}
'ORIGIN_RELOADED'{Cmd 'return-origin' '/tp @p 32 6 0';$state.phase='DUPLICATE_CHECKED'}
'DUPLICATE_CHECKED'{$stale=@((Snap 'origin-reloaded').entities|Where-Object {$_.type -eq 'minecraft:cow' -and $_.name -eq 'E2E Terg'});if($stale.Count -ne 0){throw 'TEST2 stale entity exists at origin'};$state.phase='COMPLETE'}
'COMPLETE'{$state.status='PASS'}}
if($state.phase -eq 'COMPLETE'){$state.status='PASS'};$state.lastError=$null;if(Test-Path (Join-Path $mc 'logs\latest.log')){Copy-Item (Join-Path $mc 'logs\latest.log') (Join-Path $evidence 'latest.log') -Force};Save-State
}catch{$state.status=if($_.Exception.Message -eq 'INSTANCE_STARTING'){'RUNNING'}else{'FAIL'};$state.lastError=$_.Exception.Message;Save-State;if($state.status -eq 'FAIL'){throw}}
