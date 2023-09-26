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
package org.orekit.propagation.integration;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This class maps between raw double elements and {@link SpacecraftState} instances.
 * @author Luc Maisonobe
 * @since 6.0
 */
public abstract class StateMapper {

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Propagation orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngleType angleType;

    /** Central attraction coefficient. */
    private final double mu;

    /** Inertial frame. */
    private final Frame frame;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Simple constructor.
     * <p>
     * The position parameter type is meaningful only if {@link
     * #getOrbitType() propagation orbit type}
     * support it. As an example, it is not meaningful for propagation
     * in {@link OrbitType#CARTESIAN Cartesian} parameters.
     * </p>
     * @param referenceDate reference date
     * @param mu central attraction coefficient (m³/s²)
     * @param orbitType orbit type to use for mapping, null for
     * propagating using {@link org.orekit.utils.AbsolutePVCoordinates AbsolutePVCoordinates}
     * rather than {@link org.orekit.orbits Orbit}
     * @param positionAngleType angle type to use for propagation
     * @param attitudeProvider attitude provider
     * @param frame inertial frame
     */
    protected StateMapper(final AbsoluteDate referenceDate, final double mu,
                          final OrbitType orbitType, final PositionAngleType positionAngleType,
                          final AttitudeProvider attitudeProvider, final Frame frame) {
        this.referenceDate    = referenceDate;
        this.mu               = mu;
        this.orbitType        = orbitType;
        this.angleType        = positionAngleType;
        this.attitudeProvider = attitudeProvider;
        this.frame            = frame;
    }

    /** Get reference date.
     * @return reference date
     */
    public AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation
     */
    public OrbitType getOrbitType() {
        return orbitType;
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    public PositionAngleType getPositionAngleType() {
        return angleType;
    }

    /** Get the central attraction coefficient μ.
     * @return mu central attraction coefficient (m³/s²)
     */
    public double getMu() {
        return mu;
    }

    /** Get the inertial frame.
     * @return inertial frame
     */
    public Frame getFrame() {
        return frame;
    }

    /** Get the attitude provider.
     * @return attitude provider
     */
    public AttitudeProvider getAttitudeProvider() {
        return attitudeProvider;
    }

    /** Set the attitude provider.
     * @param attitudeProvider the provider to set
     */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attitudeProvider = attitudeProvider;
    }

    /** Map the raw double time offset to a date.
     * @param t date offset
     * @return date
     */
    public AbsoluteDate mapDoubleToDate(final double t) {
        return referenceDate.shiftedBy(t);
    }

    /**
     * Map the raw double time offset to a date.
     *
     * @param t    date offset
     * @param date The expected date.
     * @return {@code date} if it is the same time as {@code t} to within the
     * lower precision of the latter. Otherwise a new date is returned that
     * corresponds to time {@code t}.
     */
    public AbsoluteDate mapDoubleToDate(final double t,
                                        final AbsoluteDate date) {
        if (date.durationFrom(referenceDate) == t) {
            return date;
        } else {
            return mapDoubleToDate(t);
        }
    }

    /** Map a date to a raw double time offset.
     * @param date date
     * @return time offset
     */
    public double mapDateToDouble(final AbsoluteDate date) {
        return date.durationFrom(referenceDate);
    }

    /** Map the raw double components to a spacecraft state.
     * @param t date offset
     * @param y state components
     * @param yDot time derivatives of the state components (null if unknown, in which case Keplerian motion is assumed)
     * @param type type of the elements used to build the state (mean or osculating).
     * @return spacecraft state
     */
    public SpacecraftState mapArrayToState(final double t, final double[] y, final double[] yDot, final PropagationType type) {
        return mapArrayToState(mapDoubleToDate(t), y, yDot, type);
    }

    /** Map the raw double components to a spacecraft state.
     * @param date of the state components
     * @param y state components
     * @param yDot time derivatives of the state components (null if unknown, in which case Keplerian motion is assumed)
     * @param type type of the elements used to build the state (mean or osculating).
     * @return spacecraft state
     */
    public abstract SpacecraftState mapArrayToState(AbsoluteDate date, double[] y, double[] yDot, PropagationType type);

    /** Map a spacecraft state to raw double components.
     * @param state state to map
     * @param y placeholder where to put the components
     * @param yDot placeholder where to put the components derivatives
     */
    public abstract void mapStateToArray(SpacecraftState state, double[] y, double[] yDot);

}
