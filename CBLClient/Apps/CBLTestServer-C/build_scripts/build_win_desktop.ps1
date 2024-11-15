param(
    [Parameter(Mandatory=$true)][string]$Version,
    [Parameter(Mandatory=$true)][string]$BuildNum,
    [Parameter(Mandatory=$true)][string]$Edition
)

#$ErrorActionPreference="Stop"
$DOWNLOAD_DIR="$PSScriptRoot\..\downloaded"
$BUILD_DIR="$PSScriptRoot\..\build"
$ZIPS_DIR="$PSScriptRoot\..\zips"

Remove-Item -Recurse -Force -ErrorAction Ignore $DOWNLOAD_DIR
New-Item -ItemType Directory $DOWNLOAD_DIR

$ZIP_FILENAME="couchbase-lite-c-$Edition-$Version-$BuildNum-windows-x86_64.zip"
Invoke-WebRequest http://latestbuilds.service.couchbase.com/builds/latestbuilds/couchbase-lite-c/${Version}/${BuildNum}/${ZIP_FILENAME} -OutFile "$DOWNLOAD_DIR\$ZIP_FILENAME"
Push-Location $DOWNLOAD_DIR
7z x -y $ZIP_FILENAME
Remove-Item $ZIP_FILENAME
Pop-Location

New-Item -ErrorAction Ignore -ItemType Directory $BUILD_DIR
Push-Location $BUILD_DIR

try {
    & "C:\Program Files\CMake\bin\cmake.exe" -G "Visual Studio 15 2017" -A x64 -DCMAKE_PREFIX_PATH="${DOWNLOAD_DIR}/libcblite-${Version}" -DCMAKE_BUILD_TYPE=Release ..
    if($LASTEXITCODE -ne 0) {
        throw "Cmake failed!"
    } 

    & "C:\Program Files\CMake\bin\cmake.exe" --build . --target install --config Release --parallel 12
    if($LASTEXITCODE -ne 0) {
        throw "Build failed!"
    } 

    Copy-Item "$DOWNLOAD_DIR\libcblite-$VERSION\bin\cblite.dll" out\bin
    Copy-Item -ErrorAction Ignore $PSScriptRoot\..\..\CBLTestServer-Dotnet\TestServer\sg_cert.pem out\bin
    Copy-Item -ErrorAction Ignore -Recurse $PSScriptRoot\..\..\CBLTestServer-Dotnet\TestServer.NetCore\certs out\bin
    Copy-Item -ErrorAction Ignore -Recurse $PSScriptRoot\..\..\CBLTestServer-Dotnet\TestServer.NetCore\Databases out\bin
    Copy-Item -ErrorAction Ignore -Recurse $PSScriptRoot\..\..\CBLTestServer-Dotnet\TestServer.NetCore\Files out\bin
    Push-Location out\bin
    7za a -bb1 -tzip -mx5 $ZIPS_DIR\testserver_windesktop-x86_64_$Edition.zip testserver.exe cblite.dll certs Databases Files sg_cert.pem
    Pop-Location
} finally {
    Pop-Location
}