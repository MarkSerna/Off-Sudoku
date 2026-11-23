Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

function Find-JdkTool {
    param([Parameter(Mandatory = $true)][string]$ToolName)

    $cmd = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += Join-Path $env:JAVA_HOME "bin\$ToolName"
    }
    $candidates += Get-ChildItem -Path "$env:ProgramFiles\Eclipse Adoptium\*\bin\$ToolName" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    $candidates += Get-ChildItem -Path "$env:ProgramFiles\Java\*\bin\$ToolName" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    $studioJbr = "$env:ProgramFiles\Android\Android Studio\jbr\bin\$ToolName"
    if (Test-Path $studioJbr) {
        $candidates += $studioJbr
    }

    foreach ($path in $candidates) {
        if ($path -and (Test-Path $path)) {
            return $path
        }
    }

    throw "No se encontró $ToolName. Asegúrate de tener JDK 17+ instalado o JAVA_HOME configurado."
}

function Convert-SecureToPlain {
    param([Parameter(Mandatory = $true)][Security.SecureString]$Secure)
    return [System.Net.NetworkCredential]::new('', $Secure).Password
}

function Read-ConfirmedPassword {
    param(
        [Parameter(Mandatory = $true)][string]$PromptText,
        [Parameter(Mandatory = $true)][string]$ConfirmText,
        [switch]$AllowEmpty
    )

    while ($true) {
        $first = Read-Host -Prompt $PromptText -AsSecureString
        $firstPlain = Convert-SecureToPlain $first
        if ($AllowEmpty -and [string]::IsNullOrEmpty($firstPlain)) {
            return ''
        }
        if ($firstPlain.Length -lt 6) {
            Write-Host 'La contraseña debe tener al menos 6 caracteres.' -ForegroundColor Yellow
            continue
        }

        $second = Read-Host -Prompt $ConfirmText -AsSecureString
        $secondPlain = Convert-SecureToPlain $second
        if ($firstPlain -ne $secondPlain) {
            Write-Host 'Las contraseñas no coinciden. Inténtalo de nuevo.' -ForegroundColor Yellow
            continue
        }
        return $firstPlain
    }
}

Write-Host '=== Altikore · Compilación de Release para Google Play ===' -ForegroundColor Cyan

$keytoolExe = Find-JdkTool 'keytool.exe'
$jarsignerExe = Find-JdkTool 'jarsigner.exe'
$propsPath = Join-Path $RepoRoot 'keystore.properties'

if (-not (Test-Path $propsPath)) {
    Write-Host ''
    Write-Host 'No existe keystore.properties en la raíz del proyecto.' -ForegroundColor Yellow
    Write-Host 'Se creará el archivo de firma (Upload Key) altikore-sudoku-release.jks y keystore.properties (ambos ignorados por Git).'

    $keystoreName = 'altikore-sudoku-release.jks'
    $keystorePath = Join-Path $RepoRoot $keystoreName
    $keyAlias = 'altikore-sudoku'

    if (Test-Path $keystorePath) {
        $reuse = Read-Host "Ya existe $keystoreName. ¿Deseas usarlo para generar keystore.properties? [S/n]"
        if ($reuse -match '^[nN]') {
            throw "Abortado para no sobrescribir $keystoreName."
        }
        $storePass = Convert-SecureToPlain (Read-Host 'Contraseña actual del keystore' -AsSecureString)
        $keyPassInput = Read-Host 'Contraseña de la llave [Enter si es la misma del keystore]' -AsSecureString
        $keyPass = Convert-SecureToPlain $keyPassInput
        if ([string]::IsNullOrEmpty($keyPass)) {
            $keyPass = $storePass
        }
    } else {
        $storePass = Read-ConfirmedPassword `
            -PromptText 'Elige una contraseña para el keystore (mín. 6 caracteres)' `
            -ConfirmText 'Confirma la contraseña del keystore'

        $keyPass = Read-ConfirmedPassword `
            -PromptText 'Contraseña para la llave [Enter para usar la misma — recomendado en PKCS12]' `
            -ConfirmText 'Confirma la contraseña de la llave' `
            -AllowEmpty
        if ([string]::IsNullOrEmpty($keyPass)) {
            $keyPass = $storePass
        }

        try {
            $env:ALTIKORE_STORE_PASS = $storePass
            $env:ALTIKORE_KEY_PASS = $keyPass
            & $keytoolExe -genkeypair -v `
                -keystore $keystorePath `
                -storetype PKCS12 `
                -keyalg RSA `
                -keysize 2048 `
                -validity 10000 `
                -alias $keyAlias `
                -storepass:env ALTIKORE_STORE_PASS `
                -keypass:env ALTIKORE_KEY_PASS `
                -dname 'CN=Altikore, O=Altikore'

            if ($LASTEXITCODE -ne 0) {
                throw "keytool falló con código de salida $LASTEXITCODE."
            }
        } finally {
            Remove-Item Env:ALTIKORE_STORE_PASS -ErrorAction SilentlyContinue
            Remove-Item Env:ALTIKORE_KEY_PASS -ErrorAction SilentlyContinue
        }
    }

    $propsLines = @(
        '# Generado localmente por build-play-release.ps1 (ignorado por Git)'
        "storeFile=$keystoreName"
        "storePassword=$storePass"
        "keyAlias=$keyAlias"
        "keyPassword=$keyPass"
    ) -join "`n"
    [System.IO.File]::WriteAllText($propsPath, "$propsLines`n", [System.Text.UTF8Encoding]::new($false))
    Write-Host "Archivo keystore.properties creado correctamente." -ForegroundColor Green
} else {
    Write-Host "Usando configuración existente: $propsPath" -ForegroundColor Green
}

Write-Host ''
Write-Host 'Compilando AAB y APK de release firmados con R8...' -ForegroundColor Cyan
& "$RepoRoot\gradlew.bat" :app:bundleRelease :app:assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) {
    throw "Gradle falló con código $LASTEXITCODE."
}

$aabPath = Join-Path $RepoRoot 'app\build\outputs\bundle\release\app-release.aab'
$apkPath = Join-Path $RepoRoot 'app\build\outputs\apk\release\app-release.apk'
$mappingPath = Join-Path $RepoRoot 'app\build\outputs\mapping\release\mapping.txt'

if (-not (Test-Path $aabPath)) {
    throw "No se generó el App Bundle esperado en $aabPath"
}

Write-Host ''
Write-Host 'Verificando firma del App Bundle con jarsigner...' -ForegroundColor Cyan
& $jarsignerExe -verify $aabPath
if ($LASTEXITCODE -ne 0) {
    throw "jarsigner reportó un problema verificando $aabPath"
}

Write-Host ''
Write-Host '=== Artefactos listos para Google Play Console ===' -ForegroundColor Green
foreach ($filePath in @($aabPath, $apkPath, $mappingPath)) {
    if (Test-Path $filePath) {
        $sizeMb = [Math]::Round(((Get-Item $filePath).Length / 1MB), 2)
        Write-Host (" - {0} ({1} MB)" -f $filePath, $sizeMb)
    }
}

Write-Host ''
Write-Host 'IMPORTANTE: Guarda una copia de seguridad de tu archivo .jks y sus contraseñas en un gestor seguro fuera de este repositorio.' -ForegroundColor Yellow
