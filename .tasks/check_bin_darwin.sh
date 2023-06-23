#!/usr/bin/env zsh

RESULT=$(which $1)

if [[ $RESULT =~ "not found" ]];
then
    echo "Binary not found!"
    echo "You can install it from here: $2"
    exit 1
else
    echo "Binary found in: $RESULT"
fi