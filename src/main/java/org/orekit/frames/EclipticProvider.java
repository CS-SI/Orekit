/* Contributed in the public domain.
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScales;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.IERSConventions;

/**
 * An inertial frame aligned with the ecliptic.
 * <p>
 * The IAU defines the ecliptic as "the plane perpendicular to the mean heliocentric
 * orbital angular momentum vector of the Earth-Moon barycentre in the BCRS (IAU 2006
 * Resolution B1)." The +z axis is aligned with the angular momentum vector, and the +x
 * axis is aligned with +x axis of {@link Frames#getMOD(IERSConventions) MOD}.
 * </p>
 *
 * <p>
 * This implementation agrees with the JPL 406 ephemerides to within 0.5 arc seconds.
 * </p>
 *
 * @since 7.0
 */
public class EclipticProvider implements TransformProvider {

    /** IERS conventions. */
    private final IERSConventions conventions;

    /** the obliquity of the ecliptic, in radians as a function of time. */
    private final transient TimeScalarFunction obliquity;

    /**
     * Create a transform provider from MOD to an ecliptically aligned frame.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param conventions IERS conventions
     * @see #EclipticProvider(IERSConventions, TimeScales)
     */
    @DefaultDataContext
    public EclipticProvider(final IERSConventions conventions) {
        this(conventions, DataContext.getDefault().getTimeScales());
    }

    /**
     * Create a transform provider from MOD to an ecliptically aligned frame.
     * @param conventions IERS conventions
     * @param timeScales to use in computing the transformation.
     * @since 10.1
     */
    public EclipticProvider(final IERSConventions conventions,
                            final TimeScales timeScales) {
        this.conventions = conventions;
        this.obliquity   = conventions.getMeanObliquityFunction(timeScales);
    }

    @Override
    public Transform getTransform(final AbsoluteDate date) {
        // use automatic differentiation to compute the rotation derivatives
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final UnivariateDerivative2 dt = new UnivariateDerivative2(0, 1, 0);
        final FieldAbsoluteDate<UnivariateDerivative2> ud2Date =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(dt);

        return new Transform(date, new AngularCoordinates(getRotation(ud2Date)));
    }

    @Override
    public KinematicTransform getKinematicTransform(final AbsoluteDate date) {
        // use automatic differentiation to compute the rotation rate
        final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
        final UnivariateDerivative1 dt = new UnivariateDerivative1(0, 1);
        final FieldAbsoluteDate<UnivariateDerivative1> ud1Date =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(dt);
        final AngularCoordinates derivatives = new AngularCoordinates(getRotation(ud1Date));

        return KinematicTransform.of(date, derivatives.getRotation(), derivatives.getRotationRate());
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation derivatives
        final FieldAbsoluteDate<FieldUnivariateDerivative2<T>> fud2Date = date.toFUD2Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud2Date));

        // preserve both field and time derivatives
        return new FieldTransform<>(date,
                                    new FieldAngularCoordinates<>(rotation,
                                                                  derivatives.getRotationRate(),
                                                                  derivatives.getRotationAcceleration()));
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> getKinematicTransform(final FieldAbsoluteDate<T> date) {
        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation rate
        final FieldAbsoluteDate<FieldUnivariateDerivative1<T>> fud1Date = date.toFUD1Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud1Date));

        return FieldKinematicTransform.of(date, rotation, derivatives.getRotationRate());
    }

    /** Compute the ecliptic rotation.
     * @param date current date
     * @param <T> type of the field elements
     * @return ecliptic rotation
     */
    private <T extends CalculusFieldElement<T>> FieldRotation<T> getRotation(final FieldAbsoluteDate<T> date) {
        //mean obliquity of date
        final T epsA = obliquity.value(date);
        return new FieldRotation<>(FieldVector3D.getMinusI(date.getField()), epsA,
                                   RotationConvention.VECTOR_OPERATOR);
    }

}
