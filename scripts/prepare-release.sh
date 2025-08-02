#!/bin/sh

cleanup_at_exit()
{
    # rewind git repositories (both local and in the GitLab forge)
    (cd $(dirname $0)/..
        if test ! -z "$start_branch" && test "$(git branch --show-current)" != "$start_branch" ; then
            # we need to clean up the branches and rewind everything
            git reset -q --hard; git checkout $start_branch
            if test ! -z "$(git branch --list $rc_branch)" ; then
                git branch -D $rc_branch
                test -z "$(search_in_repository "$gitlab_token" branches ${rc_branch} .[].name)" || delete_in_repository "$gitlab_token" branches ${rc_branch}
            fi
            if test ! -z "$rc_tag" ; then
              if test ! -z "$(git tag --list $rc_tag)" ; then
                  git tag -d $rc_tag
                  test -z "$(search_in_repository "$gitlab_token" tags ${rc_tag} .[].name)" || delete_in_repository "$gitlab_token" tags ${rc_tag}
              fi
            fi
            if test "$delete_release_branch_on_cleanup" = "true" ; then
                git branch -D $release_branch
                test -z "$(search_in_repository "$gitlab_token" branches ${release_branch} .[].name)" || delete_in_repository "$gitlab_token" branches ${release_branch}
            fi
            git fetch --prune ${gitlab_origin}
            echo "everything has been cleaned, branch set back to $start_branch" 1>&2
        fi
    )
}

# load common functions
top=$(dirname $0/.. ; pwd)
. $(dirname $0)/functions.sh

# safety checks
safety_ckecks src/main/java/org/orekit/time

# set the gitlab remote repository name
gitlab_origin=$(find_gitlab_origin)

# get users credentials
gitlab_token=$(enter_gitlab_token)

start_branch=$(find_start_branch)
start_sha=$(find_start_sha)

# extract version numbers
pom_version=$(find_pom_version)
changes_version=$(find_changes_version)
hipparchus_version=$(find_hipparchus_version)
release_version=$(echo $pom_version | sed 's,-SNAPSHOT,,')
release_type=$(echo $release_version | sed -e "s,^[0-9]*\.0$,major," -e "s,^[0-9]*\.[1-9][0-9]*$,minor," -e "s,^[0-9]*\.[0-9]*\.[0-9]*$,patch,")
test "$release_version"  = "$changes_version" || complain "wrong version in changes.xml ($changes_version instead of $release_version)"
echo "current version is $pom_version"
echo "release version will be $release_version, a $release_type release, depending on Hipparchus $hipparchus_version"
request_confirmation "do you agree with these version numbers?"

# compute release candidate number
next_rc=$(compute_next_rc $release_version)
rc_tag="${release_version}-RC$next_rc"

# reuse existing release branch for patch release or new release candidate, create it otherwise
release_branch=$(echo $release_version | sed 's,\([0-9]\+.[0-9]\+\)[.0-9]*,release-\1,')
if test "$release_type" = patch -o $next_rc -ge 1 ; then
    (cd $top ; test ! -z $(git rev-parse --quiet --verify $release_branch)) || complain "branch $release_branch doesn't exist, stopping"
    delete_release_branch_on_cleanup="false"
else
    (cd $top ; git branch $release_branch) || complain "branch $release_branch already exist, stopping"
    delete_release_branch_on_cleanup="true"
fi
(cd $top ; git checkout $release_branch)

# create release candidate branch
rc_branch="RC${next_rc}-${release_version}"
create_local_branch $rc_branch $start_branch

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
# the weird first pattern with a 13.0 in the middle avoids modifying the second #set that manages old versions
# the second pattern deals with release candidate 2 or more (i.e. when version was already in the file)
sed -i \
    -e "s,^\(#set *( *\$versions *= *{\)\(.*\)\(13.0.*\),\1\"$release_version\": \"$release_date\"\, \2\3," \
    -e "s,\(\"$release_version\": \"$release_date\"\,\) \"$release_version\": \"[-0-9]*\"\,,\1," \
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
test -z "$(search_in_repository "$gitlab_token" branches ${rc_branch} .[].name)" || complain "branch ${rc_branch} already exists in ${gitlab_origin}"
request_confirmation "push $rc_branch branch to ${gitlab_origin}?"
(cd $top ; git push ${gitlab_origin} $rc_branch)

