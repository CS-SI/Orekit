/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.forces.drag;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;


/** Atmospheric drag force model.
 * The drag acceleration is computed as follows :
 *
 * &gamma = (1/2 * Ro * V<sup>2</sup> * S / Mass) * DragCoefVector
 *
 * With DragCoefVector = {Cx, Cy, Cz} and S given by the user threw the interface
 * {@link DragSensitive}
 *
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class DragForce implements ForceModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 3430941727178712005L;

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Spacecraft. */
    private final DragSensitive spacecraft;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public DragForce(final Atmosphere atmosphere,
                           final DragSensitive spacecraft) {
        this.atmosphere = atmosphere;
        this.spacecraft = spacecraft;
    }

    /** Compute the contribution of the drag to the perturbing acceleration.
     * @param s the current state information : date, kinematics, attitude
     * @param adder object where the contribution should be added
     * @exception OrekitException if some specific error occurs
     */
    public void addContribution(final SpacecraftState s,
                                final TimeDerivativesEquations adder)
        throws OrekitException {
        final double rho =
            atmosphere.getDensity(s.getDate(), s.getPVCoordinates().getPosition(), s.getFrame());

        final Vector3D vAtm =
            atmosphere.getVelocity(s.getDate(), s.getPVCoordinates().getPosition(), s.getFrame());

        final Vector3D incidence = vAtm.subtract(s.getPVCoordinates().getVelocity());
        final double v2 = Vector3D.dotProduct(incidence, incidence);

        final Vector3D inSpacecraft =
            s.getAttitude().getRotation().applyTo(incidence.normalize());
        final double k = rho * v2 * spacecraft.getDragCrossSection(inSpacecraft) / (2 * s.getMass());
        final Vector3D cD = spacecraft.getDragCoef(inSpacecraft);

        // Addition of calculated acceleration to adder
        adder.addXYZAcceleration(k * cD.getX(), k * cD.getY(), k * cD.getZ());

    }

    /** There are no discrete events for this model.
     * @return an empty array
     */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

}
