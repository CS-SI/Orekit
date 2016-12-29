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
package fr.cs.examples.gnss;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.DOP;
import org.orekit.gnss.DOPComputer;
import org.orekit.gnss.GPSAlmanac;
import org.orekit.gnss.SEMParser;
import org.orekit.models.earth.tessellation.ConstantAzimuthAiming;
import org.orekit.models.earth.tessellation.EllipsoidTessellator;
import org.orekit.models.earth.tessellation.TileAiming;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.GPSPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


/**
 * Orekit tutorial for DOP computation.
 *
 * <p>This tutorial shows a basic usage for computing the DOP over a
 * geographic zone and for a period.</p>
 * <p>It uses a SEM almanac file to get GPS orbital data in order to
 * configure the GPS propagators used for the DOP computation.</p>
 * <p>It uses the tessellation of the geographic zone to get the points
 * on which DOP is computed.</p>
 *
 * @author Pascal Parraud
 */
public class DOPComputation {

    /**
     * Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {

        try {
            // configure Orekit
            final File orekitData = new File(DOPComputation.class.getResource("/tutorial-orekit-data").toURI().getPath());
            final File gnssData = new File(DOPComputation.class.getResource("/tutorial-gnss").toURI().getPath());
            DataProvidersManager.getInstance().addProvider(new DirectoryCrawler(orekitData));
            DataProvidersManager.getInstance().addProvider(new DirectoryCrawler(gnssData));

            // The Earth body shape
            final OneAxisEllipsoid shape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            // The geographic zone to consider (clockwise defined for tessellation)
            final double[][] area = new double[][] { {43.643820, 1.470092},
                                                     {43.566007, 1.488974},
                                                     {43.568246, 1.417906},
                                                     {43.613503, 1.387351},
                                                     {43.652515, 1.425460} };
            final List<GeodeticPoint> zone = new ArrayList<GeodeticPoint>(area.length);
            for (double[] point: area) {
                zone.add(new GeodeticPoint(FastMath.toRadians(point[0]),
                                           FastMath.toRadians(point[1]),
                                           0.));
            }

            // The min elevation over the zone: 10°
            final double minElevation = FastMath.toRadians(10.0);

            // Computation period and time step: 1 day, 10'
            final AbsoluteDate tStart = new AbsoluteDate(2016, 3, 2, 20, 0, 0.,
                                                         TimeScalesFactory.getUTC());
            final AbsoluteDate tStop  = tStart.shiftedBy(Constants.JULIAN_DAY);
            final double tStep = 600.;

            // Computes the DOP over the zone for the period
            new DOPComputation().run(shape, zone, 1000., minElevation, tStart, tStop, tStep);

        } catch (OrekitException oe) {
            System.err.println(oe.getLocalizedMessage());
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            System.exit(1);
        } catch (ParseException pe) {
            System.err.println(pe.getLocalizedMessage());
            System.exit(1);
        } catch (URISyntaxException use) {
            System.err.println(use.getLocalizedMessage());
            System.exit(1);
        }
    }

    private void run(final OneAxisEllipsoid shape, final List<GeodeticPoint> zone,
                     final double meshSize, final double minElevation,
                     final AbsoluteDate tStart, final AbsoluteDate tStop, final double tStep)
            throws IOException, OrekitException, ParseException {

        // Gets the GPS almanacs from the SEM file
        final SEMParser reader = new SEMParser(null);
        reader.loadData();
        final List<GPSAlmanac> almanacs = reader.getAlmanacs();

        // Creates the GPS propagators from the almanacs
        final List<Propagator> propagators = new ArrayList<Propagator>();
        for (GPSAlmanac almanac: almanacs) {
            // Only keeps almanac with health status ok
            if (almanac.getHealth() == 0) {
                propagators.add(new GPSPropagator.Builder(almanac).build());
            } else {
                System.out.println("GPS PRN " + almanac.getPRN() +
                                   " is not OK (Health status = " + almanac.getHealth() + ").");
            }
        }

        // Meshes the area of interest into a grid of geodetic points.
        final List<List<GeodeticPoint>> points = sample(shape, zone, meshSize);

        // Creates the DOP computers for all the locations of the sampled geographic zone
        final List<DOPComputer> computers = new ArrayList<DOPComputer>();
        for (List<GeodeticPoint> row: points) {
            for (GeodeticPoint point: row) {
                computers.add(DOPComputer.create(shape, point).withMinElevation(minElevation));
            }
        }

        // Computes the DOP for each point over the period
        final List<List<DOP>> allDop = new ArrayList<List<DOP>>();
        // Loops on the period
        AbsoluteDate tc = tStart;
        while (tc.compareTo(tStop) != 1) {
            // Loops on the grid points
            final List<DOP> dopAtDate = new ArrayList<DOP>();
            for (DOPComputer computer: computers) {
                try {
                    final DOP dop = computer.compute(tc, propagators);
                    dopAtDate.add(dop);
                } catch (OrekitException oe) {
                    System.out.println(oe.getLocalizedMessage());
                }
            }
            allDop.add(dopAtDate);
            tc = tc.shiftedBy(tStep);
        }

        // Post-processing: gets the statistics of PDOP over the zone at each time
        System.out.println("                           PDOP");
        System.out.println("          Date           min  max");
        for (List<DOP> dopAtDate : allDop) {
            final StreamingStatistics pDoP = new StreamingStatistics();
            for (DOP dopAtLoc : dopAtDate) {
                pDoP.addValue(dopAtLoc.getPdop());
            }
            final AbsoluteDate date = dopAtDate.get(0).getDate();
            System.out.format(Locale.ENGLISH, "%s %.2f %.2f%n", date.toString(), pDoP.getMin(), pDoP.getMax());
        }
    }

    /**
     * Mesh an area of interest into a grid of geodetic points.
     *
     * @param zone the area to mesh
     * @param meshSize the size of the square meshes as a distance on the Earth surface (in meters)
     * @return a list of geodetic points sampling the zone of interest
     * @throws OrekitException if the area cannot be meshed
     */
    private List<List<GeodeticPoint>> sample(final OneAxisEllipsoid shape,
                                             final List<GeodeticPoint> zone,
                                             final double meshSize) throws OrekitException {
        // Convert the area into a SphericalPolygonsSet
        final SphericalPolygonsSet sps = computeSphericalPolygonsSet(zone);

        // Build the tesselator
        final TileAiming aiming = new ConstantAzimuthAiming(shape, 0.);
        final EllipsoidTessellator tessellator = new EllipsoidTessellator(shape, aiming, 4);

        // Returns the sampled area as a grid of geodetic points
        return tessellator.sample(sps, meshSize, meshSize);
      }

