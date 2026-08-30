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
package org.orekit.bodies;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.orekit.frames.FieldKinematicTransform;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.KinematicTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/** Class for JPL inertial frame transform providers.
 * @author Luc Maisonobe
 * @author Davide Degavi
 * @author Romain Serra
 * @since 13.1.8
 */
class JPLInertialTransformProvider implements TransformProvider {

    /** Parent frame. */
    private final Frame definingFrame;
    /** Translation provider. */
    private final ExtendedPositionProvider pvProvider;
    /** IAU pole. */
    private final IAUPole iauPole;

    /**
     * Constructor.
     * @param definingFrame parent frame
     * @param pvProvider position provider
     * @param iauPole IAU pole
     */
    JPLInertialTransformProvider(final Frame definingFrame, final ExtendedPositionProvider pvProvider,
                                 final IAUPole iauPole) {
        this.definingFrame = definingFrame;
        this.pvProvider    = pvProvider;
        this.iauPole       = iauPole;
    }

    /**
     * Package private getter for the defining frame.
     * @return frame
     */
    Frame getDefiningFrame() {
        return definingFrame;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        // translation part
        final PVCoordinates pv = pvProvider.getPVCoordinates(date, definingFrame).negate();

        // use automatic differentiation to compute the rotation derivatives
        final AngularCoordinates angularCoordinates = getAngularCoordinates(date, true);

        // set up the transform from parent frame
        return new Transform(date, pv, angularCoordinates);

    }

    /** {@inheritDoc} */
    @Override
    public KinematicTransform getKinematicTransform(final AbsoluteDate date) {
        // translation part
        final PVCoordinates pv = pvProvider.getPVCoordinates(date, definingFrame).negate();

        // use automatic differentiation to compute the rotation rate
        final AngularCoordinates angularCoordinates = getAngularCoordinates(date, false);

        // set up the kinematic transform from parent frame
        return KinematicTransform.of(date, pv, angularCoordinates.getRotation(), angularCoordinates.getRotationRate());

    }

    /**
     * Compute the rotation with derivatives via automatic differentiation.
     * @param date date
     * @param order2 flag to use order 2 derivatives
     * @return angular coordinates
     */
    private AngularCoordinates getAngularCoordinates(final AbsoluteDate date, final boolean order2) {
        if (order2) {
            final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
            final UnivariateDerivative2 dt = new UnivariateDerivative2(0, 1, 0);
            final FieldAbsoluteDate<UnivariateDerivative2> ud2Date =
                    new FieldAbsoluteDate<>(field, date).shiftedBy(dt);
            return new AngularCoordinates(getRotation(ud2Date));
        } else {
            final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
            final UnivariateDerivative1 dt = new UnivariateDerivative1(0, 1);
            final FieldAbsoluteDate<UnivariateDerivative1> ud1Date =
                    new FieldAbsoluteDate<>(field, date).shiftedBy(dt);
            return new AngularCoordinates(getRotation(ud1Date));
        }
    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        // translation part
        final Vector3D position = pvProvider.getPVCoordinates(date, definingFrame).getPosition().negate();
        // compute rotation from ICRF frame to self,
        // as per the "Report of the IAU/IAG Working Group on Cartographic
        // Coordinates and Rotational Elements of the Planets and Satellites"
        // These definitions are common for all recent versions of this report
        // published every three years, the precise values of pole direction
        // and W angle coefficients may vary from publication year as models are
        // adjusted. These coefficients are not in this class, they are in the
        // specialized classes that do implement the getPole and getPrimeMeridianAngle
        // methods
        final Vector3D pole  = iauPole.getPole(date);
        Vector3D qNode = iauPole.getNode(date);
        if (qNode.getNorm2Sq() < Precision.SAFE_MIN) {
            qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
        }
        final Rotation rotation = new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I);
        return StaticTransform.of(date, position, rotation);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        // translation part
        final FieldPVCoordinates<T> pv = pvProvider.getPVCoordinates(date, definingFrame).negate();

        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation derivatives
        final FieldAbsoluteDate<FieldUnivariateDerivative2<T>> fud2Date = date.toFUD2Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud2Date));

        // set up the transform from parent frame
        return new FieldTransform<>(date, new FieldTransform<>(date, pv),
                new FieldTransform<>(date, new FieldAngularCoordinates<>(rotation, derivatives.getRotationRate(),
                        derivatives.getRotationAcceleration())));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> getKinematicTransform(final FieldAbsoluteDate<T> date) {
        // translation part
        final FieldPVCoordinates<T> pv = pvProvider.getPVCoordinates(date, definingFrame).negate();

        // compute the rotation while preserving the derivatives already present in the field date
        final FieldRotation<T> rotation = getRotation(date);

        // use automatic differentiation to compute the rotation rate
        final FieldAbsoluteDate<FieldUnivariateDerivative1<T>> fud1Date = date.toFUD1Field();
        final FieldAngularCoordinates<T> derivatives = new FieldAngularCoordinates<>(getRotation(fud1Date));

        // set up the kinematic transform from parent frame
        return FieldKinematicTransform.of(date, pv, rotation, derivatives.getRotationRate());

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
        final FieldVector3D<T> position = pvProvider.getPVCoordinates(date, definingFrame).getPosition().negate();
        return FieldStaticTransform.of(date, position, getRotation(date));
    }

    /** Compute the complete (Field) rotation from parent.
     * @param date current date
     * @param <T> type of the field elements
     * @return rotation
     */
    private <T extends CalculusFieldElement<T>> FieldRotation<T> getRotation(final FieldAbsoluteDate<T> date) {
        // compute rotation from ICRF frame to self,
        // as per the "Report of the IAU/IAG Working Group on Cartographic
        // Coordinates and Rotational Elements of the Planets and Satellites"
        // These definitions are common for all recent versions of this report
        // published every three years, the precise values of pole direction
        // and W angle coefficients may vary from publication year as models are
        // adjusted. These coefficients are not in this class, they are in the
        // specialized classes that do implement the getPole and getPrimeMeridianAngle
        // methods
        final FieldVector3D<T> pole  = iauPole.getPole(date);
        FieldVector3D<T> qNode = iauPole.getNode(date);
        if (qNode.getNorm2Sq().getReal() < Precision.SAFE_MIN) {
            qNode = FieldVector3D.crossProduct(Vector3D.PLUS_K, pole);
        }
        return new FieldRotation<>(pole, qNode, FieldVector3D.getPlusK(date.getField()),
                FieldVector3D.getPlusI(date.getField()));
    }

}
