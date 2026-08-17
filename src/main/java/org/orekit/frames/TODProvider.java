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
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeVectorFunction;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.IERSConventions;

/** Provider for True of Date (ToD) frame.
 * <p>This frame handles nutation effects according to selected IERS conventions.</p>
 * <p>Transform is computed with reference to the {@link MODProvider Mean of Date} frame.</p>
 * @author Pascal Parraud
 */
class TODProvider implements EOPBasedTransformProvider {

    /** Conventions. */
    private final IERSConventions conventions;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Function computing the mean obliquity. */
    private final transient TimeScalarFunction obliquityFunction;

    /** Function computing the nutation angles. */
    private final transient TimeVectorFunction nutationFunction;


    /**
     * Simple constructor.
     *  @param conventions IERS conventions to apply
     * @param eopHistory  EOP history, or {@code null} if no correction should be
     *                    applied.
     * @param timeScales         TAI time scale.
     */
    TODProvider(final IERSConventions conventions,
                final EOPHistory eopHistory,
                final TimeScales timeScales) {
        this.conventions       = conventions;
        this.eopHistory        = eopHistory;
        this.obliquityFunction = conventions.getMeanObliquityFunction(timeScales);
        this.nutationFunction  =
                conventions.getNutationFunction(timeScales);
    }

    /**
     * Private constructor.
     *
     * @param conventions       IERS conventions to use.
     * @param eopHistory        or {@code null} if no correction should be applied.
     * @param obliquityFunction to use.
     * @param nutationFunction  to use.
     */
    private TODProvider(final IERSConventions conventions,
                        final EOPHistory eopHistory,
                        final TimeScalarFunction obliquityFunction,
                        final TimeVectorFunction nutationFunction) {
        this.conventions = conventions;
        this.eopHistory = eopHistory;
        this.obliquityFunction = obliquityFunction;
        this.nutationFunction = nutationFunction;
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public TODProvider getNonInterpolatingProvider() {
        return new TODProvider(conventions, eopHistory.getEOPHistoryWithoutCachedTidalCorrection(),
                obliquityFunction, nutationFunction);
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {

        // use automatic differentiation to compute the rotation derivatives
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final UnivariateDerivative2 dt = new UnivariateDerivative2(0, 1, 0);
        final FieldAbsoluteDate<UnivariateDerivative2> ud2Date =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(dt);

        // set up the transform from parent MOD
        return new Transform(date, new AngularCoordinates(getRotation(ud2Date)));

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

        // set up the kinematic transform from parent MOD
        return KinematicTransform.of(date, derivatives.getRotation(), derivatives.getRotationRate());

    }

    /** Replace the instance with a data transfer object for serialization.
    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation derivatives
        final FieldAbsoluteDate<FieldUnivariateDerivative2<T>> fud2Date = date.toFUD2Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud2Date));

        // set up the transform from parent MOD
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

        // set up the kinematic transform from parent MOD
        return FieldKinematicTransform.of(date, rotation, derivatives.getRotationRate());

    }

    /** Compute the complete nutation rotation.
     * @param date current date
     * @param <T> type of the field elements
     * @return complete nutation rotation
     */
    private <T extends CalculusFieldElement<T>> FieldRotation<T> getRotation(final FieldAbsoluteDate<T> date) {

        // compute nutation angles
        final T[] angles = nutationFunction.value(date);

        // compute the mean obliquity of the ecliptic
        final T moe = obliquityFunction.value(date);

        T dpsi = angles[0];
        T deps = angles[1];
        if (eopHistory != null) {
            // apply the corrections for the nutation parameters
            final T[] correction = eopHistory.getEquinoxNutationCorrection(date);
            dpsi = dpsi.add(correction[0]);
            deps = deps.add(correction[1]);
        }

        // compute the true obliquity of the ecliptic
        final T toe = moe.add(deps);

        // complete nutation
        return new FieldRotation<>(RotationOrder.XZX, RotationConvention.FRAME_TRANSFORM,
                                   moe, dpsi.negate(), toe.negate());

    }

}
