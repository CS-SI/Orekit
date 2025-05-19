/* Copyright 2022-2025 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.estimation.iod;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * HerrickGibbs position-based Initial Orbit Determination (IOD) algorithm.
 * <p>
 * An orbit is determined from three position vectors. Because Gibbs IOD algorithm
 * is limited when the position vectors are to close to one other, Herrick-Gibbs
 * IOD algorithm is a variation made to address this limitation.
 * Because this method is only approximate, it is not robust as the Gibbs method
 * for other cases.
 * </p>
 * @see Vallado, D., Fundamentals of Astrodynamics and Applications, 4th Edition.
 *
 * @author Bryan Cazabonne
 * @since 13.1
 */
public class IodHerrickGibbs {

    /** Gravitational constant. **/
    private final double mu;

    /** Threshold for checking coplanar vectors (Ref: Equation 7-27). */
    private final double COPLANAR_THRESHOLD = FastMath.toRadians(3.);

    /** Constructor.
     * @param mu gravitational constant
     */
    public IodHerrickGibbs(final double mu) {
        this.mu = mu;
    }

    /** Give an initial orbit estimation, assuming Keplerian motion.
     * <p>
     * All observations should be from the same location.
     * </p>
     * @param frame measurements frame, used as output orbit frame
     * @param p1 First position measurement
     * @param p2 Second position measurement
     * @param p3 Third position measurement
     * @return an initial orbit estimation at the central date
     *         (i.e., date of the second position measurement)
     */
    public Orbit estimate(final Frame frame, final Position p1, final Position p2, final Position p3) {
        return estimate(frame, p1.getPosition(), p1.getDate(),
                        p2.getPosition(), p2.getDate(),
                        p3.getPosition(), p3.getDate());
    }

    /** Give an initial orbit estimation, assuming Keplerian motion.
     * <p>
     * All observations should be from the same location.
     * </p>
     * @param frame measurements frame, used as output orbit frame
     * @param pv1 First PV measurement
     * @param pv2 Second PV measurement
     * @param pv3 Third PV measurement
     * @return an initial orbit estimation at the central date
     *         (i.e., date of the second PV measurement)
     */
    public Orbit estimate(final Frame frame, final PV pv1, final PV pv2, final PV pv3) {
        return estimate(frame, pv1.getPosition(), pv1.getDate(),
                        pv2.getPosition(), pv2.getDate(),
                        pv3.getPosition(), pv3.getDate());
    }

    /** Give an initial orbit estimation, assuming Keplerian motion.
     * <p>
     * All observations should be from the same location.
     * </p>
     * @param frame measurements frame, used as output orbit frame
     * @param r1 position vector 1, expressed in frame
     * @param date1 epoch of position vector 1
     * @param r2 position vector 2, expressed in frame
     * @param date2 epoch of position vector 2
     * @param r3 position vector 3, expressed in frame
     * @param date3 epoch of position vector 3
     * @return an initial orbit estimation at the central date
     *         (i.e., date of the second position measurement)
     */
    public Orbit estimate(final Frame frame,
                          final Vector3D r1, final AbsoluteDate date1,
                          final Vector3D r2, final AbsoluteDate date2,
                          final Vector3D r3, final AbsoluteDate date3) {

        // Verify that measurements are not at the same date
        verifyMeasurementEpochs(date1, date2, date3);

        // Get the difference of time between the position vectors
        final double tau32 = date3.getDate().durationFrom(date2.getDate());
        final double tau31 = date3.getDate().durationFrom(date1.getDate());
        final double tau21 = date2.getDate().durationFrom(date1.getDate());

        // Check that measurements are in the same plane
        final double num = r1.normalize().dotProduct(r2.normalize().crossProduct(r3.normalize()));
        if (FastMath.abs(FastMath.PI / 2.0 - FastMath.acos(num)) > COPLANAR_THRESHOLD) {
            throw new OrekitException(OrekitMessages.NON_COPLANAR_POINTS);
        }

        // Compute velocity vector
        final double muOTwelve = mu / 12.0;
        final double coefficient1 = -tau32 * (1.0 / (tau21 * tau31) + muOTwelve / pow3(r1.getNorm()));
        final double coefficient2 = (tau32 - tau21) * (1.0 / (tau21 * tau32) + muOTwelve / pow3(r2.getNorm()));
        final double coefficient3 = tau21 * (1.0 / (tau32 * tau31) + muOTwelve / pow3(r3.getNorm()));
        final Vector3D v2 = r1.scalarMultiply(coefficient1).add(r2.scalarMultiply(coefficient2)).add(r3.scalarMultiply(coefficient3));

        // Convert to an orbit
        return new CartesianOrbit( new PVCoordinates(r2, v2), frame, date2, mu);
    }

