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

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import fr.cs.examples.Autoconfiguration;
import fr.cs.examples.KeyValueFileParser;

/** Orekit tutorial for computing visibility circles.
 * @author Luc Maisonobe
 */
public class VisibilityCircle {

    /** Program entry point.
     * @param args program arguments
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // input/out
            File input  = new File(VisibilityCircle.class.getResource("/visibility-circle.in").toURI().getPath());
            File output = new File(input.getParentFile(), "visibility-circle.csv");

            new VisibilityCircle().run(input, output, ",");

            System.out.println("visibility circle saved as file " + output);

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

        STATION_NAME,
        STATION_LATITUDE,
        STATION_LONGITUDE,
        STATION_ALTITUDE,
        MIN_ELEVATION,
        SPACECRAFT_ALTITUDE,
        POINTS_NUMBER;

    }

    private void run(final File input, final File output, final String separator)
            throws IOException, IllegalArgumentException, OrekitException {

        // read input parameters
        KeyValueFileParser<ParameterKey> parser =
                new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        try (final FileInputStream fis = new FileInputStream(input)) {
            parser.parseInput(input.getAbsolutePath(), fis);
        }

        double minElevation = parser.getAngle(ParameterKey.MIN_ELEVATION);
        double radius       = Constants.WGS84_EARTH_EQUATORIAL_RADIUS +
                              parser.getDouble(ParameterKey.SPACECRAFT_ALTITUDE);
        int points          = parser.getInt(ParameterKey.POINTS_NUMBER);

        // station properties
        double latitude  = parser.getAngle(ParameterKey.STATION_LATITUDE);
        double longitude = parser.getAngle(ParameterKey.STATION_LONGITUDE);
        double altitude  = parser.getDouble(ParameterKey.STATION_ALTITUDE);
        String name      = parser.getString(ParameterKey.STATION_NAME);

        // compute visibility circle
        List<GeodeticPoint> circle =
                computeCircle(latitude, longitude, altitude, name, minElevation, radius, points);

        // create a 2 columns csv file representing the visibility circle
        // in the user home directory, with latitude in column 1 and longitude in column 2
        DecimalFormat format = new DecimalFormat("#00.00000", new DecimalFormatSymbols(Locale.US));
        PrintStream csvFile = new PrintStream(output, "UTF-8");
        for (GeodeticPoint p : circle) {
            csvFile.println(format.format(FastMath.toDegrees(p.getLatitude())) + "," +
                            format.format(FastMath.toDegrees(p.getLongitude())));
        }
        csvFile.close();

    }

    private static List<GeodeticPoint> computeCircle(double latitude, double longitude, double altitude,
                                                     String name, double minElevation, double radius, int points)
        throws OrekitException {

        // define Earth shape, using WGS84 model
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING,
                                               FramesFactory.getITRF(IERSConventions.IERS_2010, false));

        // define an array of ground stations
        TopocentricFrame station =
                new TopocentricFrame(earth, new GeodeticPoint(latitude, longitude, altitude), name);

        // compute the visibility circle
        List<GeodeticPoint> circle = new ArrayList<GeodeticPoint>();
        for (int i = 0; i < points; ++i) {
            double azimuth = i * (2.0 * FastMath.PI / points);
            circle.add(station.computeLimitVisibilityPoint(radius, azimuth, minElevation));
        }

        // return the computed points
        return circle;

    }

}
