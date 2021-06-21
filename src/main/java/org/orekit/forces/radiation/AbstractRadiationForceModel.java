/* Copyright 2002-2021 CS GROUP
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;

/**
 * Base class for radiation force models.
 * @see SolarRadiationPressure
 * @see ECOM2
 * @since 10.2
 */
public abstract class AbstractRadiationForceModel extends AbstractForceModel {

    /** Margin to force recompute lighting ratio derivatives when we are really inside penumbra. */
    private static final double ANGULAR_MARGIN = 1.0e-10;

    /** Central body model. */
    private final double equatorialRadius;

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Other occulting bodies to consider. The Moon for instance. */
    private final Map<ExtendedPVCoordinatesProvider, Double> otherOccultingBodies;

    /**
     * Default constructor.
     * Only central body is considered.
     * @param sun Sun model
     * @param equatorialRadius central body spherical shape model (for umbra/penumbra computation)
     */
    protected AbstractRadiationForceModel(final ExtendedPVCoordinatesProvider sun, final double equatorialRadius) {
        this.sun                  = sun;
        this.equatorialRadius     = equatorialRadius;
        this.otherOccultingBodies = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        final EventDetector[] detectors = new EventDetector[2 + 2 * otherOccultingBodies.size()];
        detectors[0] = new UmbraDetector();
        detectors[1] = new PenumbraDetector();
        int i = 2;
        for (Map.Entry<ExtendedPVCoordinatesProvider, Double> entry : otherOccultingBodies.entrySet()) {
            detectors[i]     = new GeneralUmbraDetector(entry.getKey(),    entry.getValue());
            detectors[i + 1] = new GeneralPenumbraDetector(entry.getKey(), entry.getValue());
            i = i + 2;
        }
        return Stream.of(detectors);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        final T zero = field.getZero();
        @SuppressWarnings("unchecked")
        final FieldEventDetector<T>[] detectors = (FieldEventDetector<T>[]) Array.newInstance(FieldEventDetector.class,
                                                                                              2 + 2 * otherOccultingBodies.size());
        detectors[0] = new FieldUmbraDetector<>(field);
        detectors[1] = new FieldPenumbraDetector<>(field);
        int i = 2;
        for (Map.Entry<ExtendedPVCoordinatesProvider, Double> entry : otherOccultingBodies.entrySet()) {
            detectors[i]     = new FieldGeneralUmbraDetector<>(field, entry.getKey(),    zero.newInstance(entry.getValue()));
            detectors[i + 1] = new FieldGeneralPenumbraDetector<>(field, entry.getKey(), zero.newInstance(entry.getValue()));
            i = i + 2;
        }
        return Stream.of(detectors);
    }

    /**
     * Get the useful angles for eclipse computation.
     * @param sunPosition Sun position in the selected frame
     * @param position the satellite's position in the selected frame
     * @return the 3 angles {(satCentral, satSun), Central body apparent radius, Sun apparent radius}
     */
    protected double[] getEclipseAngles(final Vector3D sunPosition, final Vector3D position) {
        final double[] angle = new double[3];

        final Vector3D satSunVector = sunPosition.subtract(position);

        // Sat-Sun / Sat-CentralBody angle
        angle[0] = Vector3D.angle(satSunVector, position.negate());

        // Central body apparent radius
        final double r = position.getNorm();
        if (r <= equatorialRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, r);
        }
        angle[1] = FastMath.asin(equatorialRadius / r);

        // Sun apparent radius
        angle[2] = FastMath.asin(Constants.SUN_RADIUS / satSunVector.getNorm());

        return angle;
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
     * @param sunPosition Sun position in the selected frame
     * @param position the satellite's position in the selected frame.
     * @param <T> extends CalculusFieldElement
     * @return the 3 angles {(satCentral, satSun), Central body apparent radius, Sun apparent radius}
     */
    protected <T extends CalculusFieldElement<T>> T[] getEclipseAngles(final FieldVector3D<T> sunPosition, final FieldVector3D<T> position) {
        final T[] angle = MathArrays.buildArray(position.getX().getField(), 3);

        final FieldVector3D<T> mP           = position.negate();
        final FieldVector3D<T> satSunVector = mP.add(sunPosition);

        // Sat-Sun / Sat-CentralBody angle
        angle[0] = FieldVector3D.angle(satSunVector, mP);

        // Central body apparent radius
        final T r = position.getNorm();
        if (r.getReal() <= equatorialRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, r);
        }
        angle[1] = r.reciprocal().multiply(equatorialRadius).asin();

