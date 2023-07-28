/* Copyright 2002-2023 Luc Maisonobe
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
package org.orekit.forces;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ExtendedPVCoordinatesProvider;

/** Class representing one panel of a satellite, roughly pointing towards some target.
 * <p>
 * It is mainly used to represent a rotating solar array that points towards the Sun.
 * </p>
 * <p>
 * The panel rotation with respect to satellite body is the best pointing orientation
 * achievable when the rotation axix is fixed by body attitude. Target is therefore
 * always exactly in meridian plane defined by rotation axis and panel normal vector.
 * </p>
 * <p>
 * These panels are considered to be always {@link #isDoubleSided() double-sided}.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 3.0
 */
public class PointingPanel extends Panel {

    /** Rotation axis. */
    private final Vector3D rotationAxis;

    /** Target towards which the panel will point. */
    private final ExtendedPVCoordinatesProvider target;

    /** Simple constructor.
     * <p>
     * As the sum of absorption coefficient, specular reflection coefficient and
     * diffuse reflection coefficient is exactly 1, only the first two coefficients
     * are needed here, the third one is deduced from the other ones.
     * </p>
     * <p>
     * The panel is considered to rotate about one axis in order to make its normal
     * point as close as possible to the target. It means the target will always be
     * in the plane defined by the rotation axis and the panel normal.
     * </p>
     * @param rotationAxis rotation axis of the panel
     * @param target target towards which the panel will point (the Sun for a solar array)
     * @param area panel area in m²
     * @param drag drag coefficient
     * @param liftRatio drag lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorption radiation pressure absorption coefficient (between 0 and 1)
     * @param reflection radiation pressure specular reflection coefficient (between 0 and 1)
     */
    public PointingPanel(final Vector3D rotationAxis, final ExtendedPVCoordinatesProvider target,
                         final double area,
                         final double drag, final double liftRatio,
                         final double absorption, final double reflection) {
        super(area, true, drag, liftRatio, absorption, reflection);
        this.rotationAxis = rotationAxis.normalize();
        this.target       = target;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getNormal(final SpacecraftState state) {

        // compute orientation for best pointing
        final Vector3D targetInert = target.getPosition(state.getDate(), state.getFrame()).
                                     subtract(state.getPosition()).normalize();
        final Vector3D targetSpacecraft = state.getAttitude().getRotation().applyTo(targetInert);
        final double d = Vector3D.dotProduct(targetSpacecraft, rotationAxis);
        final double f = 1 - d * d;
        if (f < Precision.EPSILON) {
            // extremely rare case: the target is along panel rotation axis
            // (there will not be much output power if it is a solar array…)
            // we set up an arbitrary normal
            return rotationAxis.orthogonal();
        }

        final double s = 1.0 / FastMath.sqrt(f);
        return new Vector3D(s, targetSpacecraft, -s * d, rotationAxis);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getNormal(final FieldSpacecraftState<T> state) {
        // compute orientation for best pointing
        final FieldVector3D<T> targetInert = target.getPosition(state.getDate(), state.getFrame()).
                                             subtract(state.getPosition()).normalize();
        final FieldVector3D<T> targetSpacecraft = state.getAttitude().getRotation().applyTo(targetInert);
        final T d = FieldVector3D.dotProduct(targetSpacecraft, rotationAxis);
        final T f = d.multiply(d).subtract(1).negate();
        if (f.getReal() < Precision.EPSILON) {
            // extremely rare case: the target is along panel rotation axis
            // (there will not be much output power if it is a solar array…)
            // we set up an arbitrary normal
            return new FieldVector3D<>(f.getField(), rotationAxis.orthogonal());
        }

        final T s = f.sqrt().reciprocal();
        return new FieldVector3D<>(s, targetSpacecraft,
                                   s.multiply(d).negate(), new FieldVector3D<>(state.getDate().getField(), rotationAxis));
    }

}
