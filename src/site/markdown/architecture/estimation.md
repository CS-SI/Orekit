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

# Orbit Determination

The `org.orekit.estimation` package provides classes to manage orbit determination.
    
Scope
-----

Orbit determination support in Orekit is similar to other space flight dynamics topics
support: the library provides the framework with top level interfaces and classical
implementations (say distance and angular measurements among others). Some hooks are
also provided for expert users who need to supplement the framework with mission-specific
features and implementations (say specific delay models for example). The provided objects
are sufficient for classical orbit determination and can easily be extended to address more
operational needs.

Organization
------------

There are four main sub-packages: `org.orekit.estimation.measurements`, `org.orekit.estimation.leastsquares`,
`org.orekit.estimation.sequential`, and `org.orekit.estimation.iod`.

### Measurements

![orbit determination measurements class diagram](../images/design/orbit-determination-measurements-class-diagram.png)

The `measurements` package defines everything that is related to the measurements themselves, both the theoretical
values and the modifications that can be applied to them. All measurements must implement the `ObservedMeasurement`
interface, which is the public API that the engine will use to deal with all measurements. The most important
methods of this interface allow to:

* get the observed value
* estimate the theoretical value of a measurement,
* compute the corresponding partial derivatives (with respect to state and parameters)
* compute the time offset between measurement and spacecraft state

The estimated measurements can be modified by registering one or several `EstimationModifier`
objects. These objects will manage notions like ionospheric or tropospheric delays, biases,
ground antennas position offsets, Antenna Phase Center... One specific modifier `OutlierFilter`
can be used to reject outliers at run time if the current residuals exceed the theoretical
standard deviation by some user-defined factor.

A typical operational case from a ground stations network would create distance and angular measurements, create
one bias modifier for the on-board delay for distance measurements, a few modifiers for each ground station
(position offset, delay), modifiers for tropospheric and ionospheric delays and add them to corresponding measurements
(i.e. all distance measurements would share the same on-board delay object, but distance measurements performed
by two difference ground stations would refer to different sets of ground station positions offsets for example).

The classical measurements and modifiers are already provided by Orekit in the same package, but for more advanced
needs, users are expected to add their own implementations. This ensures the extensibility of this design.

#### Measurements generation

![measurements generation class diagram](../images/design/measurements-generation-class-diagram.png)

The `measurements.generation` package provides a simulation feature that can generate realistic
measurements. This is mainly useful in validation phases and in mission analysis (for example
design of a ground stations network or assessment of achievable accuracy for a mission).

For each type of measurement that must be generated, a `Scheduler` is configured to match the mission
or ground segment specific measurements schedule and includes a `MeasurementBuilder` for the measurement
type considered. One particularly important predefined scheduler is the `EventBaseScheduler`. This
scheduler uses a regular event detector to identify measurements feasibility time spans. Some event
detectors that may be useful with this scheduler are visibility from ground (`ElevationDetector`),
ground at night (`GrondAtNightDetector`) for satellite laser ranging, sunlit satellite (`EclipseDetector`)
for optical tracking, inter satellite direct view (`InterSatDirectViewDetector`) for GNSS, and
boolean combination of several detector (`BooleanDetector`) for complex settings. This scheduler as
well as the simpler `ContinuousScheduler` both rely on `DatesSelector` to select individual measurements
dates within a time range. The `FixedStepSelector` generates a continuous stream of measurements separated
by a fixed step (for example one measurement every 60s during the allowed time range) while the `BurstSelector`
generates bursts of high rate measurements separated by rest periods (for example bursts containing
256 measurements each separated by 100ms, with a new burst generated every 300s). Both selectors can
ensure the dates are aligned with a time scale (for example the first measurement in each burst being
at exact UTC minutes 5, 10, 15... in the previous example).

Several schedulers can be configured at the same time, if either different types of measurements are
available (range, range-rate, optical tracking on stars background...) or several independent schedules
are used (for example if several ground stations are available). All schedulers are registered to
a single `Generator` which when run will produce simulated measurements in the specified time range.

