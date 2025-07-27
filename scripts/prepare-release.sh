#!/bin/sh

tmpdir=$(mktemp -d /tmp/orekit-prepare-release.XXXXXX)
trap "rm -fr $tmpdir" 0
trap "rewind_git ; exit 1" 1 2 15

gitlab_fqdn=gitlab.orekit.org
gitlab_owner=orekit
gitlab_project=orekit
gitlab_api=https://${gitlab_fqdn}/api/v4/projects/${gitlab_owner}%2F${gitlab_project}

complain()
{
    echo "$1" 1>&2
    rewind_git
    exit 1
}

rewind_git()
{
    if test ! -z "$start_branch" && test "$(cd $top ; git branch --show-current)" != "$start_branch" ; then
        # we need to clean up the branches and rewind everything
        (cd $top ; git reset -q --hard; git checkout $start_branch)
        if test ! -z "$(cd $top ; git branch --list $rc_branch)" ; then
            (cd $top ; git branch -D $rc_branch)
            test -z "$(search_in_repository branches ${rc_branch} .[].name)" || delete_in_repository branches ${rc_branch}
        fi
        if test ! -z "$rc_tag" ; then
          if test ! -z "$(cd $top ; git tag --list $rc_tag)" ; then
              (cd $top ; git tag -d $rc_tag)
              test -z "$(search_in_repository tags ${rc_tag} .[].name)" || delete_in_repository tags ${rc_tag}
          fi
        fi
        if test "$delete_release_branch_on_cleanup" = "true" ; then
            (cd $top ; git branch -D $release_branch)
            test -z "$(search_in_repository branches ${release_branch} .[].name)" || delete_in_repository branches ${release_branch}
        fi
        (cd $top ; git fetch --prune ${origin})
        echo "everything has been cleaned, branch set back to $start_branch" 1>&2
    fi
}

request_confirmation()
{
    answer=""
    while test "$answer" != "yes" && test "$answer" != "no" ; do
        read -p "$1 (enter yes to continue, no to stop the release process) " answer
    done
    test $answer = "yes" || complain "release process stopped at user request"
}

search_in_repository()
{
    if test -z "$gitlab_token" ; then
        echo ""
    else
        curl \
          --silent \
          --request GET \
          --header "PRIVATE-TOKEN: $gitlab_token" \
          --data "search=^${2}$" \
          ${gitlab_api}/repository/$1 \
        | jq --raw-output "$3"
    fi
}

delete_in_repository()
{
    if test ! -z "$gitlab_token" ; then
      curl \
        --silent \
        --request DELETE \
        --header "PRIVATE-TOKEN: $gitlab_token" \
        ${gitlab_api}/repository/$1/$2
    fi
}

get_mr()
{
    if test -z "$gitlab_token" ; then
        echo ""
    else
        curl \
          --silent \
          --request GET \
          --header "PRIVATE-TOKEN: $gitlab_token" \
          ${gitlab_api}/merge_requests/$1 \
        | jq --raw-output "$2"
    fi
}

# find top level directory
top=$(cd $(dirname $0)/.. ; pwd)

# safety checks
for cmd in git sed xsltproc tee sort tail curl ; do
    which -s $cmd || complain "$cmd command not found"
done
git -C "$top" rev-parse 2>/dev/null        || complain "$top does not contain a git repository"
test -f $top/pom.xml                       || complain "$top/pom.xml not found"
test -d $top/src/main/java/org/orekit/time || complain "$top/src/main/java/org/orekit/time not found"
origin="$(cd $top ; git remote -v | sed -n 's,\([^ \t]*\)\t*.*${gitlab_fqdn}:${gitlab_owner}/${gitlab_project}.git.*(push).*,\1,p')"

# get users credentials
gitlab_token=""
while test -z "$gitlab_token" ; do
    echo "enter your gitlab private token"
    stty_orig=$(stty -g)
    stty -echo
    read gitlab_token
    stty $stty_orig
done

start_branch=$(cd $top ; git branch --show-current)
start_sha=$(cd $top ; git rev-parse --verify HEAD)
echo "start branch is ${start_branch}, commit ${start_sha}"
test -z "$(cd $top ; git status --porcelain)" || complain "there are uncommitted changes in the branch"

