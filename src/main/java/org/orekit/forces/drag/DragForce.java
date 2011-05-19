/* Copyright 2002-2011 CS Communication & Systèmes
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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.Parameterizable;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;


/** Atmospheric drag force model.
 *
 * The drag acceleration is computed as follows :
 *
 * &gamma = (1/2 * Ro * V<sup>2</sup> * S / Mass) * DragCoefVector
 *
 * With DragCoefVector = {Cx, Cy, Cz} and S given by the user through the interface
 * {@link DragSensitive}
 *
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class DragForce implements ForceModel, Parameterizable {

    /** Parameter name for drag coefficient enabling jacobian processing. */
    public static final String DRAG_COEFFICIENT = "DRAG COEFFICIENT";

    /** List of the parameters names. */
    private static final ArrayList<String> PARAMETERS_NAMES;
    static {
        PARAMETERS_NAMES = new ArrayList<String>();
        PARAMETERS_NAMES.add(DRAG_COEFFICIENT);
    }

    /** Serializable UID. */
    private static final long serialVersionUID = 5386256916056674950L;

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Spacecraft. */
    private final DragSensitive spacecraft;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public DragForce(final Atmosphere atmosphere, final DragSensitive spacecraft) {
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

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPVCoordinates().getPosition();

        final double rho    = atmosphere.getDensity(date, position, frame);
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Addition of calculated acceleration to adder
        adder.addAcceleration(spacecraft.dragAcceleration(s, rho, relativeVelocity), frame);

    }

    /** There are no discrete events for this model.
     * @return an empty array
     */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    public Collection<String> getParametersNames() {
        return PARAMETERS_NAMES;
    }

    /** {@inheritDoc} */
    public boolean isSupported(final String name) {
        return name.equals(DRAG_COEFFICIENT);
    }

    /** {@inheritDoc} */
    public double getParameter(final String name) throws IllegalArgumentException {
        if (name.matches(DRAG_COEFFICIENT)) {
            return spacecraft.getDragCoefficient();
        } else {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.UNKNOWN_PARAMETER, name);
        }
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value) throws IllegalArgumentException {
        if (name.matches(DRAG_COEFFICIENT)) {
            spacecraft.setDragCoefficient(value);
        } else {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.UNKNOWN_PARAMETER, name);
        }
    }

}
