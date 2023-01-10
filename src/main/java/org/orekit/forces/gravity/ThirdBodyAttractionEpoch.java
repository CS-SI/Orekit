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
package org.orekit.forces.gravity;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.propagation.SpacecraftState;

/** Third body attraction force model.
 * This class is a copy of {@link ThirdBodyAttraction} class.
 * The computation of derivatives of the acceleration
 * w.r.t. the Epoch has been added.
 *
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @since 10.2
 */
public class ThirdBodyAttractionEpoch extends ThirdBodyAttraction {

    /** The body to consider. */
    private final CelestialBody body;

    /** Simple constructor.
     * @param body the third body to consider
     * (ex: {@link org.orekit.bodies.CelestialBodyFactory#getSun()} or
     * {@link org.orekit.bodies.CelestialBodyFactory#getMoon()})
     */
    public ThirdBodyAttractionEpoch(final CelestialBody body) {
        super(body);
        this.body = body;
    }

    /** Compute acceleration.
     * @param s current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters
     * @return acceleration in same frame as state
     */
    private FieldVector3D<Gradient> accelerationToEpoch(final SpacecraftState s, final double[] parameters) {

        final double gm = parameters[0];

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPosition(s.getDate(), s.getFrame());

        // Spacecraft Position
        final double rx = centralToBody.getX();
        final double ry = centralToBody.getY();
        final double rz = centralToBody.getZ();

        final int freeParameters = 3;
        final Gradient fpx = Gradient.variable(freeParameters, 0, rx);
        final Gradient fpy = Gradient.variable(freeParameters, 1, ry);
        final Gradient fpz = Gradient.variable(freeParameters, 2, rz);

        final FieldVector3D<Gradient> centralToBodyFV = new FieldVector3D<>(new Gradient[] {fpx, fpy, fpz});


        final Gradient                r2Central = centralToBodyFV.getNormSq();
        final FieldVector3D<Gradient> satToBody = centralToBodyFV.subtract(s.getPosition());
        final Gradient                r2Sat     = satToBody.getNormSq();

        return new FieldVector3D<>(gm, satToBody.scalarMultiply(r2Sat.multiply(r2Sat.sqrt()).reciprocal()),
                                  -gm, centralToBodyFV.scalarMultiply(r2Central.multiply(r2Central.sqrt()).reciprocal()));
    }

    /** Compute derivatives of the state w.r.t epoch.
     * @param s current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters
     * @return derivatives
     */
    public double[] getDerivativesToEpoch(final SpacecraftState s, final double[] parameters) {

        final FieldVector3D<Gradient> acc = accelerationToEpoch(s, parameters);
        final Vector3D centralToBodyVelocity = body.getPVCoordinates(s.getDate(), s.getFrame()).getVelocity();

        final double[] dAccxdR1i = acc.getX().getGradient();
        final double[] dAccydR1i = acc.getY().getGradient();
        final double[] dAcczdR1i = acc.getZ().getGradient();
        final double[] v = centralToBodyVelocity.toArray();

        return new double[] {
            dAccxdR1i[0] * v[0] + dAccxdR1i[1] * v[1] + dAccxdR1i[2] * v[2],
            dAccydR1i[0] * v[0] + dAccydR1i[1] * v[1] + dAccydR1i[2] * v[2],
            dAcczdR1i[0] * v[0] + dAcczdR1i[1] * v[1] + dAcczdR1i[2] * v[2]
        };

    }

}
