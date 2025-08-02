#!/bin/sh

tmpdir=$(mktemp -d /tmp/orekit-script.XXXXXX)
trap "rm -fr $tmpdir" 0
trap "exit 1" 1 2 15

gitlab_fqdn=gitlab.orekit.org
gitlab_owner=luc
gitlab_project=orekit
gitlab_api=https://${gitlab_fqdn}/api/v4/projects/${gitlab_owner}%2F${gitlab_project}

# note that the following function refers to a cleanup_at_exit function
# that is *not* defined here but must be defined in the calling script
complain()
{
    echo "$1" 1>&2
    cleanup_at_exit
    # we don't use "exit" here because sometimes we call this from command substitution like in:
    # some_variable=$(some_function_which_could_call_complain)
    # in this case, we still want to exit the entire script, not just the function
    kill -HUP $$
}

request_confirmation()
{
    answer=""
    while test "$answer" != "yes" && test "$answer" != "no" ; do
        read -p "$1 (enter yes to continue, no to stop the release process) " answer
    done
    test $answer = "yes" || complain "release process stopped at user request"
}

safety_ckecks()
{
    local top=$(dirname $0)/..
    for cmd in git sed xsltproc tee sort tail curl stty base64 jq ; do
        which -s $cmd || complain "$cmd command not found"
    done
    git -C "$top" rev-parse 2>/dev/null || complain "$top does not contain a git repository"
    test -f $top/pom.xml                || complain "$top/pom.xml not found"
    test -d $top/$1                     || complain "$top/$1 not found"
}

find_gitlab_origin()
{
  (cd $(dirname $0)/.. ; git remote -v | sed -n "s,\([^ \t]*\)\t*.*${gitlab_fqdn}:${gitlab_owner}/${gitlab_project}.git.*(push).*,\1,p")
}

find_start_branch()
{
    test -z "$(cd $(dirname $0)/.. ; git status --porcelain)" || complain "there are uncommitted changes in the branch"
    (cd $(dirname $0)/.. ; git branch --show-current)
}

find_start_sha()
{
    (cd $(dirname $0)/.. ; git rev-parse --verify HEAD)
}

find_pom_version()
{
    xsltproc $(dirname $0)/get-pom-version.xsl $(dirname $0)/../pom.xml
}

find_changes_version()
{
    xsltproc $(dirname $0)/get-changes-version.xsl $(dirname $0)/../src/changes/changes.xml
}

find_hipparchus_version()
{
    xsltproc $(dirname $0)/get-hipparchus-version.xsl $(dirname $0)/../pom.xml
}

compute_next_rc()
{
    local last_rc=$(cd $(dirname $0)/..; git tag -l "${1}-RC*" | sed 's,.*-RC,,' | sort -n | tail -1)
    if test -z "$last_rc" ; then
        echo 1
    else
        expr $last_rc + 1
    fi
}

create_local_branch()
{
    test -z "$(cd $(dirname $0)/.. ; git branch --list $1)" || complain "branch $1 already exists, stopping"
    (cd $(dirname $0)/..
     git branch $1
     git checkout $1
     git merge --no-ff --no-commit $2
     if test ! -z "$(git status --porcelain)" ; then
         git status
         request_confirmation "commit merge from $2?"
         git commit -m "merging $2 to $1"
     fi
    )
}

create_remote_branch()
{

    local created_branch="" timeout=0 current_date

    created_branch=$(search_in_repository "$1" branches $2 .[].name)
    if test -z "$created_branch" ; then
        # release branch does not exist on origin yet, create it
        echo
        echo "creating remote branch $4/$2"
        curl \
          --silent \
          --output /dev/null \
          --request POST \
          --header "PRIVATE-TOKEN: $1" \
          --data "branch=$2" \
          --data "ref=$3" \
          ${gitlab_api}/repository/branches

        # waiting for remote branch to be available
        while test -z "$created_branch" ; do
          current_date=$(date +"%Y-%m-%dT%H:%M:%S")
          echo "${current_date} branch $2 not yet available in $4, waiting…"
          sleep 5
          timeout=$(expr $timeout + 5)
          test $timeout -lt 600 || complain "branch $2 not created in $4 after 10 minutes, exiting"
          created_branch=$(search_in_repository "$1" branches $2 .[].name)
        done
        echo "branch $2 has been created"
        echo ""

    fi

}

remote_merge()
{
    local mr_id merge_status="preparing" merge_state="opened" timeout=0 current_date

    # create merge request
    echo "creating merge request from $2 to $3"
    mr_id=$(curl \
              --silent \
              --request POST \
              --header "PRIVATE-TOKEN: $1" \
              --data   "source_branch=$2" \
              --data   "target_branch=$3" \
              --data   "remove_source_branch=true" \
              --data   "title=$4" \
              "${gitlab_api}/merge_requests" \
           | jq .iid)
    echo "merge request ID is $mr_id"
    echo ""

    # wait for merge request to be mergeable
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
    echo "merging merge request $mr_id from $2 to $3"
    curl \
      --silent \
      --output /dev/null \
      --request PUT \
      --header "PRIVATE-TOKEN: $1" \
      "${gitlab_api}/merge_requests/${mr_id}/merge"

    # waiting for merge request to be merged
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

}

monitor_pipeline()
{
    local pipeline_id="" timeout=0 pipeline_status="pending" current_date

    while test -z "$pipeline_id" ; do
        current_date=$(date +"%Y-%m-%dT%H:%M:%S")
        echo "${current_date} waiting for pipeline to be triggered…"
        sleep 5
        timeout=$(expr $timeout + 5)
        test $timeout -lt 1800 || complain "pipeline not started after 30 minutes, exiting"
        pipeline_id=$(curl \
                        --silent \
                        --request GET \
                        --header "PRIVATE-TOKEN: $1" \
                        "${gitlab_api}/pipelines" | \
                      jq ".[] | select(.sha==\"$2\" and .ref==\"$3\") | .id")
    done
    echo "pipeline $pipeline_id triggered"

    # monitor continuous integration pipeline run (1 hour max)
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
                          --header "PRIVATE-TOKEN: $1" \
                          "${gitlab_api}/pipelines" \
                        | jq --raw-output ".[] | select(.id==$pipeline_id) | .status")
    done

    if test "$pipeline_status" = "success" ; then
        return 0
    else
        return 1
    fi


}

enter_gitlab_token()
{
    local token=""
    while test -z "$token" ; do
        echo "enter your gitlab private token" 1>&2
        stty_orig=$(stty -g)
        stty -echo
        read token
        stty $stty_orig
    done
    echo "$token"
}

search_in_repository()
{
    if test -z "$1" ; then
        echo ""
    else
        curl \
          --silent \
          --request GET \
          --header "PRIVATE-TOKEN: $1" \
          --data "search=^${3}$" \
          ${gitlab_api}/repository/$2 \
        | jq --raw-output "$4"
    fi
}

delete_in_repository()
{
    if test ! -z "$1" ; then
      curl \
        --silent \
        --request DELETE \
        --header "PRIVATE-TOKEN: $1" \
        ${gitlab_api}/repository/$2/$3
    fi
}

get_mr()
{
    if test -z "$1" ; then
        echo ""
    else
        curl \
          --silent \
          --request GET \
          --header "PRIVATE-TOKEN: $1" \
          ${gitlab_api}/merge_requests/$2 \
        | jq --raw-output "$3"
    fi
}
