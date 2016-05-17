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

package fr.cs.examples.bodies;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.solvers.BaseUnivariateSolver;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolverUtils;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.SecularAndHarmonic;

import fr.cs.examples.Autoconfiguration;
import fr.cs.examples.KeyValueFileParser;

/** Orekit tutorial for setting up a Sun-synchronous Earth-phased Low Earth Orbit.
 * @author Luc Maisonobe
 */
public class Phasing {

    /** GMST function. */
    private final TimeFunction<DerivativeStructure> gmst;

    /** Gravity field. */
    private NormalizedSphericalHarmonicsProvider gravityField;

    /** Earth model. */
    private final BodyShape earth;

    /** Program entry point.
     * @param args program arguments
     */
    public static void main(String[] args) {
        try {

            if (args.length != 1) {
                System.err.println("usage: java fr.cs.examples.bodies.Phasing filename");
                System.exit(1);
            }

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // input/out
            URL url = Phasing.class.getResource("/" + args[0]);
            if (url == null) {
                System.err.println(args[0] + " not found");
                System.exit(1);
            }
            File input  = new File(url.toURI().getPath());

            new Phasing().run(input);

        } catch (URISyntaxException use) {
            System.err.println(use.getLocalizedMessage());
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            System.exit(1);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.err);
            System.err.println(iae.getLocalizedMessage());
            System.exit(1);
        } catch (ParseException pe) {
            System.err.println(pe.getLocalizedMessage());
            System.exit(1);
        } catch (OrekitException oe) {
            oe.printStackTrace(System.err);
            System.err.println(oe.getLocalizedMessage());
            System.exit(1);
        }
    }

    /** Input parameter keys. */
    private static enum ParameterKey {
        ORBIT_DATE,
        PHASING_ORBITS_NUMBER,
        PHASING_DAYS_NUMBER,
        SUN_SYNCHRONOUS_REFERENCE_LATITUDE,
        SUN_SYNCHRONOUS_REFERENCE_ASCENDING,
        SUN_SYNCHRONOUS_MEAN_SOLAR_TIME,
        GRAVITY_FIELD_DEGREE,
        GRAVITY_FIELD_ORDER,
        GRID_OUTPUT,
        GRID_LATITUDE_1,
        GRID_ASCENDING_1,
        GRID_LATITUDE_2,
        GRID_ASCENDING_2,
        GRID_LATITUDE_3,
        GRID_ASCENDING_3,
        GRID_LATITUDE_4,
        GRID_ASCENDING_4,
        GRID_LATITUDE_5,
        GRID_ASCENDING_5
    }

    public Phasing() throws IOException, ParseException, OrekitException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        boolean         simpleEOP   = false;
        gmst         = conventions.getGMSTFunction(TimeScalesFactory.getUT1(conventions, simpleEOP));
        earth        = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                            Constants.WGS84_EARTH_FLATTENING,
                                            FramesFactory.getGTOD(conventions, simpleEOP));
    }

    private void run(final File input)
            throws IOException, IllegalArgumentException, ParseException, OrekitException {

        // read input parameters
        KeyValueFileParser<ParameterKey> parser =
                new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        try (final FileInputStream fis = new FileInputStream(input)) {
            parser.parseInput(input.getAbsolutePath(), fis);
        }
        TimeScale utc = TimeScalesFactory.getUTC();

       // simulation properties
        AbsoluteDate date       = parser.getDate(ParameterKey.ORBIT_DATE, utc);
        int          nbOrbits   = parser.getInt(ParameterKey.PHASING_ORBITS_NUMBER);
        int          nbDays     = parser.getInt(ParameterKey.PHASING_DAYS_NUMBER);
        double       latitude   = parser.getAngle(ParameterKey.SUN_SYNCHRONOUS_REFERENCE_LATITUDE);
        boolean      ascending  = parser.getBoolean(ParameterKey.SUN_SYNCHRONOUS_REFERENCE_ASCENDING);
        double       mst        = parser.getTime(ParameterKey.SUN_SYNCHRONOUS_MEAN_SOLAR_TIME).getSecondsInUTCDay() / 3600;
        int          degree     = parser.getInt(ParameterKey.GRAVITY_FIELD_DEGREE);
        int          order      = parser.getInt(ParameterKey.GRAVITY_FIELD_ORDER);
        String       gridOutput = parser.getString(ParameterKey.GRID_OUTPUT);
        double[]     gridLatitudes = new double[] {
            parser.getAngle(ParameterKey.GRID_LATITUDE_1),
            parser.getAngle(ParameterKey.GRID_LATITUDE_2),
            parser.getAngle(ParameterKey.GRID_LATITUDE_3),
            parser.getAngle(ParameterKey.GRID_LATITUDE_4),
            parser.getAngle(ParameterKey.GRID_LATITUDE_5)
        };
        boolean[]    gridAscending = new boolean[] {
            parser.getBoolean(ParameterKey.GRID_ASCENDING_1),
            parser.getBoolean(ParameterKey.GRID_ASCENDING_2),
            parser.getBoolean(ParameterKey.GRID_ASCENDING_3),
            parser.getBoolean(ParameterKey.GRID_ASCENDING_4),
            parser.getBoolean(ParameterKey.GRID_ASCENDING_5)
        };

        gravityField = GravityFieldFactory.getNormalizedProvider(degree, order);

        // initial guess for orbit
        CircularOrbit orbit = guessOrbit(date, FramesFactory.getEME2000(), nbOrbits, nbDays,
                                         latitude, ascending, mst);
        System.out.println("initial orbit: " + orbit);
        System.out.println("please wait while orbit is adjusted...");
        System.out.println();

        // numerical model for improving orbit
        double[][] tolerances = NumericalPropagator.tolerances(0.1, orbit, OrbitType.CIRCULAR);
        DormandPrince853Integrator integrator =
                new DormandPrince853Integrator(1.0e-4 * orbit.getKeplerianPeriod(),
                                               1.0e-1 * orbit.getKeplerianPeriod(),
                                               tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(1.0e-2 * orbit.getKeplerianPeriod());
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getGTOD(IERSConventions.IERS_2010, true),
                                                                       gravityField));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));

        double deltaP = Double.POSITIVE_INFINITY;
        double deltaV = Double.POSITIVE_INFINITY;

        int counter = 0;
        DecimalFormat f = new DecimalFormat("0.000E00", new DecimalFormatSymbols(Locale.US));
        while (deltaP > 3.0e-1 || deltaV > 3.0e-4) {

            CircularOrbit previous = orbit;

            CircularOrbit tmp1 = improveEarthPhasing(previous, nbOrbits, nbDays, propagator);
            CircularOrbit tmp2 = improveSunSynchronization(tmp1, nbOrbits * tmp1.getKeplerianPeriod(),
                                              latitude, ascending, mst, propagator);
            orbit = improveFrozenEccentricity(tmp2, nbOrbits * tmp2.getKeplerianPeriod(), propagator);
            double da  = orbit.getA() - previous.getA();
            double dex = orbit.getCircularEx() - previous.getCircularEx();
            double dey = orbit.getCircularEy() - previous.getCircularEy();
            double di  = FastMath.toDegrees(orbit.getI() - previous.getI());
            double dr  = FastMath.toDegrees(orbit.getRightAscensionOfAscendingNode() -
                                           previous.getRightAscensionOfAscendingNode());
            System.out.println(" iteration " + (++counter) + ": deltaA = " + f.format(da) +
                               " m, deltaEx = " + f.format(dex) + ", deltaEy = " + f.format(dey) +
                               ", deltaI = " + f.format(di) + " deg, deltaRAAN = " + f.format(dr) +
                               " deg");

            PVCoordinates delta = new PVCoordinates(previous.getPVCoordinates(),
                                                    orbit.getPVCoordinates());
            deltaP = delta.getPosition().getNorm();
            deltaV = delta.getVelocity().getNorm();

        }

        // final orbit
        System.out.println();
        System.out.println("final orbit (osculating): " + orbit);

        // generate the ground track grid file
        try (PrintStream output = new PrintStream(new File(input.getParent(), gridOutput), "UTF-8")) {
            for (int i = 0; i < gridLatitudes.length; ++i) {
                printGridPoints(output, gridLatitudes[i], gridAscending[i], orbit, propagator, nbOrbits);
            }
        }

    }

    /** Guess an initial orbit from theoretical model.
     * @param date orbit date
     * @param frame frame to use for defining orbit
     * @param nbOrbits number of orbits in the phasing cycle
     * @param nbDays number of days in the phasing cycle
     * @param latitude reference latitude for Sun synchronous orbit
     * @param ascending if true, crossing latitude is from South to North
     * @param mst desired mean solar time at reference latitude crossing
     * @return an initial guess of Earth phased, Sun synchronous orbit
     * @exception OrekitException if mean solar time cannot be computed
     */
    private CircularOrbit guessOrbit(AbsoluteDate date, Frame frame, int nbOrbits, int nbDays,
                                     double latitude, boolean ascending, double mst)
        throws OrekitException {

        double mu = gravityField.getMu();
        NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics harmonics =
                gravityField.onDate(date);

        // initial semi major axis guess based on Keplerian period
        double period0 = (nbDays * Constants.JULIAN_DAY) / nbOrbits;
        double n0      = 2 * FastMath.PI / period0;
        double a0      = FastMath.cbrt(mu / (n0 * n0));

        // initial inclination guess based on ascending node drift due to J2
        double[][] unnormalization = GravityFieldFactory.getUnnormalizationFactors(3, 0);
        double j2       = -unnormalization[2][0] * harmonics.getNormalizedCnm(2, 0);
        double j3       = -unnormalization[3][0] * harmonics.getNormalizedCnm(3, 0);
        double raanRate = 2 * FastMath.PI / Constants.JULIAN_YEAR;
        double ae       = gravityField.getAe();
        double i0       = FastMath.acos(-raanRate * a0 * a0 / (1.5 * ae * ae * j2 * n0));

        // initial eccentricity guess based on J2 and J3
        double ex0   = 0;
        double ey0   = -j3 * ae * FastMath.sin(i0) / (2 * a0 * j2);

        // initial ascending node guess based on mean solar time
        double alpha0 = FastMath.asin(FastMath.sin(latitude) / FastMath.sin(i0));
        if (!ascending) {
            alpha0 = FastMath.PI - alpha0;
        }
        double h = meanSolarTime(new CircularOrbit(a0, ex0, ey0, i0, 0.0, alpha0,
                                                   PositionAngle.TRUE, frame, date, mu));
        double raan0 = FastMath.PI * (mst - h) / 12.0;

        return new CircularOrbit(a0, ex0, ey0, i0, raan0, alpha0,
                                 PositionAngle.TRUE, frame, date, mu);

    }

    /** Improve orbit to better match Earth phasing parameters.
     * @param previous previous orbit
     * @param nbOrbits number of orbits in the phasing cycle
     * @param nbDays number of days in the phasing cycle
     * @param propagator propagator to use
     * @return an improved Earth phased orbit
     * @exception OrekitException if orbit cannot be propagated
     */
    private CircularOrbit improveEarthPhasing(CircularOrbit previous, int nbOrbits, int nbDays,
                                              Propagator propagator)
        throws OrekitException {

        propagator.resetInitialState(new SpacecraftState(previous));

        // find first ascending node
        double period = previous.getKeplerianPeriod();
        SpacecraftState firstState = findFirstCrossing(0.0, true, previous.getDate(),
                                                       previous.getDate().shiftedBy(2 * period),
                                                       0.01 * period, propagator);

        // go to next cycle, one orbit at a time
        SpacecraftState state = firstState;
        for (int i = 0; i < nbOrbits; ++i) {
            final AbsoluteDate previousDate = state.getDate();
            state = findLatitudeCrossing(0.0, previousDate.shiftedBy(period),
                                         previousDate.shiftedBy(2 * period),
                                         0.01 * period, period, propagator);
            period = state.getDate().durationFrom(previousDate);
        }

        double cycleDuration = state.getDate().durationFrom(firstState.getDate());
        double deltaT;
        if (((int) FastMath.rint(cycleDuration / Constants.JULIAN_DAY)) != nbDays) {
            // we are very far from expected duration
            deltaT = nbDays * Constants.JULIAN_DAY - cycleDuration;
        } else {
            // we are close to expected duration
            GeodeticPoint startPoint = earth.transform(firstState.getPVCoordinates().getPosition(),
                                                       firstState.getFrame(), firstState.getDate());
            GeodeticPoint endPoint   = earth.transform(state.getPVCoordinates().getPosition(),
                                                       state.getFrame(), state.getDate());
            double deltaL =
                    MathUtils.normalizeAngle(endPoint.getLongitude() - startPoint.getLongitude(), 0.0);
            deltaT = deltaL * Constants.JULIAN_DAY / (2 * FastMath.PI);
        }

        double deltaA = 2 * previous.getA() * deltaT / (3 * nbOrbits * previous.getKeplerianPeriod());
        return new CircularOrbit(previous.getA() + deltaA,
                                 previous.getCircularEx(), previous.getCircularEy(),
                                 previous.getI(), previous.getRightAscensionOfAscendingNode(),
                                 previous.getAlphaV(), PositionAngle.TRUE,
                                 previous.getFrame(), previous.getDate(),
                                 previous.getMu());

    }

    /** Improve orbit to better match phasing parameters.
     * @param previous previous orbit
     * @param duration sampling duration
     * @param latitude reference latitude for Sun synchronous orbit
     * @param ascending if true, crossing latitude is from South to North
     * @param mst desired mean solar time at reference latitude crossing
     * @param propagator propagator to use
     * @return an improved Earth phased, Sun synchronous orbit
     * @exception OrekitException if orbit cannot be propagated
     */
    private CircularOrbit improveSunSynchronization(CircularOrbit previous, double duration,
                                                    double latitude, boolean ascending, double mst,
                                                    Propagator propagator)
        throws OrekitException {

        propagator.resetInitialState(new SpacecraftState(previous));
        AbsoluteDate start = previous.getDate();

        // find the first latitude crossing
        double period   = previous.getKeplerianPeriod();
        double stepSize = period / 100;
        SpacecraftState crossing =
                findFirstCrossing(latitude, ascending, start, start.shiftedBy(2 * period),
                                  stepSize, propagator);

        // find all other latitude crossings from regular schedule
        SecularAndHarmonic mstModel = new SecularAndHarmonic(2,
                                                             2.0 * FastMath.PI / Constants.JULIAN_YEAR,
                                                             4.0 * FastMath.PI / Constants.JULIAN_YEAR,
                                                             2.0 * FastMath.PI / Constants.JULIAN_DAY,
                                                             4.0 * FastMath.PI / Constants.JULIAN_DAY);
        mstModel.resetFitting(start, new double[] {
            mst, -1.0e-10, -1.0e-17,
            1.0e-3, 1.0e-3, 1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5
        });
        while (crossing != null && crossing.getDate().durationFrom(start) < duration) {
            final AbsoluteDate previousDate = crossing.getDate();
            crossing = findLatitudeCrossing(latitude, previousDate.shiftedBy(period),
                                            previousDate.shiftedBy(2 * period),
                                            stepSize, period / 8, propagator);
            if (crossing != null) {

                // store current point
                mstModel.addPoint(crossing.getDate(), meanSolarTime(crossing.getOrbit()));

                // use the same time separation to pinpoint next crossing
                period = crossing.getDate().durationFrom(previousDate);

            }

        }

        // fit the mean solar time to a parabolic plus medium periods model
        // we will only use the linear part for the correction
        mstModel.fit();
        final double[] fittedH = mstModel.approximateAsPolynomialOnly(1, start, 2, 2,
                                                                      start, start.shiftedBy(duration),
                                                                      stepSize);

        // solar time bias must be compensated by shifting ascending node
        double deltaRaan = FastMath.PI * (mst - fittedH[0]) / 12;

        // solar time slope must be compensated by changing inclination
        // linearized relationship between hDot and inclination:
        // hDot = alphaDot - raanDot where alphaDot is the angular rate of Sun right ascension
        // and raanDot is the angular rate of ascending node right ascension. So hDot evolution
        // is the opposite of raan evolution, which itself is proportional to cos(i) due to J2
        // effect. So hDot = alphaDot - k cos(i) and hence Delta hDot = -k sin(i) Delta i
        // so Delta hDot / Delta i = (alphaDot - hDot) tan(i)
        double dhDotDi = (24.0 / Constants.JULIAN_YEAR - fittedH[1]) * FastMath.tan(previous.getI());

        // compute inclination offset needed to achieve station-keeping target
        final double deltaI = fittedH[1] / dhDotDi;

        return new CircularOrbit(previous.getA(),
                                 previous.getCircularEx(), previous.getCircularEy(),
                                 previous.getI() + deltaI,
                                 previous.getRightAscensionOfAscendingNode() + deltaRaan,
                                 previous.getAlphaV(), PositionAngle.TRUE,
                                 previous.getFrame(), previous.getDate(),
                                 previous.getMu());

    }

    /** Improve orbit to better match frozen eccentricity property.
     * @param previous previous orbit
     * @param duration sampling duration
     * @param propagator propagator to use
     * @return an improved Earth phased, Sun synchronous orbit with frozen eccentricity
     * @exception OrekitException if orbit cannot be propagated
     */
    private CircularOrbit improveFrozenEccentricity(CircularOrbit previous, double duration,
                                                    Propagator propagator)
        throws OrekitException {

        propagator.resetInitialState(new SpacecraftState(previous));
        AbsoluteDate start = previous.getDate();

        NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics harmonics =
                gravityField.onDate(previous.getDate());
        double[][] unnormalization = GravityFieldFactory.getUnnormalizationFactors(2, 0);
        double a    = previous.getA();
        double sinI = FastMath.sin(previous.getI());
        double aeOa = gravityField.getAe() / a;
        double mu   = gravityField.getMu();
        double n    = FastMath.sqrt(mu / a) / a;
        double j2   = -unnormalization[2][0] * harmonics.getNormalizedCnm(2, 0);
        double frozenPulsation = 3 * n * j2 * aeOa * aeOa * (1 - 1.25 * sinI * sinI);

        // fit the eccentricity to an harmonic model with short and medium periods
        // we will only use the medium periods part for the correction
        SecularAndHarmonic exModel = new SecularAndHarmonic(0, frozenPulsation, n, 2 * n);
        SecularAndHarmonic eyModel = new SecularAndHarmonic(0, frozenPulsation, n, 2 * n);
        exModel.resetFitting(start, new double[] {
            previous.getCircularEx(), -1.0e-10, 1.0e-5,
            1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5
        });
        eyModel.resetFitting(start, new double[] {
            previous.getCircularEy(), -1.0e-10, 1.0e-5,
            1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5
        });

        final double step = previous.getKeplerianPeriod() / 5;
        for (double dt = 0; dt < duration; dt += step) {
            final SpacecraftState state = propagator.propagate(start.shiftedBy(dt));
            final CircularOrbit orbit   = (CircularOrbit) OrbitType.CIRCULAR.convertType(state.getOrbit());
            exModel.addPoint(state.getDate(), orbit.getCircularEx());
            eyModel.addPoint(state.getDate(), orbit.getCircularEy());
        }

        // adjust eccentricity
        exModel.fit();
        double dex = -exModel.getFittedParameters()[1];
        eyModel.fit();
        double dey = -eyModel.getFittedParameters()[1];

        // put the eccentricity at center of frozen center
        return new CircularOrbit(previous.getA(),
                                 previous.getCircularEx() + dex,
                                 previous.getCircularEy() + dey,
                                 previous.getI(), previous.getRightAscensionOfAscendingNode(),
                                 previous.getAlphaV(), PositionAngle.TRUE,
                                 previous.getFrame(), previous.getDate(),
                                 previous.getMu());

    }

    /** Print ground track grid point
     * @param out output stream
     * @param latitude point latitude
     * @param ascending indicator for latitude crossing direction
     * @param orbit phased orbit
     * @param propagator propagator for orbit
     * @param nbOrbits number of orbits in the cycle
     * @exception OrekitException if orbit cannot be propagated
     */
    private void printGridPoints(final PrintStream out,
                                 final double latitude, final boolean ascending,
                                 final Orbit orbit, final Propagator propagator, int nbOrbits)
        throws OrekitException {

        propagator.resetInitialState(new SpacecraftState(orbit));
        AbsoluteDate start = orbit.getDate();

        // find the first latitude crossing
        double period   = orbit.getKeplerianPeriod();
        double stepSize = period / 100;
        SpacecraftState crossing =
                findFirstCrossing(latitude, ascending, start, start.shiftedBy(2 * period),
                                  stepSize, propagator);

        // find all other latitude crossings from regular schedule
        DecimalFormat fTime  = new DecimalFormat("0000000.000", new DecimalFormatSymbols(Locale.US));
        DecimalFormat fAngle = new DecimalFormat("000.00000",   new DecimalFormatSymbols(Locale.US));
        while (nbOrbits-- > 0) {

            GeodeticPoint gp = earth.transform(crossing.getPVCoordinates().getPosition(),
                                               crossing.getFrame(), crossing.getDate());
            out.println(fTime.format(crossing.getDate().durationFrom(start)) +
                        " " + fAngle.format(FastMath.toDegrees(gp.getLatitude())) +
                        " " + fAngle.format(FastMath.toDegrees(gp.getLongitude())) +
                        " " + ascending);

            final AbsoluteDate previousDate = crossing.getDate();
            crossing = findLatitudeCrossing(latitude, previousDate.shiftedBy(period),
                                            previousDate.shiftedBy(2 * period),
                                            stepSize, period / 8, propagator);
            period = crossing.getDate().durationFrom(previousDate);

        }

    }

    /** Compute the mean solar time.
     * @param orbit current orbit
     * @return mean solar time
     * @exception OrekitException if state cannot be converted
     */
    private double meanSolarTime(final Orbit orbit)
        throws OrekitException {

        // compute angle between Sun and spacecraft in the equatorial plane
        final Vector3D position = orbit.getPVCoordinates().getPosition();
        final double time       = orbit.getDate().getComponents(TimeScalesFactory.getUTC()).getTime().getSecondsInUTCDay();
        final double theta      = gmst.value(orbit.getDate()).getValue();
        final double sunAlpha   = theta + FastMath.PI * (1 - time / (Constants.JULIAN_DAY * 0.5));
        final double dAlpha     = MathUtils.normalizeAngle(position.getAlpha() - sunAlpha, 0);

        // convert the angle to solar time
        return 12.0 * (1.0 + dAlpha / FastMath.PI);

    }

    /**
     * Find the first crossing of the reference latitude.
     * @param latitude latitude to search for
     * @param ascending indicator for desired crossing direction
     * @param searchStart search start
     * @param end maximal date not to overtake
     * @param stepSize step size to use
     * @param propagator propagator
     * @return first crossing
     * @throws OrekitException if state cannot be propagated
     */
    private SpacecraftState findFirstCrossing(final double latitude, final boolean ascending,
                                              final AbsoluteDate searchStart, final AbsoluteDate end,
                                              final double stepSize, final Propagator propagator)
        throws OrekitException {

        double previousLatitude = Double.NaN;
        for (AbsoluteDate date = searchStart; date.compareTo(end) < 0; date = date.shiftedBy(stepSize)) {
            final PVCoordinates pv       = propagator.propagate(date).getPVCoordinates(earth.getBodyFrame());
            final double currentLatitude = earth.transform(pv.getPosition(), earth.getBodyFrame(), date).getLatitude();
            if (((previousLatitude <= latitude) && (currentLatitude >= latitude) &&  ascending) ||
                ((previousLatitude >= latitude) && (currentLatitude <= latitude) && !ascending)) {
                return findLatitudeCrossing(latitude, date.shiftedBy(-0.5 * stepSize), end,
                                            0.5 * stepSize, 2 * stepSize, propagator);
            }
            previousLatitude = currentLatitude;
        }

        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                  "latitude " + FastMath.toDegrees(latitude) + " never crossed");

    }


    /**
     * Find the state at which the reference latitude is crossed.
     * @param latitude latitude to search for
     * @param guessDate guess date for the crossing
     * @param endDate maximal date not to overtake
     * @param shift shift value used to evaluate the latitude function bracketing around the guess date
     * @param maxShift maximum value that the shift value can take
     * @param propagator propagator used
     * @return state at latitude crossing time
     * @throws OrekitException if state cannot be propagated
     * @throws MathRuntimeException if latitude cannot be bracketed in the search interval
     */
    private SpacecraftState findLatitudeCrossing(final double latitude,
                                                 final AbsoluteDate guessDate, final AbsoluteDate endDate,
                                                 final double shift, final double maxShift,
                                                 final Propagator propagator)
        throws OrekitException, MathRuntimeException {

        // function evaluating to 0 at latitude crossings
        final UnivariateFunction latitudeFunction = new UnivariateFunction() {
            /** {@inheritDoc} */
            public double value(double x) {
                try {
                    final SpacecraftState state = propagator.propagate(guessDate.shiftedBy(x));
                    final Vector3D position = state.getPVCoordinates(earth.getBodyFrame()).getPosition();
                    final GeodeticPoint point = earth.transform(position, earth.getBodyFrame(), state.getDate());
                    return point.getLatitude() - latitude;
                } catch (OrekitException oe) {
                    throw new RuntimeException(oe);
                }
            }
        };

        // try to bracket the encounter
        double span;
        if (guessDate.shiftedBy(shift).compareTo(endDate) > 0) {
            // Take a 1e-3 security margin
            span = endDate.durationFrom(guessDate) - 1e-3;
        } else {
            span = shift;
        }

        while (!UnivariateSolverUtils.isBracketing(latitudeFunction, -span, span)) {

            if (2 * span > maxShift) {
                // let the Hipparchus exception be thrown
                UnivariateSolverUtils.verifyBracketing(latitudeFunction, -span, span);
            } else if (guessDate.shiftedBy(2 * span).compareTo(endDate) > 0) {
                // Out of range :
                return null;
            }

            // expand the search interval
            span *= 2;

        }

        // find the encounter in the bracketed interval
        final BaseUnivariateSolver<UnivariateFunction> solver =
                new BracketingNthOrderBrentSolver(0.1, 5);
        final double dt = solver.solve(1000, latitudeFunction,-span, span);
        return propagator.propagate(guessDate.shiftedBy(dt));

    }

}
