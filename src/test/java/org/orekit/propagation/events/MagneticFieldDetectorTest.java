/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.propagation.events;

import java.util.ArrayList;
import java.util.Comparator;

import org.hipparchus.geometry.euclidean.twod.PolygonsSet;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.geometry.partitioning.Region.Location;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.GeoMagneticField;
import org.orekit.models.earth.GeoMagneticFieldFactory;
import org.orekit.models.earth.GeoMagneticFieldFactory.FieldModel;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.units.UnitsConverter;

/**
 * Test class for MagneticFieldDetector.
 * @author Romaric Her
 */
public class MagneticFieldDetectorTest {

    Propagator propagator;
    Frame eme2000;
    Frame itrf;
    TimeScale utc;
    AbsoluteDate initialDate;
    OneAxisEllipsoid earth;
    GeoMagneticField wmm;
    GeoMagneticField igrf;

    double saaValidationThreshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(500);
    double saaValidationWidth = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(200);

    @BeforeEach
    public void setup() {
        // Initialize context
        Utils.setDataRoot("regular-data:earth");

        // Initialize constants
        eme2000 = FramesFactory.getEME2000();
        itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        utc = TimeScalesFactory.getUTC();
        initialDate = new AbsoluteDate("2019-01-01T12:00:00.000", utc);
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, itrf);

