/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cs.examples.propagation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.drag.atmosphere.Atmosphere;
import org.orekit.forces.drag.atmosphere.HarrisPriester;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import fr.cs.examples.KeyValueFileParser;

/** Orekit tutorial for semi-analytical extrapolation using the DSST.
 *  <p>
 *  The parameters are read from the input file dsst-propagation.in located in the user's
 *  home directory (see commented example at src/tutorial/ressources/dsst-propagation.in).
 *  The results are written to the ouput file dsst-propagation.out in the same directory.
 *  </p>
 *  <p>
 *  Comparison between the DSST propagator and the numerical propagator can be optionally
 *  performed. Numerical results are  written to the ouput file numerical-propagation.out.
 *  </p>
 *
 *  @author Romain Di Costanzo
 *  @author Pascal Parraud
 */
public class DSSTPropagation {

    /** Program entry point.
     * @param args program arguments
     */
    public static void main(String[] args) {
        try {

            // configure Orekit data access
            String className = "/" + DSSTPropagation.class.getName().replaceAll("\\.", "/") + ".class";
            File f = new File(DSSTPropagation.class.getResource(className).toURI().getPath());
            File resourcesDir = null;
            while (resourcesDir == null || !resourcesDir.exists()) {
                f = f.getParentFile();
                if (f == null) {
                    System.err.println("cannot find resources directory");
                }
                resourcesDir = new File(new File(new File(new File(f, "src"), "tutorials"), "resources"), "tutorial-orekit-data");
            }
            DataProvidersManager.getInstance().addProvider(new DirectoryCrawler(resourcesDir));

            // input/output (in user's home directory)
            File input  = new File(new File(System.getProperty("user.home")), "dsst-propagation.in");
            File output = new File(input.getParentFile(), "dsst-propagation.out");

            new DSSTPropagation().run(input, output);

        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            System.exit(1);
        } catch (IllegalArgumentException iae) {
            System.err.println(iae.getLocalizedMessage());
            System.exit(1);
        } catch (OrekitException oe) {
            System.err.println(oe.getLocalizedMessage());
            System.exit(1);
        } catch (ParseException pe) {
            System.err.println(pe.getLocalizedMessage());
            System.exit(1);
        } catch (URISyntaxException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }

    /** Input parameter keys. */
    private static enum ParameterKey {
        BODY_FRAME,
        INERTIAL_FRAME,
        ORBIT_DATE,
        ORBIT_CIRCULAR_A,
        ORBIT_CIRCULAR_EX,
        ORBIT_CIRCULAR_EY,
        ORBIT_CIRCULAR_I,
        ORBIT_CIRCULAR_RAAN,
        ORBIT_CIRCULAR_ALPHA,
        ORBIT_EQUINOCTIAL_A,
        ORBIT_EQUINOCTIAL_EX,
        ORBIT_EQUINOCTIAL_EY,
        ORBIT_EQUINOCTIAL_HX,
        ORBIT_EQUINOCTIAL_HY,
        ORBIT_EQUINOCTIAL_LAMBDA,
        ORBIT_KEPLERIAN_A,
        ORBIT_KEPLERIAN_E,
        ORBIT_KEPLERIAN_I,
        ORBIT_KEPLERIAN_PA,
        ORBIT_KEPLERIAN_RAAN,
        ORBIT_KEPLERIAN_ANOMALY,
        ORBIT_ANGLE_TYPE,
        ORBIT_CARTESIAN_PX,
        ORBIT_CARTESIAN_PY,
        ORBIT_CARTESIAN_PZ,
        ORBIT_CARTESIAN_VX,
        ORBIT_CARTESIAN_VY,
        ORBIT_CARTESIAN_VZ,
        INITIAL_ORBIT_IS_OSCULATING,
        OUTPUT_ORBIT_IS_OSCULATING,
        START_DATE,
        DURATION,
        DURATION_IN_DAYS,
        OUTPUT_STEP,
        FIXED_INTEGRATION_STEP,
        MIN_VARIABLE_INTEGRATION_STEP,
        MAX_VARIABLE_INTEGRATION_STEP,
        POSITION_TOLERANCE_VARIABLE_INTEGRATION_STEP,
        FIXED_NUMBER_OF_INTERPOLATION_POINTS,
        MAX_TIME_GAP_BETWEEN_INTERPOLATION_POINTS,
        NUMERICAL_COMPARISON,
        CENTRAL_BODY_ROTATION_RATE,
        CENTRAL_BODY_ORDER,
        CENTRAL_BODY_DEGREE,
        MAX_DEGREE_ZONAL_SHORT_PERIODS,
        MAX_ECCENTRICITY_POWER_ZONAL_SHORT_PERIODS,
        MAX_FREQUENCY_TRUE_LONGITUDE_ZONAL_SHORT_PERIODS,
        MAX_DEGREE_TESSERAL_SHORT_PERIODS,
        MAX_ORDER_TESSERAL_SHORT_PERIODS,
        MAX_ECCENTRICITY_POWER_TESSERAL_SHORT_PERIODS,
        MAX_FREQUENCY_MEAN_LONGITUDE_TESSERAL_SHORT_PERIODS,
        MAX_DEGREE_TESSERAL_M_DAILIES_SHORT_PERIODS,
        MAX_ORDER_TESSERAL_M_DAILIES_SHORT_PERIODS,
        MAX_ECCENTRICITY_POWER_TESSERAL_M_DAILIES_SHORT_PERIODS,
        THIRD_BODY_MOON,
        THIRD_BODY_SUN,
        MASS,
        DRAG,
        DRAG_CD,
        DRAG_SF,
        SOLAR_RADIATION_PRESSURE,
        SOLAR_RADIATION_PRESSURE_CR,
        SOLAR_RADIATION_PRESSURE_SF,
        OUTPUT_KEPLERIAN,
        OUTPUT_EQUINOCTIAL,
        OUTPUT_CARTESIAN,
        OUTPUT_SHORT_PERIOD_COEFFICIENTS;
    }

    private void run(final File input, final File output)
            throws IOException, IllegalArgumentException, OrekitException, ParseException {

        // read input parameters
        KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        try (final FileInputStream fis = new FileInputStream(input)) {
            parser.parseInput(input.getAbsolutePath(), fis);
        }

        // check mandatory input parameters
        if (!parser.containsKey(ParameterKey.ORBIT_DATE)) {
            throw new IOException("Orbit date is not defined.");
        }
        if (!parser.containsKey(ParameterKey.DURATION) && !parser.containsKey(ParameterKey.DURATION_IN_DAYS)) {
            throw new IOException("Propagation duration is not defined.");
        }

        // All dates in UTC
        final TimeScale utc = TimeScalesFactory.getUTC();

        final double rotationRate;
        if (!parser.containsKey(ParameterKey.CENTRAL_BODY_ROTATION_RATE)) {
            rotationRate = Constants.WGS84_EARTH_ANGULAR_VELOCITY;
        } else {
            rotationRate = parser.getDouble(ParameterKey.CENTRAL_BODY_ROTATION_RATE);
        }
        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));

