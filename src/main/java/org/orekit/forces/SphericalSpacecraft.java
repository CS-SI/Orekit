/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.propagation.SpacecraftState;

/** This class represents the features of a simplified spacecraft.
 * <p>The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend of
 * the direction.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 *
 * @see BoxAndSolarArraySpacecraft
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class SphericalSpacecraft implements RadiationSensitive, DragSensitive {

    /** Serializable UID. */
    private static final long serialVersionUID = -1596721390500187750L;

    /** Composite drag coefficient (S.Cd/2). */
    private final double kD;

    /** Composite radiation pressure coefficient. */
    private final double kP;

    /** Simple constructor.
     * @param crossSection Surface (m<sup>2</sup>)
     * @param dragCoeff drag coefficient (used only for drag)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     */
    public SphericalSpacecraft(final double crossSection, final double dragCoeff,
                               final double absorptionCoeff, final double reflectionCoeff) {
        kD = dragCoeff * crossSection / 2;
        kP = crossSection * (1 + 4 * (1.0 - absorptionCoeff) * (1.0 - reflectionCoeff) / 9);
    }

    /** {@inheritDoc} */
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity) {
        return new Vector3D(density * relativeVelocity.getNorm() * kD / state.getMass(), relativeVelocity);
    }

    /** {@inheritDoc} */
    public Vector3D radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux) {
        return new Vector3D(kP / state.getMass(), flux);
    }

}
