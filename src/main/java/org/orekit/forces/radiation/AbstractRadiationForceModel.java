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
package org.orekit.forces.radiation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEclipseDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ExtendedPVCoordinatesProviderAdapter;
import org.orekit.utils.OccultationEngine;

/**
 * Base class for radiation force models.
 * @see SolarRadiationPressure
 * @see ECOM2
 * @since 10.2
 */
public abstract class AbstractRadiationForceModel implements ForceModel {

    /** Margin to force recompute lighting ratio derivatives when we are really inside penumbra. */
    private static final double ANGULAR_MARGIN = 1.0e-10;

    /** Max check interval for eclipse detectors. */
    private static final double ECLIPSE_MAX_CHECK = 60.0;

    /** Threshold for eclipse detectors. */
    private static final double ECLIPSE_THRESHOLD = 1.0e-7; // this is consistent with ANGULAR_MARGIN = 10⁻¹⁰ rad for LEO

    /** Flatness for spherical bodies. */
    private static final double SPHERICAL_BODY_FLATNESS = 0.0;

    /** Prefix for occulting bodies frames names. */
    private static final String OCCULTING_PREFIX = "occulting-";

    /** Occulting bodies (central body is always the first one).
     * @since 12.0
     */
    private final List<OccultationEngine> occultingBodies;

    /**
     * Default constructor.
     * Only central body is considered.
     * @param sun Sun model
     * @param centralBody central body shape model (for umbra/penumbra computation)
     * @since 12.0
     */
    protected AbstractRadiationForceModel(final ExtendedPVCoordinatesProvider sun, final OneAxisEllipsoid centralBody) {
        // in most cases, there will be only Earth, sometimes also Moon so an initial capacity of 2 is appropriate
        occultingBodies = new ArrayList<>(2);
        occultingBodies.add(new OccultationEngine(sun, Constants.SUN_RADIUS, centralBody));
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        final EventDetector[] detectors = new EventDetector[2 * occultingBodies.size()];
        for (int i = 0; i < occultingBodies.size(); ++i) {
            final OccultationEngine occulting = occultingBodies.get(i);
            detectors[2 * i]     = new EclipseDetector(occulting).
                                   withUmbra().
                                   withMargin(-ANGULAR_MARGIN).
                                   withMaxCheck(ECLIPSE_MAX_CHECK).
                                   withThreshold(ECLIPSE_THRESHOLD).
                                   withHandler((state, detector, increasing) -> Action.RESET_DERIVATIVES);
            detectors[2 * i + 1] = new EclipseDetector(occulting).
                                   withPenumbra().
                                   withMargin(ANGULAR_MARGIN).
                                   withMaxCheck(ECLIPSE_MAX_CHECK).
                                   withThreshold(ECLIPSE_THRESHOLD).
                                   withHandler((state, detector, increasing) -> Action.RESET_DERIVATIVES);
        }
        // Fusion between Date detector for parameter driver span change and
        // Detector for umbra / penumbra events
        return Stream.concat(Stream.of(detectors), ForceModel.super.getEventDetectors());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final T zero = field.getZero();
        @SuppressWarnings("unchecked")
        final FieldEventDetector<T>[] detectors = (FieldEventDetector<T>[]) Array.newInstance(FieldEventDetector.class,
                                                                                              2 * occultingBodies.size());
        for (int i = 0; i < occultingBodies.size(); ++i) {
            final OccultationEngine occulting = occultingBodies.get(i);
            detectors[2 * i]     = new FieldEclipseDetector<>(field, occulting).
                                   withUmbra().
                                   withMargin(zero.newInstance(-ANGULAR_MARGIN)).
                                   withMaxCheck(ECLIPSE_MAX_CHECK).
                                   withThreshold(zero.newInstance(ECLIPSE_THRESHOLD)).
                                   withHandler((state, detector, increasing) -> Action.RESET_DERIVATIVES);
            detectors[2 * i + 1] = new FieldEclipseDetector<>(field, occulting).
                                   withPenumbra().
                                   withMargin(zero.newInstance(ANGULAR_MARGIN)).
                                   withMaxCheck(ECLIPSE_MAX_CHECK).
                                   withThreshold(zero.newInstance(ECLIPSE_THRESHOLD)).
                                   withHandler((state, detector, increasing) -> Action.RESET_DERIVATIVES);
        }
        return Stream.concat(Stream.of(detectors), ForceModel.super.getFieldEventDetectors(field));
    }

