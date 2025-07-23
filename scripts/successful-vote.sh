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
for cmd in git sed xsltproc sort tail curl base64 jq ; do
    which -s $cmd || complain "$cmd command not found"
done
git -C "$top" rev-parse 2>/dev/null        || complain "$top does not contain a git repository"
test -f $top/pom.xml                       || complain "$top/pom.xml not found"
test -d $top/src/main/java/org/orekit/time || complain "$top/src/main/java/org/orekit/time not found"

start_branch=$(cd $top ; git branch --show-current)
echo "start branch is $start_branch"
test -z "$(cd $top ; git status --porcelain)" || complain "there are uncommitted changes in the branch"

# check branch
release_version=$(xsltproc $top/scripts/get-pom-version.xsl $top/pom.xml)
release_type=$(echo $release_version | sed -e "s,^[0-9]*\.0$,major," -e "s,^[0-9]*\.[1-9][0-9]*$,minor," -e "s,^[0-9]*\.[0-9]*\.[0-9]*$,patch,")
release_branch=$(echo $release_version | sed 's,\([0-9]\+.[0-9]\+\)[.0-9]*,release-\1,')
test "$start_branch" = "$release_branch" || complain "$start_branch is not a release branch"

# check tag for successful release candidate
last_rc=$(cd $top; git tag -l ${release_version}-RC* | sed 's,.*-RC,,' | sort -n | tail -1)
test ! -z "$last_rc" || complain "there was no release candidate for $release_version"
rc_tag="${release_version}-RC$last_rc"
release_tag="$release_version"
test -z "$(cd $top; git tag -l \"${release_tag}\")" || complain "tag ${release_tag} already exists"

# retrieve artifacts from Orekit Nexus repository
bundle_dir=$tmpdir/bundle/org/orekit/orekit/${release_version};
mkdir -p $bundle_dir
base_url="https://packages.orekit.org/repository/maven-releases/org/orekit/orekit/${release_version}"
for artifact in "-cyclonedx.json" "-cyclonedx.xml" "-javadoc.jar" "-sources.jar" ".jar" ".pom" ; do
    for suffix in "" ".asc" ".md5" ".sha1" ; do
      curl --silent --output ${bundle_dir}/orekit-${release_version}${artifact}${suffix} \
           $base_url/orekit-${release_version}${artifact}${suffix}
    done
done
(cd $tmpdir/bundle ; tar czf orekit-${release_version}.tar.gz org)

# get user credentials
read -p "enter your central portal user token name" central_username
while test -z "central_password" ; do
    echo "enter your central portal user token password"
    stty_orig=$(stty -g)
    stty -echo
    read central_password
    stty $stty_orig
done
central_bearer=$(echo ${central_username}:${central_password} | base64)

# upload maven artifacts to central portal
request_confirmation "upload maven artifacts to central portal?"
deployment_id=$(cd $tmpdir/bundle ;
                curl --silent \
                     --request POST \
                     --header "Authorization: Bearer $central_bearer" \
                     --form bundle=@orekit-${release_version}.tar.gz \
                      "https://central.sonatype.com/api/v1/publisher/upload?name=orekit-${release_version}&publishingType=USER_MANAGED")

# wait for validation
status=""
while test "$status" != "\"VALIDATED\"" ; do
  sleep 2
  status=$(curl --silent \
                --request POST \
                --header "Authorization: Bearer $central_bearer" \
                "https://central.sonatype.com/api/v1/publisher/status?id=${deployment_id}" \
           | jq .deploymentState)
  echo "deployment status is \"$status\", still waiting for validation…"
done

# publish maven artifacts to central portal
request_confirmation "publish maven artifacts to central portal?"
curl --silent \
     --request POST \
     --header "Authorization: Bearer $central_bearer" \
     "https://central.sonatype.com/api/v1/publisher/deployment/${deployment_id}"

request_confirmation "create tag $release_tag?"
(cd $top ; git tag $release_tag -m "Version $release_version.")

# push to origin
request_confirmation "push $release_branch branch and $release_tag tag to origin?"
(cd $top ; git push origin $release_branch $release_tag)

# merge release branch to main branch
request_confirmation "merge $release_branch to main?"
(cd $top ; git checkout main ; git merge --no-ff $release_branch)

# push main branch
request_confirmation "push main branch to origin?"
(cd $top ; git push origin main)

# merge main branch to develop branch
request_confirmation "merge main to develop?"
(cd $top ; git checkout develop ; git merge --no-ff main)

# prepare next development cycle
current_major=$(echo $release_version | sed 's,\..*,,')
current_minor=$(echo $release_version | sed 's,[^.]*\.\([^.]\).*,\1,')
next_minor=$(expr $current_minor + 1)
next_version="${current_major}.${next_minor}"
cp -p $top/pom.xml $tmpdir/original-pom.xml
xsltproc --stringparam release-version "${next_version}-SNAPSHOT" \
         $top/scripts/update-pom-version.xsl \
         $tmpdir/original-pom.xml \
         > $top/pom.xml
cp -p $top/src/changes/changes.xml $tmpdir/original-changes.xml
xsltproc --stringparam release-version "${next_version}" \
         $top/scripts/prepare-next-release-in-changes.xsl \
         $tmpdir/original-changes.xml | \
         sed 's,<body><release \([^/]*\)/>,<body>\n    <release \1>\n    </release>,' \
         > $top/src/changes/changes.xml
echo
(cd $top ; git diff)
echo
request_confirmation "commit pom.xml and changes.xml?"
(cd $top; git add pom.xml src/changes/changes.xml; git commit -m "Incremented version number for next-release.")

# push develop branch
request_confirmation "push develop branch to origin?"
(cd $top ; git push origin develop)

echo "please navigate to https://gitlab.orekit.org/orekit/orekit/-/releases"
echo "and check release notes are OK (it may take some time for the releases to show up there)"
