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
package org.orekit.propagation.events;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;

/** Common parts shared by several orbital events finders.
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public abstract class AbstractDetector implements EventDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -8212002898109868489L;

    /** Max check interval. */
    private final double maxCheck;

    /** Convergence threshold. */
    private final double threshold;

    /** Build a new instance.
     * @param maxCheck maximal check interval
     * @param threshold convergence threshold
     */
    protected AbstractDetector(final double maxCheck, final double threshold) {
        this.maxCheck  = maxCheck;
        this.threshold = threshold;
    }

    /** {@inheritDoc} */
    public abstract int eventOccurred(SpacecraftState s) throws OrekitException;

    /** {@inheritDoc} */
    public abstract double g(SpacecraftState s) throws OrekitException;

    /** {@inheritDoc} */
    public double getMaxCheckInterval() {
        return maxCheck;
    }

    /** {@inheritDoc} */
    public int getMaxIterationCount() {
        return 100;
    }

    /** {@inheritDoc} */
    public double getThreshold() {
        return threshold;
    }

    /** {@inheritDoc} */
    public SpacecraftState resetState(final SpacecraftState oldState) {
        return oldState;
    }

}
