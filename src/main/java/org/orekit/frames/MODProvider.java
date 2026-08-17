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

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeVectorFunction;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.IERSConventions;

/** Mean Equator, Mean Equinox Frame.
 * <p>This frame handles precession effects according to to selected IERS conventions.</p>
 * <p>Its parent frame is the GCRF frame.
 * <p>It is sometimes called Mean of Date (MoD) frame.
 * @author Pascal Parraud
 */
class MODProvider implements TransformProvider {

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

        // use automatic differentiation to compute the rotation derivatives
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final UnivariateDerivative2 dt = new UnivariateDerivative2(0, 1, 0);
        final FieldAbsoluteDate<UnivariateDerivative2> ud2Date =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(dt);

        // complete precession
        final FieldRotation<UnivariateDerivative2> precession = getRotation(ud2Date);

        // set up the transform from parent GCRF
        return new Transform(date, new AngularCoordinates(precession));

    }

    /** {@inheritDoc} */
    @Override
    public KinematicTransform getKinematicTransform(final AbsoluteDate date) {

        // use automatic differentiation to compute the rotation rate
        final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
        final UnivariateDerivative1 dt = new UnivariateDerivative1(0, 1);
        final FieldAbsoluteDate<UnivariateDerivative1> ud1Date =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(dt);
        final AngularCoordinates derivatives = new AngularCoordinates(getRotation(ud1Date));

        // set up the kinematic transform from parent GCRF
        return KinematicTransform.of(date, derivatives.getRotation(), derivatives.getRotationRate());

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation derivatives
        final FieldAbsoluteDate<FieldUnivariateDerivative2<T>> fud2Date = date.toFUD2Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud2Date));

        // set up the transform from parent GCRF, preserving both field and time derivatives
        return new FieldTransform<>(date,
                                    new FieldAngularCoordinates<>(rotation,
                                                                  derivatives.getRotationRate(),
                                                                  derivatives.getRotationAcceleration()));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> getKinematicTransform(final FieldAbsoluteDate<T> date) {

        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation rate
        final FieldAbsoluteDate<FieldUnivariateDerivative1<T>> fud1Date = date.toFUD1Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud1Date));

        // set up the kinematic transform from parent GCRF
        return FieldKinematicTransform.of(date, rotation, derivatives.getRotationRate());

    }

    /** Compute the complete precession rotation.
     * @param date current date
     * @param <T> type of the field elements
     * @return complete precession rotation
     */
    private <T extends CalculusFieldElement<T>> FieldRotation<T> getRotation(final FieldAbsoluteDate<T> date) {

        // compute the precession angles phiA, omegaA, chiA
        final T[] angles = precessionFunction.value(date);

        // complete precession
        return getR4(date.getField()).compose(
                        new FieldRotation<>(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                            angles[0].negate(), angles[1].negate(), angles[2]),
                        RotationConvention.FRAME_TRANSFORM);

    }

    /** Get the constant rotation converted to the specified field.
     * @param field field to which the elements belong
     * @param <T> type of the field elements
     * @return constant rotation converted to the specified field
     */
    @SuppressWarnings("unchecked")
    private <T extends CalculusFieldElement<T>> FieldRotation<T> getR4(final Field<T> field) {
        synchronized (fieldR4) {
            return (FieldRotation<T>) fieldR4.computeIfAbsent(field, f -> new FieldRotation<>(field, r4));
        }
    }

}
