/* Copyright 2002-2025 CS GROUP
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
package org.orekit.frames;


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;

/** Terrestrial Intermediate Reference Frame.
 * <p> The pole motion is not considered : Pseudo Earth Fixed Frame. It handles
 * the earth rotation angle, its parent frame is the {@link CIRFProvider}</p>
 */
class TIRFProvider implements EOPBasedTransformProvider {

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** UT1 time scale. */
    private final transient TimeScale ut1;

    /** ERA function. */
    private final transient TimeScalarFunction era;

    /** Simple constructor.
     * @param eopHistory EOP history
     * @param ut1 the UT1 time scale.
     */
    protected TIRFProvider(final EOPHistory eopHistory, final TimeScale ut1) {

        this.ut1        = ut1;
        this.eopHistory = eopHistory;
        this.era        = eopHistory.getConventions().getEarthOrientationAngleFunction(
                ut1,
                eopHistory.getTimeScales().getTAI());

    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public TIRFProvider getNonInterpolatingProvider() {
        return new TIRFProvider(eopHistory.getEOPHistoryWithoutCachedTidalCorrection(), ut1);
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        return new Transform(date, getRotation(date), getRotationRate(date));
    }

    /** {@inheritDoc} */
    @Override
    public KinematicTransform getKinematicTransform(final AbsoluteDate date) {
        return KinematicTransform.of(date, getRotation(date), getRotationRate(date));
    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        return StaticTransform.of(date, getRotation(date));
    }

    /** Form rotation to parent CIRF.
     * @param date transform date
     * @return rotation to parent at date
     * @since 12.1
     */
    private Rotation getRotation(final AbsoluteDate date) {
        // compute proper rotation
        final double correctedERA = era.value(date);
        // set up the transform from parent CIRF
        return new Rotation(Vector3D.PLUS_K, correctedERA, RotationConvention.FRAME_TRANSFORM);
    }

    /** Form rotation rate w.r.t. parent CIRF.
     * @param date transform date
     * @return rotation rate at date
     * @since 12.1
     */
    private Vector3D getRotationRate(final AbsoluteDate date) {
        // compute true angular rotation of Earth, in rad/s
        final double lod = (eopHistory == null) ? 0.0 : eopHistory.getLOD(date);
        final double omp = AVE * (1 - lod / Constants.JULIAN_DAY);
        return new Vector3D(omp, Vector3D.PLUS_K);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        return new FieldTransform<>(date, getRotation(date), getRotationRate(date));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> getKinematicTransform(final FieldAbsoluteDate<T> date) {
        return FieldKinematicTransform.of(date, getRotation(date), getRotationRate(date));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
        return FieldStaticTransform.of(date, getRotation(date));
    }

    /** Form rotation to parent CIRF.
     * @param <T> type of the elements
     * @param date transform date
     * @return rotation to parent at date
     * @since 12.1
     */
    private <T extends CalculusFieldElement<T>> FieldRotation<T> getRotation(final FieldAbsoluteDate<T> date) {
        // compute proper rotation
        final T correctedERA = era.value(date);

        // set up the transform from parent CIRF
        return new FieldRotation<>(
                FieldVector3D.getPlusK(date.getField()),
                correctedERA,
                RotationConvention.FRAME_TRANSFORM);
    }

    /** Form rotation rate w.r.t. parent CIRF.
     * @param <T> type of the elements
     * @param date transform date
     * @return rotation rate at date
     * @since 12.1
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> getRotationRate(final FieldAbsoluteDate<T> date) {
        // compute true angular rotation of Earth, in rad/s
        final T lod = (eopHistory == null) ? date.getField().getZero() : eopHistory.getLOD(date);
        final T omp = lod.divide(Constants.JULIAN_DAY).subtract(1).multiply(-AVE);
        return new FieldVector3D<>(omp, Vector3D.PLUS_K);
    }

    /** Get the Earth Rotation Angle at the current date.
     * @param  date the date
     * @return Earth Rotation Angle at the current date in radians
     */
    public double getEarthRotationAngle(final AbsoluteDate date) {
        return MathUtils.normalizeAngle(era.value(date), 0);
    }

}
