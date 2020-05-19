/* Copyright 2002-2020 CS GROUP
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

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
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
    private FieldVector3D<DerivativeStructure> accelerationToEpoch(final SpacecraftState s, final double[] parameters) {

        final double gm = parameters[0];

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();

        // Spacecraft Position
        final double rx = centralToBody.getX();
        final double ry = centralToBody.getY();
        final double rz = centralToBody.getZ();

        final DSFactory factoryP = new DSFactory(3, 1);
        final DerivativeStructure fpx = factoryP.variable(0, rx);
        final DerivativeStructure fpy = factoryP.variable(1, ry);
        final DerivativeStructure fpz = factoryP.variable(2, rz);

        final FieldVector3D<DerivativeStructure> centralToBodyFV = new FieldVector3D<>(new DerivativeStructure[] {fpx, fpy, fpz});


        final DerivativeStructure                r2Central = centralToBodyFV.getNormSq();
        final FieldVector3D<DerivativeStructure> satToBody = centralToBodyFV.subtract(s.getPVCoordinates().getPosition());
        final DerivativeStructure                r2Sat     = satToBody.getNormSq();

        return new FieldVector3D<>(gm, satToBody.scalarMultiply(r2Sat.multiply(r2Sat.sqrt()).reciprocal()),
                                  -gm, centralToBodyFV.scalarMultiply(r2Central.multiply(r2Central.sqrt()).reciprocal()));
    }

    /** Compute derivatives of the state w.r.t epoch.
     * @param s current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters
     * @return derivatives
     */
    public double[] getDerivativesToEpoch(final SpacecraftState s, final double[] parameters) {

        final FieldVector3D<DerivativeStructure> acc = accelerationToEpoch(s, parameters);
        final Vector3D centralToBodyVelocity = body.getPVCoordinates(s.getDate(), s.getFrame()).getVelocity();

        final double[] dAccxdR1i = acc.getX().getAllDerivatives();
        final double[] dAccydR1i = acc.getY().getAllDerivatives();
        final double[] dAcczdR1i = acc.getZ().getAllDerivatives();
        final double[] v = centralToBodyVelocity.toArray();

        return new double[] {dAccxdR1i[1] * v[0] + dAccxdR1i[1] * v[1] + dAccxdR1i[1] * v[2],
            dAccydR1i[1] * v[0] + dAccydR1i[1] * v[1] + dAccydR1i[1] * v[2],
            dAcczdR1i[1] * v[0] + dAcczdR1i[1] * v[1] + dAcczdR1i[1] * v[2]};
    }

}
