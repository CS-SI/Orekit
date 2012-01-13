package fr.cs.examples.frames;

import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import fr.cs.examples.Autoconfiguration;


public class OrbitGeo {

    /**
     * @param args
     * @throws OrekitException 
     */
    public static void main(String[] args) throws OrekitException {

        // configure Orekit
        Autoconfiguration.configureOrekit();


        final Frame inert_frame = FramesFactory.getEME2000();
        final Frame earth_frame = FramesFactory.getITRF2008(false);
        final double mu         =  3.986004415E14;
        final AbsoluteDate date = new AbsoluteDate("2011-10-23T01:47:06.165",
                                                   TimeScalesFactory.getUTC()) ;

        double a  = 42168449.623;
        double ex = -6.66e-5;
        double ey =  2.17e-5;
        double hx =  0.0027459;
        double hy = -0.0015674;
        double l  =  FastMath.toRadians(0.153225665);

        final EquinoctialOrbit georb0 = new EquinoctialOrbit(a, ex, ey, hx, hy, l, PositionAngle.MEAN, inert_frame, date, mu);

        // get longitude
        final double longitud0 = georb0.getPVCoordinates(earth_frame).getPosition().getAlpha();
        System.out.println("longitude = " + longitud0 * 360. / MathUtils.TWO_PI);

        final KeplerianOrbit geork0 = new KeplerianOrbit(georb0);

        // get longitude
        final double longitudk0 = geork0.getPVCoordinates(earth_frame).getPosition().getAlpha();
        System.out.println("longitude = " + longitudk0 * 360. / MathUtils.TWO_PI);

        double aa = geork0.getA();
        double ee = geork0.getE();
        double ii = geork0.getI();
        double pp = geork0.getPerigeeArgument();
        double rr = geork0.getRightAscensionOfAscendingNode();
        double ll = geork0.getMeanAnomaly();
        ll += FastMath.toRadians(57.715);
        
        final KeplerianOrbit geork1 = new KeplerianOrbit(aa, ee, ii, pp, rr, ll, PositionAngle.MEAN, inert_frame, date, mu);

        // get longitude
        final double longitudk1 = geork1.getPVCoordinates(earth_frame).getPosition().getAlpha();
        System.out.println("longitude = " + longitudk1 * 360. / MathUtils.TWO_PI);

        final EquinoctialOrbit georb1 = new EquinoctialOrbit(geork1);

        // get longitude
        final double longitud1 = georb1.getPVCoordinates(earth_frame).getPosition().getAlpha();
        System.out.println("longitude = " + longitud1 * 360. / MathUtils.TWO_PI);
        
        double a1  = georb1.getA();
        double ex1 = georb1.getEquinoctialEx();
        double ey1 = georb1.getEquinoctialEy();
        double hx1 = georb1.getHx();
        double hy1 = georb1.getHy();
        double l1  = georb1.getLM();

        System.out.println("a  = " + a  + " " + a1);
        System.out.println("ex = " + ex + " " + ex1);
        System.out.println("ey = " + ey + " " + ey1);
        System.out.println("hx = " + hx + " " + hx1);
        System.out.println("hy = " + hy + " " + hy1);
        System.out.println("lm = " + FastMath.toDegrees(l)  + " " + FastMath.toDegrees(l1));
        
    }

}
