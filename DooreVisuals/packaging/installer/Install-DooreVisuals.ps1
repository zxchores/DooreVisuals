param(
    [string]$McDir = (Join-Path $env:APPDATA '.minecraft'),
    [string]$LoaderVersion = '0.19.3',
    [string]$GameVersion = '1.21.11'
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Fail($text) {
    Write-Host ''
    Write-Host $text -ForegroundColor Red
    Write-Host ''
    Read-Host 'Нажми Enter, чтобы закрыть'
    exit 1
}

Write-Host ''
Write-Host '  DooreVisuals — установка' -ForegroundColor Cyan
Write-Host ''

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Get-ChildItem -Path $here -Filter 'doorevisuals-*.jar' | Where-Object { $_.Name -notlike '*sources*' } | Select-Object -First 1
if (-not $jar) { Fail 'Рядом с установщиком нет файла doorevisuals-*.jar. Распакуй архив целиком.' }

$profilesPath = Join-Path $McDir 'launcher_profiles.json'
if (-not (Test-Path $profilesPath)) {
    Fail "Не найден Minecraft Launcher ($McDir). Установи его, запусти один раз и закрой, потом повтори."
}

# 1. Fabric Loader: the version description comes from the Fabric meta server; the launcher downloads the libraries itself.
$versionId = "fabric-loader-$LoaderVersion-$GameVersion"
$versionDir = Join-Path $McDir "versions\$versionId"
Write-Host "1/3  Fabric Loader $LoaderVersion для Minecraft $GameVersion"
try {
    $json = (Invoke-WebRequest -UseBasicParsing -Uri "https://meta.fabricmc.net/v2/versions/loader/$GameVersion/$LoaderVersion/profile/json").Content
} catch {
    Fail 'Не удалось скачать описание Fabric Loader. Проверь интернет и повтори.'
}
New-Item -ItemType Directory -Force -Path $versionDir | Out-Null
[IO.File]::WriteAllText((Join-Path $versionDir "$versionId.json"), $json, $utf8)

# 2. The profile in the launcher.
Write-Host '2/3  Профиль DooreVisuals в лаунчере'
Copy-Item $profilesPath "$profilesPath.doore-backup" -Force
$profiles = Get-Content -Raw -Encoding UTF8 $profilesPath | ConvertFrom-Json
if (-not $profiles.profiles) {
    $profiles | Add-Member -NotePropertyName profiles -NotePropertyValue (New-Object PSObject)
}
$stamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd'T'HH:mm:ss.000'Z'")
$entry = [PSCustomObject]@{
    name          = 'DooreVisuals'
    type          = 'custom'
    created       = $stamp
    lastUsed      = $stamp
    icon          = 'Furnace'
    lastVersionId = $versionId
    javaArgs      = '-Xmx3G'
}
if ($profiles.profiles.PSObject.Properties['doorevisuals']) {
    $profiles.profiles.PSObject.Properties.Remove('doorevisuals')
}
$profiles.profiles | Add-Member -NotePropertyName doorevisuals -NotePropertyValue $entry
[IO.File]::WriteAllText($profilesPath, ($profiles | ConvertTo-Json -Depth 20), $utf8)

# 3. The mod itself (Fabric API is inside it); an older copy is replaced.
Write-Host '3/3  Мод'
$mods = Join-Path $McDir 'mods'
New-Item -ItemType Directory -Force -Path $mods | Out-Null
Get-ChildItem -Path $mods -Filter 'doorevisuals-*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
Copy-Item $jar.FullName (Join-Path $mods $jar.Name) -Force

Write-Host ''
Write-Host 'Готово!' -ForegroundColor Green
Write-Host 'Открой Minecraft Launcher, выбери профиль «DooreVisuals» и нажми «Играть».'
Write-Host '(Если лаунчер был открыт — закрой и открой его заново.)'
Write-Host ''
Read-Host 'Нажми Enter, чтобы закрыть'
