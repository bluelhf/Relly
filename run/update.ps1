Import-Module BitsTransfer


$MC = "1.16.5";
$Info = (Invoke-WebRequest -UseBasicParsing -Uri "https://papermc.io/api/v2/projects/paper/versions/$MC").Content | ConvertFrom-Json
$VersionHistory = (Get-Content -Path "version_history.json" -ErrorAction SilentlyContinue) | ConvertFrom-Json -ErrorAction SilentlyContinue

$Build = $Info.builds[-1]
$LastBuild = $VersionHistory.currentVersion | Select-String -Pattern "git-Paper-(\d+)"
$LastMC = $VersionHistory.currentVersion | Select-String -Pattern ".*\(MC: (^[^\)]*)"
if (("$LastBuild" -eq "") -or ($LastBuild -ne $Build) -or ($MC -ne $LastMC)) {
    $BuildInfo = (Invoke-WebRequest -UseBasicParsing -Uri "https://papermc.io/api/v2/projects/paper/versions/$MC/builds/$Build") | ConvertFrom-Json
    $Version = $BuildInfo.downloads.application.name;
    Write-Host "Downloading $Version"
    $Job = Start-BitsTransfer -Source https://papermc.io/api/v2/projects/paper/versions/$MC/builds/$Build/downloads/$Version -Destination ./server.jar

}