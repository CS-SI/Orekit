/* Copyright 2002-2017 CS Systèmes d'Information
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

import java.io.Serializable;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;

/** Terrestrial Intermediate Reference Frame.
 * <p> The pole motion is not considered : Pseudo Earth Fixed Frame. It handles
 * the earth rotation angle, its parent frame is the {@link CIRFProvider}</p>
 */
class TIRFProvider implements EOPBasedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130919L;

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** UT1 time scale. */
    private final transient UT1Scale ut1;

    /** ERA function. */
    private final transient TimeScalarFunction era;

    /** Simple constructor.
     * @param eopHistory EOP history
     * @exception OrekitException if nutation cannot be computed
     */
    protected TIRFProvider(final EOPHistory eopHistory)
        throws OrekitException {

        this.ut1        = TimeScalesFactory.getUT1(eopHistory);
        this.eopHistory = eopHistory;
        this.era        = eopHistory.getConventions().getEarthOrientationAngleFunction(ut1);

    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public TIRFProvider getNonInterpolatingProvider()
        throws OrekitException {
        return new TIRFProvider(eopHistory.getNonInterpolatingEOPHistory());
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // compute proper rotation
        final double correctedERA = era.value(date);

        // compute true angular rotation of Earth, in rad/s
        final double lod = (eopHistory == null) ? 0.0 : eopHistory.getLOD(date);
        final double omp = AVE * (1 - lod / Constants.JULIAN_DAY);
        final Vector3D rotationRate = new Vector3D(omp, Vector3D.PLUS_K);

        // set up the transform from parent CIRF
        final Rotation rotation     = new Rotation(Vector3D.PLUS_K, correctedERA, RotationConvention.FRAME_TRANSFORM);
        return new Transform(date, rotation, rotationRate);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date)
        throws OrekitException {

        // compute proper rotation
        final T correctedERA = era.value(date);

        // compute true angular rotation of Earth, in rad/s
        final T lod = (eopHistory == null) ? date.getField().getZero() : eopHistory.getLOD(date);
        final T omp = lod.divide(Constants.JULIAN_DAY).subtract(1).multiply(-AVE);
        final FieldVector3D<T> rotationRate = new FieldVector3D<>(omp, Vector3D.PLUS_K);

        // set up the transform from parent CIRF
        final FieldRotation<T> rotation = new FieldRotation<>(FieldVector3D.getPlusK(date.getField()),
                                                              correctedERA,
                                                              RotationConvention.FRAME_TRANSFORM);
        return new FieldTransform<>(date, rotation, rotationRate);

    }

    /** Get the Earth Rotation Angle at the current date.
     * @param  date the date
     * @return Earth Rotation Angle at the current date in radians
     * @exception OrekitException if nutation model cannot be computed
     */
    public double getEarthRotationAngle(final AbsoluteDate date) throws OrekitException {
        return MathUtils.normalizeAngle(era.value(date), 0);
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(eopHistory);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131209L;

        /** EOP history. */
        private final EOPHistory eopHistory;

        /** Simple constructor.
         * @param eopHistory EOP history
         */
        DataTransferObject(final EOPHistory eopHistory) {
            this.eopHistory = eopHistory;
        }

        /** Replace the deserialized data transfer object with a {@link TIRFProvider}.
         * @return replacement {@link TIRFProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new TIRFProvider(eopHistory);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
