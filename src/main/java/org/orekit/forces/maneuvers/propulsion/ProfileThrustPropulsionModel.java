/* Copyright 2023 Luc Maisonobe
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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/** Thrust propulsion model based on segmented profile.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class ProfileThrustPropulsionModel implements ThrustPropulsionModel {

    /** Accuracy of switching events datation (s). */
    private static final double DATATION_ACCURACY = 1.0e-10;

    /** Thrust profile. */
    private final TimeSpanMap<PolynomialThrustSegment> profile;

    /** Specific impulse. */
    private final double isp;

    /** Name of the maneuver. */
    private final String name;

    /** Type of norm linking thrust vector to mass flow rate. */
    private final Control3DVectorCostType control3DVectorCostType;

    /** Generic constructor.
     * @param profile thrust profile (N)
     * @param isp specific impulse (s)
     * @param control3DVectorCostType control vector's cost type
     * @param name name of the maneuver
     */
    public ProfileThrustPropulsionModel(final TimeSpanMap<PolynomialThrustSegment> profile, final double isp,
                                        final Control3DVectorCostType control3DVectorCostType, final String name) {
        this.name    = name;
        this.isp     = isp;
        this.profile = profile;
        this.control3DVectorCostType = control3DVectorCostType;
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
        final PolynomialThrustSegment active = profile.get(s.getDate());
        return active == null ? Vector3D.ZERO : active.getThrustVector(s.getDate());
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final SpacecraftState s) {
        return -control3DVectorCostType.evaluate(getThrustVector(s)) / (Constants.G0_STANDARD_GRAVITY * isp);
    }

    /** {@inheritDoc}
     * <p>
     * Here the thrust vector does not depend on parameters
     * </p>
     */
    @Override
    public Vector3D getThrustVector(final SpacecraftState s, final double[] parameters) {
        return getThrustVector(s);
    }

    /** {@inheritDoc}
     * <p>
     * Here the flow rate does not depend on parameters
     * </p>
     */
    public double getFlowRate(final SpacecraftState s, final double[] parameters) {
        return getFlowRate(s);
    }

    /** {@inheritDoc}
     * <p>
     * Here the thrust vector does not depend on parameters
     * </p>
     */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final FieldSpacecraftState<T> s,
                                                                                final T[] parameters) {
        final PolynomialThrustSegment active = profile.get(s.getDate().toAbsoluteDate());
        return active == null ? FieldVector3D.getZero(s.getDate().getField()) : active.getThrustVector(s.getDate());
    }

    /** {@inheritDoc}
     * <p>
     * Here the flow rate does not depend on parameters
     * </p>
     */
    public <T extends CalculusFieldElement<T>> T getFlowRate(final FieldSpacecraftState<T> s, final T[] parameters) {
        return control3DVectorCostType.evaluate(getThrustVector(s, parameters)).divide(-Constants.G0_STANDARD_GRAVITY * isp);
    }

    /** {@inheritDoc}.
     * <p>
     * The single detector returned triggers {@link org.hipparchus.ode.events.Action#RESET_DERIVATIVES} events
     * at every {@link PolynomialThrustSegment thrust segments} boundaries.
     * </p>
     */
    @Override
    public Stream<EventDetector> getEventDetectors() {

        final double shortest = shortestSegmentDuration();
        final DateDetector detector = new DateDetector().
                                      withMaxCheck(0.5 * shortest).
                                      withMinGap(0.5 * shortest).
                                      withThreshold(DATATION_ACCURACY).
                                      withHandler((state, det, increasing) -> Action.RESET_DERIVATIVES);
        for (TimeSpanMap.Transition<PolynomialThrustSegment> transition = profile.getFirstTransition();
             transition != null;
             transition = transition.next()) {
            detector.addEventDate(transition.getDate());
        }
        return Stream.of(detector);
    }

    /** {@inheritDoc}.
     * <p>
     * The single detector returned triggers {@link org.hipparchus.ode.events.Action#RESET_DERIVATIVES} events
     * at every {@link PolynomialThrustSegment thrust segments} boundaries.
     * </p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final double shortest = shortestSegmentDuration();
        @SuppressWarnings("unchecked")
        final FieldDateDetector<T> detector = new FieldDateDetector<>(field,
                                                                      (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 0)).
                                              withMaxCheck(0.5 * shortest).
                                              withMinGap(0.5 * shortest).
                                              withThreshold(field.getZero().newInstance(DATATION_ACCURACY)).
                                              withHandler((state, det, increasing) -> Action.RESET_DERIVATIVES);
        for (TimeSpanMap.Transition<PolynomialThrustSegment> transition = profile.getFirstTransition();
             transition != null;
             transition = transition.next()) {
            detector.addEventDate(new FieldAbsoluteDate<>(field, transition.getDate()));
        }
        return Stream.of(detector);
    }

    /** Compute the duration of the shortest segment.
     * @return duration of the shortest segment
     */
    private double shortestSegmentDuration() {
        double shortest = Double.POSITIVE_INFINITY;
        for (TimeSpanMap.Span<PolynomialThrustSegment> span = profile.getFirstSpan();
             span != null;
             span = span.next()) {
            shortest = FastMath.min(shortest, span.getEnd().durationFrom(span.getStart()));
        }
        return shortest;
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }
}
