/* Copyright 2023 Thales Alenia Space
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
package org.orekit.models.earth.displacement;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.BodiesElements;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Modeling of displacement of reference points due to plate tectonics.
 * <p>
 * Instances of this class are guaranteed to be immutable
 * </p>
 * @see org.orekit.estimation.measurements.GroundStation
 * @see org.orekit.files.sinex
 * @since 12.0
 * @author Luc Maisonobe
 */
public class TectonicsDisplacement implements StationDisplacement {

    /** Coordinates reference epoch. */
    private AbsoluteDate epoch;

    /** Station velocity. */
    private Vector3D velocity;

    /** Simple constructor.
     * @param velocity station velocity in Earth frame (m/s)
     * @param epoch coordinates reference epoch
     */
    public TectonicsDisplacement(final AbsoluteDate epoch, final Vector3D velocity) {
        this.epoch    = epoch;
        this.velocity = velocity;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D displacement(final BodiesElements elements, final Frame earthFrame, final Vector3D referencePoint) {
        return new Vector3D(elements.getDate().durationFrom(epoch), velocity);
    }

}

