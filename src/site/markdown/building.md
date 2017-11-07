<!--- Copyright 2002-2017 CS Systèmes d'Information
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Building Orekit

Orekit can be built from source using several different tools.
  
All these tools are Java based and can run on many different operating
systems, including Unix, GNU/Linux, Windows and Mac OS X. Some GNU/Linux
distributions provide these tools in their packages repositories.

## Building with Maven 3

[Maven](http://maven.apache.org/) is a build tool that goes far beyond
simply compiling and packaging a product. It is also able to resolve
dependencies (including downloading the appropriate versions from the public
repositories), to run automated tests, to launch various checking tools and
to create a web site for a project. It runs on any platform supporting Java.
  
For systems not providing maven as a package, maven can be
[downloaded](http://maven.apache.org/download.cgi) from its site at the
Apache Software Foundation. This site also explains the
installation procedure.

As with all maven enabled projects, building official released versions of
Orekit is straightforward (see below for the special case of development versions),
simply run:

    mvn package assembly:single

The preceding command will perform all dependencies retrieval, compilation,
tests and packaging for you. At the end, it will create several files named
target/orekit-x.y.jar where x.y is the version number.

This command should always work for released Orekit versions as they
always depend only on released Hipparchus versions. Maven knows how
to download the pre-built binary for released Hipparchus versions.
The previous command may not work for development Orekit versions as they
may depend on unreleased Hipparchus versions. Maven cannot download
pre-built binaries for unreleased Hipparchus versions as none are
publicly available. In this case the command above will end with an error message
like:

    [ERROR] Failed to execute goal on project orekit: Could not resolve dependencies for project org.orekit:orekit:jar:8.0-SNAPSHOT: Could not find artifact org.hipparchus:hipparchus-core:jar:1.0-SNAPSHOT

In this case, you should build the missing Hipparchus artifact and
install it in your local maven repository beforehand. This is done by cloning
the Hipparchus source from Hipparchus git repository at GitHub in some
temporary folder and install it with maven. This is done by
running the commands below (using Linux command syntax):

    git clone https://github.com/Hipparchus-Math/hipparchus.git
    cd hipparchus
    mvn install

Once the Hipparchus development version has been installed locally using
the previous commands, you can delete the cloned folder if you want. You can then
attempt again the mvn command at Orekit level, this time it should succeed as the
necessary artifact is now locally available.

If you need to configure a proxy server for dependencies retrieval, see
the [Guide to using proxies](http://maven.apache.org/guides/mini/guide-proxies.html)
page at the maven site.

If you already use maven for your own projects (or simply eclipse, see
below), you may want to install Orekit in your local maven repository. This is done
with the following command:

    mvn install

For other commands like generating the site, or generating the
[checkstyle](http://checkstyle.sourceforge.net/),
[findbugs](http://findbugs.sourceforge.net/) or
[jacoco](http://www.eclemma.org/jacoco/) reports, see the maven
plugins documentation at [maven site](http://maven.apache.org/plugins/index.html).

## Building with Eclipse

[Eclipse](http://www.eclipse.org/) is a very rich Integrated Development
Environment (IDE). It is a huge product and not a simple build tool.

For systems not providing eclipse as a package, it can be downloaded from its
site at the [Eclipse Foundation](http://www.eclipse.org/downloads/).

The simplest way to use Orekit with Eclipse is to follow these steps:

  * using your operating system tools, unpack the source distribution directly
    inside your Eclipse workspace. The source distribution file name has a name
    of the form orekit-x.y-sources.zip where x.y is the version number. Unpacking
    this zip file should create a folder of the form orekit-x.y in your workspace.

  * using Eclipse, import the project by selecting in the top level "File" menu
    the entry "Import..."

  * in the wizard that should appear, select "Maven -> Existing Maven Projects"

  * select the folder you just created in your workspace by unpacking the
    source distribution. The "pom.xml" file describing the project will be
    automatically selected. Click finish

The Orekit library should be configured automatically, including the dependency
to the underlying mathematical library. Note however that the tutorials
that are present in the source distribution are not automatically added by
this process (because the tutorials correspond to extra code and as such they
are not referenced in the pom.xml file).

Now you have an orekit-x.y project in you workspace, and you can create your
own application projects that will depend on the Orekit project.

You can also check everything works correctly by running the junit tests.

If you want to go further and run the tutorials, you should update the
project configuration to add them. In the Eclipse Package Explorer tab,
right-click on the orekit-x.y project and select from the conext menu
the entry "Build Path -> Configure Build Path...". Then in the wizard that
should appear, select the "Source" tab in the right pane, click the button
"Add Folder...", open the "tutorials" folder, select the two sub-folders
"java" and "resource" and click "OK". Now the projects should display the
tutorials. Note that since 9.0, you need to have an "orekit-data" folder
in your home directory in order to run the tutorials.
