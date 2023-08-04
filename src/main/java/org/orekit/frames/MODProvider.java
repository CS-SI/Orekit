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
package org.orekit.frames;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeVectorFunction;
import org.orekit.utils.IERSConventions;

/** Mean Equator, Mean Equinox Frame.
 * <p>This frame handles precession effects according to to selected IERS conventions.</p>
 * <p>Its parent frame is the GCRF frame.
 * <p>It is sometimes called Mean of Date (MoD) frame.
 * @author Pascal Parraud
 */
class MODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130920L;

    /** Conventions. */
    private final IERSConventions conventions;

    /** Function computing the precession angles. */
    private final transient TimeVectorFunction precessionFunction;

    /** Constant rotation between ecliptic and equator poles at J2000.0. */
    private final Rotation r4;

    /** Constant rotations between ecliptic and equator poles at J2000.0. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, FieldRotation<? extends CalculusFieldElement<?>>> fieldR4;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @param timeScales used to define this frame.
     */
    MODProvider(final IERSConventions conventions, final TimeScales timeScales) {
        this.conventions        = conventions;
        this.precessionFunction = conventions.getPrecessionFunction(timeScales);
        final TimeScalarFunction epsilonAFunction =
                conventions.getMeanObliquityFunction(timeScales);
        final AbsoluteDate date0 = conventions.getNutationReferenceEpoch(timeScales);
        final double epsilon0 = epsilonAFunction.value(date0);
        r4 = new Rotation(Vector3D.PLUS_I, epsilon0, RotationConvention.FRAME_TRANSFORM);
        fieldR4 = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {

        // compute the precession angles phiA, omegaA, chiA
        final double[] angles = precessionFunction.value(date);

        // complete precession
        final Rotation precession = r4.compose(new Rotation(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                            -angles[0], -angles[1], angles[2]),
                                               RotationConvention.FRAME_TRANSFORM);

        // set up the transform from parent GCRF
        return new Transform(date, precession);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // compute the precession angles phiA, omegaA, chiA
        final T[] angles = precessionFunction.value(date);

        @SuppressWarnings("unchecked")
        FieldRotation<T> fR4 = (FieldRotation<T>) fieldR4.get(date.getField());
        if (fR4 == null) {
            fR4 = new FieldRotation<>(date.getField(), r4);
            fieldR4.put(date.getField(), fR4);
        }

        // complete precession
        final FieldRotation<T> precession = fR4.compose(new FieldRotation<>(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                                            angles[0].negate(),
                                                                            angles[1].negate(),
                                                                            angles[2]),
                                                        RotationConvention.FRAME_TRANSFORM);

        // set up the transform from parent GCRF
        return new FieldTransform<>(date, precession);

    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DataTransferObject(conventions);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131209L;

        /** Conventions. */
        private final IERSConventions conventions;

        /** Simple constructor.
         * @param conventions IERSConventions conventions
         */
        DataTransferObject(final IERSConventions conventions) {
            this.conventions = conventions;
        }

        /** Replace the deserialized data transfer object with a {@link MODProvider}.
         * @return replacement {@link MODProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new MODProvider(conventions,
                        DataContext.getDefault().getTimeScales());
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
