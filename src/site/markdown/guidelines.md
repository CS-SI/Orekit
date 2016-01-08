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

# Development Guidelines

The following guidelines are used for Orekit development.

## Development

Orekit is a low level library. It may be used in very different
contexts which cannot be foreseen, from quick studies up to critical
operations. The main driving goals are the following ones:

  * validation

  * robustness

  * maintainability

  * efficiency

The first goal, validation, implies tests must be as extensive as possible.
They should include realistic operational cases but also contingency cases.
The [jacoco](http://www.eclemma.org/jacoco/) tool must be used to
monitor test coverage. A very high level of coverage is desired. We do not
set up mandatory objective figures, but only guidelines here. However,a 60%
line coverage would clearly not be acceptable at all and 80% would be considered
deceptive.

The second goal, robustness, has some specific implications for a low level
component like Orekit. In some sense, it can be considered an extension of the
previous goal as it can also be improved by testing. It can also be improved
by automatic checking tools that analyze either source code or binary code. The
[findbugs](http://findbugs.sourceforge.net/) tool is already configured for
automatic checks of the library using a maven plugin.

This is however not sufficient. A library is intended to be used by applications
unknown to the library development team. The library development should be as
flexible as possible to be merged into an environment with specific constraints.
For example, perhaps an application should run 24/7 during months or years, so
caching all results to improve efficiency may prove disastrous in such a use case,
or it should be embedded in a server application, so printing to standard output
should never be done. Experience has shown that developing libraries is more
difficult than developing high level applications where most of the constraints
are known beforehand.

The third goal, maintainability, implies code must be readable, clear and
well documented. Part of this goal is enforced by the stylistic rules explained
in the next section, but this is only for the _automatic_ and simple checks. It
is important to keep a clean and extensible design. Achieving simplicity is
really hard, so this goal should not be taken too lightly. Good designs are a
matter of balance between two few objects that do too many things internally
an ignore each other and too many objects that do nothing alone and always need
a bunch of other objects to work. Always think in terms of balance, and check
what happens if you remove something from the design. Quite often, removing something
improves the design and should be done.

The fourth goal, efficiency, should be handled with care to not conflict with the
second and third goals (robustness and maintainability). Efficiency is necessary but
trying too much too achieve it can lead to overly complex unmaintainable code, to too
specific fragile code, and unfortunately too often without any gain after all because
of premature optimization and unfounded second-guess.

One surprising trick, that at first sight might seem strange and inappropriate has
been used in many part for Orekit and should be considered a core guideline. It
is the use of _immutable_ objects. This trick improves efficiency because many costly
copying operation are avoided, even unneeded one added for defensive programming. It
improves maintainability because both the classes themselves and the classes that use
them are much simpler. It also improves robustness because many (really many ...)
difficult to catch bugs are caused by mutable objects that are changed in some deeply
buried code and have an impact on user code that forgot to perform a defensive copy.
Orbits, dates, vectors, and rotations are all immutable objects.

## Style Rules

For reading ease and consistency, the existing code style should be
preserved for all new developments. The rules are common ones, inherited
mainly from the Sun [Code Conventions for the Java
Programming Language](http://java.sun.com/docs/codeconv/) guide style and
from the default [checkstyle](http://checkstyle.sourceforge.net/) tool
configuration. A few of these rules are displayed below. The complete
definition is given by the checkstyle configuration file in the project
root directory.

* *header rule*

  all source files start with the Apache license header,
  
* *indentation rules*

  no tabs, 4 spaces indentation, no indentation for case statements,
  
* *operators wrapping rules*

  lines are wrapped after operators (unlike Sun),
  
* *whitespace rules*

  operators are surrounded by spaces, method parameters open parenthesis
  is not preceded by space, lines do not end with white space,
  
* *curly brace rules*

  open curly brace are at end of line, with the matching closing curly brace
  aligned with the start of the corresponding keyword (_if_, _for_,
  _while_, _case_ or _do_),
  
* *encoding rules*

  characters encoding is _UTF8_, the git property _core.autocrlf_ should be
  set to _input_ on Linux development machines and to _true_ on Windows
  development machines (to ensure proper conversion on all operating systems),
  
* *naming rules*

  classes names begin with upper case, instance methods and fields
  names begin with lower case, class fields are all upper case with
  words separated by underscores,
  
* *ordering rules*

  class variables come first, followed by instance variables, followed
  by constructors, and followed by methods, public modifiers come first,
  followed by protected modifiers followed by private modifiers,
  
* *javadoc rules*

  all elements have complete javadoc, even private fields and methods
  (there are some rare exceptions, in case of code translated from
  the fortran language and models with huge parameters sets),
  
* *robustness rules*

  switch/case construct have a default argument, even when all possible
  cases are already handled, as many classes as possible are immutable,
  
* *miscellaneous rules*

  _star_ imports are forbidden, parameters and local variables are final
  wherever possible.

## Design Rules

* *coverage* (validation)

  seek for a line test coverage of at least 80% (more is better)

* *findbugs* (robustness)

  fix _all_ errors and warnings found by findbugs

* *no runtime assumptions* (robustness)

   do not make assumptions on the runtime environment of applications using Orekit
   (they may be embedded with no console, no possible user interaction, no network,
   no writable file system, no stoppable main program, have memory constraints,
   time constraints, be run in different linguistic contexts ...)

* *simplicity* (maintainability)

   follow Occam's razor principle or its declination in computer science: KISS (Keep It Simple, Stupid)

* *balanced design* (efficiency)

   seek efficiency, but do not overstep robustness and maintainability

* *immutable objects* (robustness, maintainability)

  use immutable objects as much as possible

* *checkstyle* (style)

  fix _all_ errors and warnings found by checkstyle
