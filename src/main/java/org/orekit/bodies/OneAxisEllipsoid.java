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
package org.orekit.bodies;

import java.io.Serializable;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.oned.Vector1D;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/** Modeling of a one-axis ellipsoid.

 * <p>One-axis ellipsoids is a good approximate model for most planet-size
 * and larger natural bodies. It is the equilibrium shape reached by
 * a fluid body under its own gravity field when it rotates. The symmetry
 * axis is the rotation or polar axis.</p>

 * @author Luc Maisonobe
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

    /** Eccentricity power 2. */
    private final double e2;

    /** 1 minus flatness. */
    private final double g;

    /** g * g. */
    private final double g2;

    /** Convergence limit. */
    private double angularThreshold;

    /** Simple constructor.
     * <p>Standard values for Earth models can be found in the {@link org.orekit.utils.Constants Constants} class:</p>
     * <table border="1" cellpadding="5">
     * <caption>Ellipsoid Models</caption>
     * <tr bgcolor="#ccccff"><th>model</th><th>a<sub>e</sub> (m)</th> <th>f</th></tr>
     * <tr><td bgcolor="#eeeeff">GRS 80</td>
     *     <td>{@link org.orekit.utils.Constants#GRS80_EARTH_EQUATORIAL_RADIUS Constants.GRS80_EARTH_EQUATORIAL_RADIUS}</td>
     *     <td>{@link org.orekit.utils.Constants#GRS80_EARTH_FLATTENING Constants.GRS80_EARTH_FLATTENING}</td></tr>
     * <tr><td bgcolor="#eeeeff">WGS84</td>
     *     <td>{@link org.orekit.utils.Constants#WGS84_EARTH_EQUATORIAL_RADIUS Constants.WGS84_EARTH_EQUATORIAL_RADIUS}</td>
     *     <td>{@link org.orekit.utils.Constants#WGS84_EARTH_FLATTENING Constants.WGS84_EARTH_FLATTENING}</td></tr>
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

    /** {@inheritDoc} */
    public Frame getBodyFrame() {
        return bodyFrame;
    }

    /** {@inheritDoc} */
    public GeodeticPoint getIntersectionPoint(final Line line, final Vector3D close,
                                              final Frame frame, final AbsoluteDate date)
        throws OrekitException {

        // transform line and close to body frame
        final Transform frameToBodyFrame = frame.getTransformTo(bodyFrame, date);
        final Line lineInBodyFrame = frameToBodyFrame.transformLine(line);
        final Vector3D closeInBodyFrame = frameToBodyFrame.transformPosition(close);
        final double closeAbscissa = lineInBodyFrame.toSubSpace(closeInBodyFrame).getX();

        // compute some miscellaneous variables outside of the loop
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
        final double k =
            (FastMath.abs(k1 - closeAbscissa) < FastMath.abs(k2 - closeAbscissa)) ? k1 : k2;
        final Vector3D intersection = lineInBodyFrame.toSpace(new Vector1D(k));
        final double ix = intersection.getX();
        final double iy = intersection.getY();
        final double iz = intersection.getZ();

        final double lambda = FastMath.atan2(iy, ix);
        final double phi    = FastMath.atan2(iz, g2 * FastMath.sqrt(ix * ix + iy * iy));
        return new GeodeticPoint(phi, lambda, 0.0);

    }

    /** {@inheritDoc} */
    public Vector3D transform(final GeodeticPoint point) {
        final double longitude = point.getLongitude();
        final double cLambda   = FastMath.cos(longitude);
        final double sLambda   = FastMath.sin(longitude);
        final double latitude  = point.getLatitude();
        final double cPhi      = FastMath.cos(latitude);
        final double sPhi      = FastMath.sin(latitude);
        final double h         = point.getAltitude();
        final double n         = getA() / FastMath.sqrt(1.0 - e2 * sPhi * sPhi);
        final double r         = (n + h) * cPhi;
        return new Vector3D(r * cLambda, r * sLambda, (g2 * n + h) * sPhi);
    }

    /** Transform a surface-relative point to a Cartesian point.
     * @param point surface-relative point, using time as the single derivation parameter
     * @return point at the same location but as a Cartesian point including derivatives
     */
    public PVCoordinates transform(final FieldGeodeticPoint<DerivativeStructure> point) {

        final DerivativeStructure latitude  = point.getLatitude();
        final DerivativeStructure longitude = point.getLongitude();
        final DerivativeStructure altitude  = point.getAltitude();

        final DerivativeStructure cLambda = longitude.cos();
        final DerivativeStructure sLambda = longitude.sin();
        final DerivativeStructure cPhi    = latitude.cos();
        final DerivativeStructure sPhi    = latitude.sin();
        final DerivativeStructure n       = sPhi.multiply(sPhi).multiply(e2).subtract(1.0).negate().sqrt().reciprocal().multiply(getA());
        final DerivativeStructure r       = n.add(altitude).multiply(cPhi);

        return new PVCoordinates(new FieldVector3D<DerivativeStructure>(r.multiply(cLambda),
                                                                        r.multiply(sLambda),
                                                                        sPhi.multiply(altitude.add(n.multiply(g2)))));
    }

    /** {@inheritDoc} */
    public Vector3D projectToGround(final Vector3D point, final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // transform point to body frame
        final Transform  toBody    = frame.getTransformTo(bodyFrame, date);
        final Vector3D   p         = toBody.transformPosition(point);
        final double     z         = p.getZ();
        final double     r         = FastMath.hypot(p.getX(), p.getY());

        // set up the 2D meridian ellipse
        final Ellipse meridian = new Ellipse(Vector3D.ZERO,
                                             new Vector3D(p.getX() / r, p.getY() / r, 0),
                                             Vector3D.PLUS_K,
                                             getA(), getC(), bodyFrame);

        // find the closest point in the meridian plane
        final Vector3D groundPoint = meridian.toSpace(meridian.projectToEllipse(new Vector2D(r, z)));

        // transform point back to initial frame
        return toBody.getInverse().transformPosition(groundPoint);

    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates projectToGround(final TimeStampedPVCoordinates pv, final Frame frame)
        throws OrekitException {

        // transform point to body frame
        final Transform                toBody        = frame.getTransformTo(bodyFrame, pv.getDate());
        final TimeStampedPVCoordinates pvInBodyFrame = toBody.transformPVCoordinates(pv);
        final Vector3D                 p             = pvInBodyFrame.getPosition();
        final double                   r             = FastMath.hypot(p.getX(), p.getY());

        // set up the 2D ellipse corresponding to first principal curvature along meridian
        final Vector3D meridian = new Vector3D(p.getX() / r, p.getY() / r, 0);
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
     * This method is based on Toshio Fukushima's algorithm wich uses Halley's method.
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
     * </p>
     */
    public GeodeticPoint transform(final Vector3D point, final Frame frame, final AbsoluteDate date)
        throws OrekitException {

        // transform point to body frame
        final Vector3D pointInBodyFrame = frame.getTransformTo(bodyFrame, date).transformPosition(point);
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
            final double epsH   = 1.0e-14 * getA();
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
            for (int i = 0; i < 10; ++i) { // this usually converges in 2 iterations
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
        }

        return new GeodeticPoint(phi, lambda, h);

    }

    /** Transform a Cartesian point to a surface-relative point.
     * @param point Cartesian point
     * @param frame frame in which Cartesian point is expressed
     * @param date date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point,
     * using time as the single derivation parameter
     * @exception OrekitException if point cannot be converted to body frame
     */
    public FieldGeodeticPoint<DerivativeStructure> transform(final PVCoordinates point,
                                                             final Frame frame, final AbsoluteDate date)
        throws OrekitException {

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

        return new FieldGeodeticPoint<DerivativeStructure>(DerivativeStructure.atan2(gpz, gpr.multiply(g2)),
                                                           DerivativeStructure.atan2(p.getY(), p.getX()),
                                                           DerivativeStructure.hypot(dr, dz).copySign(insideIfNegative));
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