        //Initialize magnetic field models
        wmm = GeoMagneticFieldFactory.getField(FieldModel.WMM, 2019);
        igrf = GeoMagneticFieldFactory.getField(FieldModel.IGRF, 2019);

    }

    @AfterEach
    public void tearDown() {
        // Clear the context
        DataContext.getDefault().getDataProvidersManager().clearProviders();
        DataContext.getDefault().getDataProvidersManager().clearLoadedDataNames();
    }

    /**
     * initialize an analytical propagator with a polar orbit to cross the SAA
     */
    private void initializePropagator() {
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 600000; // 600 km altitude
        double i = FastMath.toRadians(80); // 80° inclination
        Orbit initialOrbit = new CircularOrbit(a, 0, 0, i, 0, 0, PositionAngleType.TRUE, eme2000, initialDate, Constants.WGS84_EARTH_MU);
        propagator = new KeplerianPropagator(initialOrbit);
    }

    /**
     * Test for the magnetic field detector based on the WMM at sea level
     */
    @Test
    public void magneticFieldDetectorWMMSeaLevelTest() {
        initializePropagator();
        double threshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(45000);

        CustomEventHandler handler = new CustomEventHandler();
        MagneticFieldDetector detector = new MagneticFieldDetector(threshold, FieldModel.WMM, earth, true).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(initialDate, initialDate.shiftedBy(864000));

        checkEvents(handler.getEvents(), threshold, wmm, true);
    }

    /**
     * Test for the magnetic field detector based on the IGRF at sea level
     */
    @Test
    public void magneticFieldDetectorIGRFSeaLevelTest() {
        initializePropagator();
        double threshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(45000);

        CustomEventHandler handler = new CustomEventHandler();
        MagneticFieldDetector detector = new MagneticFieldDetector(threshold, FieldModel.IGRF, earth, true).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(initialDate, initialDate.shiftedBy(864000));

        checkEvents(handler.getEvents(), threshold, igrf, true);
    }

    /**
     * Test for the magnetic field detector based on the WMM at satellite's altitude.
     */
    @Test
    public void magneticFieldDetectorWMMTrueAltitudeTest() {
        initializePropagator();
        double threshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(45000);

        CustomEventHandler handler = new CustomEventHandler();
        MagneticFieldDetector detector = new MagneticFieldDetector(threshold, FieldModel.WMM, earth).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(initialDate, initialDate.shiftedBy(864000));

        checkEvents(handler.getEvents(), threshold, wmm, false);
    }

    /**
     * Test for the magnetic field detector based on the IGRF at satellite's altitude.
     */
    @Test
    public void magneticFieldDetectorIGRFTrueAltitudeTest() {
        initializePropagator();
        double threshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(45000);

        CustomEventHandler handler = new CustomEventHandler();
        MagneticFieldDetector detector = new MagneticFieldDetector(threshold, FieldModel.IGRF, earth).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(initialDate, initialDate.shiftedBy(864000));

        checkEvents(handler.getEvents(), threshold, igrf, false);
    }

    /**
     * Test for the SAA detector based on the WMM at sea level
     */
    @Test
    public void saaDetectorWMMSeaLevelTest() {
        initializePropagator();

        double altitude = 0;
        double threshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(32000);

        PolygonsSet saaIn = generateGeomagneticMap(wmm, altitude, threshold - saaValidationThreshold, saaValidationWidth);
        PolygonsSet saaOut = generateGeomagneticMap(wmm, altitude, threshold + saaValidationThreshold, saaValidationWidth);

        CustomEventHandler handler = new CustomEventHandler();
        MagneticFieldDetector detector = new MagneticFieldDetector(threshold, FieldModel.WMM, earth, true).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(initialDate, initialDate.shiftedBy(864000));

        checkEvents(handler.getEvents(), threshold, wmm, true);
        checkSaa(handler.getEvents(), saaIn, saaOut);
    }

    /**
     * Test for the SAA detector based on the IGRF at sea level
     */
    @Test
    public void saaDetectorIGRFSeaLevelTest() {
        initializePropagator();

        double altitude = 0;
        double threshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(32000);

        PolygonsSet saaIn = generateGeomagneticMap(igrf, altitude, threshold - saaValidationThreshold, saaValidationWidth);
        PolygonsSet saaOut = generateGeomagneticMap(igrf, altitude, threshold + saaValidationThreshold, saaValidationWidth);

        CustomEventHandler handler = new CustomEventHandler();
        MagneticFieldDetector detector = new MagneticFieldDetector(threshold, FieldModel.IGRF, earth, true).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(initialDate, initialDate.shiftedBy(864000));

        checkEvents(handler.getEvents(), threshold, igrf, true);
        checkSaa(handler.getEvents(), saaIn, saaOut);
    }

    /**
     * Test for the SAA detector based on the WMM at satellite's altitude.
     */
    @Test
    public void saaDetectorWMMTrueAltitudeTest() {
        initializePropagator();

        double altitude = 600000;
        double threshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(23000);

        PolygonsSet saaIn = generateGeomagneticMap(wmm, altitude, threshold - saaValidationThreshold, saaValidationWidth);
        PolygonsSet saaOut = generateGeomagneticMap(wmm, altitude, threshold + saaValidationThreshold, saaValidationWidth);

        CustomEventHandler handler = new CustomEventHandler();
        MagneticFieldDetector detector = new MagneticFieldDetector(threshold, FieldModel.WMM, earth).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(initialDate, initialDate.shiftedBy(864000));

        checkEvents(handler.getEvents(), threshold, wmm, false);
        checkSaa(handler.getEvents(), saaIn, saaOut);
    }

    /**
     * Test for the SAA detector based on the IGRF at satellite's altitude.
     */
    @Test
    public void saaDetectorIGRFTrueAltitudeTest() {
        initializePropagator();

        double altitude = 600000;
        double threshold = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(23000);

        PolygonsSet saaIn = generateGeomagneticMap(igrf, altitude, threshold - saaValidationThreshold, saaValidationWidth);
        PolygonsSet saaOut = generateGeomagneticMap(igrf, altitude, threshold + saaValidationThreshold, saaValidationWidth);

        CustomEventHandler handler = new CustomEventHandler();
        MagneticFieldDetector detector = new MagneticFieldDetector(threshold, FieldModel.IGRF, earth).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(initialDate, initialDate.shiftedBy(864000));

        checkEvents(handler.getEvents(), threshold, igrf, false);
        checkSaa(handler.getEvents(), saaIn, saaOut);
    }

    /**
     * Build a geographical zone covering the SAA.
     *
     * @param field     the geomagnetic field
     * @param altitude  the considered altitude
     * @param threshold detection threshold for magnetic field
     * @param width     margin for selecting the points on the boundary
     * @return the geographical zone covering the SAA
     */
    private PolygonsSet generateGeomagneticMap(GeoMagneticField field, double altitude, double threshold, double width) {

        //Find a polygon corresponding to the threshold field line
        ArrayList<Double[]> points = new ArrayList<Double[]>();

        for(int latitude = -89; latitude < 90; latitude++) {
            for(int longitude = -179; longitude < 180; longitude++) {

                // Check the magnetic field value at the given altitude for each latitude and longitude, with a 1° step
                double value = field.calculateField(FastMath.toRadians(latitude), FastMath.toRadians(longitude), altitude).getTotalIntensity();

                // add the vertice to the outside polygon
                if (value - threshold > -0.5*width && value - threshold < 0.5*width) {
                    Double[] point = {(double)latitude, (double)longitude};
                    points.add(point);
                }
            }
        }

        //Convert lists into arrays
        double[][] arrayPoints = new double[points.size()][2];

        for(int i = 0; i < points.size(); i++) {
            arrayPoints[i][0] = points.get(i)[0];
            arrayPoints[i][1] = points.get(i)[1];
        }

        //Build the geographical zones
        return buildZone(arrayPoints);
    }

    /**
     * Build a geographical zone with a given list of vertices
     * @param points the list of vertices
     * @return the polygon defining the geographical zone
     */
    private PolygonsSet buildZone(double[][] points) {
        //Convert arrays to a Vector2D list
        final Vector2D[] vertices = new Vector2D[points.length];
        for (int i = 0; i < points.length; ++i) {
            vertices[i] = new Vector2D(FastMath.toRadians(points[i][1]), // points[i][1] is longitude
                                      FastMath.toRadians(points[i][0])); // points[i][0] is latitude
        }

        //Sort the vertices to build the polygon
        final Vector2D[] sortedVertices = sortVertices(vertices);

        //Creates the polygon
        return new PolygonsSet(1.0e-10, sortedVertices);
    }

    /**
     * Sort the vertices to have a almost circular polygon, fitting the SAA shape
     * the vertices are sorted by their angular position around the SAA median point.
     * this angular position is defined by the angle between the vertice and the horizontal vector, regarded from the median point of the polygon
     * This sort is done to creates the polygon with the right order of vertices.
     * @param vertices the unsorted list of vertices
     * @return the sorted list of vertices
     */
    private Vector2D[] sortVertices(Vector2D[] vertices) {

        //Get the median longitude and latitude of SAA

        double midLat = 0;
        double midLon = 0;

        double minLon = vertices[0].getX();
        double maxLon = vertices[0].getX();
        double minLat = vertices[0].getY();
        double maxLat = vertices[0].getY();
        for(int i = 0; i < vertices.length; i++) {
            if(vertices[i].getX() < minLon) minLon = vertices[i].getX();
            if(vertices[i].getX() > maxLon) maxLon = vertices[i].getX();
            if(vertices[i].getY() < minLat) minLat = vertices[i].getY();
            if(vertices[i].getY() > maxLat) maxLat = vertices[i].getY();
        }
        midLat = (minLat + maxLat)/2;
        midLon = (minLon + maxLon)/2;

        Vector2D mid = new Vector2D(midLon, midLat);

        // Convert lon/lat vector in norm/angle defined from the center of the SAA
        ArrayList<Vector2D> angularVerticesList = new ArrayList<Vector2D>();
        Vector2D ref = new Vector2D(1, 0);
        Vector2D angularVertice;
        Vector2D centeredVertice;
        for(int i = 0; i < vertices.length; i++) {
            centeredVertice = vertices[i].subtract(mid);
            double norm = centeredVertice.getNorm();
            double angle = Vector2D.angle(ref, centeredVertice);
            if(centeredVertice.getY() < 0) {
                angle = -angle;
            }
            angularVertice = new Vector2D(norm, angle);
            angularVerticesList.add(angularVertice);
        }

        //Sort the norm/angle vectors by their angle
        Comparator<Vector2D> comparator = new Comparator<Vector2D>() {

            @Override
            public int compare(Vector2D o1, Vector2D o2) {
                return Double.compare(o1.getY(),o2.getY());
            }
        };

        angularVerticesList.sort(comparator);

        //Convert back the norm/angle to lon/lat vectors
        Vector2D[] sortedVertices = new Vector2D[vertices.length];
        Vector2D originalVertice;
        for(int i = 0; i < vertices.length; i++) {
            angularVertice = angularVerticesList.get(i);
            double longitude = angularVertice.getX()*Math.cos(angularVertice.getY());
            double latitude = angularVertice.getX()*Math.sin(angularVertice.getY());
            centeredVertice = new Vector2D(longitude, latitude);
            originalVertice = centeredVertice.add(mid);
            sortedVertices[i] = originalVertice;
        }

        return sortedVertices;
    }

    /**
     * Custom event handler gathering states when events occurred.
     */
    private class CustomEventHandler implements EventHandler {

        ArrayList<SpacecraftState> events = new ArrayList<SpacecraftState>();

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {

            events.add(s);

            return Action.CONTINUE;
        }

        public ArrayList<SpacecraftState> getEvents(){
            return events;
        }
    }

    /**
     * Check that the magnetic field value at the event is equal to the expected value
     * @param events the list of events
     * @param threshold the expected value
     * @param field the magnetic field
     * @param sea if the magnetic field is computed at sea level or at the satellite altitude
     */
    private void checkEvents(ArrayList<SpacecraftState> events, double threshold, GeoMagneticField field, boolean sea) {

        for (SpacecraftState s : events) {
            //Get the geodetic point corresponding to the event
            GeodeticPoint geo = earth.transform(s.getPosition(), s.getFrame(), s.getDate());
            double altitude = geo.getAltitude();
            if (sea) {
                altitude = 0;
            }
            double meas = field.calculateField(geo.getLatitude(), geo.getLongitude(), altitude).getTotalIntensity();
            Assertions.assertEquals(threshold, meas, 1e-12);
        }
    }

    /**
     * Check that the events correspond to the SAA
     * @param events the events to check
     * @param saaIn polygon defining the inside limit of SAA
     * @param saaOut polygon defining the outside limit of SAA
     */
    private void checkSaa(ArrayList<SpacecraftState> events, PolygonsSet saaIn, PolygonsSet saaOut) {

        for (SpacecraftState s : events) {
            //Get the geodetic point corresponding to the event
            GeodeticPoint geo = earth.transform(s.getPosition(itrf), itrf, s.getDate());
            Vector2D point = new Vector2D(geo.getLongitude(), geo.getLatitude());

            //Check that the event is outside the "smaller than SAA" geographical zone
            Assertions.assertTrue(saaIn.checkPoint(point).equals(Location.OUTSIDE));
            //Check that the event is inside the "bigger than SAA" geographical zone
            Assertions.assertTrue(saaOut.checkPoint(point).equals(Location.INSIDE));

        }
    }
}
