/* Copyright 2002-2024 Luc Maisonobe
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

package org.orekit.forces.maneuvers.propulsion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeSpanMap;

/** Thrust propulsion model based on segmented profile.
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 12.0
 */
public class ProfileThrustPropulsionModel implements ThrustPropulsionModel {

    /** Thrust profile. */
    private final TimeSpanMap<ThrustSegment> profile;

    /** Specific impulse. */
    private final double isp;

    /** Name of the maneuver. */
    private final String name;

    /** Type of norm linking thrust vector to mass flow rate. */
    private final Control3DVectorCostType control3DVectorCostType;

    /** Constructor with default cost type.
     * @param profile thrust profile (N)
     * @param isp specific impulse (s)
     * @param name name of the maneuver
     * @since 13.0
     */
    public ProfileThrustPropulsionModel(final TimeSpanMap<ThrustSegment> profile, final double isp,
                                        final String name) {
        this(profile, isp, Control3DVectorCostType.TWO_NORM, name);
    }

    /** Generic constructor.
     * @param profile thrust profile (N)
     * @param isp specific impulse (s)
     * @param control3DVectorCostType control vector's cost type
     * @param name name of the maneuver
     * @since 13.0
     */
    public ProfileThrustPropulsionModel(final TimeSpanMap<ThrustSegment> profile, final double isp,
                                        final Control3DVectorCostType control3DVectorCostType, final String name) {
        this.name    = name;
        this.isp     = isp;
        this.profile = profile;
        this.control3DVectorCostType = control3DVectorCostType;
    }

    /** Build with customized profile.
     * @param profile thrust profile (N)
     * @param isp specific impulse (s)
     * @param name name of the maneuver
     * @param control3DVectorCostType control vector's cost type
     * @param <T> segment type
     * @return propulsion model
     * @since 13.0
     */
    public static <T extends ThrustSegment> ProfileThrustPropulsionModel of(final TimeSpanMap<T> profile,
                                                                            final double isp,
                                                                            final Control3DVectorCostType control3DVectorCostType,
                                                                            final String name) {
        return new ProfileThrustPropulsionModel(buildSegments(profile), isp, control3DVectorCostType, name);
    }

    /**
     * Method building a map of generic segments from customized ones.
     * @param timeSpanMap input profile
     * @param <T> segment type
     * @return generic profile
     * @since 13.0
     */
    private static <T extends ThrustSegment> TimeSpanMap<ThrustSegment> buildSegments(final TimeSpanMap<T> timeSpanMap) {
        TimeSpanMap.Span<T> span = timeSpanMap.getFirstSpan();
        final TimeSpanMap<ThrustSegment> segments = new TimeSpanMap<>(null);
        while (span != null) {
            segments.addValidBetween(span.getData(), span.getStart(), span.getEnd());
            span = span.next();
        }
        return segments;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public Control3DVectorCostType getControl3DVectorCostType() {
        return control3DVectorCostType;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final SpacecraftState s) {
        return getThrustVector(s, getParameters());
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final SpacecraftState s) {
        return getFlowRate(s, getParameters());
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final SpacecraftState s, final double[] parameters) {
        return -control3DVectorCostType.evaluate(getThrustVector(s, parameters)) / ThrustPropulsionModel.getExhaustVelocity(isp);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final SpacecraftState s, final double[] parameters) {
        final ThrustSegment active = getActiveSegment(s.getDate());
        return active == null ? Vector3D.ZERO : active.getThrustVector(s.getDate(), s.getMass(), parameters);
    }

    /**
     * Return the active thrust segment at input date.
     * @param date date
     * @return active segment
     * @since 13.0
     */
    public ThrustSegment getActiveSegment(final AbsoluteDate date) {
        return profile.get(date);
    }

    /**
     * Extract the firings between given dates.
     * @param startDate start date
     * @param endDate end date
     * @return firings
     * @since 13.0
     */
    public TimeSpanMap<ThrustSegment> getFirings(final AbsoluteDate startDate, final AbsoluteDate endDate) {
        return profile.extractRange(startDate, endDate);
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final FieldSpacecraftState<T> s,
                                                                                final T[] parameters) {
        final ThrustSegment active = getActiveSegment(s.getDate().toAbsoluteDate());
        return active == null ? FieldVector3D.getZero(s.getDate().getField()) :
                active.getThrustVector(s.getDate(), s.getMass(), parameters);
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> T getFlowRate(final FieldSpacecraftState<T> s, final T[] parameters) {
        return control3DVectorCostType.evaluate(getThrustVector(s, parameters)).divide(-ThrustPropulsionModel.getExhaustVelocity(isp));
    }

    /** {@inheritDoc}.
     * <p>
     * The single detector returned triggers {@link org.hipparchus.ode.events.Action#RESET_DERIVATIVES} events
     * at every {@link ThrustSegment thrust segments} boundaries.
     * </p>
     */
    @Override
    public Stream<EventDetector> getEventDetectors() {

        final List<AbsoluteDate> transitionDates = new ArrayList<>();
        for (TimeSpanMap.Transition<ThrustSegment> transition = profile.getFirstTransition();
            transition != null;
            transition = transition.next()) {
            transitionDates.add(transition.getDate());
        }
        final DateDetector detector = getDateDetector(transitionDates.toArray(new TimeStamped[0]));
        return Stream.of(detector);
    }

    /** {@inheritDoc}.
     * <p>
     * The single detector returned triggers {@link org.hipparchus.ode.events.Action#RESET_DERIVATIVES} events
     * at every {@link ThrustSegment thrust segments} boundaries.
     * </p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final List<AbsoluteDate> transitionDates = new ArrayList<>();
        for (TimeSpanMap.Transition<ThrustSegment> transition = profile.getFirstTransition();
            transition != null;
            transition = transition.next()) {
            transitionDates.add(transition.getDate());
        }
        final FieldDateDetector<T> detector = getFieldDateDetector(field, transitionDates.toArray(new AbsoluteDate[0]));
        return Stream.of(detector);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // Get all transitions from the TimeSpanMap
        final List<ParameterDriver> listParameterDrivers = new ArrayList<>();

        // Loop on the spans
        for (TimeSpanMap.Span<ThrustSegment> span = profile.getFirstSpan(); span != null; span = span.next()) {
            // Add all the parameter drivers of the span
            if (span.getData() != null) {
                for (ParameterDriver driver : span.getData().getParametersDrivers()) {
                    // Add the driver only if the name does not exist already
                    if (!ParameterDriversProvider.findByName(listParameterDrivers, driver.getName())) {
                        listParameterDrivers.add(driver);
                    }
                }
            }
        }

        // Return an array of parameter drivers with no duplicated name
        return listParameterDrivers;
    }
}
