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

# Propagation

The next 4 tutorials detail some elementary usages of the propagation package
described in the [propagation section](../architecture/propagation.html) of
the library architecture documentation.

## Propagation modes

Three different operational modes are available for all propagators.
They are mutually exclusive.

### Slave mode

This is the default mode. It doesn't need to be explicitly set, but it can be to recover from any other mode.
  
This tutorial shows how to propagate from an initial state to a target date.

In this case, the calling application coordinates all the tasks, the propagator just propagates.
  
Let's define the EME2000 inertial frame as reference frame:
  
    Frame inertialFrame = FramesFactory.getEME2000();
  
Let's set up an initial state as:

    TimeScale utc = TimeScalesFactory.getUTC();
    AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);
    
    double mu =  3.986004415e+14;
    
    double a = 24396159;                 // semi major axis in meters
    double e = 0.72831215;               // eccentricity
    double i = Math.toRadians(7);        // inclination
    double omega = Math.toRadians(180);  // perigee argument
    double raan = Math.toRadians(261);   // right ascension of ascending node
    double lM = 0;                       // mean anomaly

The initial orbit is defined as a `KeplerianOrbit`.
More details on the orbit representation can be found
in the [orbits section](../architecture/orbits.html)
of the library architecture documentation.

    Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                            inertialFrame, initialDate, mu);

We choose to use a very simple `KeplerianPropagator` to compute basic keplerian motion.
It could be any of the available propagators.

    KeplerianPropagator kepler = new KeplerianPropagator(initialOrbit);

Let's set the propagator to slave mode for the purpose of this tutorial,
but keep in mind that it can be omitted here as it is the default mode.

    kepler.setSlaveMode();

Finally, the propagation features of duration and step size are defined and
a propagation loop is performed in order to print the results at each step:

    double duration = 600.;
    AbsoluteDate finalDate = initialDate.shiftedBy(duration);
    double stepT = 60.;
    int cpt = 1;
    for (AbsoluteDate extrapDate = initialDate;
         extrapDate.compareTo(finalDate) <= 0;
         extrapDate = extrapDate.shiftedBy(stepT))  {
        SpacecraftState currentState = kepler.propagate(extrapDate);
        System.out.println("step " + cpt++);
        System.out.println(" time : " + currentState.getDate());
        System.out.println(" " + currentState.getOrbit());
    }

The printed results are shown below

    step 1
     time : 2004-01-01T23:30:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 0.0;}
    step 2
     time : 2004-01-01T23:31:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 5.281383633573694;}
    step 3
     time : 2004-01-01T23:32:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 10.525261309411585;}
    step 4
     time : 2004-01-01T23:33:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 15.695876839823306;}
    step 5
     time : 2004-01-01T23:34:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 20.76077035517381;}
    step 6
     time : 2004-01-01T23:35:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 25.691961427063767;}
    step 7
     time : 2004-01-01T23:36:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 30.466680460539763;}
    step 8
     time : 2004-01-01T23:37:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 35.06763907945756;}
    step 9
     time : 2004-01-01T23:38:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 39.48289615024968;}
    step 10
     time : 2004-01-01T23:39:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 43.70541689946282;}
    step 11
     time : 2004-01-01T23:40:00.000
     keplerian parameters: {a: 2.4396159E7; e: 0.72831215; i: 7.0; pa: 180.0; raan: 261.0; v: 47.732436294590705;}

The complete code for this example can be found in the source tree of the library,
in file `src/tutorials/fr/cs/examples/propagation/SlaveMode.java`.

### Master mode

In this mode, the propagator, as a master, calls some callback methods
provided by the application throughout its internal integration loop.

As in the slave mode tutorial above, let's define some initial state with:

    // Inertial frame
    Frame inertialFrame = FramesFactory.getEME2000();
    // Initial date
    TimeScale utc = TimeScalesFactory.getUTC();
    AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000,utc);
    // Central attraction coefficient
    double mu =  3.986004415e+14;
    // Initial orbit
    double a = 24396159;                    // semi major axis in meters
    double e = 0.72831215;                  // eccentricity
    double i = FastMath.toRadians(7);       // inclination
    double omega = FastMath.toRadians(180); // perigee argument
    double raan = FastMath.toRadians(261);  // right ascention of ascending node
    double lM = 0;                          // mean anomaly
    Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN, 
                                            inertialFrame, initialDate, mu);
    // Initial state definition
    SpacecraftState initialState = new SpacecraftState(initialOrbit);