    /** Give an initial orbit estimation, assuming Keplerian motion.
     * <p>
     * All observations should be from the same location.
     * </p>
     * @param frame measurements frame, used as output orbit frame
     * @param r1 position vector 1, expressed in frame
     * @param date1 epoch of position vector 1
     * @param r2 position vector 2, expressed in frame
     * @param date2 epoch of position vector 2
     * @param r3 position vector 3, expressed in frame
     * @param date3 epoch of position vector 3
     * @param <T> type of the elements
     * @return an initial orbit estimation at the central date
     *         (i.e., date of the second position measurement)
     */
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> estimate(final Frame frame,
                                                                      final FieldVector3D<T> r1, final FieldAbsoluteDate<T> date1,
                                                                      final FieldVector3D<T> r2, final FieldAbsoluteDate<T> date2,
                                                                      final FieldVector3D<T> r3, final FieldAbsoluteDate<T> date3) {

        // Verify that measurements are not at the same date
        verifyMeasurementEpochs(date1.toAbsoluteDate(), date2.toAbsoluteDate(), date3.toAbsoluteDate());

        // Get the difference of time between the position vectors
        final T tau32 = date3.getDate().durationFrom(date2.getDate());
        final T tau31 = date3.getDate().durationFrom(date1.getDate());
        final T tau21 = date2.getDate().durationFrom(date1.getDate());

        // Check that measurements are in the same plane
        final T num = r1.normalize().dotProduct(r2.normalize().crossProduct(r3.normalize()));
        if (FastMath.abs(FastMath.PI / 2.0 - FastMath.acos(num.getReal())) > COPLANAR_THRESHOLD) {
            throw new OrekitException(OrekitMessages.NON_COPLANAR_POINTS);
        }

        // Compute velocity vector
        final double muOTwelve = mu / 12.0;
        final T coefficient1 = tau32.negate().multiply(tau21.multiply(tau31).reciprocal().add(pow3(r1.getNorm()).reciprocal().multiply(muOTwelve)));
        final T coefficient2 = tau32.subtract(tau21).multiply(tau21.multiply(tau32).reciprocal().add(pow3(r2.getNorm()).reciprocal().multiply(muOTwelve)));
        final T coefficient3 = tau21.multiply(tau32.multiply(tau31).reciprocal().add(pow3(r3.getNorm()).reciprocal().multiply(muOTwelve)));
        final FieldVector3D<T> v2 = r1.scalarMultiply(coefficient1).add(r2.scalarMultiply(coefficient2)).add(r3.scalarMultiply(coefficient3));

        // Convert to an orbit
        return new FieldCartesianOrbit<>( new FieldPVCoordinates<>(r2, v2), frame, date2, date1.getField().getZero().add(mu));
    }

    /** Compute the cubic value.
     * @param value value
     * @return value^3
     */
    private static double pow3(final double value) {
        return value * value * value;
    }

    /** Compute the cubic value.
     * @param value value
     * @param <T> type of the elements
     * @return value^3
     */
    private static <T extends CalculusFieldElement<T>> T pow3(final T value) {
        return value.multiply(value).multiply(value);
    }

    /** Verifies that measurements are not at the same date.
     * @param date1 epoch of position vector 1
     * @param date2 epoch of position vector 2
     * @param date3 epoch of position vector 3
     */
    private void verifyMeasurementEpochs(final AbsoluteDate date1, final AbsoluteDate date2, final AbsoluteDate date3) {
        if (date1.equals(date2) || date1.equals(date3) || date2.equals(date3)) {
            throw new OrekitException(OrekitMessages.NON_DIFFERENT_DATES_FOR_OBSERVATIONS, date1, date2, date3,
                    date2.durationFrom(date1), date3.durationFrom(date1), date3.durationFrom(date2));
        }
    }
}
