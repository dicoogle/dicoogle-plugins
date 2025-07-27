#!/bin/sh

VERSION=$(grep version ../../../pom.xml | grep -v -e '<?xml|~'| head -n 1 | sed 's/[[:space:]]//g' | sed -E 's/<.{0,1}version>//g' | awk '{print $1}')

DICOOGLE_SRC="Dicoogle_v$VERSION-source"
DICOOGLE_BIN="Dicoogle_v$VERSION"


echo "Running Dicoogle $VERSION ..."

rm -rf $DICOOGLE_BIN
rm -rf dicoogle
rm -rf dicoogle-pvt
rm $DICOOGLE_BIN.zip 


echo "Compiling..."
echo "#######################"
#ant
echo "Creating directories..."


echo "#######################"

mkdir -p $DICOOGLE_BIN
mkdir -p $DICOOGLE_BIN/Plugins

echo "Clonning Dicoogle from github"
git clone -b master git@github.com:bioinformatics-ua/dicoogle.git
git clone -b main git@github.com:dicoogle/dicoogle-plugins.git dicoogle-pvt

echo "Compiling core..."

echo "Installing dependencies and compiling UI..."
x=$(pwd)

#cd dicoogle/dicoogle/src/main/java/pt/ua/dicoogle/server/web/webapp/WEB-INF
cd dicoogle/dicoogle/src/main/resources/webapp
npm install
npm run build
cd $x

echo "Installing dependencies and compiling UI...done"


cd dicoogle 
mvn install -Dskip.installnodenpm -Dskip.npm -DskipTests

echo "Compiling core...done"
cd ..



echo "Compiling Plugins..."

cd dicoogle-pvt 
cd plugins/lucene 
mvn install -DskipTests
echo "Compiling Plugins...done"
cd ..
cd filestorage
mvn install -DskipTests




cd ../../..


echo "Copy files..."
echo "#######################"

#Binaries
cp dicoogle/dicoogle/target/dicoogle.jar $DICOOGLE_BIN/
cp dicoogle/README.md $DICOOGLE_BIN/
cp "dicoogle-pvt/plugins/lucene/target/lucene-$VERSION.jar" "$DICOOGLE_BIN/Plugins/"
cp "dicoogle-pvt/plugins/filestorage/target/filestorage-$VERSION.jar" "$DICOOGLE_BIN/Plugins/"
cp ../bin/* "$DICOOGLE_BIN/"

echo "Zipping..."

zip -9 -r $DICOOGLE_BIN.zip $DICOOGLE_BIN

echo "Zipping...done"
#Sources

#echo "Cleaning ..."

#rm -rf dicoogle-pvt
#rm -rf dicoogle