Here we use a more sophisticated `NumericalPropagator` based on an adaptive
step integrator provided by the underlying Hipparchus library,
but it doesn't matter which integrator is used.

    // Adaptive step integrator
    // with a minimum step of 0.001 and a maximum step of 1000
    double minStep = 0.001;
    double maxstep = 1000.0;
    double positionTolerance = 10.0;
    OrbitType propagationType = OrbitType.KEPLERIAN;
    double[][] tolerances =
        NumericalPropagator.tolerances(positionTolerance, initialOrbit, propagationType);
    AdaptiveStepsizeIntegrator integrator =
        new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

We set up the integrator, and force it to use Keplerian parameters for propagation.

    NumericalPropagator propagator = new NumericalPropagator(integrator);
    propagator.setOrbitType(propagationType);

A force model, reduced here to a single perturbing gravity field, is taken into account.\
More details on force models can be found
in the [forces section](../architecture/forces.html)
of the library architecture documentation.

    NormalizedSphericalHarmonicsProvider provider =
        GravityFieldFactory.getNormalizedProvider(10, 10);
    ForceModel holmesFeatherstone =
        new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                    true),
                                              provider)

This force model is simply added to the propagator:

    propagator.addForceModel(holmesFeatherstone);

The propagator operating mode is set to master mode with fixed step and a
`TutorialStepHandler` which implements the interface `OrekitFixedStepHandler`
in order to fulfill the `handleStep` method to be called within the loop.
For the purpose of this tutorial, the `handleStep` method will just print
the current state at the moment.

    propagator.setMasterMode(60., new TutorialStepHandler());

Then, the initial state is set for the propagator:

    propagator.setInitialState(initialState);

Finally, the propagator is just asked to propagate, from the initial state, for a given duration.

    SpacecraftState finalState =
        propagator.propagate(new AbsoluteDate(initialDate, 630.));

Clearly, with a few lines of code, the main application delegates to the propagator
the care of handling regular outputs through a variable step integration loop.

All that is needed is to derive some class from the interface `OrekitFixedStepHandler`
to realize a `handleStep` method, as follows:

    private static class TutorialStepHandler implements OrekitFixedStepHandler {
    
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            System.out.println("          date                a           e" +
                               "           i         \u03c9          \u03a9" +
                               "          \u03bd");
        }
    
        public void handleStep(SpacecraftState currentState, boolean isLast) {
            KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
            System.out.format(Locale.US, "%s %12.3f %10.8f %10.6f %10.6f %10.6f %10.6f%n",
                              currentState.getDate(),
                              o.getA(), o.getE(),
                              FastMath.toDegrees(o.getI()),
                              FastMath.toDegrees(o.getPerigeeArgument()),
                              FastMath.toDegrees(o.getRightAscensionOfAscendingNode()),
                              FastMath.toDegrees(o.getTrueAnomaly()));
            if (isLast) {
                System.out.println("this was the last step ");
                System.out.println();
            }
        }
    
    }

The same result, with the slave mode, would have required much more programming.

The printed results are shown below:

              date                a           e           i         ω          Ω          ν
    2004-01-01T23:30:00.000 24396159.000 0.72831215   7.000000 180.000000 261.000000   0.000000
    2004-01-01T23:31:00.000 24395672.948 0.72830573   6.999927 180.010992 260.999966   5.270439
    2004-01-01T23:32:00.000 24394149.110 0.72828580   6.999758 180.021885 260.999767  10.503710
    2004-01-01T23:33:00.000 24391676.555 0.72825352   6.999506 180.032557 260.999281  15.664370
    2004-01-01T23:34:00.000 24388396.501 0.72821073   6.999190 180.042889 260.998422  20.720220
    2004-01-01T23:35:00.000 24384487.471 0.72815976   6.998827 180.052785 260.997139  25.643463
    2004-01-01T23:36:00.000 24380142.510 0.72810308   6.998434 180.062182 260.995416  30.411433
    2004-01-01T23:37:00.000 24375546.779 0.72804310   6.998029 180.071043 260.993268  35.006866
    2004-01-01T23:38:00.000 24370861.668 0.72798191   6.997624 180.079354 260.990738  39.417786
    2004-01-01T23:39:00.000 24366216.959 0.72792119   6.997234 180.087112 260.987893  43.637076
    2004-01-01T23:40:00.000 24361709.735 0.72786221   6.996868 180.094320 260.984809  47.661865
    this was the last step 

    Final state:
    2004-01-01T23:40:30.000 24359529.816 0.72783366   6.996697 180.097720 260.983203  49.601407

