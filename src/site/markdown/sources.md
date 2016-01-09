<!--- Copyright 2002-2016 CS Systèmes d'Information
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
[Files](https://www.orekit.org/forge/projects/orekit/files) tab in Orekit
forge and select one of the orekit-x.y-src.zip files. The x.y part in the name
 specifies the version. If this is the first time you download the library and
 you have not yet set up your own data set with UTC-TAI history, JPL ephemerides,
 IERS Earth Orientation Parameters ... you may want to also download the
 orekit-data.zip file which is an example file suitable for a quick start (see
 [configuration](./configuration.html) for further reading about data configuration).

## Development version

The development of the Orekit project is done using the [Git](http://git-scm.com/)
source code control system. Orekit Git master repository is available online.

 * you can browse it using the [Repository](https://www.orekit.org/forge/projects/orekit/repository)
    tab in Orekit forge
 * you can clone it anonymously with the command:

        git clone https://www.orekit.org/git/orekit-main.git

 * if you are a committer, you can clone it using your ssh credentials with the command:

        git clone ssh://git@www.orekit.org/orekit-main.git