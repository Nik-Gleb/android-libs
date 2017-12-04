git config --global user.email nikitosgleb@gmail.com && git config --global user.name "Gleb Nikitenko"
git fetch && git checkout master && git merge dev && git push
version=v$(echo $GIT_COMMIT_DESC | grep -oP "[0-9].[0-9].[0-9].([0-9]|[0-9][0-9]|[0-9][0-9][0-9]|[0-9][0-9][0-9][0-9])")
echo $version
#