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
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Class defining a flux model from a single occulted body, casting a shadow on a spherical occulting body.
 * It cannot model oblate bodies or multiple occulting objects (for this, see {@link SolarRadiationPressure}).
 *
 * @author Romain Serra
 * @see AbstractSolarLightFluxModel
 * @see LightFluxModel
 * @see "Montenbruck, Oliver, and Gill, Eberhard. Satellite orbits : models, methods, and
 *  * applications. Berlin New York: Springer, 2000."
 * @since 12.2
 */
public class ConicallyShadowedLightFluxModel extends AbstractSolarLightFluxModel {

    /**
     * Default max. check interval for eclipse detection.
     */
    private static final double CONICAL_ECLIPSE_MAX_CHECK = 60;

    /**
     * Default threshold for eclipse detection.
     */
    private static final double CONICAL_ECLIPSE_THRESHOLD = 1e-7;

    /** Occulted body radius. */
    private final double occultedBodyRadius;

    /** Cached date. */
    private AbsoluteDate lastDate;

    /** Cached frame. */
    private Frame propagationFrame;

    /** Cached position. */
    private Vector3D lastPosition;

    /**
     * Constructor.
     * @param kRef reference flux
     * @param occultedBodyRadius radius of occulted body (light source)
     * @param occultedBody position provider for light source
     * @param occultingBodyRadius radius of central, occulting body
     * @param eventDetectionSettings user-defined detection settings for eclipses (if ill-tuned, events might be missed or performance might drop)
     */
    public ConicallyShadowedLightFluxModel(final double kRef, final double occultedBodyRadius,
                                           final ExtendedPositionProvider occultedBody,
                                           final double occultingBodyRadius, final EventDetectionSettings eventDetectionSettings) {
        super(kRef, occultedBody, occultingBodyRadius, eventDetectionSettings);
        this.occultedBodyRadius = occultedBodyRadius;
    }

    /**
     * Constructor with default event detection settings.
     * @param kRef reference flux
     * @param occultedBodyRadius radius of occulted body (light source)
     * @param occultedBody position provider for light source
     * @param occultingBodyRadius radius of central, occulting body
     */
    public ConicallyShadowedLightFluxModel(final double kRef, final double occultedBodyRadius,
                                           final ExtendedPositionProvider occultedBody, final double occultingBodyRadius) {
        this(kRef, occultedBodyRadius, occultedBody, occultingBodyRadius, getDefaultEclipseDetectionSettings());
    }

    /**
     * Constructor with default value for reference flux.
     * @param occultedBodyRadius radius of occulted body (light source)
     * @param occultedBody position provider for light source
     * @param occultingBodyRadius radius of central, occulting body
     */
    public ConicallyShadowedLightFluxModel(final double occultedBodyRadius, final ExtendedPositionProvider occultedBody,
                                           final double occultingBodyRadius) {
        super(occultedBody, occultingBodyRadius, getDefaultEclipseDetectionSettings());
        this.occultedBodyRadius = occultedBodyRadius;
    }

