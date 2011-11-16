package org.orekit.propagation.semianalytical.dsst.coefficients;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.util.ArithmeticUtils;
import org.apache.commons.math.util.FastMath;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory.MNSKey;

/**
 * Compute the &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficient evaluated at &gamma;
 * 
 * @author Romain Di Costanzo
 */
public class GammaMsnCoefficients {

    /** &Gamma; */
    private double      gamma;

    /** I = +1 for a prograde orbit, -1 otherwise */
    private int         I;

    /** Result map */
    Map<MNSKey, Double> map = new TreeMap<MNSKey, Double>();

    /**
     * Default constructor
     * 
     * @param gamma
     *            &gamma;
     * @param I
     *            orbit representation : I = +1 for a prograde orbit, -1 otherwise
     */
    public GammaMsnCoefficients(final double gamma,
                                final int I) {
        this.gamma = gamma;
        this.I = I;
    }

    /**
     * &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficient from equations 2.7.1 - (13)
     * 
     * @param n
     *            n
     * @param s
     *            s
     * @param m
     *            m
     * @return &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;)
     */
    public double getGammaMsn(final int n,
                              final int s,
                              final int m) {
        double res = 0d;
        if (map.containsKey(new MNSKey(m, n, s))) {
            res = map.get(new MNSKey(m, n, s));
        } else {
            double num;
            double den;
            if (s <= -m) {
                res = FastMath.pow(-1, m - s) * FastMath.pow(2, s) * FastMath.pow((1 + I * gamma), -I * m);
            } else if (FastMath.abs(s) <= m) {
                num = FastMath.pow(-1, m - s) * FastMath.pow(2, -m) * ArithmeticUtils.factorial(n + m) * ArithmeticUtils.factorial(n - m)
                                * FastMath.pow(1 + I * gamma, I * s);
                den = ArithmeticUtils.factorial(n + s) * ArithmeticUtils.factorial(n - s);
                res = num / den;
            } else if (s >= m) {
                res = FastMath.pow(2, -s) * FastMath.pow(1 + I * gamma, I * m);
            }
            map.put(new MNSKey(m, n, s), res);
        }
        return res;
    }

    /**
     * d&Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) / d&gamma; coefficient from equations 2.7.1 -
     * (13)
     * 
     * @param n
     *            n
     * @param s
     *            s
     * @param m
     *            m
     * @return d&Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) / d&gamma;
     */
    public double getDGammaMsn(final int n,
                               final int s,
                               final int m) {
        double res = 0d;
        if (s <= -m) {
            res = -m * getGammaMsn(n, s, m) / (1 + I * gamma);
        } else if (FastMath.abs(s) <= m) {
            res = s * getGammaMsn(n, s, m) / (1 + I * gamma);
        } else if (s >= m) {
            res = m * getGammaMsn(n, s, m) / (1 + I * gamma);
        }
        return res;
    }

}