# make sure we can merge in a release branch on the origin server
create_remote_branch "$gitlab_token" $release_branch $start_sha $gitlab_origin

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
  current_date=$(date +"%Y-%m-%dT%H:%M:%S")
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
  current_date=$(date +"%Y-%m-%dT%H:%M:%S")
  echo "${current_date} merge request ${mr_id} state: ${merge_state}, waiting…"
  sleep 5
  timeout=$(expr $timeout + 5)
  test $timeout -lt 600 || complain "merge request ${mr_id} not merged after 10 minutes, exiting"
  merge_state=$(get_mr ${mr_id} ".state")
done
echo "merge request ${mr_id} has been merged"
echo ""

# switch to release branch
(cd $top ; git fetch --prune ${gitlab_origin} ; git checkout $release_branch ; git branch --set-upstream-to ${gitlab_origin}/$release_branch $release_branch; git pull ${gitlab_origin})
(cd $top ; git branch -d $rc_branch)

echo ""
request_confirmation "create tag $rc_tag?"
(cd $top ; git tag $rc_tag -m "Release Candidate $next_rc for version $release_version." ; git push ${gitlab_origin} $rc_tag)
echo ""

# monitor continuous integration pipeline triggering (10 minutes max)
merge_sha=$(cd $top ; git rev-parse --verify HEAD)
pipeline_id=""
timeout=0
while test -z "$pipeline_id" ; do
    current_date=$(date +"%Y-%m-%dT%H:%M:%S")
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
  current_date=$(date +"%Y-%m-%dT%H:%M:%S")
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
    topic_title="[VOTE] Releasing Orekit ${release_version} from release candidate $next_rc"
    topic_raw="This is a vote in order to release version ${release_version} of the Orekit library.
Version ${release_version} is a ${release_type} release.

$(xsltproc $top/scripts/changes2release.xsl $top/src/changes/changes.xml)

The release candidate ${next_rc} can be found on the GitLab repository
as tag $rc_tag in the ${release_branch} branch:
https://gitlab.orekit.org/orekit/orekit/tree/${release_branch}

The maven artifacts are available in the Orekit Nexus repository at:
https://packages.orekit.org/#browse/browse:maven-releases:org%2Forekit%2Forekit%2F${release_version}

The generated site is available at:
https://www.orekit.org/site-orekit-${release_version}/index.html

The vote will be tallied on ${vote_date} (UTC time)"

    echo ""
    echo "proposed vote topic for the forum:"
    echo ""
    echo "$topic_raw"
    request_confirmation "OK to post vote topic on forum?"

    read -p "enter your forum user name " forum_username
    forum_api_key=""
    while test -z "$forum_api_key" ; do
        echo "enter your forum API key"
        stty_orig=$(stty -g)
        stty -echo
        read forum_api_key
        stty $stty_orig
    done

    orekit_dev_category=$(curl --silent \
                               --request GET \
                               --header "Content-Type: application/json" \
                               --header "Api-Username: $forum_username" \
                               --header "Api-Key: $forum_api_key" \
                               --header "Accept: application/json" \
                               https://forum.orekit.org/categories \
                          | jq ".category_list.categories[] | select(.name==\"Orekit development\") | .id")

    post_url=$(curl --silent \
                    --request POST \
                    --header "Content-Type: application/json" \
                    --header "Accept: application/json" \
                    --header "Api-Username: $forum_username"\
                    --header "Api-Key: $forum_api_key" \
                    --data   "{ \"title\": \"$topic_title\", \"raw\": \"$topic_raw\", \"category\": \"$orekit_dev_category\" }" \
                    https://forum.orekit.org/posts.json \
                   | jq --raw-output .post_url)
    echo ""
    echo "vote topic posted at URL: https://forum.orekit.org/$post_url"

    echo ""
    echo "please ping PMC members so they are aware of the vote."
    echo "Their vote is essential for a release as per project governance."
fi
