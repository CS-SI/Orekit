/* Contributed in the public domain.
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
package org.orekit.models.earth;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A geoid is a level surface of the gravity potential of a body. The gravity
 * potential, W, is split so W = U + T, where U is the normal potential (defined
 * by the ellipsoid) and T is the anomalous potential.[3](eq. 2-137)
 *
 * <p> The {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)}
 * method is tailored specifically for Earth's geoid. All of the other methods
 * in this class are general and will work for an arbitrary body.
 *
 * <p> There are several components that are needed to define a geoid[1]:
 *
 * <ul> <li>Geopotential field. These are the coefficients of the spherical
 * harmonics: S<sub>n,m</sub> and C<sub>n,m</sub></li>
 *
 * <li>Reference Ellipsoid. The ellipsoid is used to define the undulation of
 * the geoid (distance between ellipsoid and geoid) and U<sub>0</sub> the value
 * of the normal gravity potential at the surface of the ellipsoid.</li>
 *
 * <li>W<sub>0</sub>, the potential at the geoid. The value of the potential on
 * the level surface. This is taken to be U<sub>0</sub>, the normal gravity
 * potential at the surface of the {@link ReferenceEllipsoid}.</li>
 *
 * <li>Permanent Tide System. This implementation assumes that the geopotential
 * field and the reference ellipsoid use the same permanent tide system. If the
 * assumption is false it will produce errors of about 0.5 m. Conversion between
 * tide systems is a possible improvement.[1,2]</li>
 *
 * <li>Topographic Masses. That is mass outside of the geoid, e.g. mountains.
 * This implementation ignores topographic masses, which causes up to 3m error
 * in the Himalayas, and ~ 1.5m error in the Rockies. This could be improved
 * through the use of DTED and calculating height anomalies or using the
 * correction coefficients.[1]</li> </ul>
 *
 * <p> This implementation also assumes that the normal to the reference
 * ellipsoid is the same as the normal to the geoid. This assumption enables the
 * equation: (height above geoid) = (height above ellipsoid) - (undulation),
 * which is used in {@link #transform(GeodeticPoint)} and {@link
 * #transform(Vector3D, Frame, AbsoluteDate)}.
 *
 * <p> In testing, the error in the undulations calculated by this class were
 * off by less than 3 meters, which matches the assumptions outlined above.
 *
 * <p> References:
 *
 * <ol> <li>Dru A. Smith. There is no such thing as "The" EGM96 geoid: Subtle
 * points on the use of a global geopotential model. IGeS Bulletin No. 8:17-28,
 * 1998. <a href= "http://www.ngs.noaa.gov/PUBS_LIB/EGM96_GEOID_PAPER/egm96_geoid_paper.html"
 * >http ://www.ngs.noaa.gov/PUBS_LIB/EGM96_GEOID_PAPER/egm96_geoid_paper.html</a></li>
 *
 * <li> Martin Losch, Verena Seufer. How to Compute Geoid Undulations (Geoid
 * Height Relative to a Given Reference Ellipsoid) from Spherical Harmonic
 * Coefficients for Satellite Altimetry Applications. , 2003. <a
 * href="http://mitgcm.org/~mlosch/geoidcookbook.pdf">mitgcm.org/~mlosch/geoidcookbook.pdf</a>
 * </li>
 *
 * <li>Weikko A. Heiskanen, Helmut Moritz. Physical Geodesy. W. H. Freeman and
 * Company, 1967. (especially sections 2.13 and equation 2-144 Bruns
 * Formula)</li>
 *
 * <li>S. A. Holmes, W. E. Featherstone. A unified approach to the Clenshaw
 * summation and the recursive computation of very high degree and order
 * normalised associated Legendre functions. Journal of Geodesy, 76(5):279,
 * 2002.</li>
 *
 * <li>DMA TR 8350.2. 1984.</li>
 *
 * <li>Department of Defense World Geodetic System 1984. 2000. NIMA TR 8350.2
 * Third Edition, Amendment 1.</li> </ol>
 *
 * @author Evan Ward
 */
public class Geoid implements EarthShape {

    /**
     * uid is date of last modification.
     */
    private static final long serialVersionUID = 20150312L;

    /**
     * A number larger than the largest undulation. Wikipedia says the geoid
     * height is in [-106, 85]. I chose 100 to be safe.
     */
    private static final double MAX_UNDULATION = 100;
    /**
     * A number smaller than the smallest undulation. Wikipedia says the geoid
     * height is in [-106, 85]. I chose -150 to be safe.
     */
    private static final double MIN_UNDULATION = -150;
    /**
     * the maximum number of evaluations for the line search in {@link
     * #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)}.
     */
    private static final int MAX_EVALUATIONS = 100;

