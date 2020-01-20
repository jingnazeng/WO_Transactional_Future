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

