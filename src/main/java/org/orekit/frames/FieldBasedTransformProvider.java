/* Copyright 2002-2026 CS Group
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
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;

/** Interface for geocentric frame transform providers using automatic differentiation.
 * @author Davide Degavi
 * @author Romain Serra
 * @since 13.1.8
 */
interface FieldBasedTransformProvider extends TransformProvider {

    /** {@inheritDoc} */
    @Override
    default Transform getTransform(final AbsoluteDate date) {

        // use automatic differentiation to compute the rotation derivatives
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final UnivariateDerivative2 dt = new UnivariateDerivative2(0, 1, 0);
        final FieldAbsoluteDate<UnivariateDerivative2> ud2Date =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(dt);

        // set up the transform from parent frame
        return new Transform(date, new AngularCoordinates(getRotation(ud2Date)));

    }

    /** {@inheritDoc} */
    @Override
    default KinematicTransform getKinematicTransform(final AbsoluteDate date) {

        // use automatic differentiation to compute the rotation rate
        final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
        final UnivariateDerivative1 dt = new UnivariateDerivative1(0, 1);
        final FieldAbsoluteDate<UnivariateDerivative1> ud1Date =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(dt);
        final AngularCoordinates derivatives = new AngularCoordinates(getRotation(ud1Date));

        // set up the kinematic transform from parent frame
        return KinematicTransform.of(date, derivatives.getRotation(), derivatives.getRotationRate());

    }

    /** {@inheritDoc} */
    @Override
    default StaticTransform getStaticTransform(final AbsoluteDate date) {
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        return StaticTransform.of(date, getRotation(fieldDate).toRotation());
    }

    /** {@inheritDoc} */
    @Override
    default <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation derivatives
        final FieldAbsoluteDate<FieldUnivariateDerivative2<T>> fud2Date = date.toFUD2Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud2Date));

        // set up the transform from parent frame
        return new FieldTransform<>(date,
                                    new FieldAngularCoordinates<>(rotation,
                                                                  derivatives.getRotationRate(),
                                                                  derivatives.getRotationAcceleration()));

    }

    /** {@inheritDoc} */
    @Override
    default <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> getKinematicTransform(final FieldAbsoluteDate<T> date) {

        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation rate
        final FieldAbsoluteDate<FieldUnivariateDerivative1<T>> fud1Date = date.toFUD1Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud1Date));

        // set up the kinematic transform from parent frame
        return FieldKinematicTransform.of(date, rotation, derivatives.getRotationRate());

    }

    /** {@inheritDoc} */
    @Override
    default <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
        return FieldStaticTransform.of(date, getRotation(date));
    }

    /** Compute the complete rotation from parent.
     * @param date current date
     * @param <T> type of the field elements
     * @return rotation
     */
    <T extends CalculusFieldElement<T>> FieldRotation<T> getRotation(FieldAbsoluteDate<T> date);

}
