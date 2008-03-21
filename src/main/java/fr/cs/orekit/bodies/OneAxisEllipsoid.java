package fr.cs.orekit.bodies;

import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.utils.Line;

/** Modeling of one-axis ellipsoid.

 * <p>One-axis ellipsoids is a good approximate model for most planet-size
 * and larger natural bodies. It is the equilibrium shape reached by
 * a fluid body under its own gravity field when it rotates. The symetry
 * axis is the rotation or polar axis.</p>

 * <p>This class is a simple adaptation of the <a
 * href="http://www.spaceroots.org/documents/distance/Ellipsoid.java">Ellipsoid</a>
 * example class implementing the algorithms described in the paper <a
 * href="http://www.spaceroots.org/documents/distance/distance-to-ellipse.pdf"> Quick
 * computation of the distance between a point and an ellipse</a>.</p>
 *
 * @author Luc Maisonobe
 */
public class OneAxisEllipsoid implements BodyShape {

    /** One third. */
    private static final double ot = 1.0 / 3.0;

    /** Equatorial radius. */
    private final double ae;

    /** Eccentricity power 2. */
    private final double e2;

    /** 1 minus flatness. */
    private final double g;

    /** g * g. */
    private final double g2;

    /** Equatorial radius power 2. */
    private final double ae2;

    /** Convergence limit. */
    private double epsilon;

    /** Convergence limit. */
    private double angularThreshold;

    /** Simple constructor.
     * <p> Freqently used parameters for earth are :
     * <pre>
     * ae = 6378136.460 m
     * f  = 1 / 298.257222101
     * </pre>
     * </p>
     * @param ae earth equatorial radius
     * @param f the flatening ( f = (a-b)/a )
     */
    public OneAxisEllipsoid(double ae, double f) {
        this.ae = ae;
        e2      = f * (2.0 - f);
        g       = 1.0 - f;
        g2      = g * g;
        ae2     = ae * ae;
        setCloseApproachThreshold(1.0e-10);
        setAngularThreshold(1.0e-14);
    }

    /** Set the close approach threshold.
     * <p>The close approach threshold is a ratio used to identify
     * special cases in the {@link #transform(Vector3D)} method.</p>
     * <p>Let d = (x<sup>2</sup>+y<sup>2</sup>+z<sup>2</sup>)<sup>&frac12;</sup>
     * be the distance between the point and the ellipsoid center.</p>
     * <ul>
     *   <li>all points such that d&lt;&epsilon; a<sub>e</sub> where
     *       a<sub>e</sub> is the equatorial radius of the ellipsoid are
     *       considered at the center</li>
     *   <li>all points closer to the surface of the ellipsoid than
     *       &epsilon; d are considered on the surface </li>
     * </ul>
     * <p>If this method is not called, the default value is set to
     * 10<sup>-10</sup>.</p>
     * @param epsilon close approach threshold (no unit)
     */
    public void setCloseApproachThreshold(double epsilon) {
        this.epsilon = epsilon;
    }

    /** Set the angular convergence threshold.
     * <p>The angular threshold is the convergence threshold used to
     * stop the iterations in the {@link #transform(Vector3D)} method.
     * It applies directly to the latitude. When convergence is reached,
     * the real latitude is guaranteed to be between &phi; - &delta;&phi/2
     * and &phi; + &delta;&phi/2 where &phi; is the computed latitude
     * and &delta;&phi is the angular threshold set by this method.</p>
     * <p>If this method is not called, the default value is set to
     * 10<sup>-14</sup>.</p>
     * @param angularThreshold angular convergence threshold (rad)
     */
    public void setAngularThreshold(double angularThreshold) {
        this.angularThreshold = angularThreshold;
    }

    /** Get the equatorial radius of the body.
     * @return equatorial radius of the body (m)
     */
    public double getEquatorialRadius() {
        return ae;
    }

