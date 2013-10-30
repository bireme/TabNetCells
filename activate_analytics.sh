#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: activate_analytics <root_dir>"
  exit 1
fi

OLD_OPEN="<!-- GOOGLE_ANALYTICS"
OLD_CLOSE="GOOGLE_ANALYTICS -->"
NEW=" "

find $1 -name *.html | xargs sed -i -e "s/$OLD_OPEN/$NEW/g" -e "s/$OLD_CLOSE/$NEW/g"
