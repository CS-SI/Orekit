/* Copyright 2020-2025 Exotrail
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
package org.orekit.propagation.conversion.averaging;

import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
 * Abstract class representing averaged orbital state. It is used to define the frame and the date.
 *
 * @author Romain Serra
 * @see AveragedOrbitalState
 * @since 12.1
 */
public abstract class AbstractAveragedOrbitalState implements AveragedOrbitalState {

    /** Reference (inertial) frame. */
    private final Frame frame;
    /** Orbit epoch. */
    private final AbsoluteDate date;

    /**
     * Protected constructor.
     * @param date epoch
     * @param frame reference frame
     */
    protected AbstractAveragedOrbitalState(final AbsoluteDate date, final Frame frame) {
        this.date = date;
        this.frame = frame;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }
}
