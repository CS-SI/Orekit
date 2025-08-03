#!/bin/sh

cleanup_at_exit()
{
    if test ! -z "$start_branch" ; then
        git reset -q --hard
        git checkout $start_branch
        delete_local_and_remote_tag    "$gitlab_token" "$release_tag"
        delete_local_and_remote_branch "$gitlab_token" "$preparation_branch"
        git fetch --prune ${gitlab_origin}
        echo "everything has been cleaned, branch set back to $start_branch" 1>&2
    fi
}

# run everything from top project directory
cd $(dirname $0)/..

# load common functions
. scripts/functions.sh

# safety checks
safety_ckecks src/main/java/org/orekit/time

# set the gitlab remote repository name
gitlab_origin=$(find_gitlab_origin)

# get users credentials
gitlab_token=$(enter_private_credentials "enter your gitlab private token")

# check branch
start_branch=$(find_current_branch)
release_version=$(find_pom_version)
release_type=$(release_type $release_version)
release_branch=$(release_branch $release_version)
test "$start_branch" = "$release_branch" || complain "$start_branch is not a release branch"

# check tag for successful release candidate
last_rc=$(last_rc $release_version)
test $last_rc -ge 1 || complain "there was no release candidate for $release_version"
rc_tag=$(rc_tag $release_version $last_rc)
release_tag="$release_version"
test -z "$(git tag -l \"${release_tag}\")" || complain "tag $release_tag already exists"

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

request_confirmation "upload maven artifacts to central portal?"

# get user credentials
read -p "enter your central portal user token name" central_username
central_password=$(enter_private_credentials "enter your central portal user token password")
central_bearer=$(echo ${central_username}:$central_password | base64)

# upload maven artifacts to central portal
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
  echo "$current_date deployment status is \"$status\", waiting for validation…"
  sleep 5
  timeout=$(expr $timeout + 5)
  test $timeout -lt 600 || complain "deployment not validated after 10 minutes, exiting"
  status=$(curl --silent \
                --request POST \
                --header "Authorization: Bearer $central_bearer" \
                "https://central.sonatype.com/api/v1/publisher/status?id=${deployment_id}" \
           | jq .deploymentState)
done
echo "deployment has been validated"
echo ""

# publish maven artifacts to central portal
request_confirmation "publish maven artifacts to central portal?"
curl --silent \
     --request POST \
     --header "Authorization: Bearer $central_bearer" \
     "https://central.sonatype.com/api/v1/publisher/deployment/${deployment_id}"

request_confirmation "create tag $release_tag?"
git tag $release_tag -m "Version $release_version."

# push release tag to origin
request_confirmation "push $release_tag tag to ${gitlab_origin}?"
git push $gitlab_origin $release_tag

request_confirmation "merge $release_branch to main?"

# merge release branch to main branch
merge_remote_branch "$gitlab_token" $release_branch main false "Release $release_version is now the main official release."
git checkout main
git pull $gitlab_origin main
echo ""

# merge main branch to develop branch
request_confirmation "merge main to develop?"
merge_remote_branch "$gitlab_token" main develop false "Merging main branch back to develop."
git checkout develop
git pull $gitlab_origin develop
echo ""

release_description="$(extract_release_description)"

# prepare next development cycle
next_version=$(next_version $release_version)
preparation_branch="prepare-$next_version"
request_confirmation "prepare branch $preparation_branch for next version?"
create_local_branch $preparation_branch

cp -p pom.xml $tmpdir/original-pom.xml
xsltproc --stringparam release-version "${next_version}-SNAPSHOT" \
         scripts/update-pom-version.xsl \
         $tmpdir/original-pom.xml \
         > pom.xml
cp -p src/changes/changes.xml $tmpdir/original-changes.xml
xsltproc --stringparam release-version "$release_version" \
         --stringparam next-version    "$next_version" \
         scripts/prepare-next-release-in-changes.xsl \
         $tmpdir/original-changes.xml | \
         sed 's,<release \([^/]*\)/>,<release \1>\n    </release>\n    ,' \
         > src/changes/changes.xml

echo
git diff
echo
request_confirmation "commit pom.xml and changes.xml and push $preparation_branch to ${gitlab_origin}?"
git add pom.xml src/changes/changes.xml
git commit -m "Incremented version number for next-release."
git push $gitlab_origin $preparation_branch

# merge to develop branch
request_confirmation "merge $preparation_branch branch to develop branch at ${gitlab_origin}?"
merge_remote_branch "$gitlab_token" $preparation_branch develop true "preparing development for version $next_version."
git checkout develop
git pull --prune $gitlab_origin
git branch -d $preparation_branch
echo ""

# manage milestones
request_confirmation "close milestone $release_version?"
release_milestone_id=$(get_milestone_id "$gitlab_token" $release_version)
close_milestone "$gitlab_token" $release_milestone_id
if test -z "$(get_milestone_id "$gitlab_token" $next_version)" ; then
    create_milestone "$gitlab_token" $next_version
fi

# create GitHub release
request_confirmation "create release on GitHub mirror repository?"
github_personal_token=$(enter_private_credentials "enter your GitHub personal access token")
github_release_url=$(curl --silent \
                          --request POST \
                          --header "Accept: application/vnd.github+json" \
                          --header "Authorization: Bearer $github_personal_token" \
                          --header "X-GitHub-Api-Version: 2022-11-28" \
                          --data   "{ \"tag_name\": \"${release_tag}\", \"name\": \"$release_tag\", \"body\":\"$release_description\"}" \
                          https://api.github.com/repos/CS_SI/Orekit/releases \
                     | jq --raw-output .html_url)
echo "GitHub release URL: $github_release_url"
echo ""

# create announcement topic
topic_title="Orekit $release_version released"
topic_raw="The Orekit team is pleased to announce the release of Orekit version $release_version.
Version $release_version is a $release_type release.

$release_description

The maven artifacts are available in maven central

The sources and binaries can be retrieved from the forge releases page:
https://gitlab.orekit.org/orekit/orekit/-/releases"

echo ""
echo "proposed announcement topic for the forum:"
echo ""
echo "$topic_raw"
request_confirmation "OK to post announcement topic on forum?"

read -p "enter your forum user name " forum_username
forum_api_key=$(enter_private_credentials "enter your forum API key")

announcements_category=$(find_forum_category $forum_username $forum_api_key "Orekit announcements")
post_url=$(post_to_forum $forum_username $forum_api_key $announcements_category "$topic_title" "$topic_raw")
echo ""
echo "vote topic posted at URL: https://forum.orekit.org/$post_url"

echo "please navigate to https://${gitlab_fqdn}/${gitlab_owner}/${gitlab_project}/-/releases"
echo "and check release notes are OK (it may take some time for the releases to show up there)"
