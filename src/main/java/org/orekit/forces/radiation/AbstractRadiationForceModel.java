/* Copyright 2002-2020 CS GROUP
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

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
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

    /**
     * Constructor.
     * @param sun Sun model
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     */
    protected AbstractRadiationForceModel(final ExtendedPVCoordinatesProvider sun, final double equatorialRadius) {
        this.sun              = sun;
        this.equatorialRadius = equatorialRadius;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.of(new UmbraDetector(), new PenumbraDetector());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.of(new FieldUmbraDetector<>(field), new FieldPenumbraDetector<>(field));
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
     * @param sunPosition Sun position in the selected frame
     * @param position the satellite's position in the selected frame.
     * @param <T> extends RealFieldElement
     * @return the 3 angles {(satCentral, satSun), Central body apparent radius, Sun apparent radius}
     */
    protected <T extends RealFieldElement<T>> T[] getEclipseAngles(final FieldVector3D<T> sunPosition, final FieldVector3D<T> position) {
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
        private UmbraDetector(final double maxCheck, final double threshold,
                              final int maxIter, final EventHandler<? super UmbraDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected UmbraDetector create(final double newMaxCheck, final double newThreshold,
                                       final int newMaxIter, final EventHandler<? super UmbraDetector> newHandler) {
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
        private PenumbraDetector(final double maxCheck, final double threshold,
                                 final int maxIter, final EventHandler<? super PenumbraDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected PenumbraDetector create(final double newMaxCheck, final double newThreshold,
                                          final int newMaxIter, final EventHandler<? super PenumbraDetector> newHandler) {
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
    private class FieldUmbraDetector<T extends RealFieldElement<T>>
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
        private FieldUmbraDetector(final T maxCheck, final T threshold,
                                   final int maxIter,
                                   final FieldEventHandler<? super FieldUmbraDetector<T>, T> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected FieldUmbraDetector<T> create(final T newMaxCheck, final T newThreshold,
                                               final int newMaxIter,
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
    private class FieldPenumbraDetector<T extends RealFieldElement<T>>
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
        private FieldPenumbraDetector(final T maxCheck, final T threshold,
                                      final int maxIter,
                                      final FieldEventHandler<? super FieldPenumbraDetector<T>, T> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected FieldPenumbraDetector<T> create(final T newMaxCheck, final T newThreshold,
                                                  final int newMaxIter,
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

}
