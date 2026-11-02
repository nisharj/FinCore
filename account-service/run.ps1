$env:FINCORE_INTERNAL_SECRET = "replace-with-a-long-random-secret"

Set-Location $PSScriptRoot

mvn spring-boot:run