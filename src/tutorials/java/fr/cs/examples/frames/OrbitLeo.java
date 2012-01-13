package fr.cs.examples.frames;

import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import fr.cs.examples.Autoconfiguration;


public class OrbitLeo {

    /**
     * @param args
     * @throws OrekitException 
     */
    public static void main(String[] args) throws OrekitException {

        // configure Orekit
        Autoconfiguration.configureOrekit();

        String line1 = "1 29499U 06044A   11327.54166667  .00000000  00000+0  29980-1 0 00014";
        String line2 = "2 29499  98.7337  24.3927 0000335 238.5315 149.4780 14.21454262264373";

        TLE tle = new TLE(line1, line2);

        final AbsoluteDate date = tle.getDate();
        System.out.println(date.toString());

        final double n_TLE  = tle.getMeanMotion();
        System.out.println("n TLE : " + Constants.JULIAN_DAY * n_TLE / MathUtils.TWO_PI);

        final double n_theorique  = 412. / 29.;
        System.out.println("n theorique: " + n_theorique);

        final double mu = 3.986004415E14;
        final double a_TLE  = FastMath.pow(mu / n_TLE / n_TLE, 1./3.);
        System.out.println("a TLE : " + a_TLE);

        final double a_theorique  = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 817.e+3;
        System.out.println("a theorique : " + a_theorique);

        final double e = tle.getE();
        System.out.println("e : " + e);

        final double i = tle.getI();
        System.out.println("i : " + FastMath.toDegrees(i));

        final double pa = tle.getPerigeeArgument();
        System.out.println("pa : " + FastMath.toDegrees(pa));

        final double raan = tle.getRaan();
        System.out.println("raan : " + FastMath.toDegrees(raan));

        final double lM = tle.getMeanAnomaly();
        System.out.println("lM : " + FastMath.toDegrees(lM));

        final double period_theorique = Constants.JULIAN_DAY * 29./412.;
        System.out.println("periode theorique : " + period_theorique);

        final double period_theorique2 = 2. * FastMath.PI * FastMath.sqrt(FastMath.pow(a_theorique, 3.) / mu);;
        System.out.println("periode theorique 2 : " + period_theorique2);

        final double period_TLE = 2. * FastMath.PI * FastMath.sqrt(FastMath.pow(a_TLE, 3.) / mu);
        System.out.println("periode TLE : " + period_TLE);
        
        
    }

}