In order to handle measurements on the fly as they are generated, users can use custom implementations
of the `GeneratedMeasurementSubscriber`. This is useful for example if a very large number of measurements
are to be generated as it would be impractical to store everything in memory. If on the other hand
the number of measurements remains limited (a few hundreds or thousands), then the `GatheringSubscriber`
implementation can be used, it will store everything in a sorted set that can be retrieved after generation.
In both cases (on-the-fly handling or retrieval of a `SortedSet`), the measurements are sorted either
chronologically or reverse-chronologically according to the `startDate` and `endDate` parameters used when
calling `Generator.generate`.

### Least Squares

![orbit determination overview class diagram](../images/design/orbit-determination-overview-class-diagram.png)

The `leastsquares` package provides an implementation of a batch least squares estimator engine to perform an orbit
determination. Users will typically create one instance of this object, register all observation data as measurements
with their included modifiers, and run the least squares filter. At the end of the process, a fully configured propagator
is returned, including the estimated orbit as the initial state and the estimated propagator parameters. The estimated
measurement and propagator parameters can also be retrieved by themselves.

The `BatchLSEstimator` class creates an internal implementation of Hipparchus `LeastSquaresProblem` interface
to represent the orbit determination problem and passes it to one of the `LeastSquaresOptimizer` implementations to
solve it. Several choices are possible, among which `LevenbergMarquardtOptimizer` and `GaussNewtonOptimizer`. The former
is considered more robust and can start from initial guesses farther than the second one. If `GaussNewtonOptimizer` is
neverthelesss selected, it should be configured to use `QR` decomposition rather than `LU` decomposition for increased
stability in case of poor observability. During the resolution, the selected Hipparchus algorithm will call the `evaluate`
method of the local `LeastSquaresProblem` model at each algorithm test point. This will trigger one orbit propagation with
some test values for the orbit state and the parameters (for example biases from the measurements modifiers parameters
or drag coefficients from the force models parameters). During the propagation, the Orekit step handler mechanism is used to
collect the state and its Jacobians at measurements dates. A `MeasurementHandler` class performs the binding between the
generic step handling mechanism and the orbit determination framework. At each measurement date, it gets the state
and Jacobians from the propagator side, calls the measurement methods to get the residuals and the partial
derivatives on the measurements side, and fetches the least squares estimator with the combined values, to be
provided back to the Hipparchus least squares solver, thus closing the loop.

The `SequentialBatchLSEstimator` class creates an implementation of a sequential batch least squares estimator engine
to perform an orbit determination based on existing least square results (i.e., state and covariance). When an orbit
determination has already been estimated and new measurements are given, it is not efficient to re-optimize the whole problem.
Only considering the new measures while optimizing will neither give good results as the old measurements will not be
taken into account. Thus, a sequential estimator is used to estimate the orbit, which uses the old results of the estimation
and the new measurements. The `SequentialBatchLSEstimator` class can also be used in satellite operations when operators
want to use the estimated covariance and state vector of an orbit determination performed the day before to improve
the results of the daily orbit determination.

The two batch least squares estimator implementations in Orekit are compatible to work with a `NumericalPropagator`, a
`DSSTPropagator`, a `TLEPropagator`, an `EcksteinHechlerPropagator`, a `BrouwerLyddanePropagator`,
or a `KeplerianPropagator`.

### Kalman filter

![kalman filter overview class diagram](../images/design/kalman-overview-class-diagram.png)

The `sequential` package provides an implementation of a Kalman estimator engine to perform an orbit
determination. Users will typically create one instance of this object (using the builder), and feed the
filter one measurement at a time, each time getting a corrected state.

One important difference with the batch least squares estimator is that a Kalman filter needs an initial
covariance matrix (which can be computed from a previous orbit determination, possibly from a batch least
squares for the first estimation) and a process noise matrix, which represent the expected noise induced
by the propagation itself between the previous and the current measurement.

