#! /bin/sh

jvstmlib=../jvstmdist/jvstm.jar
javaVM=../libs/openjdk-continuation-vm2013-linux-amd64/bin/java

ant jvstmjar
cd src
rm benchmark/vacation/*.class
javac -cp $jvstmlib:. benchmark/vacation/Vacation.java

echo "120/180"
## Nest par WITH update table

echo "\n" >> wC.data
echo "150/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 1 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 1 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 1 -updatePar true -readOnly true >> wC.data

echo "\n" >> wC.data
echo "153/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 2 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 2 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 2 -updatePar true -readOnly true >> wC.data

echo "\n" >> wC.data
echo "156/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 4 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 4 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 4 -updatePar true -readOnly true >> wC.data

echo "\n" >> wC.data
echo "159/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 8 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 8 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 8 -updatePar true -readOnly true >> wC.data

echo "\n" >> wC.data
echo "162/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data

echo "\n" >> wC.data
echo "162-2/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data

echo "\n" >> wC.data
echo "162-3/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wC.data

echo "\n" >> wC.data
echo "165/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 1 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 1 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 1 -updatePar true -readOnly true >> wD.data

echo "\n" >> wD.data
echo "168/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 2 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 2 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 2 -updatePar true -readOnly true >> wD.data

echo "\n" >> wD.data
echo "171/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 4 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 4 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 4 -updatePar true -readOnly true >> wD.data

echo "\n" >> wD.data
echo "174/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 8 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 8 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 8 -updatePar true -readOnly true >> wD.data

echo "\n" >> wD.data
echo "177/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data

echo "\n" >> wD.data
echo "177-2/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 2 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data

echo "\n" >> wD.data
echo "177-3/180"

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 4 -n 2000000 -q 98 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly true >> wD.data