The complete code for this example can be found in the source tree of the library,
in file `src/tutorials/fr/cs/examples/propagation/MasterMode.java`.

### Ephemeris mode

This third mode may be used when the user needs random access to the orbit state
at any time between some initial and final dates.

As in the two tutorials above, let's first define some initial state as:

    // Inertial frame
    Frame inertialFrame = FramesFactory.getEME2000();
    // Initial date
    TimeScale utc = TimeScalesFactory.getUTC();
    AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000,utc);
    // Central attraction coefficient
    double mu =  3.986004415e+14;
    // Initial orbit
    double a = 24396159;                    // semi major axis in meters
    double e = 0.72831215;                  // eccentricity
    double i = FastMath.toRadians(7);       // inclination
    double omega = FastMath.toRadians(180); // perigee argument
    double raan = FastMath.toRadians(261);  // right ascention of ascending node
    double lM = 0;                          // mean anomaly
    Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN, 
                                            inertialFrame, initialDate, mu);
    // Initial state definition
    SpacecraftState initialState = new SpacecraftState(initialOrbit);

Here we use a simple `NumericalPropagator`, without perturbation,
based on a classical fixed step Runge-Kutta integrator provided
by the underlying Hipparchus library.

    double stepSize = 10;
    FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(stepSize);
    NumericalPropagator propagator = new NumericalPropagator(integrator);

The initial state is set for this propagator:

    propagator.setInitialState(initialState);

Then, the propagator operating mode is simply set to ephemeris mode:

    propagator.setEphemerisMode();

And the propagation is performed for a given duration.

    SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(6000));

This `finalState` can be used for anything, to be printed, for example as shown below:

     Numerical propagation :
      Final date : 2004-01-02T01:10:00.000
      equinoctial parameters: {a: 2.4396159E7;
                               ex: 0.11393312156755062; ey: 0.719345418868777;
                               hx: -0.009567941763699867; hy: -0.06040960680288257;
                               lv: 583.1250344407331;}

Throughout the propagation, intermediate states are stored
within an ephemeris which can now be recovered with a single call:
  
    BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
    System.out.println(" Ephemeris defined from " + ephemeris.getMinDate() +
                       " to " + ephemeris.getMaxDate());

The ephemeris is defined as a `BoundedPropagator`, which means that it is valid
only between the propagation initial and final dates. The code above give the
following result:

    Ephemeris defined from 2004-01-01T23:30:00.000 to 2004-01-02T01:10:00.000

Between these dates, the ephemeris can be used as any propagator to propagate
the orbital state towards any intermediate date with a single call:
  
    SpacecraftState intermediateState = ephemeris.propagate(intermediateDate);

Here are results obtained with intermediate dates set to 3000 seconds after
start date and to exactly the final date:

    Ephemeris propagation :
     date :  2004-01-02T00:20:00.000
     equinoctial parameters: {a: 2.4396159E7;
                              ex: 0.11393312156755062; ey: 0.719345418868777;
                              hx: -0.009567941763699867; hy: -0.06040960680288257;
                              lv: 559.0092657655282;}
     date :  2004-01-02T01:10:00.000
     equinoctial parameters: {a: 2.4396159E7;
                              ex: 0.11393312156755062; ey: 0.719345418868777;
                              hx: -0.009567941763699867; hy: -0.06040960680288257;
                              lv: 583.1250344407331;}