    /** Get the intersection point of a line with the surface of the body.
     * @param line test line in inertial frame (may intersect the body or not)
     * @return intersection point at altitude zero or null if the line does
     * not intersect the surface
     */
    public GeodeticPoint getIntersectionPoint(Line line) {
        // compute some miscellaneous variables outside of the loop
        final Vector3D point    = line.getOrigin();
        final double z          = point.getZ();
        final double z2         = z * z;
        final double r2         = point.getX() * point.getX() + point.getY() * point.getY();
        final double r          = Math.sqrt(r2);
        final double g2r2ma2    = g2 * (r2 - ae2);
        final double g2r2ma2pz2 = g2r2ma2 + z2;

        final Vector3D direction = line.getDirection();
        final double cz =
            Math.sqrt(direction.getX() * direction.getX() + direction.getY() * direction.getY());
        final double sz =
            direction.getZ();

        // distance to the ellipse along the current line
        // as the smallest root of a 2nd degree polynom :
        // a k^2 - 2 b k + c = 0
        final double a  = 1.0 - e2 * cz * cz;
        final double b  = g2 * r * cz + z * sz;
        final double c  = g2r2ma2pz2;
        final double b2 = b * b;
        final double ac = a * c;
        if (b2 < ac) {
            return null;
        }
        final double k  = c / (b + Math.sqrt(b2 - ac));

        final double lambda = Math.atan2(point.getY(), point.getX());
        final double y = z - k * sz;
        final double x = g2 * (r - k * cz);
        final double phi = Math.atan2(y, x);
        return new GeodeticPoint(lambda, phi, 0.0);

    }

    /** Transform a surface-relative point to a cartesian point.
     * @param point surface-relative point
     * @return point at the same location but as a cartesian point
     */
    public Vector3D transform(GeodeticPoint point) {
        final double cPhi = Math.cos(point.latitude);
        final double sPhi = Math.sin(point.latitude);
        final double n    = ae / Math.sqrt(1.0 - e2 * sPhi * sPhi);
        final double r    = (n + point.altitude) * cPhi;
        return new Vector3D(r * Math.cos (point.longitude),
                            r * Math.sin (point.longitude),
                            (g2 * n + point.altitude) * sPhi);
    }

