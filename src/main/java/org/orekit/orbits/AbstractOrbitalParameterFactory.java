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
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Factory for orbital parameters.
 * @param <P> type of the orbital parameters
 * @since 14.0
 */
public abstract class AbstractOrbitalParameterFactory<P extends OrbitalParameters>
    implements OrbitalParameterFactory<P> {

    /** Drivers for orbital parameters. */
    private final ParameterDriversList drivers;

    /** Frame in which the orbital parameters are defined. */
    private final Frame frame;

    /** Position angle type to use. */
    private final PositionAngleType positionAngleType;

    /** Date of the orbital parameters. */
    private AbsoluteDate date;

    /** Central attraction coefficient (m³/s²). */
    private double mu;

    /**
     * Simple constructor.
     *
     * @param drivers           drivers for orbital parameters
     * @param frame             frame in which the orbital parameters are defined
     * @param positionAngleType position angle type to use
     * @param date              date of the orbital parameters
     * @param mu                central attraction coefficient (m³/s²)
     */
    protected AbstractOrbitalParameterFactory(final ParameterDriversList drivers, final Frame frame,
                                              final PositionAngleType positionAngleType,
                                              final AbsoluteDate date, final double mu) {
        this.drivers           = drivers;
        this.date              = date;
        this.frame             = frame;
        this.positionAngleType = positionAngleType;
        this.mu                = mu;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getOrbitalParametersDrivers() {
        return drivers;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getNonKeplerianParametersDrivers() {
        // return an empty list
        return new ParameterDriversList();
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
    public PositionAngleType getPositionAngleType() {
        return positionAngleType;
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
    public void reset(final Orbit orbit) {

        // fix orbital parameters
        final double[] stateVector = toArray(orbit);
        for (int i = 0; i < 6; i++) {
            final ParameterDriver driver = getOrbitalParametersDrivers().getDrivers().get(i);
            driver.setReferenceValue(stateVector[i]);
            driver.setValue(stateVector[i], getDate());
        }

        // fix date
        setDate(orbit.getDate());

        // fix mu
        setMu(orbit.getMu());

    }

    /** Convert an input into an array suitable to feed orbital parameters drivers.
     * @param orbit orbit to convert
     * @return arrays corresponding to orbit
     */
    protected abstract double[] toArray(Orbit orbit);

}
