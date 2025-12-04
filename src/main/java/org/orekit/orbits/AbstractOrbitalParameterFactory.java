/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.orbits;

import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

/** Factory for orbital parameters.
 * @param <P> type of the orbital parameters
 * @since 14.0
 */
public abstract class AbstractOrbitalParameterFactory<P extends OrbitalParameters>
    implements OrbitalParameterFactory<P> {

    /** Date of the orbital parameters. */
    private AbsoluteDate date;

    /** Frame in which the orbital parameters are defined. */
    private final Frame frame;

    /** Central attraction coefficient (m³/s²). */
    private double mu;

    /** Drivers for orbital parameters. */
    private final ParameterDriversList drivers;

    /** Position scale used to scale the orbital drivers. */
    private final double positionScale;

    /** Simple constructor.
     * @param date date of the orbital parameters
     * @param frame frame in which the orbital parameters are defined
     * @param mu central attraction coefficient (m³/s²)
     * @param drivers drivers for orbital parameters
     * @param positionScale position scale used to scale the orbital drivers
     */
    protected AbstractOrbitalParameterFactory(final AbsoluteDate date, final Frame frame, final double mu,
                                              final ParameterDriversList drivers, final double positionScale) {
        this.date          = date;
        this.frame         = frame;
        this.mu            = mu;
        this.drivers       = drivers;
        this.positionScale = positionScale;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** {@inheritDoc} */
    @Override
    public void setDate(final AbsoluteDate date) {
        this.date = date;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public void setMu(final double mu) {
        this.mu = mu;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getDrivers() {
        return drivers;
    }

    /** {@inheritDoc} */
    @Override
    public double getPositionScale() {
        return positionScale;
    }

}
