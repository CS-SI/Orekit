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
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Class representing one panel of a satellite, slewing about an axis at constant rate.
 * <p>
 * It is mainly used to represent a solar array with fixed rate rotation.
 * </p>
 * <p>
 * The panel rotation evolves linearly according to a start position and an
 * angular rate (which can be set to 0 for non-rotating panels, which may
 * occur in special modes or during contingencies).
 * </p>
 * <p>
 * These panels are considered to be always {@link #isDoubleSided() double-sided}.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 3.0
 */
public class SlewingPanel extends Panel {

    /** Rotation rate of the panel (rad/s). */
    private final double rotationRate;

    /** Reference date for the panel rotation. */
    private final AbsoluteDate referenceDate;

    /** Panel reference axis in spacecraft frame (may be null). */
    private final Vector3D rX;

    /** Panel third axis in spacecraft frame (may be null). */
    private final Vector3D rY;

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
     * @param rotationRate rotation rate of the panel (rad/s)
     * @param referenceDate reference date for the panel rotation
     * @param referenceNormal direction of the panel normal at reference date in spacecraft frame
     * @param area panel area in m²
     * @param drag drag coefficient
     * @param liftRatio drag lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorption radiation pressure absorption coefficient (between 0 and 1)
     * @param reflection radiation pressure specular reflection coefficient (between 0 and 1)
     */
    public SlewingPanel(final Vector3D rotationAxis, final double rotationRate,
                        final AbsoluteDate referenceDate, final Vector3D referenceNormal,
                        final double area,
                        final double drag, final double liftRatio,
                        final double absorption, final double reflection) {
        super(area, true, drag, liftRatio, absorption, reflection);

        this.rotationRate    = rotationRate;
        this.referenceDate   = referenceDate;
        this.rY              = Vector3D.crossProduct(rotationAxis, referenceNormal).normalize();
        this.rX              = Vector3D.crossProduct(rY, rotationAxis).normalize();

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getNormal(final SpacecraftState state) {
        // use a simple rotation at fixed rate
        final SinCos sc = FastMath.sinCos(state.getDate().durationFrom(referenceDate) * rotationRate);
        return new Vector3D(sc.cos(), rX, sc.sin(), rY);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getNormal(final FieldSpacecraftState<T> state) {
        // use a simple rotation at fixed rate
        final FieldSinCos<T> sc = FastMath.sinCos(state.getDate().durationFrom(referenceDate).multiply(rotationRate));
        return new FieldVector3D<>(sc.cos(), rX, sc.sin(), rY);
    }

}
