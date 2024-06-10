<!--- Copyright 2002-2024 Thales Alenia Space
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

# Upgrading from a previous version

## Versioning policy

Orekit versions are identified with doublets or triplets of numbers separated
by dots, as in 13.0 or 12.0.1. The first number is the major version, the second
number is the minor version and when there is a third number it is a patch release.

As Orekit is used in critical operations contexts, great care is taken when
introducing evolutions into the library. The main guiding principles are the
following ones:

 - incompatible changes like removing a class, changing a method signature
   or deeply changing the semantics of a feature can only be introduced in
   major versions (i.e. when the major number is incremented by 1 and the minor
   number is reset to 0, like going from 12.1 to 13.0)
 - source compatible changes like adding a new class or a new method in an
   existing class (or to an interface if there is a default implementation) and
   bug fixes that do not need urgent fixes can be done in either major or minor
   releases (like going from 12.0 to 12.1)
 - bug fixes that do not change the API can be introduced in patch releases
   (like going from 12.0 to 12.0.1)

## Adapting users source code to Orekit version changes

The rules above imply that if some application is built on top of Orekit version x.y.z
and another version of the library is published:

  - if the new release is just a patch release (only z is incremented),
    then users are advised to take this new version into account as soon
    as possible by updating their application dependency to the new Orekit
    version. They can expect it to run as before (with just bugs fixed)
  - if the new release is a minor release (only y increased and z dropped),
    then users could update their application dependency if they are interested
    in the new features or in the bugs fixes that are included in the release.
    They can expect their existing code to run as before, and they can develop
    new code to benefit from the added features
  - if the new release is a major release (x incremented, y reset to 0 and
    z dropped), then users can decide when to take this new version into account
    depending on their own application schedule. They should be aware that in
    this case they may need to adapt their existing code, which may not compile
    anymore if it uses an API that has been changed.

There is no predefined schedule for publishing new versions. One is published when
ready. The history of released versions is available in the [downloads](./downloads.html)
page. Basically a new minor version or patch release is published every few months
and a new major version is published every one or two years. Patch releases can
be published very fast (a few hours), which happens when an urgent fix is needed.

Starting with version 13.0, when a new major version is published, a dedicated
page is devoted to each major upgrade. Users can find hints about what they
should do to adapt their code in these pages. The following table contains links
to these hints pages:

| upgrade hints                                 |
|-----------------------------------------------|
| [from 12.X to 13.0](./upgrades/12-to-13.html) |
