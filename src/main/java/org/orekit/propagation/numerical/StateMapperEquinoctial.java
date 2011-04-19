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
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Implementation of the {@link StateMapper} interface for state arrays in equinoctial parameters.
 * <p>
 * Instances of this class are guaranteed to be immutable
 * </p>
 *
 * @see org.orekit.propagation.SpacecraftState
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see TimeDerivativesEquationsEquinoctial
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class StateMapperEquinoctial implements StateMapper {

    /** Serializable UID. */
    private static final long serialVersionUID = 8314804815914578370L;

    /** Position angle type. */
    private final PositionAngle type;

    /** Attitude provider. */
    private final AttitudeProvider attitudeProvider;

    /** Create a new instance.
     * @param type position angle type
     * @param attitudeProvider attitude provider
     */
    public StateMapperEquinoctial(final PositionAngle type, final AttitudeProvider provider) {
        this.type             = type;
        this.attitudeProvider = provider;
    }

    /** {@inheritDoc} */
    public void mapStateToArray(final SpacecraftState s, final double[] stateVector) {

        final EquinoctialOrbit equinoctialOrbit =
            (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(s.getOrbit());

        stateVector[0] = equinoctialOrbit.getA();
        stateVector[1] = equinoctialOrbit.getEquinoctialEx();
        stateVector[2] = equinoctialOrbit.getEquinoctialEy();
        stateVector[3] = equinoctialOrbit.getHx();
        stateVector[4] = equinoctialOrbit.getHy();
        stateVector[5] = equinoctialOrbit.getL(type);
        stateVector[6] = s.getMass();

    }

    /** {@inheritDoc} */
    public SpacecraftState mapArrayToState(final double[] stateVector, final AbsoluteDate date,
                                           final double mu, final Frame frame) throws OrekitException {
        final EquinoctialOrbit orbit =
            new EquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                 stateVector[4], stateVector[5], type,
                                 frame, date, mu);

        final Attitude attitude = attitudeProvider.getAttitude(orbit, date, frame);

        return new SpacecraftState(orbit, attitude, stateVector[6]);

    }

}
