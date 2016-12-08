/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.integration;

import org.hipparchus.RealFieldElement;
import org.orekit.attitudes.FieldAttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** This class maps between raw double elements and {@link FieldSpacecraftState} instances.
 * @author Luc Maisonobe
 */
public abstract class FieldStateMapper<T extends RealFieldElement<T>> {

    /** Reference date. */
    private final FieldAbsoluteDate<T> referenceDate;

    /** Propagation orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Attitude provider. */
    private final FieldAttitudeProvider<T> attitudeProvider;

    /** Central attraction coefficient. */
    private final double mu;

    /** Inertial frame. */
    private final Frame frame;

    /** Simple constructor.
     * <p>
     * The position parameter type is meaningful only if {@link
     * #getOrbitType() propagation orbit type}
     * support it. As an example, it is not meaningful for propagation
     * in {@link  OrbitType#CARTESIAN Cartesian} parameters.
     * </p>
     * @param referenceDate reference date
     * @param mu central attraction coefficient (m³/s²)
     * @param orbitType orbit type to use for mapping
     * @param positionAngleType angle type to use for propagation
     * @param attitudeProvider attitude provider
     * @param frame inertial frame
     */
    protected FieldStateMapper(final FieldAbsoluteDate<T> referenceDate, final double mu,
                          final OrbitType orbitType, final PositionAngle positionAngleType,
                          final FieldAttitudeProvider<T> attitudeProvider, final Frame frame) {
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
    public FieldAbsoluteDate<T> getReferenceDate() {
        return referenceDate;
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation
     */
    public  OrbitType getOrbitType() {
        return orbitType;
    }

    /** Set position angle type.
     */
    public void setPositionAngleType() {
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    public PositionAngle getPositionAngleType() {
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
    public FieldAttitudeProvider<T> getAttitudeProvider() {
        return attitudeProvider;
    }

    /** Map the raw double time offset to a date.
     * @param t date offset
     * @return date
     */
    public FieldAbsoluteDate<T> mapDoubleToDate(final T t) {
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
    public FieldAbsoluteDate<T> mapDoubleToDate(final T t,
                                        final FieldAbsoluteDate<T> date) {
        if (date.durationFrom(referenceDate).getReal() == t.getReal()) {
            return date;
        } else {
            return mapDoubleToDate(t);
        }
    }

    /** Map a date to a raw double time offset.
     * @param date date
     * @return time offset
     */
    public T mapDateToDouble(final FieldAbsoluteDate<T> date) {
        return date.durationFrom(referenceDate);
    }

    /** Map the raw double components to a spacecraft state.
     * @param t date offset
     * @param y state components
     * @param meanOnly use only the mean elements to build the state
     * @return spacecraft state
     * @exception OrekitException if array is inconsistent or cannot be mapped
     */
    public FieldSpacecraftState<T> mapArrayToState(final T t, final T[] y, final boolean meanOnly)
            throws OrekitException {
        return mapArrayToState(mapDoubleToDate(t), y, meanOnly);
    }

    /** Map the raw double components to a spacecraft state.
     * @param date of the state components
     * @param y state components
     * @param meanOnly use only the mean elements to build the state
     * @return spacecraft state
     * @exception OrekitException if array is inconsistent or cannot be mapped
     */
    public abstract FieldSpacecraftState<T> mapArrayToState(FieldAbsoluteDate<T> date, T[] y, boolean meanOnly)
        throws OrekitException;

    /** Map a spacecraft state to raw double components.
     * @param state state to map
     * @param y placeholder where to put the components
     * @exception OrekitException if state is inconsistent or cannot be mapped
     */
    public abstract void mapStateToArray(FieldSpacecraftState<T> state, T[] y)
        throws OrekitException;

}