    /**
     * the default date to use when evaluating the {@link #harmonics}. Used when
     * no other dates are available. Should be removed in a future release.
     */
    private final AbsoluteDate defaultDate;
    /**
     * the reference ellipsoid.
     */
    private final ReferenceEllipsoid referenceEllipsoid;
    /**
     * the geo-potential combined with an algorithm for evaluating the spherical
     * harmonics. The Holmes and Featherstone method is very robust.
     */
    private final transient HolmesFeatherstoneAttractionModel harmonics;

    /**
     * Creates a geoid from the given geopotential, reference ellipsoid and the
     * assumptions in the comment for {@link Geoid}.
     *
     * @param geopotential       the gravity potential. Only the anomalous
     *                           potential will be used. It is assumed that the
     *                           {@code geopotential} and the {@code
     *                           referenceEllipsoid} are defined in the same
     *                           frame. Usually a {@link org.orekit.forces.gravity.potential.GravityFieldFactory#getConstantNormalizedProvider(int,
     *                           int) constant geopotential} is used to define a
     *                           time-invariant Geoid.
     * @param referenceEllipsoid the normal gravity potential.
     * @throws NullPointerException if {@code geopotential == null ||
     *                              referenceEllipsoid == null}
     */
    public Geoid(final NormalizedSphericalHarmonicsProvider geopotential,
                 final ReferenceEllipsoid referenceEllipsoid) {
        // parameter check
        if (geopotential == null || referenceEllipsoid == null) {
            throw new NullPointerException();
        }

        // subtract the ellipsoid from the geopotential
        final SubtractEllipsoid potential = new SubtractEllipsoid(geopotential,
                referenceEllipsoid);

        // set instance parameters
        this.referenceEllipsoid = referenceEllipsoid;
        this.harmonics = new HolmesFeatherstoneAttractionModel(
                referenceEllipsoid.getBodyFrame(), potential);
        this.defaultDate = geopotential.getReferenceDate();
    }

    @Override
    public Frame getBodyFrame() {
        // same as for reference ellipsoid.
        return this.getEllipsoid().getBodyFrame();
    }

    /**
     * Gets the Undulation of the Geoid, N at the given position. N is the
     * distance between the {@link #getEllipsoid() reference ellipsoid} and the
     * geoid. The latitude and longitude parameters are both defined with
     * respect to the reference ellipsoid. For EGM96 and the WGS84 ellipsoid the
     * undulation is between -107m and +86m.
     *
     * <p> NOTE: Restrictions are not put on the range of the arguments {@code
     * geodeticLatitude} and {@code longitude}.
     *
     * @param geodeticLatitude geodetic latitude (angle between the local normal
     *                         and the equatorial plane on the reference
     *                         ellipsoid), in radians.
     * @param longitude        on the reference ellipsoid, in radians.
     * @param date             of evaluation. Used for time varying geopotential
     *                         fields.
     * @return the undulation in m, positive means the geoid is higher than the
     * ellipsoid.
     * @throws OrekitException if an error occurs converting latitude and
     *                         longitude
     * @see Geoid
     * @see <a href="http://en.wikipedia.org/wiki/Geoid">Geoid on Wikipedia</a>
     */
    public double getUndulation(final double geodeticLatitude,
                                final double longitude,
                                final AbsoluteDate date) throws OrekitException {
            /*
             * equations references are to the algorithm printed in the geoid
             * cookbook[2]. See comment for Geoid.
             */
        // reference ellipsoid
        final ReferenceEllipsoid ellipsoid = this.getEllipsoid();

        // position in geodetic coordinates
        final GeodeticPoint gp = new GeodeticPoint(geodeticLatitude, longitude, 0);
        // position in cartesian coordinates, is converted to geocentric lat and
        // lon in the Holmes and Featherstone class
        final Vector3D position = ellipsoid.transform(gp);

        // get normal gravity from ellipsoid, eq 15
        final double normalGravity = ellipsoid
                .getNormalGravity(geodeticLatitude);

        // calculate disturbing potential, T, eq 30.
        final double T = this.harmonics.nonCentralPart(date, position);
        // calculate undulation, eq 30
        return T / normalGravity;
    }

    @Override
    public ReferenceEllipsoid getEllipsoid() {
        return this.referenceEllipsoid;
    }