        // Potential coefficients providers
        final UnnormalizedSphericalHarmonicsProvider unnormalized =
                GravityFieldFactory.getConstantUnnormalizedProvider(degree, order);
        final NormalizedSphericalHarmonicsProvider normalized =
                GravityFieldFactory.getConstantNormalizedProvider(degree, order);

        // Central body attraction coefficient (m³/s²)
        final double mu = unnormalized.getMu();

        // Earth frame definition
        final Frame earthFrame;
        if (!parser.containsKey(ParameterKey.BODY_FRAME)) {
            earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        } else {
            earthFrame = parser.getEarthFrame(ParameterKey.BODY_FRAME);
        }

        // Orbit definition
        final Orbit orbit = createOrbit(parser, utc, mu);

        // DSST propagator definition
        double mass = 1000.0;
        if (parser.containsKey(ParameterKey.MASS)) {
            mass = parser.getDouble(ParameterKey.MASS);
        }
        boolean initialIsOsculating = false;
        if (parser.containsKey(ParameterKey.INITIAL_ORBIT_IS_OSCULATING)) {
            initialIsOsculating = parser.getBoolean(ParameterKey.INITIAL_ORBIT_IS_OSCULATING);
        }
        boolean outputIsOsculating = initialIsOsculating;
        if (parser.containsKey(ParameterKey.OUTPUT_ORBIT_IS_OSCULATING)) {
            outputIsOsculating = parser.getBoolean(ParameterKey.OUTPUT_ORBIT_IS_OSCULATING);
        }
        List<String> shortPeriodCoefficients = null;
        if (parser.containsKey(ParameterKey.OUTPUT_SHORT_PERIOD_COEFFICIENTS)) {
            shortPeriodCoefficients = parser.getStringsList(ParameterKey.OUTPUT_SHORT_PERIOD_COEFFICIENTS, ',');
            if (shortPeriodCoefficients.size() == 1 && shortPeriodCoefficients.get(0).equalsIgnoreCase("all")) {
                // special case, we use the empty list to represent all possible (unknown) keys
                // we don't use Collections.emptyList() because we want the list to be populated later on
                shortPeriodCoefficients = new ArrayList<String>();
            } else if (shortPeriodCoefficients.size() == 1 && shortPeriodCoefficients.get(0).equalsIgnoreCase("none")) {
                // special case, we use null to select no coefficients at all
                shortPeriodCoefficients = null;
            } else {
                // general case, we have an explicit list of coefficients names
                Collections.sort(shortPeriodCoefficients);
            }
            if (shortPeriodCoefficients != null && !outputIsOsculating) {
                System.out.println("\nWARNING:");
                System.out.println("Short periodic coefficients can be output only if output orbit is osculating.");
                System.out.println("No coefficients will be computed here.\n");
            }
        }
        double fixedStepSize = -1.;
        double minStep       =  6000.0;
        double maxStep       = 86400.0;
        double dP            =     1.0;
        if (parser.containsKey(ParameterKey.FIXED_INTEGRATION_STEP)) {
            fixedStepSize = parser.getDouble(ParameterKey.FIXED_INTEGRATION_STEP);
        } else {
            if (parser.containsKey(ParameterKey.MIN_VARIABLE_INTEGRATION_STEP)) {
                minStep = parser.getDouble(ParameterKey.MIN_VARIABLE_INTEGRATION_STEP);
            }
            if (parser.containsKey(ParameterKey.MAX_VARIABLE_INTEGRATION_STEP)) {
                maxStep = parser.getDouble(ParameterKey.MAX_VARIABLE_INTEGRATION_STEP);
            }
            if (parser.containsKey(ParameterKey.POSITION_TOLERANCE_VARIABLE_INTEGRATION_STEP)) {
                dP = parser.getDouble(ParameterKey.POSITION_TOLERANCE_VARIABLE_INTEGRATION_STEP);
            }
        }
        final DSSTPropagator dsstProp = createDSSTProp(orbit, mass,
                                                       initialIsOsculating, outputIsOsculating,
                                                       fixedStepSize, minStep, maxStep, dP,
                                                       shortPeriodCoefficients);

