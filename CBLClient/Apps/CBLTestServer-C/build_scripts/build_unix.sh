#!/bin/bash -ex

# Global define
VERSION=${1}
BLD_NUM=${2}

case "${OSTYPE}" in
    darwin*)  OS="macosx"
              LIBCBL="libcblite*.dylib"
              ZIP_CMD="unzip"
              ZIP_EXT="zip --symlinks"
              ;;
    linux*)   OS="linux"
              LIBCBL="libcblite.so*"
              ZIP_CMD="tar xvf"
              ZIP_EXT="tar.gz"
              ;;
    *)        echo "unknown: $OSTYPE"
              exit 1;;
esac

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
DOWNLOAD_DIR=$SCRIPT_DIR/../downloaded
BUILD_DIR=$SCRIPT_DIR/../build
ZIPS_DIR=$SCRIPT_DIR/../zips

rm -rf $DOWNLOAD_DIR 2> /dev/null
mkdir -p $DOWNLOAD_DIR
pushd $DOWNLOAD_DIR

ZIP_FILENAME=couchbase-lite-c-${OS}-${VERSION}-${BLD_NUM}-enterprise.${ZIP_EXT}
curl -O http://latestbuilds.service.couchbase.com/builds/latestbuilds/couchbase-lite-c/${VERSION}/${BLD_NUM}/${ZIP_FILENAME}
${ZIP_CMD} ${ZIP_FILENAME}
rm ${ZIP_FILENAME}

popd
mkdir -p $BUILD_DIR
pushd $BUILD_DIR

cmake -DCMAKE_PREFIX_PATH=$DOWNLOAD_DIR -DCMAKE_BUILD_TYPE=Release ..
make -j8 install
cp $DOWNLOAD_DIR/lib/$LIBCBL out/bin/
if [ "${OS}" = "linux" ]; then
    cp -Pf $DOWNLOAD_DIR/lib/*.so* out/bin/
fi

ZIP_FILENAME=testserver_${OS}_x64.zip
cp $SCRIPT_DIR/../../CBLTestServer-Dotnet/TestServer/sg_cert.pem out/bin
cp -R $SCRIPT_DIR/../../CBLTestServer-Dotnet/TestServer.NetCore/certs out/bin
cp -R $SCRIPT_DIR/../../CBLTestServer-Dotnet/TestServer.NetCore/Databases out/bin
cp -R $SCRIPT_DIR/../../CBLTestServer-Dotnet/TestServer.NetCore/Files out/bin
pushd out/bin
zip -r ${ZIP_FILENAME} *
mkdir -p $ZIPS_DIR
mv ${ZIP_FILENAME} $ZIPS_DIR