# extract version numbers
pom_version=$(xsltproc $top/scripts/get-pom-version.xsl $top/pom.xml)
changes_version=$(xsltproc $top/scripts/get-changes-version.xsl $top/src/changes/changes.xml)
release_version=$(echo $pom_version | sed 's,-SNAPSHOT,,')
release_type=$(echo $release_version | sed -e "s,^[0-9]*\.0$,major," -e "s,^[0-9]*\.[1-9][0-9]*$,minor," -e "s,^[0-9]*\.[0-9]*\.[0-9]*$,patch,")
hipparchus_version=$(xsltproc $top/scripts/get-hipparchus-version.xsl $top/pom.xml)
test "$pom_version"     != "$release_version" || complain "$pom_version is not a -SNAPSHOT version"
test "$release_version"  = "$changes_version" || complain "wrong version in changes.xml ($changes_version instead of $release_version)"
echo "current version is $pom_version"
echo "release version will be $release_version, a $release_type release, depending on Hipparchus $hipparchus_version"
request_confirmation "do you agree with these version numbers?"

# reuse existing release branch for patch release, create it otherwise
release_branch=$(echo $release_version | sed 's,\([0-9]\+.[0-9]\+\)[.0-9]*,release-\1,')
if test "$release_type" = patch ; then
    (cd $top ; test ! -z $(git branch --list $release_branch)) || complain "branch $release_branch doesn't exist, stopping"
    delete_release_branch_on_cleanup="false"
else
    (cd $top ; git branch $release_branch) || complain "branch $release_branch already exist, stopping"
    delete_release_branch_on_cleanup="true"
fi
(cd $top ; git checkout $release_branch)

# compute release candidate number
last_rc=$(cd $top; git tag -l ${release_version}-RC* | sed 's,.*-RC,,' | sort -n | tail -1)
if test -z "$last_rc" ; then
    next_rc=1
else
    next_rc=$(expr $last_rc + 1)
fi
rc_tag="${release_version}-RC$next_rc"

# create release candidate branch
rc_branch="RC${next_rc}-${release_version}"
test -z "$(cd $top ; git branch --list $rc_branch)" || complain "branch $rc_branch already exists, stopping"
(cd $top ; git branch $rc_branch ; git checkout $rc_branch ; git merge --no-ff --no-commit $start_branch)
if test ! -z "$(cd $top ; git status --porcelain)" ; then
    (cd $top ; git status)
    request_confirmation "commit merge from $start_branch?"
    (cd $top ; git commit -m "merging $start_branch to $rc_branch")
fi

# modify pom
echo
echo "dropping -SNAPSHOT from version number"
cp -p $top/pom.xml $tmpdir/original-pom.xml
xsltproc --stringparam release-version "$release_version" \
         $top/scripts/update-pom-version.xsl \
         $tmpdir/original-pom.xml \
         > $top/pom.xml
echo
(cd $top ; git diff)
echo
request_confirmation "commit pom.xml?"
(cd $top; git add pom.xml ; git commit -m "Dropped -SNAPSHOT in version number for official release.")

# compute release date in the future
release_date=$(TZ=UTC date -d "+5 days" +"%Y-%m-%d")

# update changes.xml
cp -p $top/src/changes/changes.xml $tmpdir/original-changes.xml
xsltproc --stringparam release-version     "$release_version" \
         --stringparam release-date        "$release_date" \
         --stringparam release-description "$release_version is a $release_type release." \
         $top/scripts/update-changes.xsl \
         $tmpdir/original-changes.xml \
         > $top/src/changes/changes.xml
echo
(cd $top ; git diff)
echo
request_confirmation "commit changes.xml?"
(cd $top ; git add src/changes/changes.xml ; git commit -m "Updated changes.xml for official release.")

# update downloads and faq pages
# the weird pattern with a 13.0 in the middle is here to avoid modifying the second #set that manages old versions
sed -i "s,^\(#set *( *\$versions *= *{\)\(.*\)\(13.0.*\),\1\"$release_version\": \"$release_date\"\, \2\3," \
    $top/src/site/markdown/downloads.md.vm
justified_orekit=$(echo "$release_version      " | sed 's,\(......\).*,\1,')
sed -i "$(sed -n '/^ *Orekit[0-9. ]*| *Hipparchus[0-9. ]*$/=' src/site/markdown/faq.md | tail -1)a\  Orekit $justified_orekit | Hipparchus          $hipparchus_version" \
    $top/src/site/markdown/faq.md
