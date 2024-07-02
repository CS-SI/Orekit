/* Copyright 2020 Exotrail
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Exotrail licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Simple implementation of VariableThrustDirectionVector, providing a constant
 * direction.
 * @author Mikael Fillastre
 * @author Andrea Fiorentino
 * @since 10.2
 */

public class ConstantThrustDirectionProvider implements ThrustDirectionProvider {

    /** Constant direction. */
    private final Vector3D direction;

    /**
     * Constructor.
     * @param constantDirection constant direction
     */
    public ConstantThrustDirectionProvider(final Vector3D constantDirection) {
        direction = constantDirection;
    }

    @Override
    public Vector3D computeThrustDirection(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                           final Frame frame) {
        return direction;
    }
}