        // Sun apparent radius
        angle[2] = satSunVector.getNorm().reciprocal().multiply(Constants.SUN_RADIUS).asin();

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
     * Central body is already considered, it shall not be added this way.
     * @param provider body PV provider
     * @param radius body mean radius
     */
    public void addOccultingBody(final ExtendedPVCoordinatesProvider provider, final double radius) {
        otherOccultingBodies.put(provider, radius);
    }

    /**
     * Getter for other occulting bodies to consider.
     * @return the map of other occulting bodies and corresponding mean radiuses
     */
    public Map<ExtendedPVCoordinatesProvider, Double> getOtherOccultingBodies() {
        return otherOccultingBodies;
    }

    /**
     * Getter for equatorial radius.
     * @return central body equatorial radius
     */
    public double getEquatorialRadius() {
        return equatorialRadius;
    }


    /** This class defines the umbra entry/exit detector. */
    private class UmbraDetector extends AbstractDetector<UmbraDetector> {

        /** Build a new instance. */
        UmbraDetector() {
            super(60.0, 1.0e-3, DEFAULT_MAX_ITER, new EventHandler<UmbraDetector>() {

                /** {@inheritDoc} */
                public Action eventOccurred(final SpacecraftState s, final UmbraDetector detector,
                                            final boolean increasing) {
                    return Action.RESET_DERIVATIVES;
                }

            });
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         * @since 6.1
         */
        private UmbraDetector(final double maxCheck, final double threshold, final int maxIter,
                              final EventHandler<? super UmbraDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected UmbraDetector create(final double newMaxCheck, final double newThreshold, final int newMaxIter,
                                       final EventHandler<? super UmbraDetector> newHandler) {
            return new UmbraDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sun-Sat-Central-Body angle and
         * the central body apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public double g(final SpacecraftState s) {
            final double[] angle = getEclipseAngles(sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                    s.getPVCoordinates().getPosition());
            return angle[0] - angle[1] + angle[2] - ANGULAR_MARGIN;
        }

    }

    /** This class defines the penumbra entry/exit detector. */
    private class PenumbraDetector extends AbstractDetector<PenumbraDetector> {

        /** Build a new instance. */
        PenumbraDetector() {
            super(60.0, 1.0e-3, DEFAULT_MAX_ITER, new EventHandler<PenumbraDetector>() {

                /** {@inheritDoc} */
                public Action eventOccurred(final SpacecraftState s, final PenumbraDetector detector,
                                            final boolean increasing) {
                    return Action.RESET_DERIVATIVES;
                }

            });
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         * @since 6.1
         */
        private PenumbraDetector(final double maxCheck, final double threshold, final int maxIter,
                                 final EventHandler<? super PenumbraDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected PenumbraDetector create(final double newMaxCheck, final double newThreshold, final int newMaxIter,
                                          final EventHandler<? super PenumbraDetector> newHandler) {
            return new PenumbraDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sun-Sat-Central-Body angle and
         * the sum of the central body and Sun's apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public double g(final SpacecraftState s) {
            final double[] angle = getEclipseAngles(sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                    s.getPVCoordinates().getPosition());
            return angle[0] - angle[1] - angle[2] + ANGULAR_MARGIN;
        }

    }

    /** This class defines the umbra entry/exit detector. */
    private class FieldUmbraDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldUmbraDetector<T>, T> {

        /** Build a new instance.
         * @param field field to which elements belong
         */
        FieldUmbraDetector(final Field<T> field) {
            super(field.getZero().add(60.0), field.getZero().add(1.0e-3),
                  DEFAULT_MAX_ITER, new FieldEventHandler<FieldUmbraDetector<T>, T>() {

                      /** {@inheritDoc} */
                      public Action eventOccurred(final FieldSpacecraftState<T> s,
                                                  final FieldUmbraDetector<T> detector,
                                                  final boolean increasing) {
                          return Action.RESET_DERIVATIVES;
                      }

                  });
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         */
        private FieldUmbraDetector(final T maxCheck, final T threshold, final int maxIter,
                                   final FieldEventHandler<? super FieldUmbraDetector<T>, T> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected FieldUmbraDetector<T> create(final T newMaxCheck, final T newThreshold, final int newMaxIter,
                                               final FieldEventHandler<? super FieldUmbraDetector<T>, T> newHandler) {
            return new FieldUmbraDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sun-Sat-Central-Body angle and
         * the central body apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public T g(final FieldSpacecraftState<T> s) {
            final T[] angle = getEclipseAngles(sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                               s.getPVCoordinates().getPosition());
            return angle[0].subtract(angle[1]).add(angle[2]).subtract(ANGULAR_MARGIN);
        }

    }

    /** This class defines the penumbra entry/exit detector. */
    private class FieldPenumbraDetector<T extends CalculusFieldElement<T>>
          extends FieldAbstractDetector<FieldPenumbraDetector<T>, T> {

        /** Build a new instance.
         * @param field field to which elements belong
         */
        FieldPenumbraDetector(final Field<T> field) {
            super(field.getZero().add(60.0), field.getZero().add(1.0e-3),
                  DEFAULT_MAX_ITER, new FieldEventHandler<FieldPenumbraDetector<T>, T>() {

                      /** {@inheritDoc} */
                      public Action eventOccurred(final FieldSpacecraftState<T> s,
                                                  final FieldPenumbraDetector<T> detector,
                                                  final boolean increasing) {
                          return Action.RESET_DERIVATIVES;
                      }

                  });
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         */
        private FieldPenumbraDetector(final T maxCheck, final T threshold, final int maxIter,
                                      final FieldEventHandler<? super FieldPenumbraDetector<T>, T> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected FieldPenumbraDetector<T> create(final T newMaxCheck, final T newThreshold, final int newMaxIter,
                                                  final FieldEventHandler<? super FieldPenumbraDetector<T>, T> newHandler) {
            return new FieldPenumbraDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sun-Sat-Central-Body angle and
         * the sum of the central body and Sun's apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public T g(final FieldSpacecraftState<T> s) {
            final T[] angle = getEclipseAngles(sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                               s.getPVCoordinates().getPosition());
            return angle[0].subtract(angle[1]).subtract(angle[2]).add(ANGULAR_MARGIN);
        }

    }

    /** This class defines the umbra entry/exit detector. */
    private class GeneralUmbraDetector extends AbstractDetector<GeneralUmbraDetector> {

        /** Occulting body PV provider. */
        private ExtendedPVCoordinatesProvider provider;

        /** Occulting body mean radius. */
        private double radius;

        /** Build a new instance.
         * @param provider occulting body PV provider
         * @param radius occulting body mean radius
         */
        GeneralUmbraDetector(final ExtendedPVCoordinatesProvider provider, final double radius) {
            super(60.0, 1.0e-3, DEFAULT_MAX_ITER, new EventHandler<GeneralUmbraDetector>() {

                /** {@inheritDoc} */
                public Action eventOccurred(final SpacecraftState s, final GeneralUmbraDetector detector,
                                            final boolean increasing) {
                    return Action.RESET_DERIVATIVES;
                }

            });
            this.provider = provider;
            this.radius   = radius;
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         * @since 6.1
         */
        private GeneralUmbraDetector(final double maxCheck, final double threshold, final int maxIter,
                                     final EventHandler<? super GeneralUmbraDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected GeneralUmbraDetector create(final double newMaxCheck, final double newThreshold, final int newMaxIter,
                                              final EventHandler<? super GeneralUmbraDetector> newHandler) {
            return new GeneralUmbraDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sun-Sat-Central-Body angle and
         * the central body apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public double g(final SpacecraftState s) {
            final double[] angle = getGeneralEclipseAngles(s.getPVCoordinates().getPosition(),
                                                           provider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                           radius, sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                           Constants.SUN_RADIUS);
            return angle[0] - angle[1] + angle[2] - ANGULAR_MARGIN;
        }

    }

    /** This class defines the umbra entry/exit detector. */
    private class GeneralPenumbraDetector extends AbstractDetector<GeneralPenumbraDetector> {

        /** Occulting body PV provider. */
        private ExtendedPVCoordinatesProvider provider;

        /** Occulting body mean radius. */
        private double radius;

        /** Build a new instance.
         * @param provider occulting body PV provider
         * @param radius occulting body mean radius
         */
        GeneralPenumbraDetector(final ExtendedPVCoordinatesProvider provider, final double radius) {
            super(60.0, 1.0e-3, DEFAULT_MAX_ITER, new EventHandler<GeneralPenumbraDetector>() {

                /** {@inheritDoc} */
                public Action eventOccurred(final SpacecraftState s, final GeneralPenumbraDetector detector,
                                            final boolean increasing) {
                    return Action.RESET_DERIVATIVES;
                }

            });
            this.provider = provider;
            this.radius   = radius;
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         * @since 6.1
         */
        private GeneralPenumbraDetector(final double maxCheck, final double threshold, final int maxIter,
                                        final EventHandler<? super GeneralPenumbraDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected GeneralPenumbraDetector create(final double newMaxCheck, final double newThreshold, final int newMaxIter,
                                                 final EventHandler<? super GeneralPenumbraDetector> newHandler) {
            return new GeneralPenumbraDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sun-Sat-Central-Body angle and
         * the central body apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public double g(final SpacecraftState s) {
            final double[] angle = getGeneralEclipseAngles(s.getPVCoordinates().getPosition(),
                                                           provider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                           radius, sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                           Constants.SUN_RADIUS);
            return angle[0] - angle[1] - angle[2] + ANGULAR_MARGIN;
        }

    }

    /** This class defines the umbra entry/exit detector. */
    private class FieldGeneralUmbraDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldGeneralUmbraDetector<T>, T> {

        /** Occulting body PV provider. */
        private ExtendedPVCoordinatesProvider provider;

        /** Occulting body mean radius. */
        private T radius;

        /** Build a new instance.
         * @param field field to which elements belong
         * @param provider occulting body PV provider
         * @param radius occulting body mean radius
         */
        FieldGeneralUmbraDetector(final Field<T> field, final ExtendedPVCoordinatesProvider provider, final T radius) {
            super(field.getZero().add(60.0), field.getZero().add(1.0e-3),
                  DEFAULT_MAX_ITER, new FieldEventHandler<FieldGeneralUmbraDetector<T>, T>() {

                      /** {@inheritDoc} */
                      public Action eventOccurred(final FieldSpacecraftState<T> s,
                                                  final FieldGeneralUmbraDetector<T> detector,
                                                  final boolean increasing) {
                          return Action.RESET_DERIVATIVES;
                      }

                  });
            this.provider = provider;
            this.radius   = radius;
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         */
        private FieldGeneralUmbraDetector(final T maxCheck, final T threshold,
                                   final int maxIter,
                                   final FieldEventHandler<? super FieldGeneralUmbraDetector<T>, T> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected FieldGeneralUmbraDetector<T> create(final T newMaxCheck, final T newThreshold, final int newMaxIter,
                                                      final FieldEventHandler<? super FieldGeneralUmbraDetector<T>, T> newHandler) {
            return new FieldGeneralUmbraDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sun-Sat-Central-Body angle and
         * the central body apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public T g(final FieldSpacecraftState<T> s) {
            final T[] angle = getGeneralEclipseAngles(s.getPVCoordinates().getPosition(),
                                                      provider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                      radius, sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                      s.getA().getField().getZero().add(Constants.SUN_RADIUS));
            return angle[0].subtract(angle[1]).add(angle[2]).subtract(ANGULAR_MARGIN);
        }

    }

    /** This class defines the umbra entry/exit detector. */
    private class FieldGeneralPenumbraDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldGeneralPenumbraDetector<T>, T> {

        /** Occulting body PV provider. */
        private ExtendedPVCoordinatesProvider provider;

        /** Occulting body mean radius. */
        private T radius;

        /** Build a new instance.
         * @param field field to which elements belong
         * @param provider occulting body PV provider
         * @param radius occulting body mean radius
         */
        FieldGeneralPenumbraDetector(final Field<T> field, final ExtendedPVCoordinatesProvider provider, final T radius) {
            super(field.getZero().add(60.0), field.getZero().add(1.0e-3),
                  DEFAULT_MAX_ITER, new FieldEventHandler<FieldGeneralPenumbraDetector<T>, T>() {

                      /** {@inheritDoc} */
                      public Action eventOccurred(final FieldSpacecraftState<T> s,
                                                  final FieldGeneralPenumbraDetector<T> detector,
                                                  final boolean increasing) {
                          return Action.RESET_DERIVATIVES;
                      }

                  });
            this.provider = provider;
            this.radius   = radius;
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         */
        private FieldGeneralPenumbraDetector(final T maxCheck, final T threshold, final int maxIter,
                                             final FieldEventHandler<? super FieldGeneralPenumbraDetector<T>, T> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected FieldGeneralPenumbraDetector<T> create(final T newMaxCheck, final T newThreshold, final int newMaxIter,
                                                         final FieldEventHandler<? super FieldGeneralPenumbraDetector<T>, T> newHandler) {
            return new FieldGeneralPenumbraDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sun-Sat-Central-Body angle and
         * the central body apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public T g(final FieldSpacecraftState<T> s) {
            final T[] angle = getGeneralEclipseAngles(s.getPVCoordinates().getPosition(),
                                                      provider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                      radius, sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                                      s.getA().getField().getZero().add(Constants.SUN_RADIUS));
            return angle[0].subtract(angle[1]).subtract(angle[2]).add(ANGULAR_MARGIN);
        }

    }
}
