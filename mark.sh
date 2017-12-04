#git config --global user.email nikitosgleb@gmail.com && git config --global user.name "Gleb Nikitenko"
#git fetch && git checkout master && git merge dev && git push
version=$(git log --format=oneline -n 1 $CIRCLE_SHA1 | grep -oP " [0-9].[0-9].[0-9].([0-9]|[0-9][0-9]|[0-9][0-9][0-9]|[0-9][0-9][0-9][0-9])")
echo $version
#