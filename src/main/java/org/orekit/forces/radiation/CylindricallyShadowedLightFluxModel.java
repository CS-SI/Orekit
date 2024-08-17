/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.events.CylindricalShadowEclipseDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldCylindricalShadowEclipseDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.utils.ExtendedPositionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Class defining a flux model with a single occulting body, casting a shadow whose shape is a circular cylinder
 * (equivalent to the light source being infinitely distant). It is less accurate but faster to evaluate than a conical
 * model.
 *
 * @author Romain Serra
 * @see AbstractLightFluxModel
 * @see LightFluxModel
 * @since 12.1
 */
public class CylindricallyShadowedLightFluxModel extends AbstractLightFluxModel {

    /**
     * Max. check interval for eclipse detection.
     */
    private static final double CYLINDRICAL_ECLIPSE_MAX_CHECK = 100;

    /**
     * Threshold for eclipse detection.
     */
    private static final double CYLINDRICAL_ECLIPSE_THRESHOLD = 1e-7;

    /** Radius of central, occulting body (approximated as spherical).
     * Its center is assumed to be at the origin of the frame linked to the state. */
    private final double occultingBodyRadius;

    /** Reference flux normalized for a 1m distance (N). */
    private final double kRef;

    /** Eclipse detection settings. */
    private final EventDetectionSettings eventDetectionSettings;

    /**
     * Constructor.
     * @param kRef reference flux
     * @param occultedBody position provider for light source
     * @param occultingBodyRadius radius of central, occulting body
     * @param eventDetectionSettings user-defined detection settings for eclipses (if ill-tuned, events might be missed or performance might drop)
     * @since 12.2
     */
    public CylindricallyShadowedLightFluxModel(final double kRef, final ExtendedPositionProvider occultedBody,
                                               final double occultingBodyRadius, final EventDetectionSettings eventDetectionSettings) {
        super(occultedBody);
        this.kRef = kRef;
        this.occultingBodyRadius = occultingBodyRadius;
        this.eventDetectionSettings = eventDetectionSettings;
    }

    /**
     * Constructor with default event detection settings.
     * @param kRef reference flux
     * @param occultedBody position provider for light source
     * @param occultingBodyRadius radius of central, occulting body
     */
    public CylindricallyShadowedLightFluxModel(final double kRef, final ExtendedPositionProvider occultedBody,
                                               final double occultingBodyRadius) {
        this(kRef, occultedBody, occultingBodyRadius, new EventDetectionSettings(CYLINDRICAL_ECLIPSE_MAX_CHECK,
            CYLINDRICAL_ECLIPSE_THRESHOLD, EventDetectionSettings.DEFAULT_MAX_ITER));
    }

    /**
     * Constructor with default value for reference flux.
     * @param occultedBody position provider for light source
     * @param occultingBodyRadius radius of central, occulting body
     */
    public CylindricallyShadowedLightFluxModel(final ExtendedPositionProvider occultedBody,
                                               final double occultingBodyRadius) {
        this(4.56e-6 * FastMath.pow(149597870000.0, 2), occultedBody, occultingBodyRadius);
    }

    /**
     * Getter for occulting body radius.
     * @return radius
     */
    public double getOccultingBodyRadius() {
        return occultingBodyRadius;
    }

    /**
     * Getter for eclipse event detection settings.
     * @return event detection settings
     * @since 12.2
     */
    public EventDetectionSettings getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getUnoccultedFluxVector(final Vector3D relativePosition) {
        final double squaredRadius = relativePosition.getNormSq();
        final double factor = kRef / (squaredRadius * FastMath.sqrt(squaredRadius));
        return relativePosition.scalarMultiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getUnoccultedFluxVector(final FieldVector3D<T> relativePosition) {
        final T squaredRadius = relativePosition.getNormSq();
        final T factor = (squaredRadius.multiply(squaredRadius.sqrt())).reciprocal().multiply(kRef);
        return relativePosition.scalarMultiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    protected double getLightingRatio(final Vector3D position, final Vector3D occultedBodyPosition) {
        final Vector3D occultedBodyDirection = occultedBodyPosition.normalize();
        final double dotProduct = position.dotProduct(occultedBodyDirection);
        if (dotProduct < 0.) {
            final double distanceToCylinderAxis = (position.subtract(occultedBodyDirection.scalarMultiply(dotProduct))).getNorm();
            if (distanceToCylinderAxis <= occultingBodyRadius) {
                return 0.;
            }
        }
        return 1.;
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T getLightingRatio(final FieldVector3D<T> position,
                                                                     final FieldVector3D<T> occultedBodyPosition) {
        final Field<T> field = position.getX().getField();
        final FieldVector3D<T> occultedBodyDirection = occultedBodyPosition.normalize();
        final T dotProduct = position.dotProduct(occultedBodyDirection);
        if (dotProduct.getReal() < 0.) {
            final T distanceToCylinderAxis = (position.subtract(occultedBodyDirection.scalarMultiply(dotProduct))).getNorm();
            if (distanceToCylinderAxis.getReal() <= occultingBodyRadius) {
                return field.getZero();
            }
        }
        return field.getOne();
    }


    /** {@inheritDoc} */
    @Override
    public List<EventDetector> getEclipseConditionsDetector() {
        final List<EventDetector> detectors = new ArrayList<>();
        detectors.add(createCylindricalShadowEclipseDetector().withDetectionSettings(getEventDetectionSettings()));
        return detectors;
    }

    /**
     * Method to create a new eclipse detector.
     * @return detector
     */
    private CylindricalShadowEclipseDetector createCylindricalShadowEclipseDetector() {
        return new CylindricalShadowEclipseDetector(getOccultedBody(), getOccultingBodyRadius(),
                (state, detector, increasing) -> Action.RESET_DERIVATIVES);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> List<FieldEventDetector<T>> getFieldEclipseConditionsDetector(final Field<T> field) {
        final List<FieldEventDetector<T>> detectors = new ArrayList<>();
        final FieldEventDetectionSettings<T> detectionSettings = new FieldEventDetectionSettings<>(field, getEventDetectionSettings());
        detectors.add(createFieldCylindricalShadowEclipseDetector(field).withDetectionSettings(detectionSettings));
        return detectors;
    }

    /**
     * Method to create a new eclipse detector. Field version.
     * @param field field
     * @param <T> field type
     * @return detector
     */
    private <T extends CalculusFieldElement<T>> FieldCylindricalShadowEclipseDetector<T> createFieldCylindricalShadowEclipseDetector(final Field<T> field) {
        final T occultingBodyRadiusAsField = field.getZero().newInstance(getOccultingBodyRadius());
        return new FieldCylindricalShadowEclipseDetector<>(getOccultedBody(), occultingBodyRadiusAsField,
                (state, detector, increasing) -> Action.RESET_DERIVATIVES);
    }
}