    /** Transform a cartesian point to a surface-relative point.
     * @param point cartesian point
     * @return point at the same location but as a surface-relative point
     */
    public GeodeticPoint transform(Vector3D point) {

        // compute some miscellaneous variables outside of the loop
        final double z          = point.getZ();
        final double z2         = z * z;
        final double r2         = point.getX() * point.getX() + point.getY() * point.getY();
        final double r          = Math.sqrt(r2);
        final double g2r2ma2    = g2 * (r2 - ae2);
        final double g2r2ma2pz2 = g2r2ma2 + z2;
        final double dist       = Math.sqrt(r2 + z2);
        final boolean inside    = g2r2ma2pz2 <= 0;

        // point at the center
        if (dist < (epsilon * ae)) {
            return new GeodeticPoint(0.0, 0.5 * Math.PI, -ae * Math.sqrt(1.0 - e2));
        }

        final double cz = r / dist;
        final double sz = z /dist;
        double t  = z / (dist + r);

        // distance to the ellipse along the current line
        // as the smallest root of a 2nd degree polynom :
        // a k^2 - 2 b k + c = 0
        double a  = 1.0 - e2 * cz * cz;
        double b  = g2 * r * cz + z * sz;
        double c  = g2r2ma2pz2;
        double b2 = b * b;
        final double ac = a * c;
        double k  = c / (b + Math.sqrt(b2 - ac));
        final double lambda = Math.atan2(point.getY(), point.getX());
        double phi    = Math.atan2(z - k * sz, g2 * (r - k * cz));

        // point on the ellipse
        if (Math.abs(k) < (epsilon * dist)) {
            return new GeodeticPoint(lambda, phi, k);
        }

        for (int iterations = 0; iterations < 100; ++iterations) {

            // 4th degree normalized polynom describing
            // circle/ellipse intersections
            // tau^4 + b tau^3 + c tau^2 + d tau + e = 0
            // (there is no need to compute e here)
            a        = g2r2ma2pz2 + g2 * (2.0 * r + k) * k;
            b        = -4.0 * k * z / a;
            c        = 2.0 * (g2r2ma2pz2 + (1.0 + e2) * k * k) / a;
            double d = b;

            // reduce the polynom to degree 3 by removing
            // the already known real root t
            // tau^3 + b tau^2 + c tau + d = 0
            b += t;
            c += t * b;
            d += t * c;

            // find the other real root
            b2       = b * b;
            double Q = (3.0 * c - b2) / 9.0;
            final double R = (b * (9.0 * c - 2.0 * b2) - 27.0 * d) / 54.0;
            final double D = Q * Q * Q + R * R;
            double tildeT;
            double tildePhi;
            if (D >= 0) {
                final double rootD = Math.sqrt(D);
                final double rMr = R - rootD;
                final double rPr = R + rootD;
                // if Java 1.5 is available, the following statement can be rewritten
                // tildeT = Math.cbrt(rPr) + Math.cbrt(rMr) - b * ot;
                tildeT = ((rPr > 0) ?  Math.pow(rPr, ot) : -Math.pow(-rPr, ot)) +
                         ((rMr > 0) ?  Math.pow(rMr, ot) : -Math.pow(-rMr, ot)) -
                         b * ot;
                final double tildeT2   = tildeT * tildeT;
                final double tildeT2P1 = 1.0 + tildeT2;
                tildePhi= Math.atan2(z * tildeT2P1 - 2 * k * tildeT,
                                     g2 * (r * tildeT2P1 - k * (1.0 - tildeT2)));
            } else {
                Q = -Q;
                final double qRoot     = Math.sqrt(Q);
                final double theta     = Math.acos(R / (Q * qRoot));

                // first root based on theta / 3,
                tildeT           = 2.0 * qRoot * Math.cos(theta * ot) - b * ot;
                double tildeT2   = tildeT * tildeT;
                double tildeT2P1 = 1.0 + tildeT2;
                tildePhi         = Math.atan2(z * tildeT2P1 - 2 * k * tildeT,
                                              g2 * (r * tildeT2P1 - k * (1.0 - tildeT2)));
                if ((tildePhi * phi) < 0) {
                    // the first root was on the wrong hemisphere,
                    // try the second root based on (theta + 2PI) / 3
                    tildeT    = 2.0 * qRoot * Math.cos((theta + 2.0 * Math.PI) * ot) - b * ot;
                    tildeT2   = tildeT * tildeT;
                    tildeT2P1 = 1.0 + tildeT2;
                    tildePhi  = Math.atan2(z * tildeT2P1 - 2 * k * tildeT,
                                           g2 * (r * tildeT2P1 - k * (1.0 - tildeT2)));
                    if (tildePhi * phi < 0) {
                        // the second root was on the wrong  hemisphere,
                        // try the third (and last) root based on (theta + 4PI) / 3
                        tildeT    = 2.0 * qRoot * Math.cos((theta + 4.0 * Math.PI) * ot) - b * ot;
                        tildeT2   = tildeT * tildeT;
                        tildeT2P1 = 1.0 + tildeT2;
                        tildePhi  = Math.atan2(z * tildeT2P1 - 2 * k * tildeT,
                                               g2 * (r * tildeT2P1 - k * (1.0 - tildeT2)));
                    }
                }
            }

            // midpoint on the ellipse
            final double dPhi  = Math.abs(0.5 * (tildePhi - phi));
            phi          = 0.5 * (phi + tildePhi);
            final double cPhi  = Math.cos(phi);
            final double sPhi  = Math.sin(phi);
            final double coeff = Math.sqrt(1.0 - e2 * sPhi * sPhi);
            if (dPhi < angularThreshold) {
                // angular convergence reached
                return new GeodeticPoint(lambda, phi,
                                         r * cPhi + z * sPhi - ae * coeff);
            }

            b = ae / coeff;
            final double dR = r - cPhi * b;
            final double dZ = z - sPhi * b * g2;
            k = Math.sqrt(dR * dR + dZ * dZ);
            if (inside) {
                k = -k;
            }
            t = dZ / (k + dR);

        }

        // unable to converge
        throw new RuntimeException("internal error");

    }

}
