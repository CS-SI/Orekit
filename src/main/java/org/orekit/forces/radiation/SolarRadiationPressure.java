/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

/** Solar radiation pressure force model.
 *
 * @author Fabien Maussion
 * @author &Eacute;douard Delente
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 */
public class SolarRadiationPressure extends AbstractForceModel {

    /** Reference distance for the solar radiation pressure (m). */
    private static final double D_REF = 149597870000.0;

    /** Reference solar radiation pressure at D_REF (N/m²). */
    private static final double P_REF = 4.56e-6;

    /** Margin to force recompute lighting ratio derivatives when we are really inside penumbra. */
    private static final double ANGULAR_MARGIN = 1.0e-10;

    /** Reference flux normalized for a 1m distance (N). */
    private final double kRef;

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Central body model. */
    private final double equatorialRadius;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** Simple constructor with default reference values.
     * <p>When this constructor is used, the reference values are:</p>
     * <ul>
     *   <li>d<sub>ref</sub> = 149597870000.0 m</li>
     *   <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m²</li>
     * </ul>
     * @param sun Sun model
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     * @param spacecraft the object physical and geometrical information
     * @since 9.2
     */
    public SolarRadiationPressure(final ExtendedPVCoordinatesProvider sun, final double equatorialRadius,
                                  final RadiationSensitive spacecraft) {
        this(D_REF, P_REF, sun, equatorialRadius, spacecraft);
    }

    /** Complete constructor.
     * <p>Note that reference solar radiation pressure <code>pRef</code> in
     * N/m² is linked to solar flux SF in W/m² using
     * formula pRef = SF/c where c is the speed of light (299792458 m/s). So
     * at 1UA a 1367 W/m² solar flux is a 4.56 10<sup>-6</sup>
     * N/m² solar radiation pressure.</p>
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m²)
     * @param sun Sun model
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     * @param spacecraft the object physical and geometrical information
     * @since 9.2
     */
    public SolarRadiationPressure(final double dRef, final double pRef,
                                  final ExtendedPVCoordinatesProvider sun,
                                  final double equatorialRadius,
                                  final RadiationSensitive spacecraft) {
        this.kRef = pRef * dRef * dRef;
        this.sun  = sun;
        this.equatorialRadius = equatorialRadius;
        this.spacecraft = spacecraft;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        final AbsoluteDate date         = s.getDate();
        final Frame        frame        = s.getFrame();
        final Vector3D     position     = s.getPVCoordinates().getPosition();
        final Vector3D     sunSatVector = position.subtract(sun.getPVCoordinates(date, frame).getPosition());
        final double       r2           = sunSatVector.getNormSq();

        // compute flux
        final double   ratio = getLightingRatio(position, frame, date);
        final double   rawP  = ratio  * kRef / r2;
        final Vector3D flux  = new Vector3D(rawP / FastMath.sqrt(r2), sunSatVector);

        return spacecraft.radiationPressureAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                                        s.getMass(), flux, parameters);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        final FieldAbsoluteDate<T> date         = s.getDate();
        final Frame                frame        = s.getFrame();
        final FieldVector3D<T>     position     = s.getPVCoordinates().getPosition();
        final FieldVector3D<T>     sunSatVector = position.subtract(sun.getPVCoordinates(date.toAbsoluteDate(), frame).getPosition());
        final T                    r2           = sunSatVector.getNormSq();

        // compute flux
        final T                ratio = getLightingRatio(position, frame, date);
        final T                rawP  = ratio.divide(r2).multiply(kRef);
        final FieldVector3D<T> flux  = new FieldVector3D<>(rawP.divide(r2.sqrt()), sunSatVector);

