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
package org.orekit.propagation.relative.maneuver;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireProvider;

/**
 * Implements a maneuver for a chaser spacecraft whose relative orbit is defined by the Clohessy-Wiltshire equations.
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public class FieldClohessyWiltshireManeuver<T extends CalculusFieldElement<T>> extends FieldAbstractRelativeManeuver<T> {
    /**
     * Creates a new FieldClohessyWiltshireManeuver object from a trigger event, a ΔV vector, and an associated frame.
     *
     * @param trigger                   Trigger event that triggers the maneuver of the chaser spacecraft.
     * @param deltaV                    ΔV vector in the target's QSW LOF.
     * @param chaserAdditionalEquations Chaser's additional equations provider
     */
    public FieldClohessyWiltshireManeuver(final FieldEventDetector<T> trigger, final FieldVector3D<T> deltaV, final FieldClohessyWiltshireProvider<T> chaserAdditionalEquations) {
        super(trigger, deltaV, chaserAdditionalEquations);
    }
}



