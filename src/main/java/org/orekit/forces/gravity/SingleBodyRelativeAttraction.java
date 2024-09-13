/* Copyright 2002-2024 CS GROUP
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
package org.orekit.forces.gravity;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/** Body attraction force model computed as relative acceleration towards frame center.
 * @author Luc Maisonabe
 * @author Julio Hernanz
 */
public class SingleBodyRelativeAttraction extends AbstractBodyAttraction {

    /** Simple constructor.
     * @param body the body to consider
     * (ex: {@link CelestialBodies#getSun()} or
     * {@link CelestialBodies#getMoon()})
     */
    public SingleBodyRelativeAttraction(final CelestialBody body) {
        super(body);
    }

    /** {@inheritDoc} */
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // compute bodies separation vectors and squared norm
        final PVCoordinates bodyPV   = getBodyPVCoordinates(s.getDate(), s.getFrame());
        final Vector3D satToBody     = bodyPV.getPosition().subtract(s.getPosition());
        final double r2Sat           = satToBody.getNormSq();

        // compute relative acceleration
        final double gm = parameters[0];
        final double a = gm / r2Sat;
        return new Vector3D(a, satToBody.normalize()).add(bodyPV.getAcceleration());

    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        // compute bodies separation vectors and squared norm
        final FieldPVCoordinates<T> bodyPV = getBodyPVCoordinates(s.getDate(), s.getFrame());
        final FieldVector3D<T> satToBody   = bodyPV.getPosition().subtract(s.getPosition());
        final T                r2Sat       = satToBody.getNormSq();

        // compute relative acceleration
        final T gm = parameters[0];
        final T a  = gm.divide(r2Sat);
        return new FieldVector3D<>(a, satToBody.normalize()).add(bodyPV.getAcceleration());

    }

}
