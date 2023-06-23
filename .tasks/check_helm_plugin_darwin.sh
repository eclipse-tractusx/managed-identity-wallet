#!/usr/bin/env zsh

RESULT=$(helm plugin list | tail -n +2)

if [[ $RESULT =~ $1 ]];
then
    echo "Plugin found! Info: $RESULT"
else
    echo "Plugin not found"
    exit 1
fi