echo
(cd $top ; git diff)
echo
request_confirmation "commit downloads.md.vm and faq.md?"
(cd $top ; git add src/site/markdown/downloads.md.vm src/site/markdown/faq.md ; git commit -m "Updated documentation for official release.")

# push to origin
echo
test -z "$(search_in_repository branches ${rc_branch} .[].name)" || complain "branch ${rc_branch} already exists in ${origin}"
request_confirmation "push $rc_branch branch to ${origin}?"
(cd $top ; git push ${origin} $rc_branch)

# make sure we can merge in a release branch on the origin server
if test -z "$(search_in_repository branches ${release_branch} .[].name)" ; then
  # release branch does not exist on origin yet, create it
  echo
  echo "creating remote branch ${origin}/${release_branch}"  
  curl \
    --silent \
    --output /dev/null \
    --request POST \
    --header "PRIVATE-TOKEN: $gitlab_token" \
    --data "branch=${release_branch}" \
    --data "ref=${start_sha}" \
    ${gitlab_api}/repository/branches
fi

# waiting for remote branch to be available
created_branch=""
timeout=0
while test -z "$created_branch" ; do
  current_date=$(date +"%Y-%m-%dT%H:%M:%SZ")
  echo "${current_date} branch ${release_branch} not yet available in ${origin}, waiting…"
  sleep 5
  timeout=$(expr $timeout + 5)
  test $timeout -lt 600 || complain "branch ${release_branch} not created in ${origin} after 10 minutes, exiting"
  created_branch=$(search_in_repository branches ${release_branch} .[].name)
done
echo "branch ${release_branch} has been created"
echo ""

# create merge request
echo "creating merge request from ${rc_branch} to ${release_branch}"
mr_id=$(curl \
          --silent \
          --request POST \
          --header "PRIVATE-TOKEN: $gitlab_token" \
          --data   "source_branch=${rc_branch}" \
          --data   "target_branch=${release_branch}" \
          --data   "remove_source_branch=true" \
          --data   "title=preparing release ${release_version}" \
          --data   "description=prepare -release-script" \
          "${gitlab_api}/merge_requests" \
       | jq .iid)
echo "merge request ID is $mr_id"
echo ""

# wait for merge request to be mergeable
merge_status="preparing"
timeout=0
while test "${merge_status}" != "mergeable" ; do
  current_date=$(date +"%Y-%m-%dT%H:%M:%SZ")
  echo "${current_date} merge request ${mr_id} status: ${merge_status}, waiting…"
  sleep 5
  timeout=$(expr $timeout + 5)
  test $timeout -lt 600 || complain "merge request ${mr_id} not mergeable after 10 minutes, exiting"
  merge_status=$(get_mr ${mr_id} ".detailed_merge_status")
done
echo "merge request ${mr_id} is mergeable"
echo ""

# perform merging (this will trigger continuous integration pipelines)
echo "merging merge request $mr_id from ${rc_branch} to ${release_branch}"
curl \
  --silent \
  --output /dev/null \
  --request PUT \
  --header "PRIVATE-TOKEN: $gitlab_token" \
  "${gitlab_api}/merge_requests/${mr_id}/merge"

# waiting for merge request to be merged
merge_state="opened"
timeout=0
while test "${merge_state}" != "merged"; do
  current_date=$(date +"%Y-%m-%dT%H:%M:%SZ")
  echo "${current_date} merge request ${mr_id} state: ${merge_state}, waiting…"
  sleep 5
  timeout=$(expr $timeout + 5)
  test $timeout -lt 600 || complain "merge request ${mr_id} not merged after 10 minutes, exiting"
  merge_state=$(get_mr ${mr_id} ".state")
done
echo "merge request ${mr_id} has been merged"
echo ""

# switch to release branch
(cd $top ; git fetch --prune ${origin} ; git checkout $release_branch ; git branch --set-upstream-to ${origin}/$release_branch $release_branch; git pull ${origin})
(cd $top ; git branch -d $rc_branch)

