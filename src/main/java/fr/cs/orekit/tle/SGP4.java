package fr.cs.orekit.tle;

import fr.cs.orekit.errors.OrekitException;

/** This class contains methods to compute propagated coordinates with the SGP4 model.
 *
 * The user should not bother in this class since it is handled internaly by the
 * {@link TLEPropagator}.
 *
 * @author SPACETRACK Report #3 project. Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author Revisiting Spacetrack Report #3. David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (Java translation)
 */
class SGP4 extends TLEPropagator {

    /** If perige is less than 220 km, some calculus are avoided. */
    private boolean lessThan220;

    /** (1 + eta * cos(M0))<sup>3</sup>. */
    private double delM0;

    // CHECKSTYLE: stop JavadocVariable check
    private double d2;
    private double d3;
    private double d4;
    private double t3cof;
    private double t4cof;
    private double t5cof;
    private double sinM0;
    private double omgcof;
    private double xmcof;
    private double c5;
    // CHECKSTYLE: resume JavadocVariable check

    /** Build an instance.
     * @param initialTLE the unique TLE to propagate
     * @exception OrekitException if some specific error occurs
     */
    protected SGP4(final TLE initialTLE) throws OrekitException {
        super(initialTLE);
    }

    /** Initialization proper to each propagator (SGP or SDP).
     */
    protected void sxpInitialize() {

        // For perigee less than 220 kilometers, the equations are truncated to
        // linear variation in sqrt a and quadratic variation in mean anomaly.
        // Also, the c3 term, the delta omega term, and the delta m term are dropped.
        lessThan220 = perige < 220;
        if (!lessThan220) {
            final double c1sq = c1 * c1;
            delM0 = 1.0 + eta * Math.cos(tle.getMeanAnomaly());
            delM0 *= delM0 * delM0;
            d2 = 4 * a0dp * tsi * c1sq;
            final double temp = d2 * tsi * c1 / 3.0;
            d3 = (17 * a0dp + s4) * temp;
            d4 = 0.5 * temp * a0dp * tsi * (221 * a0dp + 31 * s4) * c1;
            t3cof = d2 + 2 * c1sq;
            t4cof = 0.25 * (3 * d3 + c1 * (12 * d2 + 10 * c1sq));
            t5cof = 0.2 * (3 * d4 + 12 * c1 * d3 + 6 * d2 * d2 + 15 * c1sq * (2 * d2 + c1sq));
            sinM0 = Math.sin(tle.getMeanAnomaly());
            if (tle.getE() < 1e-4) {
                omgcof = 0.;
                xmcof = 0.;
            } else  {
                final double c3 = coef * tsi * TLEConstants.A3OVK2 * xn0dp *
                                  TLEConstants.NORMALIZED_EQUATORIAL_RADIUS * sini0 / tle.getE();
                xmcof = -TLEConstants.TWO_THIRD * coef * tle.getBStar() *
                        TLEConstants.NORMALIZED_EQUATORIAL_RADIUS / eeta;
                omgcof = tle.getBStar() * c3 * Math.cos(tle.getPerigeeArgument());
            }
        }

        c5 = 2 * coef1 * a0dp * beta02 * (1 + 2.75 * (etasq + eeta) + eeta * etasq);
        // initialized
    }

    /** Propagation proper to each propagator (SGP or SDP).
     * @param tSince the offset from initial epoch (min)
     */
    protected void sxpPropagate(final double tSince) {

        // Update for secular gravity and atmospheric drag.
        final double xmdf = tle.getMeanAnomaly() + xmdot * tSince;
        final double omgadf = tle.getPerigeeArgument() + omgdot * tSince;
        final double xn0ddf = tle.getRaan() + xnodot * tSince;
        omega = omgadf;
        double xmp = xmdf;
        final double tsq = tSince * tSince;
        xnode = xn0ddf + xnodcf * tsq;
        double tempa = 1 - c1 * tSince;
        double tempe = tle.getBStar() * c4 * tSince;
        double templ = t2cof * tsq;

        if (!lessThan220) {
            final double delomg = omgcof * tSince;
            double delm = 1. + eta * Math.cos(xmdf);
            delm = xmcof * (delm * delm * delm - delM0);
            final double temp = delomg + delm;
            xmp = xmdf + temp;
            omega = omgadf - temp;
            final double tcube = tsq * tSince;
            final double tfour = tSince * tcube;
            tempa = tempa - d2 * tsq - d3 * tcube - d4 * tfour;
            tempe = tempe + tle.getBStar() * c5 * (Math.sin(xmp) - sinM0);
            templ = templ + t3cof * tcube + tfour * (t4cof + tSince * t5cof);
        }

        a = a0dp * tempa * tempa;
        e = tle.getE() - tempe;

        // A highly arbitrary lower limit on e,  of 1e-6:
        if (e < 1e-6) {
            e = 1e-6;
        }

        xl = xmp + omega + xnode + xn0dp * templ;

        i = tle.getI();

    }

}
