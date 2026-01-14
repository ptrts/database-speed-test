. "$PSScriptRoot\env.private.ps1"

$Env:YC_TOKEN=$(yc iam create-token --impersonate-service-account-id $Env:YC_SA_TERRAFORM_ID)
