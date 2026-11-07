$env:JWT_SECRET = "fincore-super-secret-key-which-is-long-enough-123456"

Set-Location $PSScriptRoot

mvn spring-boot:run