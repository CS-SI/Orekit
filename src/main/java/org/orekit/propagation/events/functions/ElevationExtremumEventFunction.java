/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.events.functions;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.KinematicTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Event function for elevation extremum with respect to a ground point.
 * @author Romain Serra
 * @since 14.0
 */
public class ElevationExtremumEventFunction extends AbstractTopocentricEventFunction {

    /** Constructor.
     * @param topo topocentric frame centered on ground point
     */
    public ElevationExtremumEventFunction(final TopocentricFrame topo) {
        super(topo);
    }

    @Override
    public double value(final SpacecraftState state) {
        // get position, velocity of spacecraft in topocentric frame
        final KinematicTransform inertToTopo = state.getFrame().getKinematicTransformTo(getTopocentricFrame(), state.getDate());
        final TimeStampedPVCoordinates pvTopo = inertToTopo.transformOnlyPV(state.getPVCoordinates());

        // convert the coordinates to UnivariateDerivative1 based vector
        // instead of having vector position, then vector velocity then vector acceleration
        // we get one vector and each coordinate is a Taylor expansion containing
        // value, first time derivative (we don't need second time derivative here)
        final FieldVector3D<UnivariateDerivative1> positionUD1 = pvTopo.toUnivariateDerivative1Vector();

        // compute elevation and its first time derivative
        final UnivariateDerivative1 elevation = positionUD1.getDelta();

        // return elevation first time derivative
        return elevation.getFirstDerivative();
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the spacecraft elevation first time derivative.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft elevation first time derivative
     */
    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> s) {

        // get position, velocity acceleration of spacecraft in topocentric frame
        final FieldStaticTransform<FieldUnivariateDerivative1<T>> inertToTopo = s.getFrame().getStaticTransformTo(getTopocentricFrame(),
                s.getDate().toFUD1Field());
        final FieldVector3D<FieldUnivariateDerivative1<T>> positionInert = s.getPVCoordinates().toUnivariateDerivative1Vector();
        final FieldVector3D<FieldUnivariateDerivative1<T>> posTopo = inertToTopo.transformPosition(positionInert);

        // compute elevation and its first time derivative
        final FieldUnivariateDerivative1<T> elevation = posTopo.getDelta();

        // return elevation first time derivative
        return elevation.getFirstDerivative();

    }

}
