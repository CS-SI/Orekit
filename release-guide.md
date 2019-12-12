# Orekit Release Guide

This release guide is largely inspired from [Hipparchus Release
Guide](https://www.hipparchus.org/release-guide.html). It lists the steps that
have been used in the past to release a new version of Orekit. When in doubt
ask the experts: Sébastien Dinot <sebastien.dinot@c-s.fr> for website questions
and Luc Maisonobe <luc.maisonobe@c-s.fr> for everything else.

## Prerequisites

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

## Install Graphviz 2.38

Graphviz (dot) 2.39 and above put too much blank space in the generated
diagrams. The bug has not yet been fixed in graphviz, so we have to use 2.38 or
earlier. The version in CentOS 7 works, the version in Ubuntu 18.04 does not.

## Verify the status of develop branch

Before anything, check on the [continuous integration
site](https://ci.orekit.org/job/Orekit/job/develop/) that everything is fine on
develop branch:

* All tests pass;
* Code coverage with JaCoCo is up to the requirements;
* There are no Maven, Java, Javadoc, Checkstyle and SpotBugs error or warning.

If not, fix the warnings and errors first!

## Prepare Git branch for release

Release will be performed on a dedicated branch, not directly on master or
develop branch. So a new branch must be created as follows and used for
everything else:

    git branch release-X.Y
    git checkout release-X.Y

## Update maven plugins versions

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

## Updating changes.xml

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

## Updating documentation

Several files must be updated to take into account the new version:

|            file name             |           usage            |                                     required update                                                    |
|----------------------------------|----------------------------|--------------------------------------------------------------------------------------------------------|
| `build.xml`                      | building file for Ant users| Update project version number. Check all dependencies' versions are consistent with pom.xml            |
| `src/site/markdown/index.md`     | site home page             | Update the text about the latest available version, including important changes from **changes.xml**   |
| `src/site/markdown/downloads.md` | downloads links            | Add a table with the links for files of the new versions, don't forget the date in the table caption   |
| `src/site/markdown/faq.md`       | FAQ                        | Add line to the table of dependencies.                                                                 |

Make sure the ant build works: `ant clean clean-lib jar javadoc`.

Once the files have been updated, commit the changes:

    git add build.xml src/site/markdown/*.md
    git commit -m "Updated documentation for the release."

## Change library version number

The `pom.xml` file contains the version number of the library. During
development, this version number has the form `X.Y-SNAPSHOT`. For release, the
`-SNAPSHOT` part must be removed.

Commit the change:

    git add pom.xml
    git commit -m "Dropped -SNAPSHOT in version number for official release."

## Tag and sign the git repository

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

## Pushing the branch and the tag

When the tag is ready, the branch and the tag must be pushed to Gitlab so
everyone can review it:

    git push --tags origin release-X.Y

## Generating signed artifacts

When these settings have been set up, generating the artifacts is done by
running the following commands:

    mvn clean
    mvn assembly:single deploy -DskipStagingRepositoryClose=true -Prelease

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
- orekit-X.Y-sources.zip

The signature and checksum files have similar names with added extensions `.asc`,
`.md5` and `.sha1`.

Sometimes, the deployment to Sonatype OSS site also adds files with double extension
`.asc.md5` and `.asc.sha1`, which are in fact checksum files on a signature file
and serve no purpose and can be deleted.

It is also possible that you get `orekit-X.Y-src.zip` (together with signature and
checksums) uploaded to Sonatype OSS site. These files can also be deleted as they
are not intended to be downloaded using maven but will rather be made available
directly on Orekit website.

Remove `orekit-X.Y.source-jar*` since they are duplicates of the
`orekit-X.Y-sources.jar*` artifacts. (We can’t figure out how to make maven
stop producing these duplicate artifacts). Then click the “Close” button.


## Site

The site is generated locally using:

    LANG=C mvn site

Once generated, the site can be archived and uploaded to the Orekit site:

    cd target/site
    scp -r * user@host:/var/www/mvn-site/site-orekit-X.Y


If you need help with this step ask ask Sébastien Dinot
<sebastien.dinot@c-s.fr>.

## Calling for the vote

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
    <https://orekit.org/staging/site-orekit-X.Y/changes-report.html>.

    Maven artifacts are available at
    <https://oss.sonatype.org/content/repositories/orgorekit-xxxx/>.

    The votes will be tallied in 120 hours for now, on 20yy-mm-ddThh:mm:00Z
    (this is UTC time).

You should also ping PMC members so they are aware of the vote. Their
vote is essential for a release as per project governance.

## Failed vote

If the vote fails, the maven artifacts must be removed from OSS site by
dropping the repository and non-maven artifacts must be removed from the
`staging` directory in the Orekit site. Then a new release candidate must
be created, with a new number, a new tag and new artifacts. Another vote is
needed for this new release candidate. So make the necessary changes and then
start from the “Tag and sign the git repository” step.

## Successful vote

When the vote for a release candidate succeeds, follow the steps below to
publish the release.

## Tag release version

As the vote passed, a final signed tag must be added to the succeeding release
candidate, verified and pushed:

    git tag X.Y -s -u 0802AB8C87B0B1AEC1C1C5871550FDBD6375C33B -m "Version X.Y."
    git tag -v X.Y
    git push --tags

## Merge release branch

Merge the release branch into the `develop` branch to include any changes made.
Then updated the version numbers to prepare for the next development cycle.

    git checkout develop
    git merge --no-ff release-X.Y

Edit pom.xml version to SNAPSHOT and make space in the changelog for new
changes. Then commit and push.

## Publish maven artifacts

The maven artifacts must be published using OSS site to release the repository.
Select the Orekit repository and click the “Release” button in Nexus Repository
Manager.

## Publish maven site

The maven generated site should then be moved out of the staging directory.
Beware to create a new `site-orekit-X.Y` directory for the site and link the
latest version to it. This allows older versions to be kept available if
needed.

    mv staging/site-orekit-X.Y ./
    ln -snf site-orekit-X.Y site-orekit-latest
    ln -snf site-orekit-X.Y site-orekit-development

## Upload to gitlab

Navigate to Self > Settings > Access Tokens. Enter a name, date, and check the
“api” box, then click “Create personal access token”. Copy the token into the
following command:

    for f in $( ls target/orekit-X.Y*{.zip,.jar}{,.asc} ) ; do
        curl --request POST --header "PRIVATE-TOKEN: <token>" --form "file=@$f" \
            https://gitlab.orekit.org/api/v4/projects/1/uploads
    done

Copy the URLs that are printed.

Next, navigate to Projects > Orekit > Repository > Tags. Find the X.Y tag and
click the edit button to enter release notes. Paste the URLs copied from the
step above.

Navigate to Projects > Orekit > Releases and make sure it looks nice.

## Update Orekit site

Several edits need to be made to the Orekit website. Fetch the current code:

    git clone https://gitlab.orekit.org/orekit/website-2015

Edit `download/.htaccess` and replace the URLs of of the 3 Orekit artifacts
with the ones created by gitlab in the previous step.

Edit `download.html` and update the URLs to point to the new Orekit artifacts.

Edit `_layouts/home.html` and edit the text of the bug button to use the new version.

Edit `_config.yml` and add the new version to the list of versions.

Run:

    jekyll serve

and make sure the website looks nice. View it on http://localhost:4000/

If everything looks good publish the changes by running:

    ./bin/build_and_publish.sh

## Mark resolved issues as closed

In gitlab select all the issues included in the release and close them.
Navigate to Projects > Orekit > Issues. Search for `label:Resolved`. Make sure
they were all fixed in this release. Click “Edit Issues”, check all the boxes,
set milestone to X.Y and status to closed.

## Announce release

The last step is to announce the release by sending a mail to the announce
list.
