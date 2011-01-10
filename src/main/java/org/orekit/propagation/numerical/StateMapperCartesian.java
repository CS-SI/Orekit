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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Implementation of the {@link StateMapper} interface for state arrays in cartesian coordinates.
 *
 * @see org.orekit.propagation.SpacecraftState
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see TimeDerivativesEquationsCartesian
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class StateMapperCartesian implements StateMapper {

    /** Serializable UID. */
    private static final long serialVersionUID = -861228811985500665L;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Create a new instance.
     */
    public StateMapperCartesian() {
    }

    /** {@inheritDoc} */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attitudeProvider = attitudeProvider;
    }

    /** {@inheritDoc} */
    public void mapStateToArray(final SpacecraftState s, final double[] stateVector) {

        final PVCoordinates pv = s.getPVCoordinates();
        final Vector3D      p  = pv.getPosition();
        final Vector3D      v  = pv.getVelocity();

        stateVector[0] = p.getX();
        stateVector[1] = p.getY();
        stateVector[2] = p.getZ();
        stateVector[3] = v.getX();
        stateVector[4] = v.getY();
        stateVector[5] = v.getZ();
        stateVector[6] = s.getMass();

    }

    /** {@inheritDoc} */
    public SpacecraftState mapArrayToState(final double[] stateVector, final AbsoluteDate date,
                                           final double mu, final Frame frame)
        throws OrekitException {

        final Vector3D       p     = new Vector3D(stateVector[0], stateVector[1], stateVector[2]);
        final Vector3D       v     = new Vector3D(stateVector[3], stateVector[4], stateVector[5]);
        final PVCoordinates  pv    = new PVCoordinates(p, v);
        final Orbit          orbit = new CartesianOrbit(pv, frame, date, mu);

        final Attitude attitude =
            attitudeProvider.getAttitude(new KeplerianPropagator(orbit), date, frame);

        return new SpacecraftState(orbit, attitude, stateVector[6]);

    }

}
