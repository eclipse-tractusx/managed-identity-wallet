git filter-branch --env-filter '
OLD_EMAIL="m.schmidt.extern@cluetec.de"
NEW_NAME="Maximilian Schmidt"
NEW_EMAIL="maximilian.schmidt.external@zf.com"
if [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL" ]
then
  export GIT_COMMITTER_NAME="$NEW_NAME"
  export GIT_COMMITTER_EMAIL="$NEW_EMAIL"
fi
if [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL" ]
then
  export GIT_AUTHOR_NAME="$NEW_NAME"
  export GIT_AUTHOR_EMAIL="$NEW_EMAIL"
fi
' --tag-name-filter cat -- --branches --tags


git filter-branch --env-filter '
OLD_EMAIL="info@pmoscode.de"
NEW_NAME="m.schmidt.extern@cluetec.de"
NEW_EMAIL="peter.motzko@volkswagen.de"
if [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL" ]
then
  export GIT_COMMITTER_NAME="$NEW_NAME"
  export GIT_COMMITTER_EMAIL="$NEW_EMAIL"
fi
if [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL" ]
then
  export GIT_AUTHOR_NAME="$NEW_NAME"
  export GIT_AUTHOR_EMAIL="$NEW_EMAIL"
fi
' --tag-name-filter cat -- --branches --tags
