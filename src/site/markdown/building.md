<!--- Copyright 2002-2015 CS Systèmes d'Information
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

Building Orekit
===============

Orekit can be built from source using several different tools.
  
All these tools are Java based and can run on many different operating
systems, including Unix, GNU/Linux, Windows and Mac OS X. Some GNU/Linux
distributions provide these tools in their packages repositories.

Building with Maven 3
---------------------

[Maven](http://maven.apache.org/) is a build tool that goes far beyond
simply compiling and packaging a product. It is also able to resolve
dependencies (including downloading the appropriate versions from the public
repositories), to run automated tests, to launch various checking tools and
to create a web site for a project. It runs on any platform supporting Java.
  
For systems not providing maven as a package, maven can be
[downloaded](http://maven.apache.org/download.cgi) from its site at the
Apache Software Foundation. This site also explains the
installation procedure.

As with all maven enabled projects, building Orekit is straightforward, simply
run:

    mvn assembly:single

The preceding command will perform all dependencies retrieval, compilation,
tests and packaging for you. At the end, it will create several files named
target/orekit-x.y.jar where x.y is the version number.

If you need to configure a proxy server for dependencies retrieval, see
the [Guide to using proxies](http://maven.apache.org/guides/mini/guide-proxies.html)
page at the maven site.

If you already use maven for your own projects (or simply eclipse, see
below), you may want to install Orekit in your local repository. This is done
with the following command:

    mvn install

For other commands like generating the site, or generating the
[checkstyle](http://checkstyle.sourceforge.net/),
[findbugs](http://findbugs.sourceforge.net/) or
[jacoco](http://www.eclemma.org/jacoco/) reports, see the maven
plugins documentation at [maven site](http://maven.apache.org/plugins/index.html).

Building with Eclipse
---------------------

[Eclipse](http://www.eclipse.org/) is a very rich Integrated Development
Environment (IDE). It is a huge product and not a simple build tool.

For systems not providing eclipse as a package, it can be downloaded from its
site at the [Eclipse Foundation](http://www.eclipse.org/downloads/).

The simplest way to use Orekit with Eclipse is to follow these steps:

  * unpack the distribution inside your Eclipse workspace

  * create a new java project from existing sources and direct Eclipse to the
     directory where you unpacked Orekit

  * set the source folders to
    * orekit/src/main/java
    * orekit/src/test/java
    * orekit/src/tutorials/java
	* orekit/src/main/resources
    * orekit/src/test/resources
    * orekit/src/tutorials/resources

    in the source tab of the Configure Build Path dialog

  * set the external libraries to JRE system library (provided by Eclipse),
    Junit 4.x (provided by Eclipse) and Apache Commons Math (available at
    Apache Software Foundation Commons Math
    [downloads page](http://commons.apache.org/proper/commons-math/download_math.cgi))
    in the libraries tab of the Configure Build Path dialog