        if (parser.containsKey(ParameterKey.FIXED_NUMBER_OF_INTERPOLATION_POINTS)) {
            if (parser.containsKey(ParameterKey.MAX_TIME_GAP_BETWEEN_INTERPOLATION_POINTS)) {
                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          "cannot specify both fixed.number.of.interpolation.points" +
                                          " and max.time.gap.between.interpolation.points");
            }
            dsstProp.setInterpolationGridToFixedNumberOfPoints(parser.getInt(ParameterKey.FIXED_NUMBER_OF_INTERPOLATION_POINTS));
        } else if (parser.containsKey(ParameterKey.MAX_TIME_GAP_BETWEEN_INTERPOLATION_POINTS)) {
            dsstProp.setInterpolationGridToMaxTimeGap(parser.getDouble(ParameterKey.MAX_TIME_GAP_BETWEEN_INTERPOLATION_POINTS));
        } else {
            dsstProp.setInterpolationGridToFixedNumberOfPoints(3);
        }

        // Set Force models
        setForceModel(parser, unnormalized, earthFrame, rotationRate, dsstProp);

        // Simulation properties
        AbsoluteDate start;
        if (parser.containsKey(ParameterKey.START_DATE)) {
            start = parser.getDate(ParameterKey.START_DATE, utc);
        } else {
            start = parser.getDate(ParameterKey.ORBIT_DATE, utc);
        }
        double duration = 0.;
        if (parser.containsKey(ParameterKey.DURATION)) {
            duration = parser.getDouble(ParameterKey.DURATION);
        }
        if (parser.containsKey(ParameterKey.DURATION_IN_DAYS)) {
            duration = parser.getDouble(ParameterKey.DURATION_IN_DAYS) * Constants.JULIAN_DAY;
        }
        double outStep = parser.getDouble(ParameterKey.OUTPUT_STEP);
        boolean displayKeplerian = true;
        if (parser.containsKey(ParameterKey.OUTPUT_KEPLERIAN)) {
            displayKeplerian = parser.getBoolean(ParameterKey.OUTPUT_KEPLERIAN);
        }
        boolean displayEquinoctial = true;
        if (parser.containsKey(ParameterKey.OUTPUT_EQUINOCTIAL)) {
            displayEquinoctial = parser.getBoolean(ParameterKey.OUTPUT_EQUINOCTIAL);
        }
        boolean displayCartesian = true;
        if (parser.containsKey(ParameterKey.OUTPUT_CARTESIAN)) {
            displayCartesian = parser.getBoolean(ParameterKey.OUTPUT_CARTESIAN);
        }

        // DSST Propagation
        dsstProp.setEphemerisMode();
        final double dsstOn = System.currentTimeMillis();
        dsstProp.propagate(start, start.shiftedBy(duration));
        final double dsstOff = System.currentTimeMillis();
        System.out.println("DSST execution time (without large file write) : " + (dsstOff - dsstOn) / 1000.);
        System.out.println("writing file...");
        final BoundedPropagator dsstEphem = dsstProp.getGeneratedEphemeris();
        dsstEphem.setMasterMode(outStep, new OutputHandler(output,
                                                           displayKeplerian, displayEquinoctial, displayCartesian,
                                                           shortPeriodCoefficients));
        dsstEphem.propagate(start, start.shiftedBy(duration));
        System.out.println("DSST results saved as file " + output);

        // Check if we want to compare numerical to DSST propagator (default is false)
        if (parser.containsKey(ParameterKey.NUMERICAL_COMPARISON)
                && parser.getBoolean(ParameterKey.NUMERICAL_COMPARISON)) {

            if ( !outputIsOsculating ) {
                System.out.println("\nWARNING:");
                System.out.println("The DSST propagator considers a mean orbit while the numerical will consider an osculating one.");
                System.out.println("The comparison will be meaningless.\n");
            }

            // Numerical propagator definition
            final NumericalPropagator numProp = createNumProp(orbit, mass);

            // Set Force models
            setForceModel(parser, normalized, earthFrame, numProp);

            // Numerical Propagation without output
            numProp.setEphemerisMode();
            final double numOn = System.currentTimeMillis();
            numProp.propagate(start, start.shiftedBy(duration));
            final double numOff = System.currentTimeMillis();
            System.out.println("Numerical execution time (including output): " + (numOff - numOn) / 1000.);

            // Add output
            final BoundedPropagator numEphemeris = numProp.getGeneratedEphemeris();
            File numOutput = new File(input.getParentFile(), "numerical-propagation.out");
            numEphemeris.setMasterMode(outStep, new OutputHandler(numOutput,
                                                                  displayKeplerian, displayEquinoctial, displayCartesian,
                                                                  null));
            System.out.println("Writing file, this may take some time ...");
            numEphemeris.propagate(numEphemeris.getMaxDate());
            System.out.println("Numerical results saved as file " + numOutput);

        }

    }

    /** Create an orbit from input parameters
     * @param parser input file parser
     * @param scale  time scale
     * @param mu     central attraction coefficient
     * @throws OrekitException if inertial frame cannot be retrieved
     * @throws NoSuchElementException if input parameters are missing
     * @throws IOException if input parameters are invalid
     */
    private Orbit createOrbit(final KeyValueFileParser<ParameterKey> parser,
                              final TimeScale scale, final double mu)
        throws OrekitException, NoSuchElementException, IOException {

        final Frame frame;
        if (!parser.containsKey(ParameterKey.INERTIAL_FRAME)) {
            frame = FramesFactory.getEME2000();
        } else {
            frame = parser.getInertialFrame(ParameterKey.INERTIAL_FRAME);
        }

        // Orbit definition
        Orbit orbit;
        PositionAngle angleType = PositionAngle.MEAN;
        if (parser.containsKey(ParameterKey.ORBIT_ANGLE_TYPE)) {
            angleType = PositionAngle.valueOf(parser.getString(ParameterKey.ORBIT_ANGLE_TYPE).toUpperCase());
        }
        if (parser.containsKey(ParameterKey.ORBIT_KEPLERIAN_A)) {
            orbit = new KeplerianOrbit(parser.getDouble(ParameterKey.ORBIT_KEPLERIAN_A) * 1000.,
                                       parser.getDouble(ParameterKey.ORBIT_KEPLERIAN_E),
                                       parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_I),
                                       parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_PA),
                                       parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_RAAN),
                                       parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_ANOMALY),
                                       angleType,
                                       frame,
                                       parser.getDate(ParameterKey.ORBIT_DATE, scale),
                                       mu
                                      );
        } else if (parser.containsKey(ParameterKey.ORBIT_EQUINOCTIAL_A)) {
            orbit = new EquinoctialOrbit(parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_A) * 1000.,
                                         parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_EX),
                                         parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_EY),
                                         parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_HX),
                                         parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_HY),
                                         parser.getAngle(ParameterKey.ORBIT_EQUINOCTIAL_LAMBDA),
                                         angleType,
                                         frame,
                                         parser.getDate(ParameterKey.ORBIT_DATE, scale),
                                         mu
                                        );
        } else if (parser.containsKey(ParameterKey.ORBIT_CIRCULAR_A)) {
            orbit = new CircularOrbit(parser.getDouble(ParameterKey.ORBIT_CIRCULAR_A) * 1000.,
                                      parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EX),
                                      parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EY),
                                      parser.getAngle(ParameterKey.ORBIT_CIRCULAR_I),
                                      parser.getAngle(ParameterKey.ORBIT_CIRCULAR_RAAN),
                                      parser.getAngle(ParameterKey.ORBIT_CIRCULAR_ALPHA),
                                      angleType,
                                      frame,
                                      parser.getDate(ParameterKey.ORBIT_DATE, scale),
                                      mu
                                     );
        } else if (parser.containsKey(ParameterKey.ORBIT_CARTESIAN_PX)) {
            final double[] pos = {parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PX) * 1000.,
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PY) * 1000.,
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PZ) * 1000.};
            final double[] vel = {parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VX) * 1000.,
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VY) * 1000.,
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VZ) * 1000.};
            orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(pos), new Vector3D(vel)),
                                       frame,
                                       parser.getDate(ParameterKey.ORBIT_DATE, scale),
                                       mu
                                      );
        } else {
            throw new IOException("Orbit definition is incomplete.");
        }

        return orbit;

    }

    /** Set up the DSST Propagator
     *
     *  @param orbit initial orbit
     *  @param mass S/C mass (kg)
     *  @param initialIsOsculating if initial orbital elements are osculating
     *  @param outputIsOsculating if we want to output osculating parameters
     *  @param fixedStepSize step size for fixed step integrator (s)
     *  @param minStep minimum step size, if step is not fixed (s)
     *  @param maxStep maximum step size, if step is not fixed (s)
     *  @param dP position tolerance for step size control, if step is not fixed (m)
     *  @param shortPeriodCoefficients list of short periodic coefficients
     *  to output (null means no coefficients at all, empty list means all
     *  possible coefficients)
     *  @throws OrekitException
     */
    private DSSTPropagator createDSSTProp(final Orbit orbit,
                                          final double mass,
                                          final boolean initialIsOsculating,
                                          final boolean outputIsOsculating,
                                          final double fixedStepSize,
                                          final double minStep,
                                          final double maxStep,
                                          final double dP,
                                          final List<String> shortPeriodCoefficients)
        throws OrekitException {
        AbstractIntegrator integrator;
        if (fixedStepSize > 0.) {
            integrator = new ClassicalRungeKuttaIntegrator(fixedStepSize);
        } else {
            final double[][] tol = DSSTPropagator.tolerances(dP, orbit);
            integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
            ((AdaptiveStepsizeIntegrator) integrator).setInitialStepSize(10. * minStep);
        }

        DSSTPropagator dsstProp = new DSSTPropagator(integrator, !outputIsOsculating);
        dsstProp.setInitialState(new SpacecraftState(orbit, mass), initialIsOsculating);
        dsstProp.setSelectedCoefficients(shortPeriodCoefficients == null ?
                                         null : new HashSet<String>(shortPeriodCoefficients));

        return dsstProp;
    }

    /** Create the numerical propagator
     *
     *  @param orbit initial orbit
     *  @param mass S/C mass (kg)
     *  @throws OrekitException
     */
    private NumericalPropagator createNumProp(final Orbit orbit, final double mass) throws OrekitException {
        final double[][] tol = NumericalPropagator.tolerances(1.0, orbit, orbit.getType());
        final double minStep = 1.e-3;
        final double maxStep = 1.e+3;
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(100.);

        NumericalPropagator numProp = new NumericalPropagator(integrator);
        numProp.setInitialState(new SpacecraftState(orbit, mass));

        return numProp;
    }

    /** Set DSST propagator force models
     *
     *  @param parser input file parser
     *  @param unnormalized spherical harmonics provider
     *  @param earthFrame Earth rotating frame
     *  @param rotationRate central body rotation rate (rad/s)
     *  @param dsstProp DSST propagator
     *  @throws IOException
     *  @throws OrekitException
     */
    private void setForceModel(final KeyValueFileParser<ParameterKey> parser,
                               final UnnormalizedSphericalHarmonicsProvider unnormalized,
                               final Frame earthFrame, final double rotationRate,
                               final DSSTPropagator dsstProp) throws IOException, OrekitException {

        final double ae = unnormalized.getAe();

        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = parser.getInt(ParameterKey.CENTRAL_BODY_ORDER);

        if (order > degree) {
            throw new IOException("Potential order cannot be higher than potential degree");
        }

        // Central Body Force Model with un-normalized coefficients
        dsstProp.addForceModel(new DSSTZonal(unnormalized,
                                             parser.getInt(ParameterKey.MAX_DEGREE_ZONAL_SHORT_PERIODS),
                                             parser.getInt(ParameterKey.MAX_ECCENTRICITY_POWER_ZONAL_SHORT_PERIODS),
                                             parser.getInt(ParameterKey.MAX_FREQUENCY_TRUE_LONGITUDE_ZONAL_SHORT_PERIODS)));
        dsstProp.addForceModel(new DSSTTesseral(earthFrame, rotationRate, unnormalized,
                                                parser.getInt(ParameterKey.MAX_DEGREE_TESSERAL_SHORT_PERIODS),
                                                parser.getInt(ParameterKey.MAX_ORDER_TESSERAL_SHORT_PERIODS),
                                                parser.getInt(ParameterKey.MAX_ECCENTRICITY_POWER_TESSERAL_SHORT_PERIODS),
                                                parser.getInt(ParameterKey.MAX_FREQUENCY_MEAN_LONGITUDE_TESSERAL_SHORT_PERIODS),
                                                parser.getInt(ParameterKey.MAX_DEGREE_TESSERAL_M_DAILIES_SHORT_PERIODS),
                                                parser.getInt(ParameterKey.MAX_ORDER_TESSERAL_M_DAILIES_SHORT_PERIODS),
                                                parser.getInt(ParameterKey.MAX_ECCENTRICITY_POWER_TESSERAL_M_DAILIES_SHORT_PERIODS)));

        // 3rd body (SUN)
        if (parser.containsKey(ParameterKey.THIRD_BODY_SUN) && parser.getBoolean(ParameterKey.THIRD_BODY_SUN)) {
            dsstProp.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getSun()));
        }

        // 3rd body (MOON)
        if (parser.containsKey(ParameterKey.THIRD_BODY_MOON) && parser.getBoolean(ParameterKey.THIRD_BODY_MOON)) {
            dsstProp.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getMoon()));
        }

        // Drag
        if (parser.containsKey(ParameterKey.DRAG) && parser.getBoolean(ParameterKey.DRAG)) {
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, Constants.WGS84_EARTH_FLATTENING, earthFrame);
            final Atmosphere atm = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);
            dsstProp.addForceModel(new DSSTAtmosphericDrag(atm,
                                                           parser.getDouble(ParameterKey.DRAG_CD),
                                                           parser.getDouble(ParameterKey.DRAG_SF)));
        }

        // Solar Radiation Pressure
        if (parser.containsKey(ParameterKey.SOLAR_RADIATION_PRESSURE) && parser.getBoolean(ParameterKey.SOLAR_RADIATION_PRESSURE)) {
            dsstProp.addForceModel(new DSSTSolarRadiationPressure(parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_CR),
                                                                  parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_SF),
                                                                  CelestialBodyFactory.getSun(), ae));
        }
    }

    /** Set numerical propagator force models
     *
     *  @param parser  input file parser
     *  @param normalized spherical harmonics provider
     *  @param earthFrame Earth rotating frame
     *  @param numProp numerical propagator
     *  @throws IOException
     *  @throws OrekitException
     */
    private void setForceModel(final KeyValueFileParser<ParameterKey> parser,
                               final NormalizedSphericalHarmonicsProvider normalized,
                               final Frame earthFrame,
                               final NumericalPropagator numProp) throws IOException, OrekitException {

        final double ae = normalized.getAe();

        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = parser.getInt(ParameterKey.CENTRAL_BODY_ORDER);

        if (order > degree) {
            throw new IOException("Potential order cannot be higher than potential degree");
        }

        // Central Body (normalized coefficients)
        numProp.addForceModel(new HolmesFeatherstoneAttractionModel(earthFrame, normalized));

        // 3rd body (SUN)
        if (parser.containsKey(ParameterKey.THIRD_BODY_SUN) && parser.getBoolean(ParameterKey.THIRD_BODY_SUN)) {
            numProp.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        }

        // 3rd body (MOON)
        if (parser.containsKey(ParameterKey.THIRD_BODY_MOON) && parser.getBoolean(ParameterKey.THIRD_BODY_MOON)) {
            numProp.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
        }

        // Drag
        if (parser.containsKey(ParameterKey.DRAG) && parser.getBoolean(ParameterKey.DRAG)) {
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, Constants.WGS84_EARTH_FLATTENING, earthFrame);
            final Atmosphere atm = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);
            final DragSensitive ssc = new IsotropicDrag(parser.getDouble(ParameterKey.DRAG_SF),
                                                        parser.getDouble(ParameterKey.DRAG_CD));
            numProp.addForceModel(new DragForce(atm, ssc));
        }

        // Solar Radiation Pressure
        if (parser.containsKey(ParameterKey.SOLAR_RADIATION_PRESSURE) && parser.getBoolean(ParameterKey.SOLAR_RADIATION_PRESSURE)) {
            final double cR = parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_CR);
            final RadiationSensitive ssc = new IsotropicRadiationSingleCoefficient(parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_SF),
                                                                                   cR);
            numProp.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(), ae, ssc));
        }
    }

    /** Specialized step handler catching the state at each step. */
    private static class OutputHandler implements OrekitFixedStepHandler {

        /** Format for delta T. */
        private static final String DT_FORMAT                             = "%20.9f";

        /** Format for Keplerian elements. */
        private static final String KEPLERIAN_ELEMENTS_FORMAT             = " %23.16e %23.16e %23.16e %23.16e %23.16e %23.16e";

        /** Format for equinoctial elements. */
        private static final String EQUINOCTIAL_ELEMENTS_WITHOUT_A_FORMAT = " %23.16e %23.16e %23.16e %23.16e %23.16e";

        /** Format for equinoctial elements. */
        private static final String EQUINOCTIAL_ELEMENTS_WITH_A_FORMAT    = " %23.16e" + EQUINOCTIAL_ELEMENTS_WITHOUT_A_FORMAT;

        /** Format for Cartesian elements. */
        private static final String CARTESIAN_ELEMENTS_FORMAT             = " %23.16e %23.16e %23.16e %23.16e %23.16e %23.16e";

        /** Format for short period coefficients. */
        private static final String SHORT_PERIOD_COEFFICIENTS_FORMAT      = " %23.16e %23.16e %23.16e %23.16e %23.16e %23.16e";

        /** Output file. */
        private final File outputFile;

        /** Indicator for Keplerian elements output. */
        private final boolean outputKeplerian;

        /** Indicator for equinoctial elements output. */
        private final boolean outputEquinoctial;

        /** Indicator for Cartesian elements output. */
        private final boolean outputCartesian;

        /** Start date of propagation. */
        private AbsoluteDate start;

        /** Indicator for first step. */
        private boolean isFirst;

        /** Stream for output. */
        private PrintStream outputStream;

        /** Number of columns already declared in the header. */
        private int nbColumns;

        /** Sorted list of short period coefficients to display. */
        private List<String> shortPeriodCoefficients;

        /** Simple constructor.
         * @param outputFile output file
         * @param outputKeplerian if true, the file should contain Keplerian elements
         * @param outputEquinoctial if true, the file should contain equinoctial elements
         * @param displayCaresian if true, the file should contain Cartesian elements
         *  @param shortPeriodCoefficients list of short periodic coefficients
         *  to output (null means no coefficients at all, empty list means all
         *  possible coefficients)
         */
        private OutputHandler(final File outputFile,
                              final boolean outputKeplerian, final boolean outputEquinoctial,
                              final boolean outputCartesian, final List<String> shortPeriodCoefficients) {
            this.outputFile              = outputFile;
            this.outputKeplerian         = outputKeplerian;
            this.outputEquinoctial       = outputEquinoctial;
            this.outputCartesian         = outputCartesian;
            this.shortPeriodCoefficients = shortPeriodCoefficients;
            this.isFirst                 = true;
        }

        /** {@inheritDoc} */
        public void init(final SpacecraftState s0, final AbsoluteDate t, final double step)
            throws OrekitException {
            try {
                nbColumns           = 0;
                outputStream        = new PrintStream(outputFile, "UTF-8");
                describeNextColumn("time from start (s)");
                if (outputKeplerian) {
                    describeNextColumn("semi major axis a (km)");
                    describeNextColumn("eccentricity e");
                    describeNextColumn("inclination i (deg)");
                    describeNextColumn("right ascension of ascending node raan (deg)");
                    describeNextColumn("perigee argument (deg)");
                    describeNextColumn("mean anomaly M (deg)");
                }
                if (outputEquinoctial) {
                    if (!outputKeplerian) {
                        describeNextColumn("semi major axis a (km)");
                    }
                    describeNextColumn("eccentricity vector component ey/h");
                    describeNextColumn("eccentricity vector component ex/k");
                    describeNextColumn("inclination vector component hy/p");
                    describeNextColumn("inclination vector component hx/q");
                    describeNextColumn("mean longitude argument L (deg)");
                }
                if (outputCartesian) {
                    describeNextColumn("position along X (km)");
                    describeNextColumn("position along Y (km)");
                    describeNextColumn("position along Z (km)");
                    describeNextColumn("velocity along X (km/s)");
                    describeNextColumn("velocity along Y (km/s)");
                    describeNextColumn("velocity along Z (km/s)");
                }
                start   = s0.getDate();
                isFirst = true;
            } catch (IOException ioe) {
                throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
            }
        }

        /** Describe next column.
         * @param description column description
         */
        private void describeNextColumn(final String description) {
            outputStream.format("# %3d %s%n", ++nbColumns, description);
        }

        /** {@inheritDoc} */
        public void handleStep(SpacecraftState s, boolean isLast) throws OrekitException {
            if (isFirst) {
                if (shortPeriodCoefficients != null) {
                    if (shortPeriodCoefficients.isEmpty()) {
                        // we want all available coefficients,
                        // they correspond to the additional states
                        for (final Map.Entry<String, double[]> entry : s.getAdditionalStates().entrySet()) {
                            shortPeriodCoefficients.add(entry.getKey());
                        }
                        Collections.sort(shortPeriodCoefficients);
                    }
                    for (final String coefficientName : shortPeriodCoefficients) {
                        describeNextColumn(coefficientName + " (a)");
                        describeNextColumn(coefficientName + " (h)");
                        describeNextColumn(coefficientName + " (k)");
                        describeNextColumn(coefficientName + " (p)");
                        describeNextColumn(coefficientName + " (q)");
                        describeNextColumn(coefficientName + " (L)");
                    }
                }
                isFirst = false;
            }
            outputStream.format(Locale.US, DT_FORMAT, s.getDate().durationFrom(start));
            if (outputKeplerian) {
                final KeplerianOrbit ko = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(s.getOrbit());
                outputStream.format(Locale.US, KEPLERIAN_ELEMENTS_FORMAT,
                                    ko.getA() / 1000.,
                                    ko.getE(),
                                    FastMath.toDegrees(ko.getI()),
                                    FastMath.toDegrees(MathUtils.normalizeAngle(ko.getRightAscensionOfAscendingNode(), FastMath.PI)),
                                    FastMath.toDegrees(MathUtils.normalizeAngle(ko.getPerigeeArgument(), FastMath.PI)),
                                    FastMath.toDegrees(MathUtils.normalizeAngle(ko.getAnomaly(PositionAngle.MEAN), FastMath.PI)));
                if (outputEquinoctial) {
                    outputStream.format(Locale.US, EQUINOCTIAL_ELEMENTS_WITHOUT_A_FORMAT,
                                        ko.getEquinoctialEy(), // h
                                        ko.getEquinoctialEx(), // k
                                        ko.getHy(),            // p
                                        ko.getHx(),            // q
                                        FastMath.toDegrees(MathUtils.normalizeAngle(ko.getLM(), FastMath.PI)));
                }
            } else if (outputEquinoctial) {
                outputStream.format(Locale.US, EQUINOCTIAL_ELEMENTS_WITH_A_FORMAT,
                                    s.getOrbit().getA(),
                                    s.getOrbit().getEquinoctialEy(), // h
                                    s.getOrbit().getEquinoctialEx(), // k
                                    s.getOrbit().getHy(),            // p
                                    s.getOrbit().getHx(),            // q
                                    FastMath.toDegrees(MathUtils.normalizeAngle(s.getOrbit().getLM(), FastMath.PI)));
            }
            if (outputCartesian) {
                final PVCoordinates pv = s.getPVCoordinates();
                outputStream.format(Locale.US, CARTESIAN_ELEMENTS_FORMAT,
                                    pv.getPosition().getX() * 0.001,
                                    pv.getPosition().getY() * 0.001,
                                    pv.getPosition().getZ() * 0.001,
                                    pv.getVelocity().getX() * 0.001,
                                    pv.getVelocity().getY() * 0.001,
                                    pv.getVelocity().getZ() * 0.001);
            }
            if (shortPeriodCoefficients != null) {
                for (final String coefficientName : shortPeriodCoefficients) {
                    final double[] coefficient = s.getAdditionalState(coefficientName);
                    outputStream.format(Locale.US, SHORT_PERIOD_COEFFICIENTS_FORMAT,
                                        coefficient[0],
                                        coefficient[2], // beware, it is really 2 (ey/h), not 1 (ex/k)
                                        coefficient[1], // beware, it is really 1 (ex/k), not 2 (ey/h)
                                        coefficient[4], // beware, it is really 4 (hy/p), not 3 (hx/q)
                                        coefficient[3], // beware, it is really 3 (hx/q), not 4 (hy/p)
                                        coefficient[5]);
                }
            }
            outputStream.format(Locale.US, "%n");
            if (isLast) {
                outputStream.close();
                outputStream = null;
            }
        }

    }

}
