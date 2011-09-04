/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.math.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.utils.Constants;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for computing visibility circles.
 * @author Luc Maisonobe
 */
public class VisibilityCircle {

    /** Parameters map. */
    private final Map<ParameterKey, String> map = new HashMap<ParameterKey, String>();

    /** Program entry point.
     * @param args program arguments
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // input/out
            File input  = new File(TrackCorridor.class.getResource("/visibility-circle.in").toURI().getPath());
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

    private void run(final File input, final File output, final String separator)
            throws IOException, IllegalArgumentException, OrekitException {

        // read input parameters
        parseInput(input);

        double minElevation = getAngle(ParameterKey.MIN_ELEVATION);
        double radius       = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + getDouble(ParameterKey.SPACECRAFT_ALTITUDE);
        int points          = getInt(ParameterKey.POINTS_NUMBER);

        // station properties
        double latitude  = getAngle(ParameterKey.STATION_LATITUDE);
        double longitude = getAngle(ParameterKey.STATION_LONGITUDE);
        double altitude  = getDouble(ParameterKey.STATION_ALTITUDE);
        String name      = getString(ParameterKey.STATION_NAME);

        // compute visibility circle
        List<GeodeticPoint> circle =
                computeCircle(latitude, longitude, altitude, name, minElevation, radius, points);

        // create a 2 columns csv file representing the visibility circle
        // in the user home directory, with latitude in column 1 and longitude in column 2
        DecimalFormat format = new DecimalFormat("#00.00000", new DecimalFormatSymbols(Locale.US));
        PrintStream csvFile = new PrintStream(output);
        for (GeodeticPoint p : circle) {
            csvFile.println(format.format(FastMath.toDegrees(p.getLatitude())) + "," +
                            format.format(FastMath.toDegrees(p.getLongitude())));
        }
        csvFile.close();

    }

    /** Parse an input file.
     * <p>
     * The input file syntax is a set of key=value lines. Blank lines and lines
     * starting with '#' (after whitespace trimming) are silently ignored. The
     * equal sign may be surrounded by space characters. Keys must correspond to
     * the {@link ParameterKey} enumerate constants, given that matching is not
     * case sensitive and that '_' characters may appear as '.' characters in the
     * file. this means that the lines:
     * <pre>
     *   # this is the station altitude
     *   station.altitude   = 133.45
     * </pre>
     * are perfectly right and correspond to key {@link ParameterKey#STATION_ALTITUDE}.
     * </p>
     * @param input input file
     * @return key/value map
     * @exception IOException if input file cannot be read
     * @exception IllegalArgumentException if a line cannot be read properly
     */
    private void parseInput(final File input)
        throws IOException, IllegalArgumentException {

        BufferedReader reader = new BufferedReader(new FileReader(input));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            // we ignore blank lines and line starting with '#'
            if ((line.length() > 0) && !line.startsWith("#")) {
                String[] fields = line.split("\\s*=\\s*");
                if (fields.length != 2) {
                    throw new IllegalArgumentException(line);
                }
                ParameterKey key = ParameterKey.valueOf(fields[0].toUpperCase().replaceAll("\\.", "_"));
                map.put(key, fields[1]);
            }
        }
        reader.close();

    }

    /** Get a raw string value from a parameters map.
     * @param key parameter key
     * @return string value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    private String getString(final ParameterKey key)
        throws NoSuchElementException {
        final String value = map.get(key);
        if (value == null) {
            throw new NoSuchElementException(key.toString());
        }
        return value.trim();
    }

    /** Get a raw integer value from a parameters map.
     * @param key parameter key
     * @return integer value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    private int getInt(final ParameterKey key)
        throws NoSuchElementException {
        return Integer.parseInt(getString(key));
    }

    /** Get a raw double value from a parameters map.
     * @param key parameter key
     * @return double value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    private double getDouble(final ParameterKey key)
        throws NoSuchElementException {
        return Double.parseDouble(getString(key));
    }

    /** Get an angle value from a parameters map.
     * <p>
     * The angle is considered to be in degrees in the file, it will be returned in radians
     * </p>
     * @param key parameter key
     * @return angular value corresponding to the key, in radians
     * @exception NoSuchElementException if key is not in the map
     */
    private double getAngle(final ParameterKey key)
        throws NoSuchElementException {
        return FastMath.toRadians(getDouble(key));
    }

    private static List<GeodeticPoint> computeCircle(double latitude, double longitude, double altitude,
                                                     String name, double minElevation, double radius, int points)
        throws OrekitException {

        // define Earth shape, using WGS84 model
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING,
                                               FramesFactory.getITRF2008());

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

}
