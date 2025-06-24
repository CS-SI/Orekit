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
releases. There are no automatic process for this, candidate release managers should
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
were able to publish Orekit releases prior to ORekit 13.1 must create a new account on
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

Note that the popup suggests to use `${server}` as the id, but we really should use `central`
as in the snippet above because this is what the `release.sh` script will use to retrieve
the credentials when it will use the central portal API.

The Sonatype [publish portal guide](https://central.sonatype.org/publish/publish-portal-guide/)
explains everything in detail, but this release guide is sufficient to perform a release.

## Note on maven plugins versions

Maven plugins should be updated from time to time, but it is probably
unwise to do it at release time, it is tool lates, so these updates
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
incompatibilities. For example when a plugin was not recently updated and
conflicts appear with newer versions of its dependencies.

Beware also that some plugins use configuration files that may need update too.
This is typically the case with `maven-checkstyle-plugin` and
`spotbugs-maven-plugin`. The `/checkstyle.xml` and
`/spotbugs-exclude-filter.xml` files may need to be checked.

Before committing these changes, you have to check that everything works. So
run the following command:

    mvn clean
    LANG=C mvn -Prelease site

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

* All tests pass;
* Code coverage is up to the requirements;
* There are no bugs, vulnerabilities or code smells.

If not, fix the warnings and errors first!

It is also necessary to check on the
[Gitlab CI/CD](https://gitlab.orekit.org/orekit/orekit/-/pipelines?scope=all&page=1&ref=develop)
that everything is fine on develop branch (i.e., all stages are passed); here again,
adapt the URL for patch release which starts from dedicated branches and not from
the develop branch.

## 2. Run the release.sh script

There is a `release.sh` shell script in the `scripts` directory that
helps perform the release. It handles many steps automatically. It
asks questions and waits for input and confirmation at various stages,
creating branches, checking them out, performing some automated
edition on several files, commiting the changes, compiling the library,
signing the artifacts and ultimately pushing everything to maven
central portal in a staging area.

It performs the release on a temporary branch that is merged only at
the final step, and it automatically removes it without merging if
something goes wrong before this final step, hence ensuring everything
remains clean.

When releasing a major or minor version, the script will automatically
post a vote thread to the forum Orekit community and PMC members
can vote on the release and then it will stop.

When releasing a patch version, the script will skip the vote thread and
continue up to promoting the release from staging to published.

The script does not handle (yet) the final stages like updating the website
or announcing the release on the GitHub mirror repository. These stages are
described in the next section.

This script must be run from the command line on a computer with several Linux
utilities (git, sed, xsltproc, gpg-agent, curl…), with the git worktree
already set to the start branch (i.e., develop or a patch branch):

    sh scripts/release.sh

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
    - put a temporary tag on the temporary branch, just to prime gpg-agent for later signatures
    - perform a complete build, including test and site generation, this can take a long time
    - merge release-X.Y-temporary branch into release-X.Y branch
    - delete release-X.Y-temporary branch
    - deploy the maven artifacts to central portal
    - tag and sign the repository (the passphrase for the key should not be asked again if gpg-agent was properly primed)
    - push the branch and the tag to origin


=========== TODO: the following steps are from the previous release guide, they should be adapted (and more steps added to the script) =========

Once the commands end, log into the Sonatype central portal site
[https://oss.sonatype.org/](https://oss.sonatype.org/) and check the staging
repository contains the expected artifacts with associated signatures and
checksums:

- orekit-X.Y.pom
- orekit-X.Y.jar
- orekit-X.Y-sources.jar
- orekit-X.Y-javadoc.jar

The signature and checksum files have similar names with added extensions `.asc`,
`.md5` and `.sha1`.

Sometimes, the deployment to Sonatype site also adds files with double extension
`.asc.md5` and `.asc.sha1`, which are in fact checksum files on a signature file
and serve no purpose and can be deleted.

Remove `orekit-X.Y.source-jar*` since they are duplicates of the
`orekit-X.Y-sources.jar*` artifacts. (We can’t figure out how to make maven
stop producing these duplicate artifacts). **Then click the “Close” button.**

## 12. Calling for the vote

Everything is now ready so the developers and PMC can vote for the release.
Create a post in the Orekit development category of the forum with a subject
line of the form:

    [VOTE] Releasing Orekit X.Y from release candidate n

and content of the form:

    This is a VOTE in order to release version X.Y of the Orekit library.
    Version X.Y is a maintenance release.


    Highlights in the X.Y release are:
      - feature 1 description
      ...
      - feature n description

    The release candidate n can be found on the GitLab repository as
    tag X.Y-RCn in the release-X.Y branch:
    <https://gitlab.orekit.org/orekit/orekit/tree/X.Y-RCn>

    The release notes can be read here:
    <https://test.orekit.org/site-orekit-X.Y/changes-report.html>

    Maven artifacts are available at
    <https://oss.sonatype.org/content/repositories/orgorekit-xxxx/>.

    The votes will be tallied in 120 hours for now, on 20yy-mm-ddThh:mm:00Z
    (this is UTC time).

You should also ping PMC members so they are aware of the vote. Their
vote is essential for a release as per project governance.

### 12.1. Failed vote

If the vote fails, the maven artifacts must be removed from OSS site by
dropping the repository and non-maven artifacts must be removed from the
`staging` directory in the Orekit site. Then a new release candidate must
be created, with a new number, a new tag and new artifacts. Another vote is
needed for this new release candidate. So make the necessary changes and then
start from the “Tag and sign the git repository” step.

### 12.2. Successful vote

When the vote for a release candidate succeeds, follow the steps below to
publish the release.

## 13. Tag release version

As the vote passed, a final signed tag must be added to the succeeding release
candidate, verified and pushed:

    git tag X.Y -s -u 0802AB8C87B0B1AEC1C1C5871550FDBD6375C33B -m "Version X.Y."
    git tag -v X.Y
    git push --tags

## 14. Merge release branch into main

Merge the release branch into the `main` branch to include any changes made.

    git checkout main
    git merge --no-ff release-X.Y

Then commit and push.

*Good practice*: Again, wait for the CI to succeed and check on SonarQube that the main branch report is fine.

## 15. Merge main branch into develop

Merge the `main` branch into the `develop` branch to include any changes made.

    git checkout develop
    git merge --no-ff main

Then updated the version numbers to prepare for the next development cycle.
Edit pom.xml version to SNAPSHOT and make space in the `/src/changes/changes.xml`
file for new changes.

Then commit and push.

*Good practice*: Again, wait for the CI to succeed and check on SonarQube that the develop branch report is fine.

## 16. Publish maven artifacts

The maven artifacts must be published using OSS site to release the repository.
Select the Orekit repository in “Staging Repositories” and click the “Release”
button in [Nexus Repository Manager](https://oss.sonatype.org/).

## 17. Upload to Gitlab

Navigate to Projects > Orekit > Deployments > Releases and make sure the X.Y release notes looks nice.

## 18. Synchronize the GitHub mirror

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

## 19. Update Orekit site

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

## 20. Close X.Y milestone

In Gitlab, navigate to Projects > Orekit > Issues > Milestones.
Click “Close Milestone” for the line corresponding to the release X.Y.

## 21. Announce release

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


# Releasing a Patch version

Here the main differences are that:
 - We're going to use a former release branch to do the release;
 - A vote of the PMC is not required to release the version.

If we're releasing patch version X.Y.Z, then we're going to use the already existing `release-X.Y` branch on the repository to do the release.  
With:
 - X: Major version number
 - Y: Minor version number
 - Z: Patch version number

 Once again, a patch version should only contain bug fixes that do not break APIs!

## 0. Prerequisites
Prerequisites are the same as for a minor/major version.

## 1.1 Verify the status of the already existing release branch

Here we will run the same checks than for a minor/major version but on the `release-X.Y` branch instead of the `develop` branch.  
See above for the checks (SonarQube: tests, code coverage, code quality / Gitlab CI/CD: all stages passed succesfully).

One could argue that the `release-X.Y` branch should always be in a clean state since it contains the latest release.  
However, for patches purpose, developers may have merged bug corrections on the `release-X.Y` branch since the last release.

## 1.2 Verify the status of the remaining opened merge requests

Here we are going to verify the status of each still-opened merge request (MR) that is in the scope of the version.  

Start from the [milestone](https://gitlab.orekit.org/orekit/orekit/-/milestones) of the version, note the issues in the scope that are not closed, and find the associated MR.  

Then, for each MR:
* Check that the pipeline was a success (i.e. all stages passed)
* If it exists, find the SonarQube report for the MR, either in a short-lived branch of [Orekit main CI report](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit) or on the developer's own [sub-project](https://sonar.orekit.org/projects?sort=-analysis_date) on SonarQube.  
Check that:
  * All tests pass;
  * Code coverage is up to the requirements;
  * There are no bugs, vulnerabilities or code smells.

If not, ask for a fix of the warnings and errors first!

## 2. Prepare Git branch for release

The patch release will be performed on the previous release branch.

So first, check out the dedicated branch:

    git checkout release-X.Y

Then, merge all the merge requests (MR) in the scope of the version in branch release-X.Y.  

There are two cases here, they are detailed in the two points below.

### 2.1. Merging the remaining merge requests

If the developer made his changes starting from branch `release-X.Y`, you can simply merge the branch of the MR in branch `release-X.Y`.

Note that if there aren't any conflicts in Gitlab you can directly do the merge from Gitlab; just make sure that the target branch is `release-X.Y` and not "develop".

Find the MR on the repository, it should be in branch `origin/merge-requests/XXX` with XXX the number of the MR  

    git merge --no-ff origin/merge-requests/XXX

The `--no-ff` option will force a merge commit to be made, instead of doing a fast-forward on the branch.  
You can keep the automatic merge message unless you want to add some content to it.

Eventually, resolve any conflict and commit the result.

### 2.2. Cherry-picking the commits

If the developer started from develop branch (or any other branch), then the MR branch may contain code that should not be added to the release.  
You will have to cherry-pick the appropriate commits and add them to a dedicated branch.  
It is advised to use an IDE to do the cherry-picking, although the command lines below will help you.

Find the MR on the repository and the commits you are interested in.

Create a dedicated branch for the issue in your local repository:

    git checkout -b issue-YYY

Where YYY is the issue number that the MR XXX fixes.  
If the branch already exists, give it a different name like `integrate-issue-YYY` or `integrate-MR-XXX`

Make a list of the IDs of the commits you want to add, example `A B C D` (in the order they were committed).

Cherry-pick the commits in a chronological order:

    git cherry-pick A B C D

Eventually, resolve any conflict and commit the result.

Return to the release branch and merge the branch issue-YYY:

    git checkout release-X.Y
    git merge --no-ff issue-YYY

## 3. Update Maven plugins versions
**Skip this** for a patch version.

## 4. Update changes.xml
Do the same as for a minor/major version.
## 5. Updating documentation
Do the same as for a minor/major version.

## 6. Change library version number

The `pom.xml` file contains the version number of the library.  
On the release-X.Y branch, the version number should be `X.Y` or `X.Y.Z` if a patch was already released for this version.

Replace version number from `X.Y` to `X.Y.1` or `X.Y.Z` to `X.Y.Z+1`

Commit the change:

    git add pom.xml
    git commit -m "Increment version number for patch release."

## Steps 7 to 11
Do the same as for a minor/major version.

## 12. Calling for the vote
**Skip this** for a patch version.

As per Orekit governance rules, a vote of the PMC is not required for a patch version.

## Steps 13 to 21
Do the same as for a minor/major version.
