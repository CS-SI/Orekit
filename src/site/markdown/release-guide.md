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


Since there are some differences in the releasing process between minor/major versions and patch versions, this guide is split in two distinct sections.  
The first one deals with releasing a major/minor version, while the second one is dedicated to patch versions and is mainly a list of differences between the two releasing processes.

# Releasing a Major / Minor version

## 0. Prerequisites

### SonaType OSS Account

1. Obtain private key of the Orekit Signing Key, key id:
   `0802AB8C87B0B1AEC1C1C5871550FDBD6375C33B`
2. Register for account on OSSRH and associate it with the Orekit project, see:
   https://central.sonatype.org/pages/ossrh-guide.html

If you need help with either ask on the development section of the Orekit
forum.

Once you have a SonaType OSS account, the corresponding credentials must be set
in the `servers` section of the `$HOME/.m2/settings.xml` file, using an id of
`ossrh`:

    <servers>
      <server>
        <id>ossrh</id>
        <username>the user name to connect to the OSS site</username>
        <password>the encrypted password</password>
      </server>
    </servers>

Use `mvn -ep` to generate an encrypted password.

### Install Graphviz 2.38

Graphviz is used to produce the UML diagrams for the site (see /src/design/*.puml files).  
Graphviz (dot) 2.39 and above put too much blank space in the generated
diagrams. The bug has not yet been fixed in graphviz, so we have to use 2.38 or
earlier. The version in CentOS 7 works, the version in Ubuntu 18.04 does not.

## 1. Verify the status of develop branch

Before anything, check on the [continuous integration
site](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit) that everything is fine on
develop branch:

* All tests pass;
* Code coverage is up to the requirements;
* There are no bugs, vulnerabilities or code smells.

If not, fix the warnings and errors first!

It is also necessary to check on the [Gitlab CI/CD](https://gitlab.orekit.org/orekit/orekit/pipelines)
that everything is fine on develop branch (i.e. all stages are passed).

## 2. Prepare Git branch for release

Release will be performed on a dedicated branch, not directly on master or
develop branch. So a new branch must be created as follows and used for
everything else:

    git branch release-X.Y
    git checkout release-X.Y

## 3. Update Maven plugins versions

Release is a good opportunity to update the maven plugin versions. They are all
gathered at one place, in a set of properties in `orekit/pom.xml`:

    <!-- Project specific plugin versions -->
    <orekit.spotbugs-maven-plugin.version>3.1.11</orekit.spotbugs-maven-plugin.version>
    <orekit.jacoco-maven-plugin.version>0.8.3</orekit.jacoco-maven-plugin.version>
    <orekit.maven-assembly-plugin.version>3.1.1</orekit.maven-assembly-plugin.version>
    ...

You can find the latest version of the plugins using the search feature at
[http://search.maven.org/#search](http://search.maven.org/#search). The
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

## 4. Updating changes.xml

Finalize the file `/src/changes/changes.xml` file.

The release date and description, which are often only set to `TBD` during
development, must be set to appropriate values. The release date at this step
is only a guess one or two weeks in the future, in order to take into account
the 5 days release vote delay.

Replace the `TBD` description with a text describing the version released:
state if it is a minor or major version, list the major features introduced by
the version etc. (see examples in descriptions of former versions).

Commit the `changes.xml` file.

    git add src/changes/changes.xml
    git commit -m "Updated changes.xml for official release."

## 5. Updating documentation

Several files must be updated to take into account the new version:

|            file name                |           usage            |                                     required update                                                    |
|-------------------------------------|----------------------------|--------------------------------------------------------------------------------------------------------|
| `build.xml`                         | building file for Ant users| Update project version number. Check all dependencies' versions are consistent with pom.xml            |
| `src/site/markdown/index.md`        | site home page             | Update the text about the latest available version, including important changes from **changes.xml**   |
| `org/orekit/overview.html`          | API documentation          | Update the text about the latest available version, including important changes from **changes.xml**   |
| `src/site/markdown/downloads.md.vm` | downloads links            | Declare the new versions, don't forget the date                                                        |
| `src/site/markdown/faq.md`          | FAQ                        | Add line to the table of dependencies.                                                                 |

Make sure the ant build works: `ant clean clean-lib jar javadoc`.

Once the files have been updated, commit the changes:

    git add build.xml src/site/markdown/*.md
    git commit -m "Updated documentation for the release."

## 6. Change library version number

The `pom.xml` file contains the version number of the library. During
development, this version number has the form `X.Y-SNAPSHOT`. For release, the
`-SNAPSHOT` part must be removed.

Commit the change:

    git add pom.xml
    git commit -m "Dropped -SNAPSHOT in version number for official release."

## 7. Check the JavaDoc

Depending the JDK version (Oracle, OpenJDK, etc), some JavaDoc warnings can be present.
Make sure there is no JavaDoc warnings by running the following command:

    mvn javadoc:javadoc

If possible, run the above command with different JDK versions.

## 8. Build the site

The site is generated locally using:

    mvn clean
    LANG=C mvn site

The official site is automatically updated on the hosting platform when work is 
merged into branches `develop`, `release-*` or `master`.

## 9. Tag and sign the git repository

When all previous steps have been performed, the local git repository holds the
final state of the sources and build files for the release. It must be tagged
and the tag must be signed. Note that before the vote is finished, the tag can
only signed with a `-RCx` suffix to denote Release Candidate. The final tag
without the `-RCx` suffix will be put once the vote succeeds, on the same
commit (which will therefore have two tags). Tagging and signing is done using
the following command, with `-RCn` replaced with the Release Candidate number:

    git tag X.Y-RCn -s -u 0802AB8C87B0B1AEC1C1C5871550FDBD6375C33B -m "Release Candidate n for version X.Y."

The tag should be verified using command:

    git tag -v X.Y-RCn

## 10. Pushing the branch and the tag

When the tag is ready, the branch and the tag must be pushed to Gitlab so
everyone can review it:

    git push --tags origin release-X.Y

*Good practice*: wait for the CI to succeed on the branch then release-X.Y branch on [SonarQube](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit) and check that everything is fine

## 11. Generating signed artifacts

When these settings have been set up, generating the artifacts is done by
running the following commands:

    mvn deploy -DskipStagingRepositoryClose=true -Prelease

During the generation, maven will trigger gpg which will ask the user for the
pass phrase to access the signing key. Maven didn’t prompt for me, so I had to
add `-Dgpg.passphrase=[passphrase]`

Once the commands ends, log into the SonaType OSS site
[https://oss.sonatype.org/](https://oss.sonatype.org/) and check the staging
repository contains the expected artifacts with associated signatures and
checksums:

- orekit-X.Y.pom
- orekit-X.Y.jar
- orekit-X.Y-sources.jar
- orekit-X.Y-javadoc.jar

The signature and checksum files have similar names with added extensions `.asc`,
`.md5` and `.sha1`.

Sometimes, the deployment to Sonatype OSS site also adds files with double extension
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

## 14. Merge release branch into master

Merge the release branch into the `master` branch to include any changes made.

    git checkout master
    git merge --no-ff release-X.Y

Then commit and push.

*Good practice*: Again, wait for the CI to succeed and check on SonarQube that the master branch report is fine.

## 15. Merge master branch into develop

Merge the `master` branch into the `develop` branch to include any changes made.

    git checkout develop
    git merge --no-ff master

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

## 18. Synchronize the Github mirror

To enhance the visibility of the project,
[a mirror](https://github.com/CS-SI/Orekit) is maintained on Github. The
releases created on Gitlab are not automatically pushed on this mirror. They
have to be declared manually to make visible the vitality of Orekit.

1. Login to Github
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

Check that everything looks nice and then merge `develop` on `master` branch and push the modifications.  
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

Here the main difference are that:
 - We're going to use a former release branch to do the release;
 - A vote of the PMC is not required to release the version.

If we're releasing patch version X.Y.Z, then we're going to use the already existing `release-X.Y` branch on the repository to do the release.  
With:
 - X: Major version number
 - Y: Minor version number
 - Z: Patch version number

 Once again, a patch version should only contain bug fixes that do not break APIs !

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
* If it exists, find the SonarQube report for the MR, either in a short-lived branch of [Orekit main CI report](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit) or on the developer own [sub-project](https://sonar.orekit.org/projects?sort=-analysis_date) on SonarQube.  
Check that:
  * All tests pass;
  * Code coverage is up to the requirements;
  * There are no bugs, vulnerabilities or code smells.

If not, ask for a fix of the warnings and errors first!

## 2. Prepare Git branch for release

The patch release will be performed on previous release branch.

So first, check out the dedicated branch:

    git checkout release-X.Y

Then, merge all the merge requests (MR) in the scope of the version in branch release-X.Y.  

There are two cases here, they are detailed in the two points below.

### 2.1. Merging the remaining merge requests

If the developer made his changes starting from branch `release-X.Y`, you can simply merge the branch of the MR in branch `release-X.Y`.

Note that if there aren't any conflict in Gitlab you can directly do the merge from Gitlab; just make sure that the target branch is `release-X.Y` and not "develop".

Find the MR on the repository, it should be in branch `origin/merge-requests/XXX` with XXX the number of the MR  

    git merge --no-ff origin/merge-requests/XXX

The `--no-ff` option will force a merge commit to be made, instead of doing a fast-forward on the branch.  
You can keep the automatic merge message unless you want to add some content to it.

Eventually, resolve any conflict and commit the result.

### 2.2. Cherry-picking the commits

If the developer started from develop branch (or any other branch), then the MR branch may contain code that should not be added to the release.  
You will have to cherry-pick the appropriate commits and add them to a dedicated branch.  
It is advised to use an IDE to do the cherry-picking although the command lines below will help you.

Find the MR on the repository and the commits you are interested in.

Create a dedicated branch for the issue on your local repository:

    git checkout -b issue-YYY

Where YYY is the number of the issue that the MR XXX fixes.  
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
