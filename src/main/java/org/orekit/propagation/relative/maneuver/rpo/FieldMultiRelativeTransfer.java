/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative.maneuver.rpo;

import org.hipparchus.CalculusFieldElement;
import org.orekit.forces.maneuvers.FieldImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.propagation.relative.FieldRelativeProvider;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.propagation.relative.maneuver.FieldRelativeManeuver;

import java.util.List;

/**
 * Interface for FieldMultiRelativeTransfers.
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public interface FieldMultiRelativeTransfer<T extends CalculusFieldElement<T>> {

    /**
     * Compute the list of TwoImpulseTransfer to realize the path defined by the waypoints
     * in the associated LocalOrbitalFrame.
     * @return List of TwoImpulseTransfer.
     */
    List<FieldTwoImpulseTransfer<T>> computeMultiRelativeTransfers();

    /**
     * Compute the list of TwoImpulseTransfer to realize the path defined by the waypoints in the waypoints Frame.
     * @param waypointsFrame Frame of the waypoints.
     * @return List of FieldTwoImpulseTransfer.
     */
    List<FieldTwoImpulseTransfer<T>> computeMultiRelativeTransfers(Frame waypointsFrame);

    /**
     * Compute the list of relative maneuvers to realize the trajectory defined by the waypoints.
     * @param relativeProvider FieldRelativeProvider propagated with the target's propagator.
     * @return List of FieldRelativeManeuvers.
     */
    List<? extends FieldRelativeManeuver<T>> computeRelativeManeuvers(FieldRelativeProvider<T> relativeProvider);

    /**
     * Compute the list of FieldImpulseManeuvers in the desired frame to realize the trajectory defined by the waypoints.
     * @param frame Desired frame to apply the ImpulseManeuvers.
     * @param Isp specific impulse of the chaser.
     * @return List of ImpulseManeuver.
     */
    List<FieldImpulseManeuver<T>> computeImpulseManeuvers(Frame frame, T Isp);
}