    /**
     * Computes a spherical polygons set from a geographic zone.
     *
     * @param zone the geographic zone
     * @return the spherical polygons set
     */
    private static SphericalPolygonsSet computeSphericalPolygonsSet(final List<GeodeticPoint> zone) {
        // Convert the area into a SphericalPolygonsSet
        final SphericalPolygonsSet sps = computeSPS(zone);
        // If the zone is not defined counterclockwise
        if (sps.getSize() > MathUtils.TWO_PI) {
            // Inverts the order of the points
            final List<GeodeticPoint> zone2 = new ArrayList<GeodeticPoint>(zone.size());
            for (int j = zone.size() - 1; j > -1; j--) {
                zone2.add(zone.get(j));
            }
            return computeSPS(zone2);
        } else {
            return sps;
        }
    }

    /**
     * Computes a spherical polygons set from a geographic zone.
     *
     * @param zone the geographic zone
     * @return the spherical polygons set
     */
    private static SphericalPolygonsSet computeSPS(final List<GeodeticPoint> zone) {
        // Convert the area into a SphericalPolygonsSet
        final S2Point[] vertices = new S2Point[zone.size()];
        int i = 0;
        for (GeodeticPoint point : zone) {
            final double theta = point.getLongitude();
            final double phi   = 0.5 * FastMath.PI - point.getLatitude();
            vertices[i++] = new S2Point(theta, phi);
        }
        return new SphericalPolygonsSet(1.0e-10, vertices);
    }
}
