/* Copyright 2002-2011 Centre National d'Études Spatiales
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
package org.orekit.propagation.numerical;

import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Implementation of the {@link StateMapper} interface for state arrays in Keplerian parameters.
*
* @see org.orekit.propagation.SpacecraftState
* @see org.orekit.propagation.numerical.NumericalPropagator
* @see TimeDerivativesEquationsKeplerian
* @author Luc Maisonobe
*/
public class StateMapperKeplerian implements StateMapper {

    /** Serializable UID. */
    private static final long serialVersionUID = -5937981084552542611L;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** {@inheritDoc} */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attitudeProvider = attitudeProvider;
    }

    /** {@inheritDoc} */
    public void mapStateToArray(final SpacecraftState s, final double[] stateVector) {

        final Orbit baseOrbit = s.getOrbit();
        final KeplerianOrbit keplerianOrbit = (baseOrbit instanceof KeplerianOrbit) ?
                                              (KeplerianOrbit) baseOrbit :
                                              new KeplerianOrbit(baseOrbit);
        stateVector[0] = keplerianOrbit.getA();
        stateVector[1] = keplerianOrbit.getE();
        stateVector[2] = keplerianOrbit.getI();
        stateVector[3] = keplerianOrbit.getPerigeeArgument();
        stateVector[4] = keplerianOrbit.getRightAscensionOfAscendingNode();
        stateVector[5] = keplerianOrbit.getTrueAnomaly();
        stateVector[6] = s.getMass();

    }

    /** {@inheritDoc} */
    public SpacecraftState mapArrayToState(final double[] stateVector, final AbsoluteDate date,
                                           final double mu, final Frame frame) throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                               stateVector[4], stateVector[5], PositionAngle.TRUE,
                               frame, date, mu);

        final Attitude attitude = attitudeProvider.getAttitude(orbit, date, frame);

        return new SpacecraftState(orbit, attitude, stateVector[6]);

    }

}