    /**
     * This class implements equations 20-24 in the geoid cook book.(Losch and
     * Seufer) It modifies C<sub>2n,0</sub> where n = 1,2,...,5.
     *
     * @see "DMA TR 8350.2. 1984."
     */
    private static final class SubtractEllipsoid implements
            NormalizedSphericalHarmonicsProvider {
        /**
         * provider of the fully normalized coefficients, includes the reference
         * ellipsoid.
         */
        private final NormalizedSphericalHarmonicsProvider provider;
        /**
         * the reference ellipsoid to subtract from {@link #provider}.
         */
        private final ReferenceEllipsoid ellipsoid;

        /**
         * @param provider  potential used for GM<sub>g</sub> and a<sub>g</sub>,
         *                  and of course the coefficients Cnm, and Snm.
         * @param ellipsoid Used to calculate the fully normalized
         *                  J<sub>2n</sub>
         */
        private SubtractEllipsoid(
                final NormalizedSphericalHarmonicsProvider provider,
                final ReferenceEllipsoid ellipsoid) {
            super();
            this.provider = provider;
            this.ellipsoid = ellipsoid;
        }

        @Override
        public int getMaxDegree() {
            return this.provider.getMaxDegree();
        }

        @Override
        public int getMaxOrder() {
            return this.provider.getMaxOrder();
        }

        @Override
        public double getMu() {
            return this.provider.getMu();
        }

        @Override
        public double getAe() {
            return this.provider.getAe();
        }

        @Override
        public AbsoluteDate getReferenceDate() {
            return this.provider.getReferenceDate();
        }

        @Override
        public double getOffset(final AbsoluteDate date) {
            return this.provider.getOffset(date);
        }

        @Override
        public NormalizedSphericalHarmonics onDate(final AbsoluteDate date)
            throws OrekitException {
            return new NormalizedSphericalHarmonics() {

                /** the original harmonics */
                private final NormalizedSphericalHarmonics delegate = provider.onDate(date);

                @Override
                public double getNormalizedCnm(final int n, final int m) throws OrekitException {
                    return getCorrectedCnm(n, m, this.delegate.getNormalizedCnm(n, m));
                }

                @Override
                public double getNormalizedSnm(final int n, final int m) throws OrekitException {
                    return this.delegate.getNormalizedSnm(n, m);
                }

                @Override
                public AbsoluteDate getDate() {
                    return date;
                }
            };
        }

        /**
         * Get the corrected Cnm for different GM or a values.
         *
         * @param n              degree
         * @param m              order
         * @param uncorrectedCnm uncorrected Cnm coefficient
         * @return the corrected Cnm coefficient.
         */
        private double getCorrectedCnm(final int n,
                                       final int m,
                                       final double uncorrectedCnm) {
            double Cnm = uncorrectedCnm;
            // n = 2,4,6,8, or 10 and m = 0
            if (m == 0 && n <= 10 && n % 2 == 0 && n > 0) {
                // correction factor for different GM and a, 1 if no difference
                final double gmRatio = this.ellipsoid.getGM() / this.getMu();
                final double aRatio = this.ellipsoid.getEquatorialRadius() /
                        this.getAe();
                /*
                 * eq 20 in the geoid cook book[2], with eq 3-61 in chapter 3 of
                 * DMA TR 8350.2
                 */
                // halfN = 1,2,3,4,5 for n = 2,4,6,8,10, respectively
                final int halfN = n / 2;
                Cnm = Cnm - gmRatio * FastMath.pow(aRatio, halfN) *
                        this.ellipsoid.getC2n0(halfN);
            }
            // return is a modified Cnm
            return Cnm;
        }

        @Override
        public TideSystem getTideSystem() {
            return this.provider.getTideSystem();
        }

    }

