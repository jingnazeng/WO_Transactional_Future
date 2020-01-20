#!/bin/sh

if [ "$#" -ne 2 ]; then
    echo "Usage: push server user"
    exit 0
fi
server=$1
user=$2

rsync -ravh --include="*/" --include="*.java" --exclude="*" ${server}:/home/${user}/workspace/WeakTF_PatternBankAgency/WeakTFEnabled_Pattern6/src/ .
