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
package org.orekit.bodies;

import java.io.Serializable;
import java.util.function.DoubleFunction;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/** Modeling of a one-axis ellipsoid.

 * <p>One-axis ellipsoids is a good approximate model for most planet-size
 * and larger natural bodies. It is the equilibrium shape reached by
 * a fluid body under its own gravity field when it rotates. The symmetry
 * axis is the rotation or polar axis.</p>

 * @author Luc Maisonobe
 * @author Guylaine Prat
 */
public class OneAxisEllipsoid extends Ellipsoid implements BodyShape {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130518L;

    /** Threshold for polar and equatorial points detection. */
    private static final double ANGULAR_THRESHOLD = 1.0e-4;

    /** Body frame related to body shape. */
    private final Frame bodyFrame;

    /** Equatorial radius power 2. */
    private final double ae2;

    /** Polar radius power 2. */
    private final double ap2;

    /** Flattening. */
    private final double f;

    /** Eccentricity. */
    private final double e;

    /** Eccentricity squared. */
    private final double e2;

    /** 1 minus flatness. */
    private final double g;

    /** g squared. */
    private final double g2;

    /** Convergence limit. */
    private double angularThreshold;

    /** Simple constructor.
     * <p>Standard values for Earth models can be found in the {@link org.orekit.utils.Constants Constants} class:</p>
     * <table border="1" style="background-color:#f5f5dc;">
     * <caption>Ellipsoid Models</caption>
     * <tr style="background-color:#c9d5c9;"><th>model</th><th>a<sub>e</sub> (m)</th> <th>f</th></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">GRS 80</td>
     *     <td>{@link org.orekit.utils.Constants#GRS80_EARTH_EQUATORIAL_RADIUS Constants.GRS80_EARTH_EQUATORIAL_RADIUS}</td>
     *     <td>{@link org.orekit.utils.Constants#GRS80_EARTH_FLATTENING Constants.GRS80_EARTH_FLATTENING}</td></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">WGS84</td>
     *     <td>{@link org.orekit.utils.Constants#WGS84_EARTH_EQUATORIAL_RADIUS Constants.WGS84_EARTH_EQUATORIAL_RADIUS}</td>
     *     <td>{@link org.orekit.utils.Constants#WGS84_EARTH_FLATTENING Constants.WGS84_EARTH_FLATTENING}</td></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">IERS96</td>
     *     <td>{@link org.orekit.utils.Constants#IERS96_EARTH_EQUATORIAL_RADIUS Constants.IERS96_EARTH_EQUATORIAL_RADIUS}</td>
     *     <td>{@link org.orekit.utils.Constants#IERS96_EARTH_FLATTENING Constants.IERS96_EARTH_FLATTENING}</td></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">IERS2003</td>
     *     <td>{@link org.orekit.utils.Constants#IERS2003_EARTH_EQUATORIAL_RADIUS Constants.IERS2003_EARTH_EQUATORIAL_RADIUS}</td>
     *     <td>{@link org.orekit.utils.Constants#IERS2003_EARTH_FLATTENING Constants.IERS2003_EARTH_FLATTENING}</td></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">IERS2010</td>
     *     <td>{@link org.orekit.utils.Constants#IERS2010_EARTH_EQUATORIAL_RADIUS Constants.IERS2010_EARTH_EQUATORIAL_RADIUS}</td>
     *     <td>{@link org.orekit.utils.Constants#IERS2010_EARTH_FLATTENING Constants.IERS2010_EARTH_FLATTENING}</td></tr>
     * </table>
     * @param ae equatorial radius
     * @param f the flattening (f = (a-b)/a)
     * @param bodyFrame body frame related to body shape
     * @see org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean)
     */
    public OneAxisEllipsoid(final double ae, final double f,
                            final Frame bodyFrame) {
        super(bodyFrame, ae, ae, ae * (1.0 - f));
        this.f    = f;
        this.ae2  = ae * ae;
        this.e2   = f * (2.0 - f);
        this.e    = FastMath.sqrt(e2);
        this.g    = 1.0 - f;
        this.g2   = g * g;
        this.ap2  = ae2 * g2;
        setAngularThreshold(1.0e-12);
        this.bodyFrame = bodyFrame;
    }

    /** Set the angular convergence threshold.
     * <p>The angular threshold is used both to identify points close to
     * the ellipse axes and as the convergence threshold used to
     * stop the iterations in the {@link #transform(Vector3D, Frame,
     * AbsoluteDate)} method.</p>
     * <p>If this method is not called, the default value is set to
     * 10<sup>-12</sup>.</p>
     * @param angularThreshold angular convergence threshold (rad)
     */
    public void setAngularThreshold(final double angularThreshold) {
        this.angularThreshold = angularThreshold;
    }

    /** Get the equatorial radius of the body.
     * @return equatorial radius of the body (m)
     */
    public double getEquatorialRadius() {
        return getA();
    }

    /** Get the flattening of the body: f = (a-b)/a.
     * @return the flattening
     */
    public double getFlattening() {
        return f;
    }

    /** Get the first eccentricity squared of the ellipsoid: e^2 = f * (2.0 - f).
     * @return the eccentricity squared
     */
    public double getEccentricitySquared() {
        return e2;
    }

    /** Get the first eccentricity of the ellipsoid: e = sqrt(f * (2.0 - f)).
     * @return the eccentricity
     */
    public double getEccentricity() {
        return e;
    }

