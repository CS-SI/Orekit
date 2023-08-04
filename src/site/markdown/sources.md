<!--- Copyright 2002-2023 CS GROUP
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

# Getting the sources

## Released versions

In order to get the source for officially released versions, go to the
[Release notes](https://gitlab.orekit.org/orekit/orekit/tags) in Orekit
forge and select one of the orekit-x.y-sources.zip files. The x.y part in the name
specifies the version. If this is the first time you download the library and
you have not yet set up your own data set with UTC-TAI history, JPL ephemerides,
IERS Earth Orientation Parameters ... you may want to also download the
orekit-data.zip file which is an example file suitable for a quick start (see
[configuration](./data/default-configuration.html) for further reading about data configuration).

It is also possible to retrieve published versions from the Git repository
(see next section below), if retrieving either release-x.y branches or the
master branch.

## Development version

The development of the Orekit project is done using the [Git](http://git-scm.com/)
source code control system. Orekit Git master repository is available online. The
latest developments are in the develop branch. This is the one you want to retrieve
if you need the latest feature before they are published in an official release.
See [guidelines](./guidelines.html) for the branching workflow used in Orekit.

 * you can browse it using the [Repository](https://gitlab.orekit.org/orekit/orekit/tree/develop)
    tab in Orekit forge

 * you can clone it anonymously with the command:

        git clone -b develop https://gitlab.orekit.org/orekit/orekit.git

 * if you are a committer, you can clone it using your ssh credentials with the command:

        git clone -b develop git@gitlab.orekit.org:orekit/orekit.git