        return spacecraft.radiationPressureAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                                        s.getMass(), flux, parameters);

    }

    /** Get the lighting ratio ([0-1]).
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return lighting ratio
          * @since 7.1
     */
    public double getLightingRatio(final Vector3D position, final Frame frame, final AbsoluteDate date) {

        final Vector3D sunPosition = sun.getPVCoordinates(date, frame).getPosition();
        if (sunPosition.getNorm() < 2 * Constants.SUN_RADIUS) {
            // we are in fact computing a trajectory around Sun (or solar system barycenter),
            // not around a planet,we consider lighting ratio is always 1
            return 1.0;
        }

        // Compute useful angles
        final double[] angle = getEclipseAngles(sunPosition, position);

        // Sat-Sun / Sat-CentralBody angle
        final double sunSatCentralBodyAngle = angle[0];

        // Central Body apparent radius
        final double alphaCentral = angle[1];

        // Sun apparent radius
        final double alphaSun = angle[2];

        double result = 1.0;

        // Is the satellite in complete umbra ?
        if (sunSatCentralBodyAngle - alphaCentral + alphaSun <= ANGULAR_MARGIN) {
            result = 0.0;
        } else if (sunSatCentralBodyAngle - alphaCentral - alphaSun < -ANGULAR_MARGIN) {
            // Compute a lighting ratio in penumbra
            final double sEA2    = sunSatCentralBodyAngle * sunSatCentralBodyAngle;
            final double oo2sEA  = 1.0 / (2. * sunSatCentralBodyAngle);
            final double aS2     = alphaSun * alphaSun;
            final double aE2     = alphaCentral * alphaCentral;
            final double aE2maS2 = aE2 - aS2;

            final double alpha1  = (sEA2 - aE2maS2) * oo2sEA;
            final double alpha2  = (sEA2 + aE2maS2) * oo2sEA;

            // Protection against numerical inaccuracy at boundaries
            final double almost0 = Precision.SAFE_MIN;
            final double almost1 = FastMath.nextDown(1.0);
            final double a1oaS   = FastMath.min(almost1, FastMath.max(-almost1, alpha1 / alphaSun));
            final double aS2ma12 = FastMath.max(almost0, aS2 - alpha1 * alpha1);
            final double a2oaE   = FastMath.min(almost1, FastMath.max(-almost1, alpha2 / alphaCentral));
            final double aE2ma22 = FastMath.max(almost0, aE2 - alpha2 * alpha2);

            final double P1 = aS2 * FastMath.acos(a1oaS) - alpha1 * FastMath.sqrt(aS2ma12);
            final double P2 = aE2 * FastMath.acos(a2oaE) - alpha2 * FastMath.sqrt(aE2ma22);

            result = 1. - (P1 + P2) / (FastMath.PI * aS2);
        }

        return result;

    }

    /** Get the lighting ratio ([0-1]).
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @param <T> extends RealFieldElement
     * @return lighting ratio
          * @since 7.1
     */
    public <T extends RealFieldElement<T>> T getLightingRatio(final FieldVector3D<T> position,
                                                              final Frame frame,
                                                              final FieldAbsoluteDate<T> date) {

        final T one = date.getField().getOne();

        final FieldVector3D<T> sunPosition = sun.getPVCoordinates(date, frame).getPosition();
        if (sunPosition.getNorm().getReal() < 2 * Constants.SUN_RADIUS) {
            // we are in fact computing a trajectory around Sun (or solar system barycenter),
            // not around a planet,we consider lighting ratio is always 1
            return one;
        }

        // Compute useful angles
        final T[] angle = getEclipseAngles(sunPosition, position);

        // Sat-Sun / Sat-CentralBody angle
        final T sunsatCentralBodyAngle = angle[0];

        // Central Body apparent radius
        final T alphaCentral = angle[1];

        // Sun apparent radius
        final T alphaSun = angle[2];

        T result = one;
        // Is the satellite in complete umbra ?
        if (sunsatCentralBodyAngle.getReal() - alphaCentral.getReal() + alphaSun.getReal() <= ANGULAR_MARGIN) {
            result = date.getField().getZero();
        } else if (sunsatCentralBodyAngle.getReal() - alphaCentral.getReal() - alphaSun.getReal() < -ANGULAR_MARGIN) {
            // Compute a lighting ratio in penumbra
            final T sEA2    = sunsatCentralBodyAngle.multiply(sunsatCentralBodyAngle);
            final T oo2sEA  = sunsatCentralBodyAngle.multiply(2).reciprocal();
            final T aS2     = alphaSun.multiply(alphaSun);
            final T aE2     = alphaCentral.multiply(alphaCentral);
            final T aE2maS2 = aE2.subtract(aS2);

            final T alpha1  = sEA2.subtract(aE2maS2).multiply(oo2sEA);
            final T alpha2  = sEA2.add(aE2maS2).multiply(oo2sEA);

            // Protection against numerical inaccuracy at boundaries
            final double almost0 = Precision.SAFE_MIN;
            final double almost1 = FastMath.nextDown(1.0);
            final T a1oaS   = min(almost1, max(-almost1, alpha1.divide(alphaSun)));
            final T aS2ma12 = max(almost0, aS2.subtract(alpha1.multiply(alpha1)));
            final T a2oaE   = min(almost1, max(-almost1, alpha2.divide(alphaCentral)));
            final T aE2ma22 = max(almost0, aE2.subtract(alpha2.multiply(alpha2)));

            final T P1 = aS2.multiply(a1oaS.acos()).subtract(alpha1.multiply(aS2ma12.sqrt()));
            final T P2 = aE2.multiply(a2oaE.acos()).subtract(alpha2.multiply(aE2ma22.sqrt()));

            result = one.subtract(P1.add(P2).divide(aS2.multiply(FastMath.PI)));
        }

        return result;
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

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return spacecraft.getRadiationParametersDrivers();
    }

    /** Get the useful angles for eclipse computation.
     * @param sunPosition Sun position in the selected frame
     * @param position the satellite's position in the selected frame
     * @return the 3 angles {(satCentral, satSun), Central body apparent radius, Sun apparent radius}
     */
    private double[] getEclipseAngles(final Vector3D sunPosition, final Vector3D position) {
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

    /** Get the useful angles for eclipse computation.
     * @param sunPosition Sun position in the selected frame
     * @param position the satellite's position in the selected frame.
     * @param <T> extends RealFieldElement
     * @return the 3 angles {(satCentral, satSun), Central body apparent radius, Sun apparent radius}
     */
    private <T extends RealFieldElement<T>> T[] getEclipseAngles(final FieldVector3D<T> sunPosition, final FieldVector3D<T> position) {
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

    /** Compute min of two values, one double and one field element.
     * @param d double value
     * @param f field element
     * @param <T> type fo the field elements
     * @return min value
     */
    private <T extends RealFieldElement<T>> T min(final double d, final T f) {
        return (f.getReal() > d) ? f.getField().getZero().add(d) : f;
    }

    /** Compute max of two values, one double and one field element.
     * @param d double value
     * @param f field element
     * @param <T> type fo the field elements
     * @return max value
     */
    private <T extends RealFieldElement<T>> T max(final double d, final T f) {
        return (f.getReal() <= d) ? f.getField().getZero().add(d) : f;
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

    /** This class defines the umbra entry/exit detector.
     * @since 9.2
     */
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

    /** This class defines the penumbra entry/exit detector.
     * @since 9.2
     */
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