    /** {@inheritDoc} */
    public Frame getBodyFrame() {
        return bodyFrame;
    }

    /** Get the intersection point of a line with the surface of the body.
     * <p>A line may have several intersection points with a closed
     * surface (we consider the one point case as a degenerated two
     * points case). The close parameter is used to select which of
     * these points should be returned. The selected point is the one
     * that is closest to the close point.</p>
     * @param line test line (may intersect the body or not)
     * @param close point used for intersections selection
     * @param frame frame in which line is expressed
     * @param date date of the line in given frame
     * @return intersection point at altitude zero or null if the line does
     * not intersect the surface
     * @since 9.3
     */
    public Vector3D getCartesianIntersectionPoint(final Line line, final Vector3D close,
                                                  final Frame frame, final AbsoluteDate date) {

        // transform line and close to body frame
        final StaticTransform frameToBodyFrame =
                frame.getStaticTransformTo(bodyFrame, date);
        final Line lineInBodyFrame = frameToBodyFrame.transformLine(line);

        // compute some miscellaneous variables
        final Vector3D point    = lineInBodyFrame.getOrigin();
        final double x          = point.getX();
        final double y          = point.getY();
        final double z          = point.getZ();
        final double z2         = z * z;
        final double r2         = x * x + y * y;

        final Vector3D direction = lineInBodyFrame.getDirection();
        final double dx         = direction.getX();
        final double dy         = direction.getY();
        final double dz         = direction.getZ();
        final double cz2        = dx * dx + dy * dy;

        // abscissa of the intersection as a root of a 2nd degree polynomial :
        // a k^2 - 2 b k + c = 0
        final double a  = 1.0 - e2 * cz2;
        final double b  = -(g2 * (x * dx + y * dy) + z * dz);
        final double c  = g2 * (r2 - ae2) + z2;
        final double b2 = b * b;
        final double ac = a * c;
        if (b2 < ac) {
            return null;
        }
        final double s  = FastMath.sqrt(b2 - ac);
        final double k1 = (b < 0) ? (b - s) / a : c / (b + s);
        final double k2 = c / (a * k1);

        // select the right point
        final Vector3D closeInBodyFrame = frameToBodyFrame.transformPosition(close);
        final double   closeAbscissa    = lineInBodyFrame.getAbscissa(closeInBodyFrame);
        final double k =
            (FastMath.abs(k1 - closeAbscissa) < FastMath.abs(k2 - closeAbscissa)) ? k1 : k2;
        return lineInBodyFrame.pointAt(k);

    }

    /** {@inheritDoc} */
    public GeodeticPoint getIntersectionPoint(final Line line, final Vector3D close,
                                              final Frame frame, final AbsoluteDate date) {

        final Vector3D intersection = getCartesianIntersectionPoint(line, close, frame, date);
        if (intersection == null) {
            return null;
        }
        final double ix = intersection.getX();
        final double iy = intersection.getY();
        final double iz = intersection.getZ();

        final double lambda = FastMath.atan2(iy, ix);
        final double phi    = FastMath.atan2(iz, g2 * FastMath.sqrt(ix * ix + iy * iy));
        return new GeodeticPoint(phi, lambda, 0.0);

    }

