package org.orekit.models.earth;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class EarthITU453AtmosphereRefractionTest {

    private final double onehundredth = 1e-2;
    private final double twohundredth = 2e-2;
    private final double onethousandth = 1e-3;
    private final double epsilon = 1e-15;

    // Table (ref. Astronomical Refraction, Michael E. Thomas and Richard I. Joseph)
    //       (JOHNS HOPKINS APL TECHNICAL DIGEST, VOLUME 17, NUMBER 3 (1996))
    // elevation (deg)
    private final double[] ref_elevation  = new double[] {0.00, 0.25, 0.50, 0.75, 1.00, 1.25, 1.50, 1.75, 2.00, 2.25, 2.50, 2.75, 3.00, 4.50, 5.00,
                                                          6.00, 7.00, 8.00, 9.00, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0,
                                                          25.0, 30.0, 35.0, 50.0, 55.0, 60.0, 65.0, 70.0, 80.0, 90.0};
    // refraction correction angle (in arcminutes)
    private final double[] ref_refraction = new double[] {34.5, 31.4, 28.7, 26.4, 24.3, 22.5, 20.9, 19.5, 18.3, 17.2, 16.1, 15.2, 14.4, 10.7, 9.90,
                                                          8.50, 7.40, 6.60, 5.90, 5.30, 4.90, 4.50, 4.10, 3.80, 3.60, 3.30, 3.10, 2.90, 2.80, 2.60,
                                                          2.10, 1.70, 1.40, 0.80, 0.70, 0.60, 0.50, 0.40, 0.20, 0.00};


    // Kiruna-2 ESTRACK Station (Sweden)
    private TopocentricFrame stationk;
    private String namek = "Kiruna-2";
    // Hartebeesthoek IGS Station (South Africa)
    // lowest elevation angle that verify inequality number 11 : theta0 = -1.039 degree;
    private TopocentricFrame stationh;
    private String nameh = "Hartebeesthoek";
    // Everest Fake Station (China/Nepal)
    private TopocentricFrame statione;
    private String namee = "Everest";
    // Dead Sea Fake Station (Israel)
    private TopocentricFrame stationd;
    private String named = "Dead Sea";
    // Altitude0 Fake Station ()
    private TopocentricFrame stationa;
    private String namea = "Alt0";

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data:potential:tides");
        IERSConventions  conventions = IERSConventions.IERS_2010;
        OneAxisEllipsoid earth       = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(conventions, true));

        // Kiruna-2 (Sweden)
        final GeodeticPoint kir = new GeodeticPoint(FastMath.toRadians(67.858428),
                                                   FastMath.toRadians(20.966880),
                                                   385.8);

        // Hartebeesthoek (South Africa)
        final GeodeticPoint har = new GeodeticPoint(FastMath.toRadians(-24.110243),
                                                    FastMath.toRadians(27.685308),
                                                    1415.821);

        // Everest (fake station)
        final GeodeticPoint eve = new GeodeticPoint(FastMath.toRadians(27.988333),
                                                    FastMath.toRadians(86.991944),
                                                    8848.0);

        // Dead Sea (fake station)
        final GeodeticPoint des = new GeodeticPoint(FastMath.toRadians(31.500000),
                                                    FastMath.toRadians(35.500000),
                                                    -422.0);

        // Alt0 (fake station)
        final GeodeticPoint alt = new GeodeticPoint(FastMath.toRadians(31.500000),
                                                    FastMath.toRadians(35.500000),
                                                    0.0);
        stationk = new TopocentricFrame(earth, kir, namek);
        stationh = new TopocentricFrame(earth, har, nameh);
        statione = new TopocentricFrame(earth, eve, namee);
        stationd = new TopocentricFrame(earth, des, named);
        stationa = new TopocentricFrame(earth, alt, namea);
    }


    @Test
    public void testEarthITU453AtmosphereRefractionHighest() throws OrekitException {

        // elevation angle of the space station under free-space propagation conditions
        final double elevation = FastMath.toRadians(2.0);

        // Station altitude
        final double altitude = statione.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        // refraction correction in degrees
        double refraction = FastMath.toDegrees(modelTropo.getRefraction(elevation));
        Assert.assertEquals(0.11458177523385392, refraction, epsilon);
    }

    @Test
    public void testEarthITU453AtmosphereRefractionLowest() throws OrekitException {

        // elevation angle of the space station under free-space propagation conditions
        final double elevation = FastMath.toRadians(2.0);

        // Station altitude
        final double altitude = stationd.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        // refraction correction in degrees
        double refraction = FastMath.toDegrees(modelTropo.getRefraction(elevation));
        Assert.assertEquals(0.3550620274090111, refraction, epsilon);
    }

    @Test
    public void testEarthITU453AtmosphereRefraction2degree() throws OrekitException {

        // elevation angle of the space station under free-space propagation conditions
        final double elevation = FastMath.toRadians(2.0);

        // Station altitude
        final double altitude = stationk.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        // refraction correction in degrees
        final double refraction = FastMath.toDegrees(modelTropo.getRefraction(elevation));
        Assert.assertEquals(refraction, 0.32, onehundredth);

        final double thetamin = FastMath.toDegrees(modelTropo.getThetaMin());
        Assert.assertEquals(-0.5402509318003884, thetamin, epsilon);
        final double theta0 = FastMath.toDegrees(modelTropo.getTheta0());
        Assert.assertEquals(-1.4959064751203384, theta0, epsilon);

    }

    @Test
    public void testEarthITU453AtmosphereRefraction4degree() throws OrekitException {

        // elevation angle of the space station under free-space propagation conditions
        final double elevation = FastMath.toRadians(4.0);

        // Station altitude
        final double altitude = stationk.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        // refraction correction in degrees
        double refraction = FastMath.toDegrees(modelTropo.getRefraction(elevation));
        Assert.assertEquals(0.21, refraction, onehundredth);
    }

    @Test
    public void testEarthITU453AtmosphereRefraction10degree() throws OrekitException {

        // elevation angle of the space station under free-space propagation conditions
        final double elevation = FastMath.toRadians(10.0);

        // Station altitude
        final double altitude = stationk.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        // refraction correction in degrees
        double refraction = FastMath.toDegrees(modelTropo.getRefraction(elevation));
        Assert.assertEquals(0.10, refraction, twohundredth);
    }

    @Test
    public void testEarthITU453AtmosphereRefraction30degree() throws OrekitException {

        // elevation angle of the space station under free-space propagation conditions
        final double elevation = FastMath.toRadians(30.0);

        // Station altitude
        final double altitude = stationk.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        // refraction correction in degrees
        double refraction = FastMath.toDegrees(modelTropo.getRefraction(elevation));
        Assert.assertEquals(0.02, refraction, onehundredth);
    }

    @Test
    public void testEarthITU453AtmosphereRefraction90degree() throws OrekitException {

        // elevation angle of the space station under free-space propagation conditions
        final double elevation = FastMath.toRadians(90.0);

        // Station altitude
        final double altitude = stationk.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        // refraction correction in degrees
        double refraction = FastMath.toDegrees(modelTropo.getRefraction(elevation));
        Assert.assertEquals(0.002, refraction, onethousandth);

    }
    @Test
    public void testEarthITU453AtmosphereRefractionminusdegree() throws OrekitException {

        // elevation angle of the space station under free-space propagation conditions
        final double elevation = FastMath.toRadians(-10.);

        // Station altitude
        final double altitude = stationh.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        // refraction correction in degrees
        double refraction = FastMath.toDegrees(modelTropo.getRefraction(elevation));
        Assert.assertEquals(1.7367073234643113, refraction, onethousandth);
    }

    @Test
    public void testEarthITU453AtmosphereRefractiontable() throws OrekitException {

        // Station altitude
        final double altitude = stationa.getPoint().getAltitude();
        EarthITU453AtmosphereRefraction modelTropo = new EarthITU453AtmosphereRefraction(altitude);

        for (int itab=0; itab<40; itab++) {
            // elevation angle of the space station under free-space propagation conditions
            final double elevation = FastMath.toRadians(ref_elevation[itab]);

            // refraction correction in arcminutes
            final double refraction = 60.0 * FastMath.toDegrees(modelTropo.getRefraction(elevation));
            Assert.assertEquals(ref_refraction[itab], refraction, 2.1);
        }
    }
}