echo ""
request_confirmation "create tag $rc_tag?"
(cd $top ; git tag $rc_tag -m "Release Candidate $next_rc for version $release_version." ; git push ${origin} $rc_tag)
echo ""

# monitor continuous integration pipeline triggering (10 minutes max)
merge_sha=$(cd $top ; git rev-parse --verify HEAD)
pipeline_id=""
timeout=0
while test -z "$pipeline_id" ; do
    current_date=$(date +"%Y-%m-%dT%H:%M:%SZ")
    echo "${current_date} waiting for pipeline to be triggered…"
    sleep 5
    timeout=$(expr $timeout + 5)
    test $timeout -lt 1800 || complain "pipeline not started after 30 minutes, exiting"
    pipeline_id=$(curl \
                    --silent \
                    --request GET \
                    --header "PRIVATE-TOKEN: $gitlab_token" \
                    "${gitlab_api}/pipelines" | \
                  jq ".[] | select(.sha==\"$merge_sha\" and .ref==\"$release_branch\") | .id")
done
echo "pipeline $pipeline_id triggered"

# monitor continuous integration pipeline run (1 hour max)
pipeline_status="pending"
timeout=0
# the status is one of
# created, waiting_for_resource, preparing, pending, running, success, failed, canceling, canceled, skipped, manual, scheduled
while test "${pipeline_status}" != "success" -a "${pipeline_status}" != "failed"  -a "${pipeline_status}" != "canceled" ; do
  current_date=$(date +"%Y-%m-%dT%H:%M:%SZ")
  echo "${current_date} pipeline ${pipeline_id} status: ${pipeline_status}, waiting…"
  sleep 30
  timeout=$(expr $timeout + 30)
  test $timeout -lt 3600 || complain "pipeline not completed after 1 hour, exiting"
  pipeline_status=$(curl  \
                      --silent \
                      --request GET \
                      --header "PRIVATE-TOKEN: $gitlab_token" \
                      "${gitlab_api}/pipelines" \
                    | jq --raw-output ".[] | select(.id==$pipeline_id) | .status")
done
test "${pipeline_status}" = "success" || complain "pipeline did not succeed"

if test "$release_type" = "patch" ; then
    # for patch release, there are no votes, we jump directly to the publish step
    sh $top/scripts/successful-vote.sh
else
    # create vote topic
    vote_date=$(TZ=UTC date -d "+5 days" +"%Y-%m-%dT%H:%M:%SZ")
    orekit_dev_category=5
    topic_title="[VOTE] Releasing Orekit ${release_version} from release candidate $next_rc"
    topic_raw="This is a vote in order to release version ${release_version} of the Orekit library.
Version ${release_version} is a ${release_type} release.

$(xsltproc $top/scripts/changes2release.xsl $top/src/changes/changes.xml)

The release candidate ${next_rc} can be found on the GitLab repository
as tag $rc_tag in the ${release_branch} branch:
https://gitlab.orekit.org/orekit/orekit/tree/${release_branch}

The maven artifacts are available in the Orekit Nexus repository at:
https://packages.orekit.org/#browse/browse:maven-release:org%2Forekit%2Forekit%2F${release_version}

The generated site is available at:
https://www.orekit.org/site-orekit-${release_version}/index.html

The vote will be tallied on ${vote_date} (UTC time)"

    echo ""
    echo "proposed vote topic for the forum:"
    echo ""
    echo "$topic_raw"
    request_confirmation "OK to post vote topic on forum?"

    read -p "enter your forum user name" forum_username
    while test -z "forum_api_key" ; do
        echo "enter your forum API key"
        stty_orig=$(stty -g)
        stty -echo
        read forum_api_key
        stty $stty_orig
    done

    post_url=$(curl -H "Content-Type: application/json" \
                    -H "Accept: application/json" \
                    -H "Api-Username: $forum_username"\
                    -H "Api-Key: $forum_api_key" \
                    -X POST https://forum.orekit.org/posts.json
                    -d "{ \"title\": \"topic_title\", \"raw\": \"$topic_raw\", \"category\": \"$orekit_dev_category\" }" \
            | jp .post_url | sed "s,\"\(.*\)\",https://forum.orekit.org\1,")
    echo "post URLis : $post_url"

    echo ""
    echo "please ping PMC members so they are aware of the vote."
    echo "Their vote is essential for a release as per project governance."
fi
