#!/bin/sh

tmpdir=$(mktemp -d /tmp/orekit-failed-vote.XXXXXX)
trap "rm -fr $tmpdir" 0
trap "exit 1" 1 2 15

complain()
{
    echo "$1" 1>&2
    exit 1
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
for cmd in git sed xsltproc sort tail ; do
    which -s $cmd || complain "$cmd command not found"
done
git -C "$top" rev-parse 2>/dev/null        || complain "$top does not contain a git repository"
test -f $top/pom.xml                       || complain "$top/pom.xml not found"
test -d $top/src/main/java/org/orekit/time || complain "$top/src/main/java/org/orekit/time not found"
test -f $HOME/.m2/settings.xml             || complain "$HOME/.m2/settings.xml not found"

start_branch=$(cd $top ; git branch --show-current)
echo "start branch is $start_branch"
test -z "$(cd $top ; git status --porcelain)" || complain "there are uncommitted changes in the branch"

# check branch
release_version=$(xsltproc $top/scripts/get-pom-version.xsl $top/pom.xml)
release_type=$(echo $release_version | sed -e "s,^[0-9]*\.0$,major," -e "s,^[0-9]*\.[1-9][0-9]*$,minor," -e "s,^[0-9]*\.[0-9]*\.[0-9]*$,patch,")
test "release_type" != "patch" || complain "this script should not be used on a patch release since there are no votes"
release_branch=$(echo $release_version | sed 's,\([0-9]\+.[0-9]\+\)[.0-9]*,release-\1,')
test "$start_branch" = "$release_branch" || complain "$start_branch is not a release branch"

# check tag for failed release candidate
last_rc=$(cd $top; git tag -l ${release_version}-RC* | sed 's,.*-RC,,' | sort -n | tail -1)
test ! -z "$last_rc" || complain "there was no release candidate for $release_version"
release_tag="${release_version}-RC$last_rc"

# delete maven artifacts on Orekit Nexus repository
read -p "enter your Nexus repository user name" nexus_username
while test -z "nexus_password" ; do
    echo "enter your Nexus repository API key"
    stty_orig=$(stty -g)
    stty -echo
    read nexus_password
    stty $stty_orig
done
cat > $tmpdir.curl-config <<EOD
--user ${nexus_username}:${nexus_password}
EOD

request_confirmation "delete maven artifacts for release tag ${release_tag}?"
base_url="https://packages.orekit.org/service/rest/v1/assets?repository=maven-staging/org/orekit/orekit"
for asset in "-cyclonedx.json" "-javadoc.jar" "-sources.zip" ".jar" ".pom" ".zip" ; do
    curl --config $tmpdir.curl-config \\
         --request DELETE \
         "${base_url}/${release_version}/orekit-${release_version}${asset}"
done
