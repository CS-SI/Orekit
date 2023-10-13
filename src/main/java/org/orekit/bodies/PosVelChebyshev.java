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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;


/** Position-Velocity model based on Chebyshev polynomials.
 * <p>This class represent the most basic element of the piecewise ephemerides
 * for solar system bodies like JPL DE 405 ephemerides.</p>
 * @see JPLEphemeridesLoader
 * @author Luc Maisonobe
 */
class PosVelChebyshev implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20151023L;

    /** Time scale in which the ephemeris is defined. */
    private final TimeScale timeScale;

    /** Start of the validity range of the instance. */
    private final AbsoluteDate start;

    /** Duration of validity range of the instance. */
    private final double duration;

    /** Chebyshev polynomials coefficients for the X component. */
    private final double[] xCoeffs;

    /** Chebyshev polynomials coefficients for the Y component. */
    private final double[] yCoeffs;

    /** Chebyshev polynomials coefficients for the Z component. */
    private final double[] zCoeffs;

    /** Velocity scale for internal use. */
    private final double vScale;
    /** Acceleration scale for internal use. */
    private final double aScale;

    /** Simple constructor.
     * @param start start of the validity range of the instance
     * @param timeScale time scale in which the ephemeris is defined
     * @param duration duration of the validity range of the instance
     * @param xCoeffs Chebyshev polynomials coefficients for the X component
     * (a reference to the array will be stored in the instance)
     * @param yCoeffs Chebyshev polynomials coefficients for the Y component
     * (a reference to the array will be stored in the instance)
     * @param zCoeffs Chebyshev polynomials coefficients for the Z component
     * (a reference to the array will be stored in the instance)
     */
    PosVelChebyshev(final AbsoluteDate start, final TimeScale timeScale, final double duration,
                    final double[] xCoeffs, final double[] yCoeffs, final double[] zCoeffs) {
        this.start     = start;
        this.timeScale = timeScale;
        this.duration  = duration;
        this.xCoeffs   = xCoeffs;
        this.yCoeffs   = yCoeffs;
        this.zCoeffs   = zCoeffs;
        this.vScale = 2 / duration;
        this.aScale = this.vScale * this.vScale;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return start;
    }

    /** Compute value of Chebyshev's polynomial independent variable.
     * @param date date
     * @return double independent variable value
     */
    private double computeValueIndependentVariable(final AbsoluteDate date) {
        return (2 * date.offsetFrom(start, timeScale) - duration) / duration;
    }

    /** Compute value of Chebyshev's polynomial independent variable.
     * @param date date
     * @param <T> type of the field elements
     * @return <T> independent variable value
     */
    private <T extends CalculusFieldElement<T>> T computeValueIndependentVariable(final FieldAbsoluteDate<T> date) {
        return date.offsetFrom(new FieldAbsoluteDate<>(date.getField(), start), timeScale).multiply(2).subtract(duration).divide(duration);
    }

    /** Check if a date is in validity range.
     * @param date date to check
     * @return true if date is in validity range
     */
    public boolean inRange(final AbsoluteDate date) {
        final double dt = date.offsetFrom(start, timeScale);
        return dt >= -0.001 && dt <= duration + 0.001;
    }

    /** Get the position at a specified date.
     * @param date date at which position is requested
     * @return position at specified date
     */
    Vector3D getPosition(final AbsoluteDate date) {

        // normalize date
        final double t = computeValueIndependentVariable(date);
        final double twoT = 2 * t;

        // initialize Chebyshev polynomials recursion
        double pKm1 = 1;
        double pK   = t;
        double xP   = xCoeffs[0];
        double yP   = yCoeffs[0];
        double zP   = zCoeffs[0];

        // combine polynomials by applying coefficients
        for (int k = 1; k < xCoeffs.length; ++k) {

            // consider last computed polynomials on position
            xP += xCoeffs[k] * pK;
            yP += yCoeffs[k] * pK;
            zP += zCoeffs[k] * pK;

            // compute next Chebyshev polynomial value
            final double pKm2 = pKm1;
            pKm1 = pK;
            pK   = twoT * pKm1 - pKm2;

        }

        return new Vector3D(xP, yP, zP);
    }

    /** Get the position at a specified date.
     * @param date date at which position is requested
     * @param <T> type of the field elements
     * @return position at specified date
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date) {

        final T zero = date.getField().getZero();
        final T one  = date.getField().getOne();

        // normalize date
        final T t = computeValueIndependentVariable(date);
        final T twoT = t.add(t);

        // initialize Chebyshev polynomials recursion
        T pKm1 = one;
        T pK   = t;
        T xP   = zero.add(xCoeffs[0]);
        T yP   = zero.add(yCoeffs[0]);
        T zP   = zero.add(zCoeffs[0]);

        // combine polynomials by applying coefficients
        for (int k = 1; k < xCoeffs.length; ++k) {

            // consider last computed polynomials on position
            xP = xP.add(pK.multiply(xCoeffs[k]));
            yP = yP.add(pK.multiply(yCoeffs[k]));
            zP = zP.add(pK.multiply(zCoeffs[k]));

            // compute next Chebyshev polynomial value
            final T pKm2 = pKm1;
            pKm1 = pK;
            pK   = twoT.multiply(pKm1).subtract(pKm2);

        }

        return new FieldVector3D<>(xP, yP, zP);

    }

    /** Get the position-velocity-acceleration at a specified date.
     * @param date date at which position-velocity-acceleration is requested
     * @return position-velocity-acceleration at specified date
     */
    PVCoordinates getPositionVelocityAcceleration(final AbsoluteDate date) {

        // normalize date
        final double t = computeValueIndependentVariable(date);
        final double twoT = 2 * t;

        // initialize Chebyshev polynomials recursion
        double pKm1 = 1;
        double pK   = t;
        double xP   = xCoeffs[0];
        double yP   = yCoeffs[0];
        double zP   = zCoeffs[0];

        // initialize Chebyshev polynomials derivatives recursion
        double qKm1 = 0;
        double qK   = 1;
        double xV   = 0;
        double yV   = 0;
        double zV   = 0;

        // initialize Chebyshev polynomials second derivatives recursion
        double rKm1 = 0;
        double rK   = 0;
        double xA   = 0;
        double yA   = 0;
        double zA   = 0;

        // combine polynomials by applying coefficients
        for (int k = 1; k < xCoeffs.length; ++k) {

            // consider last computed polynomials on position
            xP += xCoeffs[k] * pK;
            yP += yCoeffs[k] * pK;
            zP += zCoeffs[k] * pK;

            // consider last computed polynomials on velocity
            xV += xCoeffs[k] * qK;
            yV += yCoeffs[k] * qK;
            zV += zCoeffs[k] * qK;

            // consider last computed polynomials on acceleration
            xA += xCoeffs[k] * rK;
            yA += yCoeffs[k] * rK;
            zA += zCoeffs[k] * rK;

            // compute next Chebyshev polynomial value
            final double pKm2 = pKm1;
            pKm1 = pK;
            pK   = twoT * pKm1 - pKm2;

            // compute next Chebyshev polynomial derivative
            final double qKm2 = qKm1;
            qKm1 = qK;
            qK   = twoT * qKm1 + 2 * pKm1 - qKm2;

            // compute next Chebyshev polynomial second derivative
            final double rKm2 = rKm1;
            rKm1 = rK;
            rK   = twoT * rKm1 + 4 * qKm1 - rKm2;

        }

        return new PVCoordinates(new Vector3D(xP, yP, zP),
                                 new Vector3D(xV * vScale, yV * vScale, zV * vScale),
                                 new Vector3D(xA * aScale, yA * aScale, zA * aScale));

    }

    /** Get the position-velocity-acceleration at a specified date.
     * @param date date at which position-velocity-acceleration is requested
     * @param <T> type of the field elements
     * @return position-velocity-acceleration at specified date
     */
    <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> getPositionVelocityAcceleration(final FieldAbsoluteDate<T> date) {

        final T zero = date.getField().getZero();
        final T one  = date.getField().getOne();

        // normalize date
        final T t = computeValueIndependentVariable(date);
        final T twoT = t.add(t);

        // initialize Chebyshev polynomials recursion
        T pKm1 = one;
        T pK   = t;
        T xP   = zero.add(xCoeffs[0]);
        T yP   = zero.add(yCoeffs[0]);
        T zP   = zero.add(zCoeffs[0]);

        // initialize Chebyshev polynomials derivatives recursion
        T qKm1 = zero;
        T qK   = one;
        T xV   = zero;
        T yV   = zero;
        T zV   = zero;

        // initialize Chebyshev polynomials second derivatives recursion
        T rKm1 = zero;
        T rK   = zero;
        T xA   = zero;
        T yA   = zero;
        T zA   = zero;

        // combine polynomials by applying coefficients
        for (int k = 1; k < xCoeffs.length; ++k) {

            // consider last computed polynomials on position
            xP = xP.add(pK.multiply(xCoeffs[k]));
            yP = yP.add(pK.multiply(yCoeffs[k]));
            zP = zP.add(pK.multiply(zCoeffs[k]));

            // consider last computed polynomials on velocity
            xV = xV.add(qK.multiply(xCoeffs[k]));
            yV = yV.add(qK.multiply(yCoeffs[k]));
            zV = zV.add(qK.multiply(zCoeffs[k]));

            // consider last computed polynomials on acceleration
            xA = xA.add(rK.multiply(xCoeffs[k]));
            yA = yA.add(rK.multiply(yCoeffs[k]));
            zA = zA.add(rK.multiply(zCoeffs[k]));

            // compute next Chebyshev polynomial value
            final T pKm2 = pKm1;
            pKm1 = pK;
            pK   = twoT.multiply(pKm1).subtract(pKm2);

            // compute next Chebyshev polynomial derivative
            final T qKm2 = qKm1;
            qKm1 = qK;
            qK   = twoT.multiply(qKm1).add(pKm1.multiply(2)).subtract(qKm2);

            // compute next Chebyshev polynomial second derivative
            final T rKm2 = rKm1;
            rKm1 = rK;
            rK   = twoT.multiply(rKm1).add(qKm1.multiply(4)).subtract(rKm2);

        }

        return new FieldPVCoordinates<>(new FieldVector3D<>(xP, yP, zP),
                                        new FieldVector3D<>(xV.multiply(vScale), yV.multiply(vScale), zV.multiply(vScale)),
                                        new FieldVector3D<>(xA.multiply(aScale), yA.multiply(aScale), zA.multiply(aScale)));

    }

}
