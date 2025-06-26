# Orekit Release Guide

This release guide is largely inspired from [Hipparchus Release
Guide](https://www.hipparchus.org/release-guide.html). It lists the steps that
have been used in the past to release a new version of Orekit.  
When in doubt, ask a question on the ["Orekit development" section of the forum](https://forum.orekit.org/c/orekit-development/5).


Three types of versions can be released:
 - **Major version**: numbered xx.0, a version that can introduce both new features and bug corrections.  
 This is the only type of version where APIs are allowed to evolve, creating eventual incompatibilities with former versions.
 - **Minor version**: numbered xx.y, a version that can introduce both new features and bug corrections but where breaking APIs is forbidden.  
 A version xx.y must be perfectly compatible with its xx.0 major version counterpart.
 - **Patch version**: numbered xx.y.z, a version where only bug corrections are allowed.  
 Once again APIs incompatibility with version xx.y or xx.0 are not allowed.  
 A patch version is the only type of version where a vote from the PMC isn't required to publish the release.

# 0. Prerequisites

The following tasks are one-time actions, they just prepare the credentials for
performing the release, but these credentials will remain valid for years and
for several releases.

## Signing Key

Orekit artifacts are signed with a specific GPG signing key that is shared among
all release managers. The id of this key is `0802AB8C87B0B1AEC1C1C5871550FDBD6375C33B`.
Release managers should ask for this key and its password before they can sign
releases. There are no automatic processes for this, candidate release managers should
just send direct messages to existing release managers, and they will proceed with
sending the key and password, using any means they see fit.

## Sonatype central portal

Binary artifacts are published in the maven central repository, which is managed by Sonatype.
The `org.orekit` namespace is used for these artifacts, and only people that have
been identified as allowed to publish releases can push binary artifacts for publication.

In order to publish, candidate relese managers should therefore create an account on
Sonatype central repository using the
[central portal login page](https://central.sonatype.com/account). Once the
account has been created they should ask existing release managers to add them to the
allowed list for the `org.orekit` namespace.

Note that the central portal account and the associated credentials are different from
the legacy OSSRH system that was used before Orekit 13.1. This implies that people that
were able to publish Orekit releases prior to Orekit 13.1 must create a new account on
the new central portal system, their older account on legacy OSSRH is obsolete.

After creating the account on Sonatype central portal, release managers should create
a User Token. This is done by login to
[central portal login page](https://central.sonatype.com/account), selecting the
`View Account` item from the menu drop list at the upper right of the page (small
button that displays the start of the username) and selecting the
`Generate User Token` button in the `Setup Token-Based Authentication` page. This
will generate a token with a small random Id and a long random password. They
should copy the xml snippet that has been generated into their
`$HOME/.m2/settings.xml` file, in the `servers` section (except for server name that
should be set to `central`), taking care that the popup window automatically disappears
after one minute:

    <servers>
      <server>
        <id>central</id>
        <username>the generated username</username>
        <password>the generated password</password>
      </server>
    </servers>

Note that the popup suggests to use `${server}` as the id, but users should really use
`central` as in the snippet above because this is what both the maven deployment plugin,
the `failed-vode.sh` script, and the `successful-vote.sh` script will use to retrieve
the credentials when connecting to the central portal API.

The Sonatype [publish portal guide](https://central.sonatype.org/publish/publish-portal-guide/)
explains everything in detail, but this release guide is sufficient to perform a release.

## Note on maven plugins versions

Maven plugins should be updated from time to time, but it is probably
unwise to do it at release time, it is too late, so these updates
should happen well before the release. All maven plugin versions are
gathered at one place, in a set of properties in `pom.xml`:

    <!-- Project specific plugin versions -->
    <orekit.spotbugs-maven-plugin.version>3.1.11</orekit.spotbugs-maven-plugin.version>
    <orekit.jacoco-maven-plugin.version>0.8.3</orekit.jacoco-maven-plugin.version>
    <orekit.maven-assembly-plugin.version>3.1.1</orekit.maven-assembly-plugin.version>
    ...

You can find the latest version of the plugins using the search feature at
[https://central.sonatype.com/](https://central.sonatype.com/). The
properties name all follow the pattern `orekit.some-plugin-name.version`, the
plugin name should be used in the web form to check for available versions.

Beware that in some cases, the latest version cannot be used due to
incompatibilities. For example, when a plugin was not recently updated and
conflicts appear with newer versions of its dependencies.

Beware also that some plugins use configuration files that may need update too.
This is typically the case with `maven-checkstyle-plugin` and
`spotbugs-maven-plugin`. The `/checkstyle.xml` and
`/spotbugs-exclude-filter.xml` files may need to be checked.

Before committing these changes, you have to check that everything works. So
run the following command, taking care to use the Java version that matches
the compatibility requirements (Java 8 at time of writing):

    mvn clean
    LANG=C JAVA_HOME=/path/to/correct/java/version PATH=${JAVA_HOME}/bin:$PATH mvn -Prelease site

If something goes wrong, either fix it by changing the plugin configuration or
roll back to an earlier version of the plugin.

Browse the generated site starting at page `target/site/index.html` and check
that everything is rendered properly.

When everything runs fine and the generated site is OK, then you can commit the
changes:

    git add orekit/pom.xml orekit/checkstyle.xml orekit/spotbugs-exclude-filter.xml
    git commit -m "Updated maven plugins versions."


# 1. Verify the status of start branch

Major and minor versions are released starting from the develop branch. Patch versions
are released from a dedicated branch (typically named patch-X.Y.Z where X.Y.Z is the
candidate version number).

Before anything, check on the
[continuous integration](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit&branch=develop)
that everything is fine on develop branch for major or minor release; adapt the URL for patch
release which starts from dedicated branches and not from the develop branch.

Start from the
[milestone](https://gitlab.orekit.org/orekit/orekit/-/milestones) of
the version, note the issues in the scope that are not closed, and
find the associated MR.

Then, for each MR:
* Check that the pipeline was a success (i.e. all stages passed)
* If it exists, find the SonarQube report for the MR, either in a short-lived branch of
  [Orekit main CI report](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit) or on
  the developer's own [sub-project](https://sonar.orekit.org/projects?sort=-analysis_date)
  on SonarQube.  

On the start branch, check that:
* All tests pass;
* Code coverage is up to the requirements;
* There are no bugs, vulnerabilities or code smells.

If not, fix the warnings and errors first!

It is also necessary to check on the
[Gitlab CI/CD](https://gitlab.orekit.org/orekit/orekit/-/pipelines?scope=all&page=1&ref=develop)
that everything is fine on develop branch (i.e., all stages are passed); here again,
adapt the URL for patch release which starts from dedicated branches and not from
the develop branch.

### 1.1. Merging the remaining merge requests

For patch releases, it is possible to include merge requests if they were created from the
`release-X.Y` branch, but not if they were created from the `develop` branch as it would
include evolutions and not only bug fixes.

Note that if there aren't any conflicts in Gitlab you can directly do
the merge from Gitlab; just make sure that the target branch is
`release-X.Y` and not "develop".

Find the MR on the repository, it should be in branch `origin/merge-requests/XXX` with XXX the number of the MR  

    git merge --no-ff origin/merge-requests/XXX

The `--no-ff` option will force a merge commit to be made, instead of doing a fast-forward on the branch.  
You can keep the automatic merge message unless you want to add some content to it.

Eventually, resolve any conflict and commit the result.

### 1.2. Cherry-picking the commits

If the developers started from develop branch (or any other branch),
then the MR branch may contain code that should not be added to the
release.  You will have to cherry-pick the appropriate commits and add
them to a dedicated branch.  It is advised to use an IDE to do the
cherry-picking, although the command lines below will help you.

Find the MR on the repository and the commits you are interested in.

Create a dedicated branch for the issue in your local repository:

    git checkout -b issue-YYY

Where YYY is the issue number that the MR XXX fixes.  
If the branch already exists, give it a different name like `integrate-issue-YYY` or `integrate-MR-XXX`

Make a list of the IDs of the commits you want to add, example `A B C D` (in the order they were committed).

Cherry-pick the commits in a chronological order:

    git cherry-pick A B C D

Eventually, resolve any conflict and commit the result.

Return to the start branch and merge the branch issue-YYY:

    git checkout <name of the start branch>
    git merge --no-ff issue-YYY

## 2. Run the prepare-release.sh script

There is a `prepare-release.sh` shell script in the `scripts` directory that
helps perform the release. It handles many steps automatically. It
asks questions and waits for input and confirmation at various stages,
creating branches, checking them out, performing some automated
edition on several files, commiting the changes, compiling the library,
signing the artifacts and ultimately pushing everything to maven
central portal in a staging area.

It performs the release on a temporary branch that is merged only at
the final step, and it automatically removes it without merging if
something goes wrong before this final step, hence ensuring everything
remains clean. It will also delete the uploaded artifacts on central
portal if something goes wrong after upload but before script completion.

When releasing a major or minor version, the script will automatically
post a vote thread to the forum Orekit community and PMC members
can vote on the release and then it will stop. When releasing a patch version,
the script will skip the vote thread and continue up to promoting the release
from staging to published.

The script does not handle (yet) the final stages like updating the website
or announcing the release on the GitHub mirror repository. These stages are
described in the next section.

This script must be run from the command line on a computer with several Linux
utilities (git, sed, xsltproc, gpg-agent, curl…), with the git worktree
already set to the start branch (i.e., develop or a patch branch):

    sh scripts/prepare-release.sh

Here are the steps the script will perform on its own, asking
for user confirmation before any commit:

    - perform safety checks (files and directories present, utilities available, java version)
    - retrieve central portal credentials from `$HOME/.m2/settings.xml`
    - check if the release is a major, minor or patch release
      using the -SNAPSHOT version number from the current branch `pom.xml`
    - for major or minor release, create a release-X.Y branch from develop (reuse existing branch for patch release)
    - checkout the release-X.Y branch
    - create a release-X.Y-temporary branch
    - checkout the release-X.Y-temporary branch
    - merge the start branch into the release-X.Y-temporary branch
    - drop -SNAPSHOT version number from `pom.xml` and commit the change
    - compute candidate release date 5 days in the future
    - update `changes.xml` with date and user-provided description and commit the change
    - update downloads and faq pages and commit the changes
    - merge release-X.Y-temporary branch into release-X.Y branch
    - delete release-X.Y-temporary branch
    - tag and sign the repository
      (the passphrase for the signing key should be asked for at this stage)
    - perform a full build and deploy the maven artifacts to central portal
      (the passphrase for the signing key may be asked for again at the end of this stage if it was not cached)
    - save the deployment id in `$HOME/.local/share/orekit-release-scripts/deployment-ids` for future scripts
    - push the branch and the tag to origin
    - if the release is a patch release, call immediately the `successful-vote.sh` script
    - if the release is a minor or major release, propose text to copy-paste into the forum for creating the vote topic

## 3. Create the vote topic on the forum

For minor and major releases, the script proposes title and content for the vote topic,
so release managers should just have to log into the forum and copy-paste the proposed
text and content to create the vote topic. The script does not (yet) create the vote
topic by itself.

### 3.1. Failed vote

If the vote fails, the maven artifacts must be removed from the central portal.
This is done by running the `failed-vote.sh` script, taking care to be in
the release-X.Y branch:

    sh scripts/failed-vote.sh

Here are the steps the script will perform on its own, asking
for user confirmation before any commit:

    - perform safety checks (files and directories present, utilities available)
    - retrieve central portal credentials from `$HOME/.m2/settings.xml`
    - check the branch to see it is really a release branch
    - check the tag for the failed release candidate
    - delete maven artifacts from the central portal, using the deployment id that was saved in
      `$HOME/.local/share/orekit-release-scripts/deployment_ids` by the `prepare-release.sh` script
    - remove the obsolete deployment id from `$HOME/.local/share/orekit-release-scripts/deployment_ids`

Then the process should be started again using the `prepare-release.sh` script.
Note that the script keeps track of the release candidate number on its own as it looks
for the tags in the git repository and as release candidate tags are never deleted.

### 3.2. Successful vote

When the vote for a release candidate succeeds (or automatically when the release is a
patch release that is published without a vote), the maven artifacts must be published
from the central portal and a final tag (without reference to release candiate number)
must be created. This is done by running the `successful-vote.sh` script, taking
care to be in the release-X.Y branch:

    sh scripts/successful-vote.sh

Here are the steps the script will perform on its own, asking
for user confirmation before any commit:

    - perform safety checks (files and directories present, utilities available)
    - retrieve central portal credentials from `$HOME/.m2/settings.xml`
    - check the branch to see it is really a release branch
    - check the tag for the successful release candidate
    - publish maven artifacts from the central portal, using the deployment id that was saved in
      `$HOME/.local/share/orekit-release-scripts/deployment_ids` by the `prepare-release.sh` script
    - remove the obsolete deployment id from `$HOME/.local/share/orekit-release-scripts/deployment_ids`
    - tag and sign the repository (the passphrase for the key should be asked at this stage)
    - push the branch and the tag to origin
    - merge release branch to main branch
    - push main branch to origin
    - merge main branch to develop branch
    - update version number in `pom.xml` for next development cycle
    - update version number in `changes.xml` for next development cycle
    - push develop branch to origin

## 4. Synchronize the GitHub mirror

To enhance the visibility of the project,
[a mirror](https://github.com/CS-SI/Orekit) is maintained on GitHub. The
releases created on Gitlab are not automatically pushed on this mirror. They
have to be declared manually to make visible the vitality of Orekit.

1. Login to GitHub
2. Go to the [Orekit releases](https://github.com/CS-SI/Orekit/releases) page
3. Click on the [Draft a new release](https://github.com/CS-SI/Orekit/releases) button
4. In the “Tag version” field of the form and in the “Release title” field,
   enter the tag of the release to be declared
5. Describe the release as it has been done on Gitlab
6. Click on “Publish release”

Github automically adds two assets (zip and tarball archives of the tagged source code)

## 5. Update Orekit site

Several edits need to be done to the Orekit website after the vote.

First, clone the current code:

    git clone https://gitlab.orekit.org/orekit/website-2015

Switch to `develop` branch.
Edit `overview.html`:
 - (If needed) Update the new Hipparchus version.  
 - Update the `overview.png` image with the new version numbers.
 - (If needed) Update the *Features* section with the new features added by the new version of Orekit.

Create a new post for the release in `_post/`, it will be visible in the `News` page (see section *Announce Release* for the content of the post).

Push the modifications on `develop` branch, wait until the pipeline on Gitlab is finished, then the [test website](https://test.orekit.org/) will be updated.

Check that everything looks nice and then merge `develop` on `main` branch and push the modifications.  
When the Gitlab pipeline is finished, the [official website](https://orekit.org/) should be updated according to your changes.

## 6. Close X.Y milestone

In Gitlab, navigate to Projects > Orekit > Issues > Milestones.
Click “Close Milestone” for the line corresponding to the release X.Y.

## 7. Announce release

The last step is to announce the release by creating a post in the Orekit
announcements category of the forum with a subject line of the form:

    Orekit X.Y released

and content of the form:

    The Orekit team is pleased to announce the release of Orekit version X.Y.
    This is a minor/major version, including both new features and bug fixes.
    The main changes are:

      - feature 1 description
      ...
      - feature n description

    This version depends on Hipparchus X'.Y'

    For complete release notes please see:
    https://www.orekit.org/site-orekit-X.Y/changes-report.html

    The maven artifacts are available in maven central. 
    The source and binaries can be retrieved from the forge releases page:
    https://gitlab.orekit.org/orekit/orekit/-/releases
