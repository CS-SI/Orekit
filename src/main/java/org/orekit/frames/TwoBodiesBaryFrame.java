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
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.OrekitConfiguration;

/**
 * Class creating the inertial barycenter frame from two bodies.
 * @author Vincent Mouraux
 * @since 10.2
 */
public class TwoBodiesBaryFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 20190725L;

    /**
     * Simple constructor.
     * @param primaryBody Primary body.
     * @param secondaryBody Secondary body.
     */
    public TwoBodiesBaryFrame(final CelestialBody primaryBody,
                              final CelestialBody secondaryBody) {
        super(primaryBody.getInertiallyOrientedFrame(),
              new ShiftingTransformProvider(new TwoBodiesBaryTransformProvider(primaryBody, secondaryBody),
                                            CartesianDerivativesFilter.USE_P,
                                            AngularDerivativesFilter.USE_R, 5,
                                            Constants.JULIAN_DAY / 24,
                                            OrekitConfiguration.getCacheSlotsNumber(),
                                            Constants.JULIAN_YEAR,
                                            30 * Constants.JULIAN_DAY),
              primaryBody .getName() + "-" + secondaryBody.getName() + "-Barycenter", true);
    }

}