    /** Get the intersection point of a line with the surface of the body.
     * <p>A line may have several intersection points with a closed
     * surface (we consider the one point case as a degenerated two
     * points case). The close parameter is used to select which of
     * these points should be returned. The selected point is the one
     * that is closest to the close point.</p>
     * @param line test line (may intersect the body or not)
     * @param close point used for intersections selection
     * @param frame frame in which line is expressed
     * @param date date of the line in given frame
     * @param <T> type of the field elements
     * @return intersection point at altitude zero or null if the line does
     * not intersect the surface
     * @since 9.3
     */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getCartesianIntersectionPoint(final FieldLine<T> line,
                                                                                          final FieldVector3D<T> close,
                                                                                          final Frame frame,
                                                                                          final FieldAbsoluteDate<T> date) {

        // transform line and close to body frame
        final FieldStaticTransform<T> frameToBodyFrame = frame.getStaticTransformTo(bodyFrame, date);
        final FieldLine<T>            lineInBodyFrame  = frameToBodyFrame.transformLine(line);

        // compute some miscellaneous variables
        final FieldVector3D<T> point = lineInBodyFrame.getOrigin();
        final T x  = point.getX();
        final T y  = point.getY();
        final T z  = point.getZ();
        final T z2 = z.multiply(z);
        final T r2 = x.multiply(x).add(y.multiply(y));

        final FieldVector3D<T> direction = lineInBodyFrame.getDirection();
        final T dx  = direction.getX();
        final T dy  = direction.getY();
        final T dz  = direction.getZ();
        final T cz2 = dx.multiply(dx).add(dy.multiply(dy));

        // abscissa of the intersection as a root of a 2nd degree polynomial :
        // a k^2 - 2 b k + c = 0
        final T a  = cz2.multiply(e2).subtract(1.0).negate();
        final T b  = x.multiply(dx).add(y.multiply(dy)).multiply(g2).add(z.multiply(dz)).negate();
        final T c  = r2.subtract(ae2).multiply(g2).add(z2);
        final T b2 = b.multiply(b);
        final T ac = a.multiply(c);
        if (b2.getReal() < ac.getReal()) {
            return null;
        }
        final T s  = b2.subtract(ac).sqrt();
        final T k1 = (b.getReal() < 0) ? b.subtract(s).divide(a) : c.divide(b.add(s));
        final T k2 = c.divide(a.multiply(k1));

        // select the right point
        final FieldVector3D<T>  closeInBodyFrame = frameToBodyFrame.transformPosition(close);
        final T                 closeAbscissa    = lineInBodyFrame.getAbscissa(closeInBodyFrame);
        final T k = (FastMath.abs(k1.getReal() - closeAbscissa.getReal()) < FastMath.abs(k2.getReal() - closeAbscissa.getReal())) ?
                    k1 : k2;
        return lineInBodyFrame.pointAt(k);
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> getIntersectionPoint(final FieldLine<T> line,
                                                                                      final FieldVector3D<T> close,
                                                                                      final Frame frame,
                                                                                      final FieldAbsoluteDate<T> date) {

        final FieldVector3D<T> intersection = getCartesianIntersectionPoint(line, close, frame, date);
        if (intersection == null) {
            return null;
        }
        final T ix = intersection.getX();
        final T iy = intersection.getY();
        final T iz = intersection.getZ();

        final T lambda = iy.atan2(ix);
        final T phi    = iz.atan2(ix.multiply(ix).add(iy.multiply(iy)).sqrt().multiply(g2));
        return new FieldGeodeticPoint<>(phi, lambda, phi.getField().getZero());

    }

    /** {@inheritDoc} */
    public Vector3D transform(final GeodeticPoint point) {
        final double longitude = point.getLongitude();
        final SinCos scLambda  = FastMath.sinCos(longitude);
        final double latitude  = point.getLatitude();
        final SinCos scPhi     = FastMath.sinCos(latitude);
        final double h         = point.getAltitude();
        final double n         = getA() / FastMath.sqrt(1.0 - e2 * scPhi.sin() * scPhi.sin());
        final double r         = (n + h) * scPhi.cos();
        return new Vector3D(r * scLambda.cos(), r * scLambda.sin(), (g2 * n + h) * scPhi.sin());
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> transform(final FieldGeodeticPoint<T> point) {

        final T latitude  = point.getLatitude();
        final T longitude = point.getLongitude();
        final T altitude  = point.getAltitude();

        final FieldSinCos<T> scLambda = FastMath.sinCos(longitude);
        final FieldSinCos<T> scPhi    = FastMath.sinCos(latitude);
        final T cLambda = scLambda.cos();
        final T sLambda = scLambda.sin();
        final T cPhi    = scPhi.cos();
        final T sPhi    = scPhi.sin();
        final T n       = sPhi.multiply(sPhi).multiply(e2).subtract(1.0).negate().sqrt().reciprocal().multiply(getA());
        final T r       = n.add(altitude).multiply(cPhi);

        return new FieldVector3D<>(r.multiply(cLambda),
                                   r.multiply(sLambda),
                                   sPhi.multiply(altitude.add(n.multiply(g2))));
    }

    /** {@inheritDoc} */
    public Vector3D projectToGround(final Vector3D point, final AbsoluteDate date, final Frame frame) {

        // transform point to body frame
        final StaticTransform toBody = frame.getStaticTransformTo(bodyFrame, date);
        final Vector3D   p         = toBody.transformPosition(point);
        final double     z         = p.getZ();
        final double     r         = FastMath.hypot(p.getX(), p.getY());

        // set up the 2D meridian ellipse
        final Ellipse meridian = new Ellipse(Vector3D.ZERO,
                                             r == 0 ? Vector3D.PLUS_I : new Vector3D(p.getX() / r, p.getY() / r, 0),
                                             Vector3D.PLUS_K,
                                             getA(), getC(), bodyFrame);

        // find the closest point in the meridian plane
        final Vector3D groundPoint = meridian.toSpace(meridian.projectToEllipse(new Vector2D(r, z)));

        // transform point back to initial frame
        return toBody.getInverse().transformPosition(groundPoint);

    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates projectToGround(final TimeStampedPVCoordinates pv, final Frame frame) {

        // transform point to body frame
        final Transform                toBody        = frame.getTransformTo(bodyFrame, pv.getDate());
        final TimeStampedPVCoordinates pvInBodyFrame = toBody.transformPVCoordinates(pv);
        final Vector3D                 p             = pvInBodyFrame.getPosition();
        final double                   r             = FastMath.hypot(p.getX(), p.getY());

        // set up the 2D ellipse corresponding to first principal curvature along meridian
        final Vector3D meridian = r == 0 ? Vector3D.PLUS_I : new Vector3D(p.getX() / r, p.getY() / r, 0);
        final Ellipse firstPrincipalCurvature =
                new Ellipse(Vector3D.ZERO, meridian, Vector3D.PLUS_K, getA(), getC(), bodyFrame);

        // project coordinates in the meridian plane
        final TimeStampedPVCoordinates gpFirst = firstPrincipalCurvature.projectToEllipse(pvInBodyFrame);
        final Vector3D                 gpP     = gpFirst.getPosition();
        final double                   gr      = MathArrays.linearCombination(gpP.getX(), meridian.getX(),
                                                                              gpP.getY(), meridian.getY());
        final double                   gz      = gpP.getZ();

        // topocentric frame
        final Vector3D east   = new Vector3D(-meridian.getY(), meridian.getX(), 0);
        final Vector3D zenith = new Vector3D(gr * getC() / getA(), meridian, gz * getA() / getC(), Vector3D.PLUS_K).normalize();
        final Vector3D north  = Vector3D.crossProduct(zenith, east);

        // set up the ellipse corresponding to second principal curvature in the zenith/east plane
        final Ellipse secondPrincipalCurvature  = getPlaneSection(gpP, north);
        final TimeStampedPVCoordinates gpSecond = secondPrincipalCurvature.projectToEllipse(pvInBodyFrame);

        final Vector3D gpV = gpFirst.getVelocity().add(gpSecond.getVelocity());
        final Vector3D gpA = gpFirst.getAcceleration().add(gpSecond.getAcceleration());

        // moving projected point
        final TimeStampedPVCoordinates groundPV =
                new TimeStampedPVCoordinates(pv.getDate(), gpP, gpV, gpA);

        // transform moving projected point back to initial frame
        return toBody.getInverse().transformPVCoordinates(groundPV);

    }

    /** {@inheritDoc}
     * <p>
     * This method is based on Toshio Fukushima's algorithm which uses Halley's method.
     * <a href="https://www.researchgate.net/publication/227215135_Transformation_from_Cartesian_to_Geodetic_Coordinates_Accelerated_by_Halley's_Method">
     * transformation from Cartesian to Geodetic Coordinates Accelerated by Halley's Method</a>,
     * Toshio Fukushima, Journal of Geodesy 9(12):689-693, February 2006
     * </p>
     * <p>
     * Some changes have been added to the original method:
     * </p>
     * <ul>
     *   <li>in order to handle more accurately corner cases near the pole</li>
     *   <li>in order to handle properly corner cases near the equatorial plane, even far inside the ellipsoid</li>
     *   <li>in order to handle very flat ellipsoids</li>
     * </ul>
     * <p>
     * In some rare cases (for example very flat ellipsoid, or points close to ellipsoid center), the loop
     * may fail to converge. As this seems to happen only in degenerate cases, a design choice was to return
     * an approximate point corresponding to last iteration. This point may be incorrect and fail to give the
     * initial point back if doing roundtrip by calling {@link #transform(GeodeticPoint)}. This design choice
     * was made to avoid NaNs appearing for example in inter-satellites visibility checks when two satellites
     * are almost on opposite sides of Earth. The intermediate points far within the Earth should not prevent
     * the detection algorithm to find visibility start/end.
     * </p>
     */
    public GeodeticPoint transform(final Vector3D point, final Frame frame, final AbsoluteDate date) {

        // transform point to body frame
        final Vector3D pointInBodyFrame = frame.getStaticTransformTo(bodyFrame, date)
                .transformPosition(point);
        final double   r2               = pointInBodyFrame.getX() * pointInBodyFrame.getX() +
                                          pointInBodyFrame.getY() * pointInBodyFrame.getY();
        final double   r                = FastMath.sqrt(r2);
        final double   z                = pointInBodyFrame.getZ();

        final double   lambda           = FastMath.atan2(pointInBodyFrame.getY(), pointInBodyFrame.getX());

        double h;
        double phi;
        if (r <= ANGULAR_THRESHOLD * FastMath.abs(z)) {
            // the point is almost on the polar axis, approximate the ellipsoid with
            // the osculating sphere whose center is at evolute cusp along polar axis
            final double osculatingRadius = ae2 / getC();
            final double evoluteCuspZ     = FastMath.copySign(getA() * e2 / g, -z);
            final double deltaZ           = z - evoluteCuspZ;
            // we use π/2 - atan(r/Δz) instead of atan(Δz/r) for accuracy purposes, as r is much smaller than Δz
            phi = FastMath.copySign(0.5 * FastMath.PI - FastMath.atan(r / FastMath.abs(deltaZ)), deltaZ);
            h   = FastMath.hypot(deltaZ, r) - osculatingRadius;
        } else if (FastMath.abs(z) <= ANGULAR_THRESHOLD * r) {
            // the point is almost on the major axis

            final double osculatingRadius = ap2 / getA();
            final double evoluteCuspR     = getA() * e2;
            final double deltaR           = r - evoluteCuspR;
            if (deltaR >= 0) {
                // the point is outside of the ellipse evolute, approximate the ellipse
                // with the osculating circle whose center is at evolute cusp along major axis
                phi = (deltaR == 0) ? 0.0 : FastMath.atan(z / deltaR);
                h   = FastMath.hypot(deltaR, z) - osculatingRadius;
            } else {
                // the point is on the part of the major axis within ellipse evolute
                // we can compute the closest ellipse point analytically, and it is NOT near the equator
                final double rClose = r / e2;
                final double zClose = FastMath.copySign(g * FastMath.sqrt(ae2 - rClose * rClose), z);
                phi = FastMath.atan((zClose - z) / (rClose - r));
                h   = -FastMath.hypot(r - rClose, z - zClose);
            }

        } else {
            // use Toshio Fukushima method, with several iterations
            final double epsPhi = 1.0e-15;
            final double epsH   = 1.0e-14 * FastMath.max(getA(), FastMath.sqrt(r2 + z * z));
            final double c     = getA() * e2;
            final double absZ  = FastMath.abs(z);
            final double zc    = g * absZ;
            double sn  = absZ;
            double sn2 = sn * sn;
            double cn  = g * r;
            double cn2 = cn * cn;
            double an2 = cn2 + sn2;
            double an  = FastMath.sqrt(an2);
            double bn  = 0;
            phi = Double.POSITIVE_INFINITY;
            h   = Double.POSITIVE_INFINITY;
            for (int i = 0; i < 1000; ++i) {
                // this usually converges in 2 iterations, but in rare cases it can take much more
                // see https://gitlab.orekit.org/orekit/orekit/-/issues/1224 for examples
                // with points near Earth center which need 137 iterations for the first example
                // and 1150 iterations for the second example
                final double oldSn  = sn;
                final double oldCn  = cn;
                final double oldPhi = phi;
                final double oldH   = h;
                final double an3    = an2 * an;
                final double csncn  = c * sn * cn;
                bn    = 1.5 * csncn * ((r * sn - zc * cn) * an - csncn);
                sn    = (zc * an3 + c * sn2 * sn) * an3 - bn * sn;
                cn    = (r  * an3 - c * cn2 * cn) * an3 - bn * cn;
                if (sn * oldSn < 0 || cn < 0) {
                    // the Halley iteration went too far, we restrict it and iterate again
                    while (sn * oldSn < 0 || cn < 0) {
                        sn = (sn + oldSn) / 2;
                        cn = (cn + oldCn) / 2;
                    }
                } else {

                    // rescale components to avoid overflow when several iterations are used
                    final int exp = (FastMath.getExponent(sn) + FastMath.getExponent(cn)) / 2;
                    sn = FastMath.scalb(sn, -exp);
                    cn = FastMath.scalb(cn, -exp);

                    sn2 = sn * sn;
                    cn2 = cn * cn;
                    an2 = cn2 + sn2;
                    an  = FastMath.sqrt(an2);

                    final double cc = g * cn;
                    h = (r * cc + absZ * sn - getA() * g * an) / FastMath.sqrt(an2 - e2 * cn2);
                    if (FastMath.abs(oldH   - h)   < epsH) {
                        phi = FastMath.copySign(FastMath.atan(sn / cc), z);
                        if (FastMath.abs(oldPhi - phi) < epsPhi) {
                            break;
                        }
                    }

                }

            }

            if (Double.isInfinite(phi)) {
                // we did not converge, the point is probably within the ellipsoid
                // we just compute the "best" phi we can to avoid NaN
                phi = FastMath.copySign(FastMath.atan(sn / (g * cn)), z);
            }

        }

        return new GeodeticPoint(phi, lambda, h);

    }

    /** {@inheritDoc}
     * <p>
     * This method is based on Toshio Fukushima's algorithm which uses Halley's method.
     * <a href="https://www.researchgate.net/publication/227215135_Transformation_from_Cartesian_to_Geodetic_Coordinates_Accelerated_by_Halley's_Method">
     * transformation from Cartesian to Geodetic Coordinates Accelerated by Halley's Method</a>,
     * Toshio Fukushima, Journal of Geodesy 9(12):689-693, February 2006
     * </p>
     * <p>
     * Some changes have been added to the original method:
     * <ul>
     *   <li>in order to handle more accurately corner cases near the pole</li>
     *   <li>in order to handle properly corner cases near the equatorial plane, even far inside the ellipsoid</li>
     *   <li>in order to handle very flat ellipsoids</li>
     * </ul>
     * <p>
     * In some rare cases (for example very flat ellipsoid, or points close to ellipsoid center), the loop
     * may fail to converge. As this seems to happen only in degenerate cases, a design choice was to return
     * an approximate point corresponding to last iteration. This point may be incorrect and fail to give the
     * initial point back if doing roundtrip by calling {@link #transform(GeodeticPoint)}. This design choice
     * was made to avoid NaNs appearing for example in inter-satellites visibility checks when two satellites
     * are almost on opposite sides of Earth. The intermediate points far within the Earth should not prevent
     * the detection algorithm to find visibility start/end.
     * </p>
     */
    public <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> transform(final FieldVector3D<T> point,
                                                                           final Frame frame,
                                                                           final FieldAbsoluteDate<T> date) {

        // transform point to body frame
        final FieldVector3D<T> pointInBodyFrame = (frame == bodyFrame) ?
                                                  point :
                                                  frame.getStaticTransformTo(bodyFrame, date).transformPosition(point);
        final T   r2                            = pointInBodyFrame.getX().multiply(pointInBodyFrame.getX()).
                                                  add(pointInBodyFrame.getY().multiply(pointInBodyFrame.getY()));
        final T   r                             = r2.sqrt();
        final T   z                             = pointInBodyFrame.getZ();

        final T   lambda                        = pointInBodyFrame.getY().atan2(pointInBodyFrame.getX());

        T h;
        T phi;
        if (r.getReal() <= ANGULAR_THRESHOLD * FastMath.abs(z.getReal())) {
            // the point is almost on the polar axis, approximate the ellipsoid with
            // the osculating sphere whose center is at evolute cusp along polar axis
            final double osculatingRadius = ae2 / getC();
            final double evoluteCuspZ     = FastMath.copySign(getA() * e2 / g, -z.getReal());
            final T      deltaZ           = z.subtract(evoluteCuspZ);
            // we use π/2 - atan(r/Δz) instead of atan(Δz/r) for accuracy purposes, as r is much smaller than Δz
            phi = r.divide(deltaZ.abs()).atan().negate().add(r.getPi().multiply(0.5)).copySign(deltaZ);
            h   = deltaZ.hypot(r).subtract(osculatingRadius);
        } else if (FastMath.abs(z.getReal()) <= ANGULAR_THRESHOLD * r.getReal()) {
            // the point is almost on the major axis

            final double osculatingRadius = ap2 / getA();
            final double evoluteCuspR     = getA() * e2;
            final T      deltaR           = r.subtract(evoluteCuspR);
            if (deltaR.getReal() >= 0) {
                // the point is outside of the ellipse evolute, approximate the ellipse
                // with the osculating circle whose center is at evolute cusp along major axis
                phi = (deltaR.getReal() == 0) ? z.getField().getZero() : z.divide(deltaR).atan();
                h   = deltaR.hypot(z).subtract(osculatingRadius);
            } else {
                // the point is on the part of the major axis within ellipse evolute
                // we can compute the closest ellipse point analytically, and it is NOT near the equator
                final T rClose = r.divide(e2);
                final T zClose = rClose.multiply(rClose).negate().add(ae2).sqrt().multiply(g).copySign(z);
                phi = zClose.subtract(z).divide(rClose.subtract(r)).atan();
                h   = r.subtract(rClose).hypot(z.subtract(zClose)).negate();
            }

        } else {
            // use Toshio Fukushima method, with several iterations
            final double epsPhi = 1.0e-15;
            final double epsH   = 1.0e-14 * getA();
            final double c      = getA() * e2;
            final T      absZ   = z.abs();
            final T      zc     = absZ.multiply(g);
            T            sn     = absZ;
            T            sn2    = sn.multiply(sn);
            T            cn     = r.multiply(g);
            T            cn2    = cn.multiply(cn);
            T            an2    = cn2.add(sn2);
            T            an     = an2.sqrt();
            T            bn     = an.getField().getZero();
            phi = an.getField().getZero().add(Double.POSITIVE_INFINITY);
            h   = an.getField().getZero().add(Double.POSITIVE_INFINITY);
            for (int i = 0; i < 1000; ++i) {
                // this usually converges in 2 iterations, but in rare cases it can take much more
                // see https://gitlab.orekit.org/orekit/orekit/-/issues/1224 for examples
                // with points near Earth center which need 137 iterations for the first example
                // and 1150 iterations for the second example
                final T oldSn  = sn;
                final T oldCn  = cn;
                final T oldPhi = phi;
                final T oldH   = h;
                final T an3    = an2.multiply(an);
                final T csncn  = sn.multiply(cn).multiply(c);
                bn    = csncn.multiply(1.5).multiply((r.multiply(sn).subtract(zc.multiply(cn))).multiply(an).subtract(csncn));
                sn    = zc.multiply(an3).add(sn2.multiply(sn).multiply(c)).multiply(an3).subtract(bn.multiply(sn));
                cn    = r.multiply(an3).subtract(cn2.multiply(cn).multiply(c)).multiply(an3).subtract(bn.multiply(cn));
                if (sn.getReal() * oldSn.getReal() < 0 || cn.getReal() < 0) {
                    // the Halley iteration went too far, we restrict it and iterate again
                    while (sn.getReal() * oldSn.getReal() < 0 || cn.getReal() < 0) {
                        sn = sn.add(oldSn).multiply(0.5);
                        cn = cn.add(oldCn).multiply(0.5);
                    }
                } else {

                    // rescale components to avoid overflow when several iterations are used
                    final int exp = (FastMath.getExponent(sn.getReal()) + FastMath.getExponent(cn.getReal())) / 2;
                    sn = sn.scalb(-exp);
                    cn = cn.scalb(-exp);

                    sn2 = sn.multiply(sn);
                    cn2 = cn.multiply(cn);
                    an2 = cn2.add(sn2);
                    an  = an2.sqrt();

                    final T cc = cn.multiply(g);
                    h = r.multiply(cc).add(absZ.multiply(sn)).subtract(an.multiply(getA() * g)).divide(an2.subtract(cn2.multiply(e2)).sqrt());
                    if (FastMath.abs(oldH.getReal()  - h.getReal())   < epsH) {
                        phi = sn.divide(cc).atan().copySign(z);
                        if (FastMath.abs(oldPhi.getReal() - phi.getReal()) < epsPhi) {
                            break;
                        }
                    }

                }

            }

            if (Double.isInfinite(phi.getReal())) {
                // we did not converge, the point is probably within the ellipsoid
                // we just compute the "best" phi we can to avoid NaN
                phi = sn.divide(cn.multiply(g)).atan().copySign(z);
            }

        }

        return new FieldGeodeticPoint<>(phi, lambda, h);

    }

    /** Transform a Cartesian point to a surface-relative point.
     * @param point Cartesian point
     * @param frame frame in which Cartesian point is expressed
     * @param date date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point,
     * using time as the single derivation parameter
     */
    public FieldGeodeticPoint<DerivativeStructure> transform(final PVCoordinates point,
                                                             final Frame frame, final AbsoluteDate date) {

        // transform point to body frame
        final Transform toBody = frame.getTransformTo(bodyFrame, date);
        final PVCoordinates pointInBodyFrame = toBody.transformPVCoordinates(point);
        final FieldVector3D<DerivativeStructure> p = pointInBodyFrame.toDerivativeStructureVector(2);
        final DerivativeStructure   pr2 = p.getX().multiply(p.getX()).add(p.getY().multiply(p.getY()));
        final DerivativeStructure   pr  = pr2.sqrt();
        final DerivativeStructure   pz  = p.getZ();

        // project point on the ellipsoid surface
        final TimeStampedPVCoordinates groundPoint = projectToGround(new TimeStampedPVCoordinates(date, pointInBodyFrame),
                                                                     bodyFrame);
        final FieldVector3D<DerivativeStructure> gp = groundPoint.toDerivativeStructureVector(2);
        final DerivativeStructure   gpr2 = gp.getX().multiply(gp.getX()).add(gp.getY().multiply(gp.getY()));
        final DerivativeStructure   gpr  = gpr2.sqrt();
        final DerivativeStructure   gpz  = gp.getZ();

        // relative position of test point with respect to its ellipse sub-point
        final DerivativeStructure dr  = pr.subtract(gpr);
        final DerivativeStructure dz  = pz.subtract(gpz);
        final double insideIfNegative = g2 * (pr2.getReal() - ae2) + pz.getReal() * pz.getReal();

        return new FieldGeodeticPoint<>(DerivativeStructure.atan2(gpz, gpr.multiply(g2)),
                                                                  DerivativeStructure.atan2(p.getY(), p.getX()),
                                                                  DerivativeStructure.hypot(dr, dz).copySign(insideIfNegative));
    }

    /** Compute the azimuth angle from local north between the two points.
     *
     * The angle is calculated clockwise from local north at the origin point
     * and follows the rhumb line to the destination point.
     *
     * @param origin the origin point, at which the azimuth angle will be computed (non-{@code null})
     * @param destination the destination point, to which the angle is defined (non-{@code null})
     * @return the resulting azimuth angle (radians, {@code [0-2pi)})
     * @since 11.3
     */
    public double azimuthBetweenPoints(final GeodeticPoint origin, final GeodeticPoint destination) {
        final double dLon = MathUtils.normalizeAngle(destination.getLongitude(), origin.getLongitude()) - origin.getLongitude();
        final double originIsoLat = geodeticToIsometricLatitude(origin.getLatitude());
        final double destIsoLat = geodeticToIsometricLatitude(destination.getLatitude());

        final double az = FastMath.atan2(dLon, destIsoLat - originIsoLat);
        if (az < 0.) {
            return az + MathUtils.TWO_PI;
        }
        return az;
    }

    /** Compute the azimuth angle from local north between the two points.
     *
     * The angle is calculated clockwise from local north at the origin point
     * and follows the rhumb line to the destination point.
     *
     * @param origin the origin point, at which the azimuth angle will be computed (non-{@code null})
     * @param destination the destination point, to which the angle is defined (non-{@code null})
     * @param <T> the type of field elements
     * @return the resulting azimuth angle (radians, {@code [0-2pi)})
     * @since 11.3
     */
    public <T extends CalculusFieldElement<T>> T azimuthBetweenPoints(final FieldGeodeticPoint<T> origin, final FieldGeodeticPoint<T> destination) {
        final T dLon = MathUtils.normalizeAngle(destination.getLongitude().subtract(origin.getLongitude()), origin.getLongitude().getField().getZero());
        final T originIsoLat = geodeticToIsometricLatitude(origin.getLatitude());
        final T destIsoLat = geodeticToIsometricLatitude(destination.getLatitude());

        final T az = FastMath.atan2(dLon, destIsoLat.subtract(originIsoLat));
        if (az.getReal() < 0.) {
            return az.add(az.getPi().multiply(2));
        }
        return az;
    }

    /** Compute the <a href="https://mathworld.wolfram.com/IsometricLatitude.html">isometric latitude</a>
     *  corresponding to the provided latitude.
     *
     * @param geodeticLatitude the latitude (radians, within interval {@code [-pi/2, +pi/2]})
     * @return the isometric latitude (radians)
     * @since 11.3
     */
    public double geodeticToIsometricLatitude(final double geodeticLatitude) {
        if (FastMath.abs(geodeticLatitude) <= angularThreshold) {
            return 0.;
        }

        final double eSinLat = e * FastMath.sin(geodeticLatitude);

        // first term: ln(tan(pi/4 + lat/2))
        final double a = FastMath.log(FastMath.tan(FastMath.PI / 4. + geodeticLatitude / 2.));
        // second term: (ecc / 2) * ln((1 - ecc*sin(lat)) / (1 + ecc * sin(lat)))
        final double b = (e / 2.) * FastMath.log((1. - eSinLat) / (1. + eSinLat));

        return a + b;
    }

    /** Compute the <a href="https://mathworld.wolfram.com/IsometricLatitude.html">isometric latitude</a>
     *  corresponding to the provided latitude.
     *
     * @param geodeticLatitude the latitude (radians, within interval {@code [-pi/2, +pi/2]})
     * @param <T> the type of field elements
     * @return the isometric latitude (radians)
     * @since 11.3
     */
    public <T extends CalculusFieldElement<T>> T geodeticToIsometricLatitude(final T geodeticLatitude) {
        if (geodeticLatitude.abs().getReal() <= angularThreshold) {
            return geodeticLatitude.getField().getZero();
        }
        final Field<T> field = geodeticLatitude.getField();
        final T ecc = geodeticLatitude.newInstance(e);
        final T eSinLat = ecc.multiply(geodeticLatitude.sin());

        // first term: ln(tan(pi/4 + lat/2))
        final T a = FastMath.log(FastMath.tan(geodeticLatitude.getPi().divide(4.).add(geodeticLatitude.divide(2.))));
        // second term: (ecc / 2) * ln((1 - ecc*sin(lat)) / (1 + ecc * sin(lat)))
        final T b = ecc.divide(2.).multiply(FastMath.log(field.getOne().subtract(eSinLat).divide(field.getOne().add(eSinLat))));

        return a.add(b);
    }

    /** Find intermediate point of lowest altitude along a line between two endpoints.
     * @param endpoint1 first endpoint, in body frame
     * @param endpoint2 second endpoint, in body frame
     * @return point with lowest altitude between {@code endpoint1} and {@code endpoint2}.
     * @since 12.0
     */
    public GeodeticPoint lowestAltitudeIntermediate(final Vector3D endpoint1, final Vector3D endpoint2) {

        final Vector3D delta = endpoint2.subtract(endpoint1);

        // function computing intermediate point above ellipsoid (lambda varying between 0 and 1)
        final DoubleFunction<GeodeticPoint> intermediate =
                        lambda -> transform(new Vector3D(1 - lambda, endpoint1, lambda, endpoint2),
                                            bodyFrame, null);

        // first endpoint
        final GeodeticPoint gp1 = intermediate.apply(0.0);

        if (Vector3D.dotProduct(delta, gp1.getZenith()) >= 0) {
            // the line from first endpoint to second endpoint is going away from central body
            // the minimum altitude is reached at first endpoint
            return gp1;
        } else {
            // the line from first endpoint to second endpoint is closing the central body

            // second endpoint
            final GeodeticPoint gp2 = intermediate.apply(1.0);

            if (Vector3D.dotProduct(delta, gp2.getZenith()) <= 0) {
                // the line from first endpoint to second endpoint is still decreasing when reaching second endpoint,
                // the minimum altitude is reached at second endpoint
                return gp2;
            } else {
                // the line from first endpoint to second endpoint reaches a minimum between first and second endpoints
                final double lambdaMin = new BracketingNthOrderBrentSolver(1.0e-14, 5).
                                         solve(1000,
                                               lambda -> Vector3D.dotProduct(delta, intermediate.apply(lambda).getZenith()),
                                               0.0, 1.0);
                return intermediate.apply(lambdaMin);
            }
        }

    }

    /** Find intermediate point of lowest altitude along a line between two endpoints.
     * @param endpoint1 first endpoint, in body frame
     * @param endpoint2 second endpoint, in body frame
     * @param <T> type of the field elements
     * @return point with lowest altitude between {@code endpoint1} and {@code endpoint2}.
     * @since 12.0
     */
    public <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> lowestAltitudeIntermediate(final FieldVector3D<T> endpoint1,
                                                                                                final FieldVector3D<T> endpoint2) {

        final FieldVector3D<T> delta = endpoint2.subtract(endpoint1);

        // function computing intermediate point above ellipsoid (lambda varying between 0 and 1)
        final DoubleFunction<FieldGeodeticPoint<T>> intermediate =
                        lambda -> transform(new FieldVector3D<>(1 - lambda, endpoint1, lambda, endpoint2),
                                            bodyFrame, null);

        // first endpoint
        final FieldGeodeticPoint<T> gp1 = intermediate.apply(0.0);

        if (FieldVector3D.dotProduct(delta, gp1.getZenith()).getReal() >= 0) {
            // the line from first endpoint to second endpoint is going away from central body
            // the minimum altitude is reached at first endpoint
            return gp1;
        } else {
            // the line from first endpoint to second endpoint is closing the central body

            // second endpoint
            final FieldGeodeticPoint<T> gp2 = intermediate.apply(1.0);

            if (FieldVector3D.dotProduct(delta, gp2.getZenith()).getReal() <= 0) {
                // the line from first endpoint to second endpoint is still decreasing when reaching second endpoint,
                // the minimum altitude is reached at second endpoint
                return gp2;
            } else {
                // the line from first endpoint to second endpoint reaches a minimum between first and second endpoints
                final double lambdaMin = new BracketingNthOrderBrentSolver(1.0e-14, 5).
                                         solve(1000,
                                               lambda -> FieldVector3D.dotProduct(delta, intermediate.apply(lambda).getZenith()).getReal(),
                                               0.0, 1.0);
                return intermediate.apply(lambdaMin);
            }
        }

    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes the files supported names, the
     * ephemeris type and the body name.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(getA(), f, bodyFrame, angularThreshold);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20130518L;

        /** Equatorial radius. */
        private final double ae;

        /** Flattening. */
        private final double f;

        /** Body frame related to body shape. */
        private final Frame bodyFrame;

        /** Convergence limit. */
        private final double angularThreshold;

        /** Simple constructor.
         * @param ae equatorial radius
         * @param f the flattening (f = (a-b)/a)
         * @param bodyFrame body frame related to body shape
         * @param angularThreshold convergence limit
         */
        DataTransferObject(final double ae, final double f,
                                  final Frame bodyFrame, final double angularThreshold) {
            this.ae               = ae;
            this.f                = f;
            this.bodyFrame        = bodyFrame;
            this.angularThreshold = angularThreshold;
        }

        /** Replace the deserialized data transfer object with a
         * {@link JPLCelestialBody}.
         * @return replacement {@link JPLCelestialBody}
         */
        private Object readResolve() {
            final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(ae, f, bodyFrame);
            ellipsoid.setAngularThreshold(angularThreshold);
            return ellipsoid;
        }

    }

}
