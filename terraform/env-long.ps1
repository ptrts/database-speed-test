$cloudId=$(yc config get cloud-id)
$folderId=$(yc config get folder-id)
$saId = (yc iam service-account get terraform --format json | ConvertFrom-Json).id

$template = Get-Content ".\env.private.ps1.template" -Raw

$result = $ExecutionContext.SessionState.InvokeCommand.ExpandString($template)

$result | Set-Content ".\env.private.ps1" -Encoding UTF8

. "$PSScriptRoot\env-fast.ps1"
