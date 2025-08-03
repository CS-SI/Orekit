#!/bin/sh

cleanup_at_exit()
{
    # rewind git repositories (both local and in the GitLab forge)
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
}

# run everything from top project directory
cd $(dirname $0)

# load common functions
. scripts/functions.sh

# safety checks
safety_ckecks src/main/java/org/orekit/time

# set the gitlab remote repository name
gitlab_origin=$(find_gitlab_origin)

# get users credentials
gitlab_token=$(enter_private_credentials "enter your gitlab private token")

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
    test ! -z $(git rev-parse --quiet --verify $release_branch) || complain "branch $release_branch doesn't exist, stopping"
    delete_release_branch_on_cleanup="false"
else
    git branch $release_branch || complain "branch $release_branch already exist, stopping"
    delete_release_branch_on_cleanup="true"
fi
git checkout $release_branch

# create release candidate branch
rc_branch="RC${next_rc}-${release_version}"
create_local_branch $rc_branch $start_branch

# modify pom
echo
echo "dropping -SNAPSHOT from version number"
cp -p pom.xml $tmpdir/original-pom.xml
xsltproc --stringparam release-version "$release_version" \
         scripts/update-pom-version.xsl \
         $tmpdir/original-pom.xml \
         > pom.xml
echo
git diff
echo
request_confirmation "commit pom.xml?"
git add pom.xml ; git commit -m "Dropped -SNAPSHOT in version number for official release."

# compute release date in the future
release_date=$(TZ=UTC date -d "+5 days" +"%Y-%m-%d")

# update changes.xml
cp -p src/changes/changes.xml $tmpdir/original-changes.xml
xsltproc --stringparam release-version     "$release_version" \
         --stringparam release-date        "$release_date" \
         --stringparam release-description "$release_version is a $release_type release." \
         scripts/update-changes.xsl \
         $tmpdir/original-changes.xml \
         > src/changes/changes.xml
echo
git diff
echo
request_confirmation "commit changes.xml?"
git add src/changes/changes.xml
git commit -m "Updated changes.xml for official release."

# update downloads and faq pages
# the weird first pattern with a 13.0 in the middle avoids modifying the second #set that manages old versions
# the second pattern deals with release candidate 2 or more (i.e. when version was already in the file)
sed -i \
    -e "s,^\(#set *( *\$versions *= *{\)\(.*\)\(13.0.*\),\1\"$release_version\": \"$release_date\"\, \2\3," \
    -e "s,\(\"$release_version\": \"$release_date\"\,\) \"$release_version\": \"[-0-9]*\"\,,\1," \
    src/site/markdown/downloads.md.vm
justified_orekit=$(echo "$release_version      " | sed 's,\(......\).*,\1,')
sed -i "$(sed -n '/^ *Orekit[0-9. ]*| *Hipparchus[0-9. ]*$/=' src/site/markdown/faq.md | tail -1)a\  Orekit $justified_orekit | Hipparchus          $hipparchus_version" \
    src/site/markdown/faq.md
echo
git diff
echo
request_confirmation "commit downloads.md.vm and faq.md?"
git add src/site/markdown/downloads.md.vm src/site/markdown/faq.md
git commit -m "Updated documentation for official release."


# push to origin
echo
test -z "$(search_in_repository "$gitlab_token" branches ${rc_branch} .[].name)" || complain "branch ${rc_branch} already exists in ${gitlab_origin}"
request_confirmation "push $rc_branch branch to ${gitlab_origin}?"
git push ${gitlab_origin} $rc_branch

# make sure we can merge in a release branch on the origin server
create_remote_branch "$gitlab_token" $release_branch $start_sha $gitlab_origin

# remotely merge release candidate branch into release branch
merge_remote_branch "$gitlab_token" $rc_branch $release_branch true "preparing release $release_version"

# switch to release branch and delete release candidate branch
git fetch --prune ${gitlab_origin}
git checkout $release_branch
git branch --set-upstream-to ${gitlab_origin}/$release_branch $release_branch
git pull ${gitlab_origin}
git branch -d $rc_branch

echo ""
request_confirmation "create tag $rc_tag?"
git tag $rc_tag -m "Release Candidate $next_rc for version $release_version."
git push ${gitlab_origin} $rc_tag
echo ""

# monitor continuous integration pipeline triggering (10 minutes max)
merge_sha=$(git rev-parse --verify HEAD)
monitor_pipeline "$gitlab_token" $merge_sha $release_branch || complain "pipeline did not succeed"

if test "$release_type" = "patch" ; then
    # for patch release, there are no votes, we jump directly to the publish step
    sh scripts/successful-vote.sh
else
    # create vote topic
    vote_date=$(TZ=UTC date -d "+5 days" +"%Y-%m-%dT%H:%M:%SZ")
    topic_title="[VOTE] Releasing Orekit ${release_version} from release candidate $next_rc"
    topic_raw="This is a vote in order to release version ${release_version} of the Orekit library.
Version ${release_version} is a ${release_type} release.

$(xsltproc scripts/changes2release.xsl src/changes/changes.xml)

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
    forum_api_key=$(enter_private_credentials "enter your forum API key")

    orekit_dev_category=$(find_forum_category $forum_username $forum_api_key "Orekit development")
    post_url=$(post_to_forum $forum_username $forum_api_key $orekit_dev_category "$topic_title" "$topic_raw")
    echo ""
    echo "vote topic posted at URL: https://forum.orekit.org/$post_url"

    echo ""
    echo "please ping PMC members so they are aware of the vote."
    echo "Their vote is essential for a release as per project governance."
fi
