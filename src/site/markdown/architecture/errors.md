<!--- Copyright 2002-2025 CS GROUP
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

# Errors

The `org.orekit.errors` package provides classes to generate and handle errors, based on
the classical mechanism of exceptions.

Starting with version 9.3, Orekit only uses unchecked exceptions, and most functions
do throw such exceptions. As they are unchecked, they are not advertised in either
`throws` statements in the function signature or in the javadoc. So users must consider
that as soon as they use any Orekit feature, an unchecked `OrekitException` may be thrown.
In most cases, users will not attempt to recover for this but will only use them to display
or log a meaningful error message.

The error messages generated are automatically translated, with the available languages being:

* Danish
* English,
* French,
* Galician,
* German,
* Greek,
* Italian,
* Norwegian,
* Romanian,
* Spanish.
