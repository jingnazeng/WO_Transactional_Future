#! /bin/sh

jvstmlib=../jvstmdist/jvstm.jar
javaVM=../libs/openjdk-continuation-vm2013-linux-amd64/bin/java

ant jvstmjar
cd src
rm benchmark/vacation/*.class
javac -cp $jvstmlib:. benchmark/vacation/Vacation.java

$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 200000 -q 1 -u 98 -r 10485 -t 64 -nest false -sib 0 -updatePar false -readOnly false >> results.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 400000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 2 -updatePar true -readOnly false >> results.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 200000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 8 -updatePar true -readOnly false >> results.data
$javaVM -Xms2g -Xmx8g -cp $jvstmlib:. benchmark/vacation/Vacation -c 1 -n 200000 -q 1 -u 98 -r 10485 -t 64 -nest true -sib 16 -updatePar true -readOnly false >> results.data
