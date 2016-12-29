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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import fr.cs.examples.Autoconfiguration;
import fr.cs.examples.KeyValueFileParser;

/** Orekit tutorial for track corridor display.
 * @author Luc Maisonobe
 */
public class TrackCorridor {

    /** Program entry point.
     * @param args program arguments
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // input/out
            File input  = new File(TrackCorridor.class.getResource("/track-corridor.in").toURI().getPath());
            File output = new File(input.getParentFile(), "track-corridor.csv");

            new TrackCorridor().run(input, output, ",");

            System.out.println("corridor saved as file " + output);

        } catch (URISyntaxException use) {
            System.err.println(use.getLocalizedMessage());
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            System.exit(1);
        } catch (IllegalArgumentException iae) {
            System.err.println(iae.getLocalizedMessage());
            System.exit(1);
        } catch (OrekitException oe) {
            System.err.println(oe.getLocalizedMessage());
            System.exit(1);
        }
    }

    /** Input parameter keys. */
    private static enum ParameterKey {

        TLE_LINE1,
        TLE_LINE2,
        ORBIT_CIRCULAR_DATE,
        ORBIT_CIRCULAR_A,
        ORBIT_CIRCULAR_EX,
        ORBIT_CIRCULAR_EY,
        ORBIT_CIRCULAR_I,
        ORBIT_CIRCULAR_RAAN,
        ORBIT_CIRCULAR_ALPHA,
        START_DATE,
        DURATION,
        STEP,
        ANGULAR_OFFSET;

    }

