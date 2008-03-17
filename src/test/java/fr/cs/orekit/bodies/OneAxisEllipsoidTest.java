package fr.cs.orekit.bodies;


import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.Utils;
import fr.cs.orekit.utils.Line;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OneAxisEllipsoidTest extends TestCase {

    public OneAxisEllipsoidTest(String name) {
        super(name);
    }

    public void testOrigin() {
        double ae = 6378137.0;
        checkCartesianToEllipsoidic(ae, 1.0 / 298.257222101,
                                    ae, 0, 0,
                                    0, 0, 0);
    }

    public void testStandard() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    4637885.347, 121344.608, 4362452.869,
                                    0.026157811533131, 0.757987116290729, 260.455572965555);
    }

    public void testLongitudeZero() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    6378400.0, 0, 6379000.0,
                                    0.0, 0.787815771252351, 2653416.77864152);
    }

    public void testLongitudePi() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    -6379999.0, 0, 6379000.0,
                                    3.14159265358979, 0.787690146758403, 2654544.7767725);
    }

    public void testNorthPole() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    0.0, 0.0, 7000000.0,
                                    0.0, 1.57079632679490, 643247.685859644);
    }

    public void testEquator() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    6379888.0, 6377000.0, 0.0,
                                    0.785171775899913, 0.0, 2642345.24279301);
    }

    public void testInside3Roots() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    9219.0, -5322.0, 6056743.0,
                                    5.75963470503781, 1.56905114598949, -300000.009586231);
    }

    public void testInsideLessThan3Roots() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    1366863.0, -789159.0, -5848.988,
                                    -0.523598928689, -0.00380885831963, -4799808.27951);
    }

    public void testOutside() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    5722966.0, -3304156.0, -24621187.0,
                                    5.75958652642615, -1.3089969725151, 19134410.3342696);
    }

    public void testGeoCar() {
        OneAxisEllipsoid model = new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101);
        GeodeticPoint nsp =
            new GeodeticPoint(0.0423149994747243, 0.852479154923577, 111.6);
        Vector3D p = model.transform(nsp);
        assertEquals(4201866.69291890, p.getX(), 1.0e-6);
        assertEquals(177908.184625686, p.getY(), 1.0e-6);
        assertEquals(4779203.64408617, p.getZ(), 1.0e-6);
    }

    public void testLineIntersection() {
        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9);
        Line line = new Line(new Vector3D(0.0, 93.7139699, 3.5930796),
                             new Vector3D(0.0, 1.0, 1.0));
        GeodeticPoint gp = model.getIntersectionPoint(line);
        assertEquals(gp.altitude, 0.0, 1.0e-12);
        assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9);
        line = new Line(new Vector3D(0.0, -93.7139699, -3.5930796),
                        new Vector3D(0.0, -1.0, -1.0));
        gp = model.getIntersectionPoint(line);
        assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9);
        line = new Line(new Vector3D(0.0, -93.7139699, 3.5930796),
                        new Vector3D(0.0, -1.0, 1.0));
        gp = model.getIntersectionPoint(line);
        assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9);
        line = new Line(new Vector3D(-93.7139699, 0.0, 3.5930796),
                        new Vector3D(-1.0, 0.0, 1.0));
        gp = model.getIntersectionPoint(line);
        assertTrue(line.contains(model.transform(gp)));

        line = new Line(new Vector3D(0.0, 0.0, 110),
                        new Vector3D(0.0, 0.0, 1.0));
        gp = model.getIntersectionPoint(line);
        assertEquals(gp.latitude, Math.PI/2, 1.0e-12);
        line = new Line(new Vector3D(0.0, 110, 0),
                        new Vector3D(0.0, 1.0, 0.0));
        gp = model.getIntersectionPoint(line);
        assertEquals(gp.latitude,0, 1.0e-12);

    }

    public void testNoLineIntersection() {
        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9);
        Line line = new Line(new Vector3D(0.0, 93.7139699, 3.5930796),
                             new Vector3D(0.0, 9.0, -2.0));
        assertNull(model.getIntersectionPoint(line));
    }

    private void checkCartesianToEllipsoidic(double ae, double f,
                                             double x, double y, double z,
                                             double longitude, double latitude,
                                             double altitude) {

        OneAxisEllipsoid model = new OneAxisEllipsoid(ae, f);
        GeodeticPoint gp = model.transform(new Vector3D(x, y, z));
        assertEquals(longitude, Utils.trimAngle(gp.longitude, longitude), 1.0e-10);
        assertEquals(latitude,  gp.latitude,  1.0e-10);
        assertEquals(altitude,  gp.altitude,  1.0e-10 * Math.abs(altitude));
    }

    public static Test suite() {
        return new TestSuite(OneAxisEllipsoidTest.class);
    }

}

