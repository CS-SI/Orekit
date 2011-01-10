/* Copyright 2002-2010 Centre National d'Études Spatiales
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

import org.orekit.attitudes.AttitudeLaw;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Implementation of the {@link StateMapper} interface for state arrays in equinoctial parameters.
*
* @see org.orekit.propagation.SpacecraftState
* @see org.orekit.propagation.numerical.NumericalPropagator
* @see TimeDerivativesEquationsEquinoctial
* @author V&eacute;ronique Pommier-Maurussane
* @version $Revision$ $Date$
*/
public class StateMapperEquinoctial implements StateMapper {

    /** Serializable UID. */
    private static final long serialVersionUID = 2882088208801391437L;

    /** Attitude law. */
    private AttitudeLaw attitudeLaw;

    /** Set the attitude law.
     * @param attitudeLaw attitude law
     */
    public void setAttitudeLaw(final AttitudeLaw attitudeLaw) {
        this.attitudeLaw = attitudeLaw;
    }

    /** {@inheritDoc} */
    public void mapStateToArray(final SpacecraftState s, final double[] stateVector) {

        stateVector[0] = s.getA();
        stateVector[1] = s.getEquinoctialEx();
        stateVector[2] = s.getEquinoctialEy();
        stateVector[3] = s.getHx();
        stateVector[4] = s.getHy();
        stateVector[5] = s.getLv();
        stateVector[6] = s.getMass();

    }

    /** {@inheritDoc} */
    public SpacecraftState mapArrayToState(final double[] stateVector, final AbsoluteDate date,
                                           final double mu, final Frame frame) throws OrekitException {
        final EquinoctialOrbit orbit =
            new EquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                 stateVector[4], stateVector[5], EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                 frame, date, mu);

        return new SpacecraftState(orbit, attitudeLaw.getAttitude(orbit), stateVector[6]);

    }

}
