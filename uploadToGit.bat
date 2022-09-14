@echo off
call git add .
call git commit -u "Upload after build"
call git push