    /**
     * Define default detection settings for eclipses.
     * @return default settings
     */
    public static EventDetectionSettings getDefaultEclipseDetectionSettings() {
        return new EventDetectionSettings(CONICAL_ECLIPSE_MAX_CHECK, CONICAL_ECLIPSE_THRESHOLD,
                EventDetectionSettings.DEFAULT_MAX_ITER);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate targetDate) {
        super.init(initialState, targetDate);
        lastDate = initialState.getDate();
        propagationFrame = initialState.getFrame();
        lastPosition = getOccultedBodyPosition(lastDate, propagationFrame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void init(final FieldSpacecraftState<T> initialState,
                                                         final FieldAbsoluteDate<T> targetDate) {
        super.init(initialState, targetDate);
        lastDate = initialState.getDate().toAbsoluteDate();
        propagationFrame = initialState.getFrame();
        lastPosition = getOccultedBodyPosition(initialState.getDate(), propagationFrame).toVector3D();
    }

    /**
     * Get occulted body position using cache.
     * @param date date
     * @return occulted body position
     */
    private Vector3D getOccultedBodyPosition(final AbsoluteDate date) {
        if (!lastDate.isEqualTo(date)) {
            lastPosition = getOccultedBodyPosition(date, propagationFrame);
            lastDate = date;
        }
        return lastPosition;
    }

    /**
     * Get occulted body position using cache (non-Field and no derivatives case).
     * @param fieldDate date
     * @param <T> field type
     * @return occulted body position
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> getOccultedBodyPosition(final FieldAbsoluteDate<T> fieldDate) {
        if (fieldDate.hasZeroField()) {
            final AbsoluteDate date = fieldDate.toAbsoluteDate();
            if (!lastDate.isEqualTo(date)) {
                lastPosition = getOccultedBodyPosition(date, propagationFrame);
                lastDate = date;
            }
            return new FieldVector3D<>(fieldDate.getField(), lastPosition);
        } else {
            return getOccultedBodyPosition(fieldDate, propagationFrame);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected double getLightingRatio(final Vector3D position, final Vector3D occultedBodyPosition) {
        final double distanceSun = occultedBodyPosition.getNorm();
        final double squaredDistance = position.getNormSq();
        final Vector3D occultedBodyDirection = occultedBodyPosition.normalize();
        final double s0 = -position.dotProduct(occultedBodyDirection);
        if (s0 > 0.0) {
            final double l = FastMath.sqrt(squaredDistance - s0 * s0);
            final double sinf2 = (occultedBodyRadius - getOccultingBodyRadius()) / distanceSun;
            final double l2 = (s0 * sinf2 - getOccultingBodyRadius()) / FastMath.sqrt(1.0 - sinf2 * sinf2);
            if (FastMath.abs(l2) - l >= 0.0) { // umbra
                return 0.;
            }
            final double sinf1 = (occultedBodyRadius + getOccultingBodyRadius()) / distanceSun;
            final double l1 = (s0 * sinf1 + getOccultingBodyRadius()) / FastMath.sqrt(1.0 - sinf1 * sinf1);
            if (l1 - l > 0.0) { // penumbra
                final Vector3D relativePosition = occultedBodyPosition.subtract(position);
                final double relativeDistance = relativePosition.getNorm();
                final double a = FastMath.asin(occultedBodyRadius / relativeDistance);
                final double a2 = a * a;
                final double r = FastMath.sqrt(squaredDistance);
                final double b = FastMath.asin(getOccultingBodyRadius() / r);
                final double c = FastMath.acos(-(relativePosition.dotProduct(position)) / (r * relativeDistance));
                final double x = (c * c + a2 - b * b) / (2 * c);
                final double y = FastMath.sqrt(FastMath.max(0., a2 - x * x));
                final double arcCosXOverA = FastMath.acos(FastMath.max(-1, x / a));
                final double intermediate = (arcCosXOverA + (b * b * FastMath.acos((c - x) / b) - c * y) / a2) / FastMath.PI;
                return 1. - intermediate;
            }
        }
        return 1.;
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T getLightingRatio(final FieldVector3D<T> position,
                                                                     final FieldVector3D<T> occultedBodyPosition) {
        final Field<T> field = position.getX().getField();
        final T distanceSun = occultedBodyPosition.getNorm();
        final T squaredDistance = position.getNormSq();
        final FieldVector3D<T> occultedBodyDirection = occultedBodyPosition.normalize();
        final T s0 = position.dotProduct(occultedBodyDirection).negate();
        if (s0.getReal() > 0.0) {
            final T reciprocalDistanceSun = distanceSun.reciprocal();
            final T sinf2 = reciprocalDistanceSun.multiply(occultedBodyRadius - getOccultingBodyRadius());
            final T l2 = (s0.multiply(sinf2).subtract(getOccultingBodyRadius())).divide(FastMath.sqrt(sinf2.square().negate().add(1)));
            final T l = FastMath.sqrt(squaredDistance.subtract(s0.square()));
            if (FastMath.abs(l2).subtract(l).getReal() >= 0.0) { // umbra
                return field.getZero();
            }
            final T sinf1 = reciprocalDistanceSun.multiply(occultedBodyRadius + getOccultingBodyRadius());
            final T l1 = (s0.multiply(sinf1).add(getOccultingBodyRadius())).divide(FastMath.sqrt(sinf1.square().negate().add(1)));
            if (l1.subtract(l).getReal() > 0.0) { // penumbra
                final FieldVector3D<T> relativePosition = occultedBodyPosition.subtract(position);
                final T relativeDistance = relativePosition.getNorm();
                final T a = FastMath.asin(relativeDistance.reciprocal().multiply(occultedBodyRadius));
                final T a2 = a.square();
                final T r = FastMath.sqrt(squaredDistance);
                final T b = FastMath.asin(r.reciprocal().multiply(getOccultingBodyRadius()));
                final T b2 = b.square();
                final T c = FastMath.acos((relativePosition.dotProduct(position).negate()).divide(r.multiply(relativeDistance)));
                final T x = (c.square().add(a2).subtract(b2)).divide(c.multiply(2));
                final T x2 = x.square();
                final T y = (a2.getReal() - x2.getReal() <= 0) ? s0.getField().getZero() : FastMath.sqrt(a2.subtract(x2));
                final T arcCosXOverA = (x.getReal() / a.getReal() < -1) ? s0.getPi().negate() : FastMath.acos(x.divide(a));
                final T intermediate = arcCosXOverA.add(((b2.multiply(FastMath.acos((c.subtract(x)).divide(b))))
                        .subtract(c.multiply(y))).divide(a2));
                return intermediate.divide(-FastMath.PI).add(1);
            }
        }
        return field.getOne();
    }

    /** {@inheritDoc} */
    @Override
    public List<EventDetector> getEclipseConditionsDetector() {
        final List<EventDetector> detectors = new ArrayList<>();
        detectors.add(createUmbraEclipseDetector());
        detectors.add(createPenumbraEclipseDetector());
        return detectors;
    }

    /**
     * Method to create a new umbra detector.
     * @return detector
     */
    private InternalEclipseDetector createUmbraEclipseDetector() {
        return new InternalEclipseDetector() {
            @Override
            public double g(final SpacecraftState s) {
                final Vector3D position = s.getPosition();
                final Vector3D occultedBodyPosition = getOccultedBodyPosition(s.getDate());
                final Vector3D occultedBodyDirection = occultedBodyPosition.normalize();
                final double s0 = -position.dotProduct(occultedBodyDirection);
                final double distanceSun = occultedBodyPosition.getNorm();
                final double squaredDistance = position.getNormSq();
                final double sinf2 = (occultedBodyRadius - getOccultingBodyRadius()) / distanceSun;
                final double l = FastMath.sqrt(squaredDistance - s0 * s0);
                final double l2 = (s0 * sinf2 - getOccultingBodyRadius()) / FastMath.sqrt(1.0 - sinf2 * sinf2);
                return FastMath.abs(l2) / l - 1.;
            }
        };
    }

    /**
     * Method to create a new penumbra detector.
     * @return detector
     */
    private InternalEclipseDetector createPenumbraEclipseDetector() {
        return new InternalEclipseDetector() {
            @Override
            public double g(final SpacecraftState s) {
                final Vector3D position = s.getPosition();
                final Vector3D occultedBodyPosition = getOccultedBodyPosition(s.getDate());
                final Vector3D occultedBodyDirection = occultedBodyPosition.normalize();
                final double s0 = -position.dotProduct(occultedBodyDirection);
                final double distanceSun = occultedBodyPosition.getNorm();
                final double squaredDistance = position.getNormSq();
                final double l = FastMath.sqrt(squaredDistance - s0 * s0);
                final double sinf1 = (occultedBodyRadius + getOccultingBodyRadius()) / distanceSun;
                final double l1 = (s0 * sinf1 + getOccultingBodyRadius()) / FastMath.sqrt(1.0 - sinf1 * sinf1);
                return l1 / l - 1.;
            }
        };
    }

    /**
     * Internal class for event detector.
     */
    private abstract class InternalEclipseDetector implements EventDetector {
        /** Event handler. */
        private final ResetDerivativesOnEvent handler;

        /**
         * Constructor.
         */
        InternalEclipseDetector() {
            this.handler = new ResetDerivativesOnEvent();
        }

        @Override
        public EventDetectionSettings getDetectionSettings() {
            return getEventDetectionSettings();
        }

        @Override
        public double getThreshold() {
            return getDetectionSettings().getThreshold();
        }

        @Override
        public AdaptableInterval getMaxCheckInterval() {
            return getDetectionSettings().getMaxCheckInterval();
        }

        @Override
        public int getMaxIterationCount() {
            return getDetectionSettings().getMaxIterationCount();
        }

        @Override
        public EventHandler getHandler() {
            return handler;
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> List<FieldEventDetector<T>> getFieldEclipseConditionsDetector(final Field<T> field) {
        final List<FieldEventDetector<T>> detectors = new ArrayList<>();
        final FieldEventDetectionSettings<T> detectionSettings = new FieldEventDetectionSettings<>(field,
            getEventDetectionSettings());
        detectors.add(createFieldUmbraEclipseDetector(detectionSettings));
        detectors.add(createFieldPenumbraEclipseDetector(detectionSettings));
        return detectors;
    }

    /**
     * Method to create a new umbra detector. Field version.
     * @param detectionSettings non-Field detection settings
     * @param <T> field type
     * @return detector
     */
    private <T extends CalculusFieldElement<T>> FieldInternalEclipseDetector<T> createFieldUmbraEclipseDetector(final FieldEventDetectionSettings<T> detectionSettings) {
        return new FieldInternalEclipseDetector<T>(detectionSettings) {
            @Override
            public T g(final FieldSpacecraftState<T> s) {
                final FieldVector3D<T> position = s.getPosition();
                final FieldVector3D<T> occultedBodyPosition = getOccultedBodyPosition(s.getDate());
                final FieldVector3D<T> occultedBodyDirection = occultedBodyPosition.normalize();
                final T s0 = position.dotProduct(occultedBodyDirection).negate();
                final T distanceSun = occultedBodyPosition.getNorm();
                final T squaredDistance = position.getNormSq();
                final T reciprocalDistanceSun = distanceSun.reciprocal();
                final T sinf2 = reciprocalDistanceSun.multiply(occultedBodyRadius - getOccultingBodyRadius());
                final T l2 = (s0.multiply(sinf2).subtract(getOccultingBodyRadius())).divide(FastMath.sqrt(sinf2.square().negate().add(1)));
                final T l = FastMath.sqrt(squaredDistance.subtract(s0.square()));
                return FastMath.abs(l2).divide(l).subtract(1);
            }
        };
    }

    /**
     * Method to create a new penumbra detector. Field version.
     * @param detectionSettings non-Field detection settings
     * @param <T> field type
     * @return detector
     */
    private <T extends CalculusFieldElement<T>> FieldInternalEclipseDetector<T> createFieldPenumbraEclipseDetector(final FieldEventDetectionSettings<T> detectionSettings) {
        return new FieldInternalEclipseDetector<T>(detectionSettings) {
            @Override
            public T g(final FieldSpacecraftState<T> s) {
                final FieldVector3D<T> position = s.getPosition();
                final FieldVector3D<T> occultedBodyPosition = getOccultedBodyPosition(s.getDate());
                final FieldVector3D<T> occultedBodyDirection = occultedBodyPosition.normalize();
                final T s0 = position.dotProduct(occultedBodyDirection).negate();
                final T distanceSun = occultedBodyPosition.getNorm();
                final T squaredDistance = position.getNormSq();
                final T reciprocalDistanceSun = distanceSun.reciprocal();
                final T sinf1 = reciprocalDistanceSun.multiply(occultedBodyRadius + getOccultingBodyRadius());
                final T l1 = (s0.multiply(sinf1).add(getOccultingBodyRadius())).divide(FastMath.sqrt(sinf1.square().negate().add(1)));
                final T l = FastMath.sqrt(squaredDistance.subtract(s0.square()));
                return l1.divide(l).subtract(1);
            }
        };
    }

    /**
     * Internal class for event detector.
     */
    private abstract static class FieldInternalEclipseDetector<T extends CalculusFieldElement<T>> implements FieldEventDetector<T> {
        /** Event handler. */
        private final FieldResetDerivativesOnEvent<T> handler;

        /** Detection settings. */
        private final FieldEventDetectionSettings<T> fieldEventDetectionSettings;

        /**
         * Constructor.
         * @param fieldEventDetectionSettings detection settings
         */
        FieldInternalEclipseDetector(final FieldEventDetectionSettings<T> fieldEventDetectionSettings) {
            this.handler = new FieldResetDerivativesOnEvent<>();
            this.fieldEventDetectionSettings = fieldEventDetectionSettings;
        }

        @Override
        public FieldEventDetectionSettings<T> getDetectionSettings() {
            return fieldEventDetectionSettings;
        }

        @Override
        public T getThreshold() {
            return getDetectionSettings().getThreshold();
        }

        @Override
        public FieldAdaptableInterval<T> getMaxCheckInterval() {
            return getDetectionSettings().getMaxCheckInterval();
        }

        @Override
        public int getMaxIterationCount() {
            return getDetectionSettings().getMaxIterationCount();
        }

        @Override
        public FieldEventHandler<T> getHandler() {
            return handler;
        }
    }
}
