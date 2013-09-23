/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.frames;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.utils.IERSConventions;

/** Provider for True of Date (ToD) frame.
 * <p>This frame handles nutation effects according to selected IERS conventions.</p>
 * <p>Transform is computed with reference to the {@link MODProvider Mean of Date} frame.</p>
 * @author Pascal Parraud
 */
class TODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130922L;

    /** EOP history. */
    private final EOPHistoryEquinox eopHistory;

    /** Function computing the mean obliquity. */
    private final TimeFunction<Double> obliquityFunction;

    /** Function computing the nutation angles. */
    private final TimeFunction<double[]> nutationFunction;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @param eopHistory EOP history
     * @exception OrekitException if IERS conventions tables cannot be read
     */
    public TODProvider(final IERSConventions conventions, final EOPHistoryEquinox eopHistory)
        throws OrekitException {
        this.eopHistory        = eopHistory;
        this.obliquityFunction = conventions.getMeanObliquityFunction();
        this.nutationFunction  = conventions.getNutationFunction();
    }

    /** Get the EOP history.
     * @return EOP history
     */
    EOPHistoryEquinox getEOPHistory() {
        return eopHistory;
    }

    /** Get the LoD (Length of Day) value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     * @exception TimeStampedCacheException if EOP data cannot be retrieved
     */
    double getLOD(final AbsoluteDate date) throws TimeStampedCacheException {
        return (eopHistory == null) ? 0.0 : eopHistory.getLOD(date);
    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        return (eopHistory == null) ? PoleCorrection.NULL_CORRECTION : eopHistory.getPoleCorrection(date);
    }

    /** Get the transform from Mean Of Date at specified date.
     * <p>The update considers the nutation effects from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // compute nutation angles
        final double[] angles = nutationFunction.value(date);

        // compute the mean obliquity of the ecliptic
        final double moe = obliquityFunction.value(date);

        double dpsi = angles[0];
        double deps = angles[1];
        if (eopHistory != null) {
            // apply the corrections for the nutation parameters
            final double[] correction = eopHistory.getNutationCorrection(date);
            dpsi += correction[0];
            deps += correction[1];
        }

        // compute the true obliquity of the ecliptic
        final double toe = moe + deps;

        // set up the elementary rotations for nutation
        final Rotation r1 = new Rotation(Vector3D.PLUS_I,  toe);
        final Rotation r2 = new Rotation(Vector3D.PLUS_K,  dpsi);
        final Rotation r3 = new Rotation(Vector3D.PLUS_I, -moe);

        // complete nutation
        final Rotation precession = r1.applyTo(r2.applyTo(r3));

        // set up the transform from parent MOD
        return new Transform(date, precession);

    }

    /** Get the Equation of the Equinoxes at the current date.
     * @param  date the date
     * @return equation of the equinoxes
     * @exception OrekitException if nutation model cannot be computed
     */
    public double getEquationOfEquinoxes(final AbsoluteDate date)
        throws OrekitException {

        // compute nutation angles
        final double[] angles = nutationFunction.value(date);

        // nutation in longitude
        final double dPsi = angles[0];

        // mean obliquity of ecliptic
        final double moe = obliquityFunction.value(date);

        // original definition of equation of equinoxes
        final double eqe = dPsi * FastMath.cos(moe);

        // apply correction if needed
        return eqe + angles[2];

    }

}