    /**
     * Get the useful angles for eclipse computation.
     * @param position the satellite's position in the selected frame
     * @param occultingPosition Oculting body position in the selected frame
     * @param occultingRadius Occulting body mean radius
     * @param occultedPosition Occulted body position in the selected frame
     * @param occultedRadius Occulted body mean radius
     * @return the 3 angles {(satOcculting, satOcculted), Occulting body apparent radius, Occulted body apparent radius}
     */
    protected double[] getGeneralEclipseAngles(final Vector3D position, final Vector3D occultingPosition, final double occultingRadius,
                                               final Vector3D occultedPosition, final double occultedRadius) {
        final double[] angle = new double[3];

        final Vector3D satOccultedVector = occultedPosition.subtract(position);
        final Vector3D satOccultingVector = occultingPosition.subtract(position);

        // Sat-Occulted / Sat-Occulting angle
        angle[0] = Vector3D.angle(satOccultedVector, satOccultingVector);

        // Occulting body apparent radius
        angle[1] = FastMath.asin(occultingRadius / satOccultingVector.getNorm());

        // Occulted body apparent radius
        angle[2] = FastMath.asin(occultedRadius / satOccultedVector.getNorm());

        return angle;
    }

    /**
     * Get the useful angles for eclipse computation.
     * @param occultingPosition Oculting body position in the selected frame
     * @param occultingRadius Occulting body mean radius
     * @param occultedPosition Occulted body position in the selected frame
     * @param occultedRadius Occulted body mean radius
     * @param position the satellite's position in the selected frame
     * @param <T> extends RealFieldElement
     * @return the 3 angles {(satOcculting, satOcculted), Occulting body apparent radius, Occulted body apparent radius}
     */
    protected <T extends CalculusFieldElement<T>> T[] getGeneralEclipseAngles(final FieldVector3D<T> position,
                                                                              final FieldVector3D<T> occultingPosition, final T occultingRadius,
                                                                              final FieldVector3D<T> occultedPosition, final T occultedRadius) {
        final T[] angle = MathArrays.buildArray(position.getX().getField(), 3);

        final FieldVector3D<T> satOccultedVector = occultedPosition.subtract(position);
        final FieldVector3D<T> satOccultingVector = occultingPosition.subtract(position);

        // Sat-Occulted / Sat-Occulting angle
        angle[0] = FieldVector3D.angle(satOccultedVector, satOccultingVector);

        // Occulting body apparent radius
        angle[1] = occultingRadius.divide(satOccultingVector.getNorm()).asin();

        // Occulted body apparent radius
        angle[2] = occultedRadius.divide(satOccultedVector.getNorm()).asin();

        return angle;
    }

    /**
     * Add a new occulting body.
     * <p>
     * Central body is already considered, it shall not be added this way.
     * </p>
     * @param provider body PV provider
     * @param radius body mean radius
     * @see #addOccultingBody(OneAxisEllipsoid)
     */
    public void addOccultingBody(final ExtendedPVCoordinatesProvider provider, final double radius) {

        // as parent frame for occulting body frame,
        // we select the first inertial frame in central body hierarchy
        Frame parent = occultingBodies.get(0).getOcculting().getBodyFrame();
        while (!parent.isPseudoInertial()) {
            parent = parent.getParent();
        }

        // as the occulting body will be spherical, we can use an inertially oriented body frame
        final Frame inertiallyOrientedBodyFrame =
                        new ExtendedPVCoordinatesProviderAdapter(parent,
                                                                 provider,
                                                                 OCCULTING_PREFIX + occultingBodies.size());

        // create the spherical occulting body
        final OneAxisEllipsoid sphericalOccultingBody =
                        new OneAxisEllipsoid(radius, SPHERICAL_BODY_FLATNESS, inertiallyOrientedBodyFrame);

        addOccultingBody(sphericalOccultingBody);

    }

    /**
     * Add a new occulting body.
     * <p>
     * Central body is already considered, it shall not be added this way.
     * </p>
     * @param occulting occulting body to add
     * @see #addOccultingBody(ExtendedPVCoordinatesProvider, double)
     * @since 12.0
     */
    public void addOccultingBody(final OneAxisEllipsoid occulting) {

        // retrieve Sun from the central occulting body engine
        final OccultationEngine central = occultingBodies.get(0);

        // create a new occultation engine for the new occulting body
        final OccultationEngine additional =
                        new OccultationEngine(central.getOcculted(), central.getOccultedRadius(), occulting);

        // add it to the list
        occultingBodies.add(additional);

    }

    /**
     * Get all occulting bodies to consider.
     * <p>
     * The list always contains at least one element: the central body
     * which is always the first one in the list.
     * </p>
     * @return immutable list of all occulting bodies to consider, including the central body
     * @since 12.0
     */
    public List<OccultationEngine> getOccultingBodies() {
        return Collections.unmodifiableList(occultingBodies);
    }

}