In simple cases, or if the user has no clue about what to use, a simple provider is available in Orekit that
uses only constant matrices. This is however not suitable for the process noise as its depends on both
previous and current states and most importantly on the time gap between them. If two measurements are only
a few seconds apart, the noise introduced by the estimation step should be far smaller than the noise
introduced by an estimation step between measurements separated by a few hours.

A simplified model expanding an error ellipsoid with principal axes in the along-track, across-track and
normal directions and using polynomials lengths for the ellipsoid axes is expected to be included in the
library. Finding the right coefficients for the polynomials will remain the responsibility of user. One
way to tune it properly for a family of orbits is to use a first run of Taylor algebra during the mission
analysis phase, to monitor how the high order uncertainties evolve, and to fit an ellipsoid on this evolution.
Then a Kalman filter using a covariance matrix provider with such a polynomial ellipsoid model
will be more realistic than a basic constant matrix.

For even more accurate representations, users are free to set up their own models, which could go up to
evaluating the effect of each force models. This is done by providing a custom implementation of
`CovarianceMatrixProvider`.

Because the DSST orbit propagator uses large step size to perform the numerical integration
of the equations of motion for the mean equinoctial elements (e.g., half-day for GEO satellites), it
is not suitable for a classical Extended Kalman Filter orbit determination. The classical Kalman filter
algorithm needs to re-initialize the orbital state at each observation epoch. However, the time difference
between two observations is usually much smaller than the DSST step size. In order to take advantage of the
DSST theory within a recursive filter orbit determination, Orekit implements the Extended Semi-analytical Kalman Filter.

![semi-analytical kalman filter overview class diagram](../images/design/extended-semi-analytical-kalman-filter-diagram.png)

It is also possible to perform orbit determination using an Unscented Kalman Filter.
The Unscented Kalman Filter should generate a better description of the state-error and measurement-error
distribution functions relative to the Extended Kalman Filter when those error-distributions
are non-Gaussian. It uses a set of sample points, also called sigma points, to represent the uncertainty
distribution of the state. Therefore, the Unscented Kalman Filter is an interesting algorithm to solve nonlinear systems.

![unscented kalman filter overview class diagram](../images/design/unscented-kalman-filter-diagram.png)

Like for the Extended Kalman Filter, Orekit implements a semi-analytical version of the Unscented Kalman Filter
named Unscented Semi-analytical Kalman Filter

![unscented semi-analytical kalman filter overview class diagram](../images/design/unscented-semi-analytical-kalman-filter-diagram.png)

### Estimated parameters

Users can decide what they want to estimate. The 6 orbital parameters are typically always estimated and are selected
by default, but it is possible to fix some or all of these parameters. Users can also estimate some propagator parameters
(like drag coefficient or radiation pressure coefficients) and measurements parameters (like biases, stations position
offsets or Earth Orientation Parameters). One use case for estimating only a subset of the orbital parameters is when
observations are very scarce (say the first few measurements on a newly detected debris or asteroid). One use case for
not estimating any orbital parameters at all is when calibrating measurements biases from a reference orbit considered
to be perfect. Selecting which parameters should be estimated and which parameters should remain fixed is done thanks
to the `ParameterDriver` class. During setup, the user can retrieve three different `ParametersDriversList` from the
`BatchLSEstimator`:

* one list containing the 6 orbital parameters, which are estimated by default
* one list containing the propagator parameters, which depends on the force models used and
  are not estimated by default
* one list containing the measurements parameters, which are not estimated by default

Then, looping on the elements of these lists, the user can change the default settings depending on his/her needs
and for example fix a few orbital parameters while estimating a few propagation and measurements parameters.

### Ephemeris based estimation

Since Orekit 11.3 it is possible to perform ephemeris based estimation. The purpose of the ephemeris based estimation
is to estimate only the measurement parameters. The dynamical parameter (i.e., orbital and propagator parameters) are
not estimated. A typical application is the calibration of a telescope clock bias using precise GNSS ephemeris computed
by analysis centers.

