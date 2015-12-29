/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.models.earth.tessellation;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.handlers.EventHandler;

/** Event detector for half track span.
 * <p>
 * This detector is triggered when velocity is 0 along body Z axis
 * (i.e at minimum and maximum latitudes). Increasing events correspond
 * to minimum latitude while decreasing events correspond to maximum latitude.
 * </p>
 * @since 7.1
 * @see AlongTrackAiming
 * @author Luc Maisonobe
 */
class HalfTrackSpanDetector extends AbstractDetector<HalfTrackSpanDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150317;

    /** Body frame. */
    private final Frame bodyFrame;

    /** Build a new instance.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param bodyFrame body frame
     */
    protected HalfTrackSpanDetector(final double maxCheck, final double threshold, final int maxIter,
                                    final EventHandler<? super HalfTrackSpanDetector> handler, final Frame bodyFrame) {
        super(maxCheck, threshold, maxIter, handler);
        this.bodyFrame = bodyFrame;
    }

    /** {@inheritDoc} */
    @Override
    protected HalfTrackSpanDetector create(final double newMaxCheck,
                                           final double newThreshold,
                                           final int newMaxIter,
                                           final EventHandler<? super HalfTrackSpanDetector> newHandler) {
        return new HalfTrackSpanDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, bodyFrame);
    }

    /** {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) throws OrekitException {
        return s.getPVCoordinates(bodyFrame).getVelocity().getZ();
    }

}