    /**
     * {@inheritDoc}
     *
     * <p> The intersection point is computed using a line search along the
     * specified line. This is accurate when the geoid is slowly varying.
     */
    @Override
    public GeodeticPoint getIntersectionPoint(final Line lineInFrame,
                                              final Vector3D closeInFrame,
                                              final Frame frame,
                                              final AbsoluteDate date)
        throws OrekitException {
        /*
         * It is assumed that the geoid is slowly varying over it's entire
         * surface. Therefore there will one local intersection.
         */
        // transform to body frame
        final Frame bodyFrame = this.getBodyFrame();
        final Transform frameToBody = frame.getTransformTo(bodyFrame, date);
        final Vector3D close = frameToBody.transformPosition(closeInFrame);
        final Line lineInBodyFrame = frameToBody.transformLine(lineInFrame);

        // set the line's direction so the solved for value is always positive
        final Line line;
        if (lineInBodyFrame.getAbscissa(close) < 0) {
            line = lineInBodyFrame.revert();
        } else {
            line = lineInBodyFrame;
        }

        final ReferenceEllipsoid ellipsoid = this.getEllipsoid();
        // calculate end points
        // distance from line to center of earth, squared
        final double d2 = line.pointAt(0.0).getNormSq();
        // the minimum abscissa, squared
        final double minAbscissa2 =
                FastMath.pow(ellipsoid.getPolarRadius() + MIN_UNDULATION, 2) - d2;
        // smaller end point of the interval = 0.0 or intersection with
        // min_undulation sphere
        final double lowPoint = FastMath.sqrt(FastMath.max(minAbscissa2, 0.0));
        // the maximum abscissa, squared
        final double maxAbscissa2 =
                FastMath.pow(ellipsoid.getEquatorialRadius() + MAX_UNDULATION, 2) - d2;
        // larger end point of the interval
        final double highPoint = FastMath.sqrt(maxAbscissa2);

        // line search function
        final UnivariateFunction heightFunction = new UnivariateFunction() {
            @Override
            public double value(final double x) {
                try {
                    final GeodeticPoint geodetic =
                            transform(line.pointAt(x), bodyFrame, date);
                    return geodetic.getAltitude();
                } catch (OrekitException e) {
                    // due to frame transform -> re-throw
                    throw new RuntimeException(e);
                }
            }
        };

        // compute answer
        if (maxAbscissa2 < 0) {
            // ray does not pierce bounding sphere -> no possible intersection
            return null;
        }
        // solve line search problem to find the intersection
        final UnivariateSolver solver = new BracketingNthOrderBrentSolver();
        try {
            final double abscissa = solver.solve(MAX_EVALUATIONS, heightFunction, lowPoint, highPoint);
            // return intersection point
            return this.transform(line.pointAt(abscissa), bodyFrame, date);
        } catch (MathRuntimeException e) {
            // no intersection
            return null;
        }
    }

    @Override
    public Vector3D projectToGround(final Vector3D point,
                                    final AbsoluteDate date,
                                    final Frame frame) throws OrekitException {
        final GeodeticPoint gp = this.transform(point, frame, date);
        final GeodeticPoint gpZero =
                new GeodeticPoint(gp.getLatitude(), gp.getLongitude(), 0);
        final Transform bodyToFrame = this.getBodyFrame().getTransformTo(frame, date);
        return bodyToFrame.transformPosition(this.transform(gpZero));
    }

    @Override
    public TimeStampedPVCoordinates projectToGround(
            final TimeStampedPVCoordinates pv,
            final Frame frame) throws OrekitException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @param date date of the conversion. Used for computing frame
     *             transformations and for time dependent geopotential.
     * @return The surface relative point at the same location. Altitude is
     * orthometric height, that is height above the {@link Geoid}. Latitude and
     * longitude are both geodetic and defined with respect to the {@link
     * #getEllipsoid() reference ellipsoid}.
     * @see #transform(GeodeticPoint)
     * @see <a href="http://en.wikipedia.org/wiki/Orthometric_height">Orthometric_height</a>
     */
    @Override
    public GeodeticPoint transform(final Vector3D point, final Frame frame,
                                   final AbsoluteDate date) throws OrekitException {
        // convert using reference ellipsoid, altitude referenced to ellipsoid
        final GeodeticPoint ellipsoidal = this.getEllipsoid().transform(
                point, frame, date);
        // convert altitude to orthometric using the undulation.
        final double undulation = this.getUndulation(ellipsoidal.getLatitude(),
                ellipsoidal.getLongitude(), date);
        // add undulation to the altitude
        return new GeodeticPoint(
                ellipsoidal.getLatitude(),
                ellipsoidal.getLongitude(),
                ellipsoidal.getAltitude() - undulation
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param point The surface relative point to transform. Altitude is
     *              orthometric height, that is height above the {@link Geoid}.
     *              Latitude and longitude are both geodetic and defined with
     *              respect to the {@link #getEllipsoid() reference ellipsoid}.
     * @return point at the same location but as a Cartesian point in the {@link
     * #getBodyFrame() body frame}.
     * @see #transform(Vector3D, Frame, AbsoluteDate)
     */
    @Override
    public Vector3D transform(final GeodeticPoint point) {
        try {
            // convert orthometric height to height above ellipsoid using undulation
            // TODO pass in date to allow user to specify
            final double undulation = this.getUndulation(
                    point.getLatitude(),
                    point.getLongitude(),
                    this.defaultDate
            );
            final GeodeticPoint ellipsoidal = new GeodeticPoint(
                    point.getLatitude(),
                    point.getLongitude(),
                    point.getAltitude() + undulation
            );
            // transform using reference ellipsoid
            return this.getEllipsoid().transform(ellipsoidal);
        } catch (OrekitException e) {
            //this method, as defined in BodyShape, is not permitted to throw
            //an OrekitException, so wrap in an exception we can throw.
            throw new RuntimeException(e);
        }
    }

}
