#!/bin/sh

tmpdir=/tmp/tmp-dir.$$
trap "rm -fr $tmpdir" 0
trap "exit 1" 1 2 15
mkdir $tmpdir

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

find_java_8_home()
{
    for d in /usr/lib/jvm/* ; do
        if test -f "$d/bin/java" ; then
            if test "$($d/bin/java -version 2>&1 | sed -n 's,.*version *\"\([0-9]*\.[0-9]*\).*,\1,p')" = "1.8" ; then
                echo $d
                return
            fi
        fi
    done
    complain "Java 8 home not found"
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
for cmd in git sed curl xsltproc ; do
    which -s $cmd || complain "$cmd command not found"
done
test -d $top/.git                          || complain "$top/.git folder not found"
test -f $top/pom.xml                       || complain "$top/pom.xml not found"
test -d $top/src/main/java/org/orekit/time || complain "$top/src/main/java/org/orekit/time not found"
test -f $HOME/.m2/settings.xml             || complain "$HOME/.m2/settings.xml not found"
export JAVA_HOME=$(find_java_8_home)
export PATH=${JAVA_HOME}/bin:$PATH
echo "JAVA_HOME set to $JAVA_HOME"

# get user credentials
central_portal_username=$(xsltproc $top/scripts/get-central-username.xsl $HOME/.m2/settings.xml)
test ! -z "central_portal_username" || complain "username for central portal not found in $HOME/.m2/settings.xml"
central_portal_password=$(xsltproc $top/scripts/get-central-password.xsl $HOME/.m2/settings.xml)
test ! -z "central_portal_password" || complain "password for central portal not found in $HOME/.m2/settings.xml"

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

# compute release date 5 days in the future
release_date=$(date -d "+5 days" +"%Y-%m-%d")

# ask for release description
release_description=""
echo "enter release description to be put in changes.xml (end by Ctrl-D)"
while IFS= read line ; do
    release_description="$release_description $line"
done

# update changes.xml
cp -p $top/src/changes/changes.xml $tmpdir/original-changes.xml
xsltproc --stringparam release-version     "$release_version" \
         --stringparam release-date        "$release_date" \
         --stringparam release-description "$release_description" \
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

# perform a full build
(cd $top ; mvn clean ; LANG=C mvn site)
request_confirmation "please review generated site (javadoc, reports…)"

# delete temporary branch
(cd $top ; git checkout $release_branch ; git merge --no-ff -m "merging $temporary_branch into $release_branch" $temporary_branch ; git branch -d $temporary_branch)


# deploy maven artifacts to central portal
request_confirmation "deploy maven artifacts to central portal?"
(cd $top ; mvn deploy -Prelease)

if test "$release_type" = "patch" ; then
    # patch release do not need release candidates
    release_tag="$release_version"
else
    # compute RC number
    last_tag="$(cd $top; git tag -l \"${release_version}-RC*\")"
    if test -z "$last_tag" ; then
        last_rc=0
    else
        last_rc=$(echo $last_tag | sed 's,.*-RC,,')
    fi
    next_rc=$(expr $last_rc + 1)
    release_tag="${release_version}-RC$next_rc"
fi
signing_key="0802AB8C87B0B1AEC1C1C5871550FDBD6375C33B"
echo "BEWARE! In the next step, the signing key will be used."
echo "Gpg-agent will most likely display a dialog window that will PREVENT"
echo "retrieving the passphrase from password management tools like KeePassXC."
echo "If you need to retrieve the passphrase from such a password management tool,"
echo "do it now, and enter 'yes' fast on the following prompt so you can paste it in gpg-dialog."
request_confirmation "create and sign tag $release_tag?"
(cd $top ; git tag $release_tag -s -u $signing_key -m "Release Candidate $next_rc for version $release_version."; git tag -v $release_tag ; git push)

# push to origin
request_confirmation "push $release_branch branch and $release_tag tag to origin?"
(cd $top ; git push origin $release_branch $release_tag)
