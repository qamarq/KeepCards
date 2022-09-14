#!/bin/sh
git checkout dev
git add .
git commit -am "Upload after build"
git push
echo Press Enter...
read