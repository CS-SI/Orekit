/* Copyright 2022-2026 Romain Serra
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
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
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
        // See Section 3.4.2, Figure 3.8
        final double occultingBodyRadius = getOccultingBodyRadius();
        final Vector3D relativePosition = occultedBodyPosition.subtract(position);
        final double relativeDistance = relativePosition.getNorm();
        final double a = FastMath.asin(occultedBodyRadius / relativeDistance);
        final double a2 = a * a;
        final double r = position.getNorm();
        final double b = FastMath.asin(occultingBodyRadius / r);
        final double b2 = b * b;
        final double c = Vector3D.angle(relativePosition.negate(), position);
        final double c2 = c * c;
        if (a + b <= c) {
            // no occultation
            return 1.0;
        }
        if (a <= b && c + a <= b) {
            // may have total eclipse and in umbra
            return 0.0;
        }
        if (a >= b && c + b <= a) {
            // occulting body too small for total eclipse
            // and maximally eclipsed
            // simple ratio of areas
            return 1.0 - b2 / a2;
        }
        final double x = (c2 + a2 - b2) / (2 * c);
        final double x2 = x * x;
        // expression for (c - x), i.e. BE in Fig 3.8. See #1892
        final double cMinusX = (c2 + b2 - a2) / (2 * c);
        final double y = FastMath.sqrt(FastMath.max(0., a2 - x2));
        // ref Figure 3.8
        final double alpha = FastMath.atan2(y, x);
        final double beta = FastMath.atan2(y, cMinusX);
        // because a, b, c are sides of a triangle, which is verified by the
        // three if's above, every expression in ( ... ) must be positive.
        final double triangleArea2 =
                (-c + a + b) * (c + a - b) * (c - a + b) * (c + a + b) / 4;
        // equivalent to c*y, see #1892
        final double triangleArea = FastMath.sqrt(triangleArea2);
        final double intermediate =
                (alpha + (b2 * beta - triangleArea) / a2) / FastMath.PI;
        return 1. - intermediate;
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T getLightingRatio(
            final FieldVector3D<T> position,
            final FieldVector3D<T> occultedBodyPosition) {

        final Field<T> field = position.getX().getField();
        final T zero = field.getZero();
        final T one = field.getOne();

        // See Section 3.4.2, Figure 3.8
        final T occultingBodyRadius = zero.add(getOccultingBodyRadius());
        final FieldVector3D<T> relativePosition =
                occultedBodyPosition.subtract(position);
        final T relativeDistance = relativePosition.getNorm();
        final T a = zero.add(occultedBodyRadius).divide(relativeDistance).asin();
        final T a2 = a.square();
        final T r = position.getNorm();
        final T b = occultingBodyRadius.divide(r).asin();
        final T b2 = b.square();
        final T c = FieldVector3D.angle(relativePosition.negate(), position);
        final T c2 = c.square();
        if (a.add(b).getReal() <= c.getReal()) {
            // no occultation
            return one;
        }
        if (a.getReal() <= b.getReal() && c.add(a).getReal() <= b.getReal()) {
            // may have total eclipse and in umbra
            return zero;
        }
        if (a.getReal() >= b.getReal() && c.add(b).getReal() <= a.getReal()) {
            // occulting body too small for total eclipse
            // and maximally eclipsed
            // simple ratio of areas
            return one.subtract(b2.divide(a2));
        }
        final T x = c2.add(a2).subtract(b2).divide(c.multiply(2));
        final T x2 = x.square();
        // expression for (c - x), i.e. BE in Fig 3.8. See #1892
        final T cMinusX = c2.add(b2).subtract(a2).divide(c.multiply(2));
        final T y = FastMath.sqrt(FastMath.max(zero, a2.subtract(x2)));
        // ref Figure 3.8
        final T alpha = FastMath.atan2(y, x);
        final T beta = FastMath.atan2(y, cMinusX);
        // because a, b, c are sides of a triangle, which is verified by the
        // three if's above, every expression in ( ... ) must be positive.
        final T triangleArea2 = c.negate().add(a).add(b)
                .multiply(c.add(a).subtract(b))
                .multiply(c.subtract(a).add(b))
                .multiply(c.add(a).add(b))
                .divide(4);
        // equivalent to c*y, see #1892
        final T triangleArea = FastMath.sqrt(triangleArea2);
        final T intermediate = alpha
                .add(b2.multiply(beta).subtract(triangleArea).divide(a2))
                .divide(FastMath.PI);
        return one.subtract(intermediate);
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
    private EventDetector createUmbraEclipseDetector() {
        return EventDetector.of(new UmbraEclipseFunction(), new ResetDerivativesOnEvent(), getEventDetectionSettings());
    }

    /**
     * Method to create a new penumbra detector.
     * @return detector
     */
    private EventDetector createPenumbraEclipseDetector() {
        return EventDetector.of(new PenumbraEclipseFunction(), new ResetDerivativesOnEvent(), getEventDetectionSettings());
    }

    private class PenumbraEclipseFunction implements EventFunction {

        @Override
        public double value(final SpacecraftState s) {
            final Vector3D position = s.getPosition();
            final Vector3D occultedBodyPosition = getOccultedBodyPosition(s.getDate());
            final Vector3D occultedBodyDirection = occultedBodyPosition.normalize();
            final double s0 = -position.dotProduct(occultedBodyDirection);
            final double distanceSun = occultedBodyPosition.getNorm();
            final double squaredDistance = position.getNorm2Sq();
            final double l = FastMath.sqrt(squaredDistance - s0 * s0);
            final double sinf1 = (occultedBodyRadius + getOccultingBodyRadius()) / distanceSun;
            final double l1 = (s0 * sinf1 + getOccultingBodyRadius()) / FastMath.sqrt(1.0 - sinf1 * sinf1);
            return l1 / l - 1.;
        }

        @Override
        public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
            final FieldVector3D<T> position = fieldState.getPosition();
            final FieldVector3D<T> occultedBodyPosition = getOccultedBodyPosition(fieldState.getDate());
            final FieldVector3D<T> occultedBodyDirection = occultedBodyPosition.normalize();
            final T s0 = position.dotProduct(occultedBodyDirection).negate();
            final T distanceSun = occultedBodyPosition.getNorm();
            final T squaredDistance = position.getNorm2Sq();
            final T reciprocalDistanceSun = distanceSun.reciprocal();
            final T sinf1 = reciprocalDistanceSun.multiply(occultedBodyRadius + getOccultingBodyRadius());
            final T l1 = (s0.multiply(sinf1).add(getOccultingBodyRadius())).divide(FastMath.sqrt(sinf1.square().negate().add(1)));
            final T l = FastMath.sqrt(squaredDistance.subtract(s0.square()));
            return l1.divide(l).subtract(1);
        }
    }

    private class UmbraEclipseFunction implements EventFunction {

        @Override
        public double value(final SpacecraftState state) {
            final Vector3D position = state.getPosition();
            final Vector3D occultedBodyPosition = getOccultedBodyPosition(state.getDate());
            final Vector3D occultedBodyDirection = occultedBodyPosition.normalize();
            final double s0 = -position.dotProduct(occultedBodyDirection);
            final double distanceSun = occultedBodyPosition.getNorm();
            final double squaredDistance = position.getNorm2Sq();
            final double sinf2 = (occultedBodyRadius - getOccultingBodyRadius()) / distanceSun;
            final double l = FastMath.sqrt(squaredDistance - s0 * s0);
            final double l2 = (s0 * sinf2 - getOccultingBodyRadius()) / FastMath.sqrt(1.0 - sinf2 * sinf2);
            return FastMath.abs(l2) / l - 1.;
        }

        @Override
        public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
            final FieldVector3D<T> position = fieldState.getPosition();
            final FieldVector3D<T> occultedBodyPosition = getOccultedBodyPosition(fieldState.getDate());
            final FieldVector3D<T> occultedBodyDirection = occultedBodyPosition.normalize();
            final T s0 = position.dotProduct(occultedBodyDirection).negate();
            final T distanceSun = occultedBodyPosition.getNorm();
            final T squaredDistance = position.getNorm2Sq();
            final T reciprocalDistanceSun = distanceSun.reciprocal();
            final T sinf2 = reciprocalDistanceSun.multiply(occultedBodyRadius - getOccultingBodyRadius());
            final T l2 = (s0.multiply(sinf2).subtract(getOccultingBodyRadius())).divide(FastMath.sqrt(sinf2.square().negate().add(1)));
            final T l = FastMath.sqrt(squaredDistance.subtract(s0.square()));
            return FastMath.abs(l2).divide(l).subtract(1);
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
    private <T extends CalculusFieldElement<T>> FieldEventDetector<T> createFieldUmbraEclipseDetector(final FieldEventDetectionSettings<T> detectionSettings) {
        return FieldEventDetector.of(new UmbraEclipseFunction(), new FieldResetDerivativesOnEvent<>(), detectionSettings);
    }

    /**
     * Method to create a new penumbra detector. Field version.
     * @param detectionSettings non-Field detection settings
     * @param <T> field type
     * @return detector
     */
    private <T extends CalculusFieldElement<T>> FieldEventDetector<T> createFieldPenumbraEclipseDetector(final FieldEventDetectionSettings<T> detectionSettings) {
        return FieldEventDetector.of(new PenumbraEclipseFunction(), new FieldResetDerivativesOnEvent<>(), detectionSettings);
    }

}
