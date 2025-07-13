#!/bin/sh

tmpdir=$(mktemp -d /tmp/orekit-prepare-release.XXXXXX)
trap "rm -fr $tmpdir" 0
trap "exit 1" 1 2 15

complain()
{
    echo "$1" 1>&2
    if test ! -z "$start_branch" && test "$(cd $top ; git branch --show-current)" != "$start_branch" ; then
        # we need to clean up the branches and rewind everything
        (cd $top ; git reset -q --hard; git checkout $start_branch)
        test -z "$(git branch --list $temporary_branch)"  || (cd $top ; git branch -D $temporary_branch)
        test "$delete_release_branch_on_cleanup" = "true" && (cd $top ; git branch -D $release_branch)
        echo "everything has been cleaned, branch set back to $start_branch" 1>&2
    fi
    exit 1
}

find_java_home()
{
    for d in /usr/lib/jvm/* ; do
        if test -f "$d/bin/java" ; then
            if test "$($d/bin/java -version 2>&1 | sed -n 's,.*version *\"\([0-9]*\.[0-9]*\).*,\1,p')" = $1 ; then
                echo $d
                return
            fi
        fi
    done
    complain "Java home $1 not found"
}

request_confirmation()
{
    answer=""
    while test "$answer" != "yes" && test "$answer" != "no" ; do
        read -p "$1 (enter yes to continue, no to stop the release process) " answer
    done
    test $answer = "yes" || complain "release process stopped at user request"
}

# find top level directory
top=$(cd $(dirname $0)/.. ; pwd)

# safety checks
for cmd in git sed xsltproc mvn tee sort tail curl ; do
    which -s $cmd || complain "$cmd command not found"
done
git -C "$top" rev-parse 2>/dev/null        || complain "$top does not contain a git repository"
test -f $top/pom.xml                       || complain "$top/pom.xml not found"
test -d $top/src/main/java/org/orekit/time || complain "$top/src/main/java/org/orekit/time not found"
export JAVA_HOME=$(find_java_home 1.8)
export PATH=${JAVA_HOME}/bin:$PATH
echo "JAVA_HOME set to $JAVA_HOME"

start_branch=$(cd $top ; git branch --show-current)
echo "start branch is $start_branch"
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

# create temporary branch for the release process, starting from the release branch
temporary_branch=${release_branch}-temporary
test -z "$(git branch --list $temporary_branch)" || complain "branch $temporary_branch already exists, stopping"
(cd $top ; git branch $temporary_branch ; git checkout $temporary_branch ; git merge --no-ff --no-commit $start_branch)
if test ! -z "$(cd $top ; git status --porcelain)" ; then
    (cd $top ; git status)
    request_confirmation "commit merge from $start_branch?"
    (cd $top ; git commit -m "merging $start_branch to $temporary_branch")
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

# compute release date in the future (1 hour allocated to build, and 5 days allocated to vote)
vote_date=$(TZ=UTC date -d "+5 days 1 hour" +"%Y-%m-%dT%H:%M:%SZ")
release_date=$(echo $vote_date | sed 's,T.*,,')

if test "$release_type" = "patch" ; then
    # patch release do not need release candidates
    release_tag="$release_version"
else
    # compute release candidate number
    last_rc=$(cd $top; git tag -l ${release_version}-RC* | sed 's,.*-RC,,' | sort -n | tail -1)
    if test -z "$last_rc" ; then
        next_rc=1
    else
        next_rc=$(expr $last_rc + 1)
    fi
    release_tag="${release_version}-RC$next_rc"
fi

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

# delete temporary branch
(cd $top ; git checkout $release_branch ; git merge --no-ff -m "merging $temporary_branch into $release_branch" $temporary_branch ; git branch -d $temporary_branch)

request_confirmation "create tag $release_tag?"
(cd $top ; git tag $release_tag -m "Release Candidate $next_rc for version $release_version.")

# push to origin (this will trigger automatic deployment to Orekit Nexus instance)
request_confirmation "push $release_branch branch and $release_tag tag to origin?"
(cd $top ; git push origin $release_branch $release_tag)

if test "$release_type" = "patch" ; then
    # for patch release, there are no votes, we jump directly to the publish step
    sh successful-vote.sh
else
    # create vote topic
    orekit_dev_category=5
    topic_title="[VOTE] Releasing Orekit ${release_version} from release candidate $next_rc"
    topic_raw="This is a vote in order to release version ${release_version} of the Orekit library.
Version ${release_version} is a ${release_type} release.

$(xsltproc $top/scripts/changes2release.xsl $top/src/changes/changes.xml)

The release candidate ${next_rc} can be found on the GitLab repository
as tag $release_tag in the ${release_branch} branch:
https://gitlab.orekit.org/orekit/orekit/tree/${release_branch}

Once the Continuous Integration has finished its job (this should hopefully take less than
one hour), it will put:

  - the maven artifacts in the Orekit Nexus repository at:
    https://packages.orekit.org/#browse/browse:maven-release:org%2Forekit%2Forekit%2F${release_version}
  - the generated site  available at:
    https://www.orekit.org/site-orekit-${release_version}/index.html

The vote will be tallied on ${release_date}"

    echo "proposed vote topic for the forum:"
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