The following shows the error message we get when we try to use a date outside
of the ephemeris range (in this case, the intermediate date was set to 1000 seconds
before ephemeris start:

    out of range date for ephemerides: 2004-01-01T23:13:20.000

The complete code for this example can be found in the source tree of the library,
in file `src/tutorials/fr/cs/examples/propagation/EphemerisMode.java`.

## Events management

This tutorial aims to demonstrate the power and simplicity of the event-handling mechanism.
  
One needs to check the visibility between a satellite and a ground station during some
time range.
  
We will use, and extend, the predefined `ElevationDetector` to perform the task.
  
First, let's set up an initial state for the satellite defined by a position
and a velocity at one date in some inertial frame.

    Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
    Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
    PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
    AbsoluteDate initialDate =
        new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
    Frame inertialFrame = FramesFactory.getEME2000();

We also need to set the central attraction coefficient
to define the initial orbit as a `KeplerianOrbit`.

    double mu =  3.986004415e+14;
    Orbit initialOrbit =
        new KeplerianOrbit(pvCoordinates, inertialFrame, initialDate, mu);

More details on the orbit representation can be found
in the [orbits section](../architecture/orbits.html)
of the library architecture documentation.

As a propagator, we consider a `KeplerianPropagator` to compute the simple keplerian motion.
It could be more elaborate without modifying the general purpose of this tutorial.

    Propagator kepler = new KeplerianPropagator(initialOrbit);

Then, let's define the ground station by its coordinates as a `GeodeticPoint`:

    double longitude = FastMath.toRadians(45.);
    double latitude  = FastMath.toRadians(25.);
    double altitude  = 0.;
    GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);

And let's associate to it a `TopocentricFrame` related to a `BodyShape` in some terrestrial frame.

    Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
    BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           Constants.WGS84_EARTH_FLATTENING,
                                           earthFrame);
    TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");

More details on `BodyShape` and `GeodeticPoint` can be found
in the [bodies section](../architecture/bodies.html)
of the library architecture documentation.\
More details on `TopocentricFrame` can be found
in the [frames section](../architecture/frames.html)
of the library architecture documentation.

An `EventDetector` is defined as a an
`ElevationDetector` with constant limit elevation and a dedicated
handler.

    double maxcheck  = 60.0;
    double threshold =  0.001;
    double elevation = FastMath.toRadians(5.);
    EventDetector sta1Visi =
      new ElevationDetector(maxcheck, threshold, sta1Frame).
      withConstantElevation(elevation).
      withHandler(new VisibilityHandler());

This `EventDetector` is added to the propagator:

    kepler.addEventDetector(sta1Visi);

Finally, the propagator is simply asked to perform until some final date,
in slave mode by default.

It will propagate from the initial date to the first raising or for the fixed duration
according to the behavior implemented in the `VisibilityHandler` class.

    SpacecraftState finalState =
        kepler.propagate(new AbsoluteDate(initialDate, 1500.));
    System.out.println(" Final state : " + finalState.getDate().durationFrom(initialDate));

The main application code is very simple, all the work is done inside the propagator
thanks to the `VisibilityHandler` class especially created for the purpose.

This class implements the `EventHandler` interface and is registered as the handler
to be called by the `ElevationDetector` class already provided by OREKIT. It defines
the behaviour to have at events occurrences, here printing the results of the visibility
check, both the raising and the setting time, and to stop propagation after the setting
event.

    private static class VisibilityHandler implements EventHandler<ElevationDetector> {
    
        public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
                                    final boolean increasing) {
            if (increasing) {
                System.out.println(" Visibility on " + detector.getTopocentricFrame().getName()
                                                     + " begins at " + s.getDate());
                return Action.CONTINUE;
            } else {
                System.out.println(" Visibility on " + detector.getTopocentricFrame().getName()
                                                     + " ends at " + s.getDate());
                return Action.STOP;
                }
        }
    
        public SpacecraftState resetState(final ElevationDetector detector, final SpacecraftState oldState) {
            return oldState;
        }
    
    }

The printed result is shown below.
We can see that the propagation stopped just after detecting the setting:

    Visibility on station1 begins at 2004-01-01T23:31:52.098
    Visibility on station1 ends at 2004-01-01T23:42:48.850
    Final state : 768.850266549196

The complete code for this example can be found in the source tree of the library,
in file `src/tutorials/fr/cs/examples/propagation/VisibilityCheck.java`.
