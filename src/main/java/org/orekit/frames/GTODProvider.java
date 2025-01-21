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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Greenwich True Of Date Frame, also known as True of Date Rotating frame (TDR)
 * or Greenwich Rotating Coordinate frame (GCR).
 * <p> This frame handles the sidereal time according to IAU-82 model.</p>
 * <p> Its parent frame is the {@link TODProvider}.</p>
 * <p> The pole motion is not applied here.</p>
 * @author Pascal Parraud
 * @author Thierry Ceolin
 */
public class GTODProvider implements EOPBasedTransformProvider {

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** Conventions. */
    private final IERSConventions conventions;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** GAST function. */
    private final transient TimeScalarFunction gastFunction;

    /** Simple constructor.
     * @param conventions IERS conventions to use
     * @param eopHistory EOP history (may be null)
     * @param timeScales  set of time scales to use.
     * @since 10.1
     */
    protected GTODProvider(final IERSConventions conventions,
                           final EOPHistory eopHistory,
                           final TimeScales timeScales) {
        final TimeScale ut1 = eopHistory == null ?
                timeScales.getUTC() : // UT1 wihthout EOP is UTC
                timeScales.getUT1(eopHistory.getConventions(), eopHistory.isSimpleEop());
        this.conventions   = conventions;
        this.eopHistory    = eopHistory;
        this.gastFunction  = conventions.getGASTFunction(ut1, eopHistory, timeScales);
    }

    /**
     * Private constructor.
     *
     * @param conventions  IERS conventions to use
     * @param eopHistory   EOP history (may be null)
     * @param gastFunction GAST function
     */
    private GTODProvider(final IERSConventions conventions,
                         final EOPHistory eopHistory,
                         final TimeScalarFunction gastFunction) {
        this.conventions = conventions;
        this.eopHistory = eopHistory;
        this.gastFunction = gastFunction;
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public GTODProvider getNonInterpolatingProvider() {
        return new GTODProvider(conventions, eopHistory.getEOPHistoryWithoutCachedTidalCorrection(),
                gastFunction);
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

    /** Form rotation to parent TOD.
     * @param date transform date
     * @return rotation to parent at date
     * @since 12.1
     */
    private Rotation getRotation(final AbsoluteDate date) {
        // compute Greenwich apparent sidereal time, in radians
        final double gast = gastFunction.value(date);

        // set up the transform from parent TOD
        return new Rotation(Vector3D.PLUS_K, gast, RotationConvention.FRAME_TRANSFORM);
    }

    /** Form rotation rate w.r.t. parent TOD.
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

    /** Form rotation to parent TOD.
     * @param <T> type of the elements
     * @param date transform date
     * @return rotation to parent at date
     * @since 12.1
     */
    private <T extends CalculusFieldElement<T>> FieldRotation<T> getRotation(final FieldAbsoluteDate<T> date) {
        // compute Greenwich apparent sidereal time, in radians
        final T gast = gastFunction.value(date);

        // set up the transform from parent TOD
        return new FieldRotation<>(FieldVector3D.getPlusK(date.getField()), gast, RotationConvention.FRAME_TRANSFORM);
    }

    /** Form rotation rate w.r.t. parent TOD.
     * @param <T> type of the elements
     * @param date transform date
     * @return rotation rate at date
     * @since 12.1
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> getRotationRate(final FieldAbsoluteDate<T> date) {
        // compute true angular rotation of Earth, in rad/s
        final T lod = (eopHistory == null) ? date.getField().getZero() : eopHistory.getLOD(date);
        final T omp = lod.multiply(-1.0 / Constants.JULIAN_DAY).add(1).multiply(AVE);
        return new FieldVector3D<>(date.getField().getZero(),
                date.getField().getZero(),
                date.getField().getZero().add(omp));
    }

}