To perform the ephemeris based estimation, the user must provide to the estimator an `EphemerisPropagatorBuilder`
containing the ephemeris. Orekit automatically disable the estimation of the dynamical parameters.

#### Parameters values changes

Once everything has been set up, the `estimate` method of `BatchLSEstimator` is called. The least squares solver will
then modify the values of the parameters that have been flagged as selected (and hence should be estimated). The
estimator does not know the meaning of any of the parameters, they appear all the same for it. Under the hood,
each parameter was in fact created by an object which knows what the parameter means, like for example an object
involved in the drag computation. This object uses the observer design pattern to monitor each change attempted by
the optimization algorithm, and it will adapt its computation according to the last change performed. This design
improves the decoupling between the upper layer managing the batch least square estimation and the lower layer to
which force models or biases belong. It therefore allows user to add their own parameters if they create specific
force models, specific measurements or specific measurements modifiers. All they need to do is provide some
`ParameterDriver` instances and implement the `ParameterObserver` interface to monitor when the estimator will
change these new parameters.

![orbit determination parameters class diagram](../images/design/orbit-determination-parameters-class-diagram.png)

The class diagram above depicts the parameter update mechanism for the case of ground station position offset. The
`Range` and `RangeRate` measurements classes refer to a `GroundStation` instance (one instance shared by all
measurements using this station) that provides to the upper layer 3 parameters representing the East, North and
Zenith offset for station position. If some station position offset is flagged to be estimated, the `BatchLSEstimator`
will change its value at each new evaluation, without knowing what this change really involves underneath. As the
parameters values are changed, the ground station will be notified of the change thanks to the `ParameterObserver`
it did register to the parameters, and it moves its associated `TopocentricFrame` according to the updated offsets.
The `Range` and `RangeRate` measurements theoretical values will therefore be computed naturally using the updated
station position. Orbital parameters, propagation parameters and measurements parameters are all handled the same
way.

#### Parameters normalization

Parameters normalization is used to present a more balanced vector to the least squares algorithm. Without normalization,
the vector component corresponding to the semi-major axis would have an order of magnitude of a few millions whereas
the vector component corresponding to the eccentricity would be 10 orders of magnitude smaller (assuming user decided
to estimate an orbit in Keplerian parameters set). If central attraction coefficient were estimated, the discrepancy
between the largest and smallest component could even reach 20 orders of magnitudes. Least squares optimizers do not
handle such vectors properly. In order to cope with this problem, the mathematical least squares algorithm only sees
normalized values for parameters, while the physical models see real values for the same parameters. The normalized
value is always computed as:

    normalized = (physical - reference) / scale

The reference value and the scale are fixed. The scale is related to the expected excursion around reference that
can be expected in a typical problem. It is not really important to have it precisely computed as the goal is only
to avoid huge orders of magnitudes. Any scale that allows the normalized value to be somewhere between 1/1000 and 1000
is good enough. For this reason, the scale is hard-coded for each parameter. In order to increase numerical stability,
the hard-coded values are powers of 2 so sequences of multiplications and divisions when converting between normalized
and physical values do not introduce computation errors. As an example, the scale factor for drag coefficient has
been set to 2⁻³ whereas the scale factor for central attraction coefficient has been set to 2⁺³².

#### Parameters bounds

Some parameters values are forbidden and should not be used by the least squares estimator. Unfortunately, as of
mid 2017 the Hipparchus library does not support simple bounds constraints for these algorithms. There is
however a workaround with parameters validator. Orekit uses this workaround and set up a validator for the full
set of parameters. This validator checks the test values provided by the least squares solver are within the
parameters bounds, and if not it simply forces them at boundary, effectively clipping the values. Just like
scaling factors, the minimum and maximum bounds are currently hard-coded in the library. The limits have been
set to quite loose value, as they are only meant to prevent computation failures (like negative eccentricities
or semi-major axes). If anyway the least squares algorithm tries such extreme values, there is probably a
problem with either the measurements or the propagator configuration.

