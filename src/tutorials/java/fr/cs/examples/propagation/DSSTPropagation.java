/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.HarrisPriester;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTCentralBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
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

            // configure Orekit data acces
            Utils.setDataRoot("tutorial-orekit-data");

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
        }
    }

    /** Input parameter keys. */
    private static enum ParameterKey {
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
        ORBIT_IS_OSCULATING,
        START_DATE,
        DURATION,
        DURATION_IN_DAYS,
        OUTPUT_STEP,
        FIXED_INTEGRATION_STEP,
        NUMERICAL_COMPARISON,
        CENTRAL_BODY_ORDER,
        CENTRAL_BODY_DEGREE,
        THIRD_BODY_MOON,
        THIRD_BODY_SUN,
        DRAG,
        DRAG_CD,
        DRAG_SF,
        SOLAR_RADIATION_PRESSURE,
        SOLAR_RADIATION_PRESSURE_CR,
        SOLAR_RADIATION_PRESSURE_SF;
    }

    private void run(final File input, final File output)
            throws IOException, IllegalArgumentException, OrekitException, ParseException {

        // read input parameters
        KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        parser.parseInput(new FileInputStream(input));

        // check mandatory input parameters
        if (!parser.containsKey(ParameterKey.ORBIT_DATE)) {
            throw new IOException("Orbit date is not defined.");
        }
        if (!parser.containsKey(ParameterKey.DURATION) && !parser.containsKey(ParameterKey.DURATION_IN_DAYS)) {
            throw new IOException("Propagation duration is not defined.");
        }

        // All dates in UTC
        final TimeScale utc = TimeScalesFactory.getUTC();
        
        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));

        // Potential coefficients providers
        final UnnormalizedSphericalHarmonicsProvider unnormalized =
                GravityFieldFactory.getUnnormalizedProvider(degree, order);
        final NormalizedSphericalHarmonicsProvider normalized =
                GravityFieldFactory.getNormalizedProvider(degree, order);

        // Central body attraction coefficient (m³/s²)
        final double mu = unnormalized.getMu();

        // Earth frame definition (for faster computation)
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        // Orbit definition (inertial frame is EME2000)
        final Orbit orbit = createOrbit(parser, FramesFactory.getEME2000(), utc, mu);

        // DSST propagator definition
        Boolean isOsculating = false;
        if (parser.containsKey(ParameterKey.ORBIT_IS_OSCULATING)) {
            isOsculating = parser.getBoolean(ParameterKey.ORBIT_IS_OSCULATING);
        }
        double fixedStepSize = -1.;
        if (parser.containsKey(ParameterKey.FIXED_INTEGRATION_STEP)) {
            fixedStepSize = parser.getDouble(ParameterKey.FIXED_INTEGRATION_STEP);
        }
        final DSSTPropagator dsstProp = createDSSTProp(orbit, isOsculating, fixedStepSize);

        // Set Force models
        setForceModel(parser, unnormalized, earthFrame, dsstProp);

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

        // Add orbit handler
        OrbitHandler dsstHandler = new OrbitHandler();
        dsstProp.setMasterMode(outStep, dsstHandler);

        // DSST Propagation
        final double dsstOn = System.currentTimeMillis();
        dsstProp.propagate(start, start.shiftedBy(duration));
        final double dsstOff = System.currentTimeMillis();
        System.out.println("DSST execution time: " + (dsstOff - dsstOn) / 1000.);
        
        // Print results
        printOutput(output, dsstHandler, start);
        System.out.println("DSST results saved as file " + output);

        // Check if we want to compare numerical to DSST propagator (default is false)
        if (parser.containsKey(ParameterKey.NUMERICAL_COMPARISON)
                && parser.getBoolean(ParameterKey.NUMERICAL_COMPARISON)) {
            
            if ( !isOsculating ) {
                System.out.println("\nWARNING:");
                System.out.println("The DSST propagator considers a mean orbit while the numerical will consider an osculating one.");
                System.out.println("The comparison will be meaningless.\n");
            }

            // output (in user's home directory)
            File output_num = new File(input.getParentFile(), "numerical-propagation.out");

            // Numerical propagator definition
            final NumericalPropagator numProp = createNumProp(orbit);
            
            // Set Force models
            setForceModel(parser, normalized, earthFrame, numProp);

            // Add orbit handler
            OrbitHandler numHandler = new OrbitHandler();
            numProp.setMasterMode(outStep, numHandler);

            // Numerical Propagation
            final double numOn = System.currentTimeMillis();
            numProp.propagate(start, start.shiftedBy(duration));
            final double numOff = System.currentTimeMillis();
            System.out.println("Numerical execution time: " + (numOff - numOn) / 1000.);
            
            // Print results
            printOutput(output_num, numHandler, start);
            System.out.println("Numerical results saved as file " + output_num);
        }

    }

    /** Create an orbit from input parameters
     * @param parser input file parser
     * @param frame  inertial frame
     * @param scale  time scale
     * @param mu     central attraction coefficient
     * @throws NoSuchElementException if input parameters are mising
     * @throws IOException if input parameters are invalid
     */
    private Orbit createOrbit(final KeyValueFileParser<ParameterKey> parser,
                              final Frame frame, final TimeScale scale, final double mu)
        throws NoSuchElementException, IOException {

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
     *  @param orbit
     *  @param isOsculating if orbital elements are osculating
     *  @param fixedStepSize step size for fixed step integrator (s)
     *  @throws OrekitException
     */
    private DSSTPropagator createDSSTProp(final Orbit orbit, final boolean isOsculating,
                                          final double fixedStepSize) throws OrekitException {

        FirstOrderIntegrator integrator;
        if (fixedStepSize > 0.) {
            integrator = new ClassicalRungeKuttaIntegrator(fixedStepSize);
        } else {
            final double minStep = orbit.getKeplerianPeriod();
            final double maxStep = minStep * 100.;
            final double[][] tol = DSSTPropagator.tolerances(1.0, orbit);
            integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
            ((AdaptiveStepsizeIntegrator) integrator).setInitialStepSize(10. * minStep);
        }

        DSSTPropagator dsstProp = new DSSTPropagator(integrator);
        dsstProp.setInitialState(new SpacecraftState(orbit), isOsculating);

        return dsstProp;
    }

    /** Create the numerical propagator
     *
     *  @param orbit initial orbit
     *  @throws OrekitException 
     */
    private NumericalPropagator createNumProp(final Orbit orbit) throws OrekitException {
        final double[][] tol = NumericalPropagator.tolerances(1.0, orbit, orbit.getType());
        final double minStep = 1.e-3;
        final double maxStep = 1.e+3;
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(100.);

        NumericalPropagator numProp = new NumericalPropagator(integrator);
        numProp.setInitialState(new SpacecraftState(orbit));

        return numProp;
    }

    /** Set DSST propagator force models
     *
     *  @param parser input file parser
     *  @param unnormalized spherical harmonics provider
     *  @param earthFrame Earth rotating frame
     *  @param dsstProp DSST propagator
     *  @throws IOException
     *  @throws OrekitException
     */
    private void setForceModel(final KeyValueFileParser<ParameterKey> parser,
                               final UnnormalizedSphericalHarmonicsProvider unnormalized,
                               final Frame earthFrame,
                               final DSSTPropagator dsstProp) throws IOException, OrekitException {

        final double ae = unnormalized.getAe();
        
        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = parser.getInt(ParameterKey.CENTRAL_BODY_ORDER);

        if (order > degree) {
            throw new IOException("Potential order cannot be higher than potential degree");
        }

        // Central Body Force Model with un-normalized coefficients
        dsstProp.addForceModel(new DSSTCentralBody(earthFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, unnormalized));

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
        numProp.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF2005(),
                                                                    normalized));

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
            final SphericalSpacecraft ssc = new SphericalSpacecraft(parser.getDouble(ParameterKey.DRAG_SF),
                                                                    parser.getDouble(ParameterKey.DRAG_CD),
                                                                    0., 0.);
            numProp.addForceModel(new DragForce(atm, ssc));
        }

        // Solar Radiation Pressure
        if (parser.containsKey(ParameterKey.SOLAR_RADIATION_PRESSURE) && parser.getBoolean(ParameterKey.SOLAR_RADIATION_PRESSURE)) {
            final double cR = parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_CR);
            // cR being the DSST SRP coef and assuming a spherical spacecraft, the conversion is:
            // cR = 1 + (1 - kA) * (1 - kR) * 4 / 9
            // with kA arbitrary sets to 0
            final double kR = 3.25 - 2.25 * cR;
            final SphericalSpacecraft ssc = new SphericalSpacecraft(parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_SF),
                                                                    0., 0., kR);
            numProp.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(), ae, ssc));
        }
    }

    /** Print the results in the output file
     *
     *  @param handler orbit handler
     *  @param output output file
     *  @param sart start date of propagation
     *  @throws OrekitException 
     *  @throws IOException 
     */
    private void printOutput(final File output,
                             final OrbitHandler handler,
                             final AbsoluteDate start) throws OrekitException, IOException {
        // Output format:
        // time_from_start, a, e, i, raan, pa, aM, h, k, p, q, lM, px, py, pz, vx, vy, vz
        final String format = new String(" %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e");
        final BufferedWriter buffer = new BufferedWriter(new FileWriter(output));
        buffer.write("##   time_from_start(s)            a(km)                      e                      i(deg)         ");
        buffer.write("         raan(deg)                pa(deg)              mean_anomaly(deg)              ey/h          ");
        buffer.write("           ex/k                    hy/p                     hx/q             mean_longitude_arg(deg)");
        buffer.write("       Xposition(km)           Yposition(km)             Zposition(km)           Xvelocity(km/s)    ");
        buffer.write("      Yvelocity(km/s)         Zvelocity(km/s)");
        buffer.newLine();
        for (Orbit o : handler.getOrbits()) {
            final Formatter f = new Formatter(new StringBuilder(), Locale.ENGLISH);
            // Time from start (s)
            final double time = o.getDate().durationFrom(start);
            // Semi-major axis (km)
            final double a = o.getA() / 1000.;
            // Keplerian elements
            // Eccentricity
            final double e = o.getE();
            // Inclination (degrees)
            final double i = Math.toDegrees(MathUtils.normalizeAngle(o.getI(), FastMath.PI));
            // Right Ascension of Ascending Node (degrees)
            KeplerianOrbit ko = new KeplerianOrbit(o);
            final double ra = Math.toDegrees(MathUtils.normalizeAngle(ko.getRightAscensionOfAscendingNode(), FastMath.PI));
            // Perigee Argument (degrees)
            final double pa = Math.toDegrees(MathUtils.normalizeAngle(ko.getPerigeeArgument(), FastMath.PI));
            // Mean Anomaly (degrees)
            final double am = Math.toDegrees(MathUtils.normalizeAngle(ko.getAnomaly(PositionAngle.MEAN), FastMath.PI));
            // Equinoctial elements
            // ey/h component of eccentricity vector
            final double h = o.getEquinoctialEy();
            // ex/k component of eccentricity vector
            final double k = o.getEquinoctialEx();
            // hy/p component of inclination vector
            final double p = o.getHy();
            // hx/q component of inclination vector
            final double q = o.getHx();
            // Mean Longitude Argument (degrees)
            final double lm = Math.toDegrees(MathUtils.normalizeAngle(o.getLM(), FastMath.PI));
            // Cartesian elements
            // Position along X in inertial frame (km)
            final double px = o.getPVCoordinates().getPosition().getX() / 1000.;
            // Position along Y in inertial frame (km)
            final double py = o.getPVCoordinates().getPosition().getY() / 1000.;
            // Position along Z in inertial frame (km)
            final double pz = o.getPVCoordinates().getPosition().getZ() / 1000.;
            // Velocity along X in inertial frame (km/s)
            final double vx = o.getPVCoordinates().getVelocity().getX() / 1000.;
            // Velocity along Y in inertial frame (km/s)
            final double vy = o.getPVCoordinates().getVelocity().getY() / 1000.;
            // Velocity along Z in inertial frame (km/s)
            final double vz = o.getPVCoordinates().getVelocity().getZ() / 1000.;
            buffer.write(f.format(format, time, a, e, i, ra, pa, am, h, k, p, q, lm, px, py, pz, vx, vy, vz).toString());
            buffer.newLine();
            f.close();
        }
        buffer.close();
    }

    /** Specialized step handler catching the orbit at each step. */
    private static class OrbitHandler implements OrekitFixedStepHandler {

        /** List of orbits. */
        private final List<Orbit> orbits;

        private OrbitHandler() {
            // initialise an empty list of orbit
            orbits = new ArrayList<Orbit>();
        }

        /** {@inheritDoc} */
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        /** {@inheritDoc} */
        public void handleStep(SpacecraftState currentState, boolean isLast) {
            // fill in the list with the orbit from the current step
            orbits.add(currentState.getOrbit());
        }

        /** Get the list of propagated orbits.
         * @return orbits
         */
        public List<Orbit> getOrbits() {
            return orbits;
        }
    }

}
