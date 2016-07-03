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
Orbit Determination
===================

The `org.orekit.estimation` package provides classes to manage orbit determination.
    
Scope
-----

Orbit determination support in Orekit is similar to other space flight dynamics topics
support: the library provides the framework with top level interfaces and classical
implementations (say distance and angular measurements among others). Some hooks are
also provided for expert users who need to supplement the framework with mission-specific
features and implementations (say specific delay models for example). The provided objects
are sufficient for basic orbit determination and can easily be extended to address more
operational needs.

Organization
------------

There are two main sub-packages: `org.orekit.estimation.measurements` and `org.orekit.estimation.leastsquares`.

### Measurements

![orbit determination measurements class diagram](../images/design/orbit-determination-measurements-class-diagram.png)

The `measurements` package defines everything that is related to the measurements themselves, both the theoretical
values and the modifications that can be applied to them. All measurements must implement the `Measurements`
interface, which is the public API that the engine will use to deal with all measurements. The most important
methods of this interface allow to:

* get the observed value
* compute the theoretical value of a measurement,
* compute the corresponding partial derivatives (with respect to state and parameters)
* commute the time offset between measurement and spacecraft state

The theoretical evaluations can be modified by registering one or several `EvaluationModifier`
objects. These objects will manage notions like tropospheric delays, biases, ground antennas mounts ...

A typical operational case from a ground stations network would create distance and angular measurements, create
one bias modifier for the on-board delay for distance measurements, a few modifiers for each ground station
(position offset, antenna mount, delay), one modifier for tropospheric delay and add them to corresponding measurements
(i.e. all distance measurements would share the same on-board delay object, but distance measurements performed
by two difference ground stations would refer to different sets of ground station positions offsets for example).

The classical measurements and modifiers are already provided by Orekit in the same package, but for more advanced
needs, users are expected to implement their own implementations. This ensures the extensibility of this design.

### Least Squares

![orbit determination overview class diagram](../images/design/orbit-determination-overview-class-diagram.png)

The `leastsquares` package provides an implementation of a batch least squares estimator engine to perform an orbit
determination. Users will typically create one instance of this object, register all observation data as measurements
with their included modifiers, and run the least squares filter. At the end of the process, a fully configured propagator
is returned, including the estimated orbit as the initial state and the estimated propagator parameters. The estimated
measurement and propagator parameters can also be retrieved by themselves.

The `BatchLSEstimator` class creates an internal implementation of Hipparchus `LeastSquaresProblem` interface
to represent the orbit determination problem and passes it to one of the `LeastSquaresOptimizer` implementations to
solve it. During the resolution, the selected Hipparchus algorithm will call the `value` method of the
local `LeastSquaresProblem` model at each algorithm test point. This will trigger one orbit propagation with
some test values for the orbit state and the parameters (for example biases from the measurements modifiers parameters
or drag coefficients from the force models parameters). During the propagation, the Orekit event mechanism is used to
collect the state and its Jacobians at measurements dates. A `MeasurementsHandler` class performs the binding between the
generic events handling mechanism and the orbit determination framework. At each measurement date, it gets the state
and Jacobians from the propagator side, it calls the measurement methods to get the residuals and the partial
derivatives on the measurements side, and it fetches the least squares estimator with the combined values, to be
provided back to the Hipparchus algorithm, thus closing the loop.

The orbital state is always estimated. Users can also estimate some propagator  and measurements parameters. These parameters
are modeled using the `Parameter` class, which is essentially  a key-value pair with multi-dimensional values. The
parameters can be flagged as estimated or fixed, and the values can be modified. Each measurement or modifier that
support some parameters will create one instance for each parameter. The `BatchLSEstimator` estimator will retrieve
all the parameters and the caller can select the set of parameters that should be estimated for a given run. At each
iteration of the least squares solver, the test values for the estimated parameters will be reset.

![orbit determination parameters class diagram](../images/design/orbit-determination-parameters-class-diagram.png)

The class diagram above depicts the parameter update mechanism for the case of ground station position offset. The
`Range` and `RangeRate` measurements classes refer to a `GroundStation` instance (one instance shared by all
measurements using this station) that itself is a 3-dimensional parameter representing the adjustable
offset for station position. When station position offset is flagged to be estimated, the `BatchLSEstimator` will
change its value at each iteration. The `Range` and `RangeRate` measurements theoretical values will therefore
be computed naturally using the updated station position. Other parameters like biases or calibration factors
are estimated using the same process.