    private void run(final File input, final File output, final String separator)
            throws IOException, IllegalArgumentException, OrekitException {

        // read input parameters
        KeyValueFileParser<ParameterKey> parser =
                new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        try (final FileInputStream fis = new FileInputStream(input)) {
            parser.parseInput(input.getAbsolutePath(), fis);
        }
        TimeScale utc = TimeScalesFactory.getUTC();

        Propagator propagator;
        if (parser.containsKey(ParameterKey.TLE_LINE1)) {
            propagator = createPropagator(parser.getString(ParameterKey.TLE_LINE1),
                                          parser.getString(ParameterKey.TLE_LINE2));
        } else {
            propagator = createPropagator(parser.getDate(ParameterKey.ORBIT_CIRCULAR_DATE, utc),
                                          parser.getDouble(ParameterKey.ORBIT_CIRCULAR_A),
                                          parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EX),
                                          parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EY),
                                          parser.getAngle(ParameterKey.ORBIT_CIRCULAR_I),
                                          parser.getAngle(ParameterKey.ORBIT_CIRCULAR_RAAN),
                                          parser.getAngle(ParameterKey.ORBIT_CIRCULAR_ALPHA));
        }

        // simulation properties
        AbsoluteDate start = parser.getDate(ParameterKey.START_DATE, utc);
        double duration    = parser.getDouble(ParameterKey.DURATION);
        double step        = parser.getDouble(ParameterKey.STEP);
        double angle       = parser.getAngle(ParameterKey.ANGULAR_OFFSET);

        // set up a handler to gather all corridor points
        CorridorHandler handler = new CorridorHandler(angle);
        propagator.setMasterMode(step, handler);

        // perform propagation, letting the step handler populate the corridor
        propagator.propagate(start, start.shiftedBy(duration));

        // retrieve the built corridor
        List<CorridorPoint> corridor = handler.getCorridor();

        // create a 7 columns csv file representing the corridor in the user home directory, with
        // date in column 1 (in ISO-8601 format)
        // left limit latitude in column 2 and left limit longitude in column 3
        // center track latitude in column 4 and center track longitude in column 5
        // right limit latitude in column 6 and right limit longitude in column 7
        DecimalFormat format = new DecimalFormat("#00.00000", new DecimalFormatSymbols(Locale.US));
        try (final PrintStream stream = new PrintStream(output, "UTF-8")) {
            for (CorridorPoint p : corridor) {
                stream.println(p.getDate() + separator +
                               format.format(FastMath.toDegrees(p.getLeft().getLatitude()))    + separator +
                               format.format(FastMath.toDegrees(p.getLeft().getLongitude()))   + separator +
                               format.format(FastMath.toDegrees(p.getCenter().getLatitude()))  + separator +
                               format.format(FastMath.toDegrees(p.getCenter().getLongitude())) + separator +
                               format.format(FastMath.toDegrees(p.getRight().getLatitude()))   + separator +
                               format.format(FastMath.toDegrees(p.getRight().getLongitude())));
            }
        }

    }

    /** Create an orbit propagator for a circular orbit
     * @param a  semi-major axis (m)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param alpha  an + ω, mean latitude argument (rad)
     * @param date date of the orbital parameters
     * @return an orbit propagator
     * @exception OrekitException if propagator cannot be built
     */
    private Propagator createPropagator(final AbsoluteDate date,
                                        final double a, final double ex, final double ey,
                                        final double i, final double raan,
                                        final double alpha)
        throws OrekitException {

        // create orbit
        Orbit initialOrbit = new CircularOrbit(a, ex, ey, i, raan, alpha, PositionAngle.MEAN,
                                               FramesFactory.getEME2000(), date,
                                               Constants.EIGEN5C_EARTH_MU);

        // create propagator
        Propagator propagator =
                new EcksteinHechlerPropagator(initialOrbit,
                                              new LofOffset(initialOrbit.getFrame(), LOFType.TNW),
                                              Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                              Constants.EIGEN5C_EARTH_MU,  Constants.EIGEN5C_EARTH_C20,
                                              Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                              Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);

        return propagator;

    }

    /** Create an orbit propagator for a TLE orbit
     * @param line1 firs line of the TLE
     * @param line2 second line of the TLE
     * @return an orbit propagator
     * @exception OrekitException if the TLE lines are corrupted (wrong checksums ...)
     */
    private Propagator createPropagator(final String line1, final String line2)
        throws OrekitException {

        // create pseudo-orbit
        TLE tle = new TLE(line1, line2);

        // create propagator
        Propagator propagator = TLEPropagator.selectExtrapolator(tle);

        return propagator;

    }

    /** Step handler storing corridor points. */
    private static class CorridorHandler implements OrekitFixedStepHandler {

        /** Earth model. */
        private final BodyShape earth;

        /** Radial offset from satellite to some distant point at specified angular offset. */
        private final double deltaR;

        /** Cross-track offset from satellite to some distant point at specified angular offset. */
        private final double deltaC;

        /** Corridor. */
        private final List<CorridorPoint> corridor;

        /** simple constructor.
         * @param angle angular offset of corridor boundaries
         * @exception OrekitException if Earth frame cannot be built
         */
        public CorridorHandler(final double angle) throws OrekitException {

            // set up Earth model
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, false));

            // set up position offsets, using Earth radius as an arbitrary distance
            deltaR = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * FastMath.cos(angle);
            deltaC = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * FastMath.sin(angle);

            // prepare an empty corridor
            corridor = new ArrayList<TrackCorridor.CorridorPoint>();

        }

        /** {@inheritDoc} */
        public void handleStep(SpacecraftState currentState, boolean isLast)
            throws OrekitException {

            // compute sub-satellite track
            AbsoluteDate  date    = currentState.getDate();
            PVCoordinates pvInert = currentState.getPVCoordinates();
            Transform     t       = currentState.getFrame().getTransformTo(earth.getBodyFrame(), date);
            Vector3D      p       = t.transformPosition(pvInert.getPosition());
            Vector3D      v       = t.transformVector(pvInert.getVelocity());
            GeodeticPoint center  = earth.transform(p, earth.getBodyFrame(), date);

            // compute left and right corridor points
            Vector3D      nadir      = p.normalize().negate();
            Vector3D      crossTrack = p.crossProduct(v).normalize();
            Line          leftLine   = new Line(p, new Vector3D(1.0, p, deltaR, nadir,  deltaC, crossTrack), 1.0e-10);
            GeodeticPoint left       = earth.getIntersectionPoint(leftLine, p, earth.getBodyFrame(), date);
            Line          rightLine  = new Line(p, new Vector3D(1.0, p, deltaR, nadir, -deltaC, crossTrack), 1.0e-10);
            GeodeticPoint right      = earth.getIntersectionPoint(rightLine, p, earth.getBodyFrame(), date);

            // add the corridor points
            corridor.add(new CorridorPoint(date, left, center, right));

        }

        /** Get the corridor.
         * @return build corridor
         */
        public List<CorridorPoint> getCorridor() {
            return corridor;
        }

    }

    /** Container for corridor points. */
    private static class CorridorPoint {

        /** Point date. */
        private final AbsoluteDate date;

        /** Left limit. */
        private final GeodeticPoint left;

        /** Central track point. */
        private final GeodeticPoint center;

        /** Right limit. */
        private final GeodeticPoint right;

        /** Simple constructor.
         * @param date point date
         * @param left left limit
         * @param center central track point
         * @param right right limit
         */
        public CorridorPoint(AbsoluteDate date, GeodeticPoint left,
                             GeodeticPoint center, GeodeticPoint right) {
            this.date   = date;
            this.left   = left;
            this.center = center;
            this.right  = right;
        }

        /** Get point date. */
        public AbsoluteDate getDate() {
            return date;
        }

        /** Get left limit. */
        public GeodeticPoint getLeft() {
            return left;
        }

        /** Get central track point. */
        public GeodeticPoint getCenter() {
            return center;
        }

        /** Get right limit. */
        public GeodeticPoint getRight() {
            return right;
        }

    }

}
