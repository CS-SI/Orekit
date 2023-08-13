/* Copyright 2002-2023 CS GROUP
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
package org.orekit.frames;

import org.orekit.bodies.CelestialBody;

/**
 * Class creating the rotating frame centered on the barycenter of the CR3BP System.
 * @author Vincent Mouraux
 * @since 10.2
 */
public class CR3BPRotatingFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 20190520L;

    /**
     * Simple constructor.
     * @param mu Mass ratio
     * @param primaryBody Primary body.
     * @param secondaryBody Secondary body.
     */
    public CR3BPRotatingFrame(final double mu, final CelestialBody primaryBody,
                              final CelestialBody secondaryBody) {
        super(primaryBody.getInertiallyOrientedFrame(), new CR3BPRotatingTransformProvider(mu, primaryBody,  secondaryBody),
              primaryBody.getName() + "-" + secondaryBody.getName() + "-CR3BPBarycenter", true);
    }

}
