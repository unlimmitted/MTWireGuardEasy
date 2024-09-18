$path = Get-Location
Set-Location -Path "../../MTWireGuardEasy-frontend" -ErrorAction Stop

npm -i
npm run build

if (Test-Path "./dist/") {
    Copy-Item -Recurse -Force "./dist/*" "$path/src/main/resources/static/"

} else {
    Write-Error "Building error"
    Set-Location -Path "D:\dev\Projects\intellij\MTWGEasy" -ErrorAction Stop
    exit 1

}

Set-Location -Path "D:\dev\Projects\intellij\MTWGEasy" -ErrorAction Stop