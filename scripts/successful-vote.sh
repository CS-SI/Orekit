#!/bin/sh

cleanup_at_exit()
{
  # nothing to cleanup in this script
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
gitlab_token=$(enter_gitlab_token)

start_branch=$(git branch --show-current)
echo "start branch is $start_branch"
test -z "$(git status --porcelain)" || complain "there are uncommitted changes in the branch"

# check branch
release_version=$(xsltproc scripts/get-pom-version.xsl pom.xml)
release_type=$(echo $release_version | sed -e "s,^[0-9]*\.0$,major," -e "s,^[0-9]*\.[1-9][0-9]*$,minor," -e "s,^[0-9]*\.[0-9]*\.[0-9]*$,patch,")
release_branch=$(echo $release_version | sed 's,\([0-9]\+.[0-9]\+\)[.0-9]*,release-\1,')
test "$start_branch" = "$release_branch" || complain "$start_branch is not a release branch"

# check tag for successful release candidate
last_rc=$(git tag -l ${release_version}-RC* | sed 's,.*-RC,,' | sort -n | tail -1)
test ! -z "$last_rc" || complain "there was no release candidate for $release_version"
rc_tag="${release_version}-RC$last_rc"
release_tag="$release_version"
test -z "$(git tag -l \"${release_tag}\")" || complain "tag ${release_tag} already exists"

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
timeout=0
while test "$status" != "\"VALIDATED\"" ; do
  current_date=$(date +"%Y-%m-%dT%H:%M:%S")
  echo "${current_date} deployment status is \"$status\", waiting for validation…"
  sleep 5
  timeout=$(expr $timeout + 5)
  test $timeout -lt 600 || complain "deployment not validated after 10 minutes, exiting"
  status=$(curl --silent \
                --request POST \
                --header "Authorization: Bearer $central_bearer" \
                "https://central.sonatype.com/api/v1/publisher/status?id=${deployment_id}" \
           | jq .deploymentState)
done

# publish maven artifacts to central portal
request_confirmation "publish maven artifacts to central portal?"
curl --silent \
     --request POST \
     --header "Authorization: Bearer $central_bearer" \
     "https://central.sonatype.com/api/v1/publisher/deployment/${deployment_id}"

request_confirmation "create tag $release_tag?"
git tag $release_tag -m "Version $release_version."

# push to origin
request_confirmation "push $release_branch branch and $release_tag tag to ${gitlab_origin}?"
git push ${gitlab_origin} $release_branch $release_tag

# merge release branch to main branch
request_confirmation "merge $release_branch to main?"
git checkout main ; git merge --no-ff $release_branch

# push main branch
request_confirmation "push main branch to ${gitlab_origin}?"
git push ${gitlab_origin} main

# merge main branch to develop branch
request_confirmation "merge main to develop?"
git checkout develop ; git merge --no-ff main

# prepare next development cycle
current_major=$(echo $release_version | sed 's,\..*,,')
current_minor=$(echo $release_version | sed 's,[^.]*\.\([^.]\).*,\1,')
next_minor=$(expr $current_minor + 1)
next_version="${current_major}.${next_minor}"
cp -p pom.xml $tmpdir/original-pom.xml
xsltproc --stringparam release-version "${next_version}-SNAPSHOT" \
         scripts/update-pom-version.xsl \
         $tmpdir/original-pom.xml \
         > pom.xml
cp -p src/changes/changes.xml $tmpdir/original-changes.xml
xsltproc --stringparam release-version "${next_version}" \
         scripts/prepare-next-release-in-changes.xsl \
         $tmpdir/original-changes.xml | \
         sed 's,<body><release \([^/]*\)/>,<body>\n    <release \1>\n    </release>,' \
         > src/changes/changes.xml
echo
git diff
echo
request_confirmation "commit pom.xml and changes.xml?"
git add pom.xml src/changes/changes.xml
git commit -m "Incremented version number for next-release."

# push develop branch
request_confirmation "push develop branch to ${gitlab_origin}?"
git push ${gitlab_origin} develop
echo ""

echo "please navigate to https://${gitlab_fqdn}/${gitlab_owner}/${gitlab_project}/-/releases"
echo "and check release notes are OK (it may take some time for the releases to show up there)"
