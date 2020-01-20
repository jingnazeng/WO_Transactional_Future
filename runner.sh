#!/bin/sh

backend="WTF"

benchmark="bank-100-1k-1"


for t in 1 2 4 7 14 28 56
do
	for a in 1 2 3
	do
		java -cp build/classes -Xms8G -Xmx16G benchmark.synthetic.ArrayAccess 100 1 $t 1 10 56 0 0 0 false 1000 1 > /home/shady/futures/results/May09-1813/runs/${benchmark}-${backend}-$t-$a.data 2> /home/shady/futures/results/May09-1813/runs/${benchmark}-${backend}-F-$t-$a.err
	done
done
