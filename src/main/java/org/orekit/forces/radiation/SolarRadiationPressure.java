/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.forces.radiation;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;

/** Solar radiation pressure force model.
 *
 * @author Fabien Maussion
 * @author &Eacute;douard Delente
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 */
public class SolarRadiationPressure extends AbstractParameterizable implements ForceModel {

    /** Reference distance for the solar radiation pressure (m). */
    private static final double D_REF = 149597870000.0;

    /** Reference solar radiation pressure at D_REF (N/m²). */
    private static final double P_REF = 4.56e-6;

    /** Reference flux normalized for a 1m distance (N). */
    private final double kRef;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Earth model. */
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
     */
    public SolarRadiationPressure(final PVCoordinatesProvider sun, final double equatorialRadius,
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
     */
    public SolarRadiationPressure(final double dRef, final double pRef,
                                  final PVCoordinatesProvider sun,
                                  final double equatorialRadius,
                                  final RadiationSensitive spacecraft) {
        super(RadiationSensitive.ABSORPTION_COEFFICIENT, RadiationSensitive.REFLECTION_COEFFICIENT);
        this.kRef = pRef * dRef * dRef;
        this.sun  = sun;
        this.equatorialRadius = equatorialRadius;
        this.spacecraft = spacecraft;
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {

        final AbsoluteDate date         = s.getDate();
        final Frame        frame        = s.getFrame();
        final Vector3D     position     = s.getPVCoordinates().getPosition();
        final Vector3D     sunSatVector = position.subtract(sun.getPVCoordinates(date, frame).getPosition());
        final double       r2           = sunSatVector.getNormSq();

        // compute flux
        final double   rawP = kRef * getLightningRatio(position, frame, date) / r2;
        final Vector3D flux = new Vector3D(rawP / FastMath.sqrt(r2), sunSatVector);

        final Vector3D acceleration = spacecraft.radiationPressureAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                                                               s.getMass(), flux);

        // provide the perturbing acceleration to the derivatives adder
        adder.addAcceleration(acceleration, s.getFrame());

    }

    /** Get the lightning ratio ([0-1]).
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return lightning ratio
     * @exception OrekitException if the trajectory is inside the Earth
     */
    public double getLightningRatio(final Vector3D position, final Frame frame, final AbsoluteDate date)
        throws OrekitException {

        // Compute useful angles
        final double[] angle = getEclipseAngles(position, frame, date);

        // Sat-Sun / Sat-CentralBody angle
        final double sunEarthAngle = angle[0];

        // Central Body apparent radius
        final double alphaCentral = angle[1];

        // Sun apparent radius
        final double alphaSun = angle[2];

        double result = 1.0;

        // Is the satellite in complete umbra ?
        if (sunEarthAngle - alphaCentral + alphaSun <= 0.0) {
            result = 0.0;
        } else if (sunEarthAngle - alphaCentral - alphaSun < 0.0) {
            // Compute a lightning ratio in penumbra
            final double sEA2    = sunEarthAngle * sunEarthAngle;
            final double oo2sEA  = 1.0 / (2. * sunEarthAngle);
            final double aS2     = alphaSun * alphaSun;
            final double aE2     = alphaCentral * alphaCentral;
            final double aE2maS2 = aE2 - aS2;

            final double alpha1  = (sEA2 - aE2maS2) * oo2sEA;
            final double alpha2  = (sEA2 + aE2maS2) * oo2sEA;

            // Protection against numerical inaccuracy at boundaries
            final double a1oaS   = FastMath.min(1.0, FastMath.max(-1.0, alpha1 / alphaSun));
            final double aS2ma12 = FastMath.max(0.0, aS2 - alpha1 * alpha1);
            final double a2oaE   = FastMath.min(1.0, FastMath.max(-1.0, alpha2 / alphaCentral));
            final double aE2ma22 = FastMath.max(0.0, aE2 - alpha2 * alpha2);

            final double P1 = aS2 * FastMath.acos(a1oaS) - alpha1 * FastMath.sqrt(aS2ma12);
            final double P2 = aE2 * FastMath.acos(a2oaE) - alpha2 * FastMath.sqrt(aE2ma22);

            result = 1. - (P1 + P2) / (FastMath.PI * aS2);
        }

        return result;
    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[] {
            new UmbraDetector(), new PenumbraDetector()
        };
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                              final FieldVector3D<DerivativeStructure> position, final FieldVector3D<DerivativeStructure> velocity,
                                              final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass)
        throws OrekitException {

        final FieldVector3D<DerivativeStructure> sunSatVector = position.subtract(sun.getPVCoordinates(date, frame).getPosition());
        final DerivativeStructure r2  = sunSatVector.getNormSq();

        // compute flux
        final double ratio = getLightningRatio(position.toVector3D(), frame, date);
        final DerivativeStructure rawP = r2.reciprocal().multiply(kRef * ratio);
        final FieldVector3D<DerivativeStructure> flux = new FieldVector3D<DerivativeStructure>(rawP.divide(r2.sqrt()), sunSatVector);

        // compute acceleration with all its partial derivatives
        return spacecraft.radiationPressureAcceleration(date, frame, position, rotation, mass, flux);

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {

        complainIfNotSupported(paramName);
        final AbsoluteDate date         = s.getDate();
        final Frame        frame        = s.getFrame();
        final Vector3D     position     = s.getPVCoordinates().getPosition();
        final Vector3D     sunSatVector = position.subtract(sun.getPVCoordinates(date, frame).getPosition());
        final double       r2           = sunSatVector.getNormSq();

        // compute flux
        final double   rawP = kRef * getLightningRatio(position, frame, date) / r2;
        final Vector3D flux = new Vector3D(rawP / FastMath.sqrt(r2), sunSatVector);

        return spacecraft.radiationPressureAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                                        s.getMass(), flux, paramName);

    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        if (name.equals(RadiationSensitive.ABSORPTION_COEFFICIENT)) {
            return spacecraft.getAbsorptionCoefficient();
        }
        return spacecraft.getReflectionCoefficient();
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        if (name.equals(RadiationSensitive.ABSORPTION_COEFFICIENT)) {
            spacecraft.setAbsorptionCoefficient(value);
        } else {
            spacecraft.setReflectionCoefficient(value);
        }
    }

    /** Get the useful angles for eclipse computation.
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return the 3 angles {(satCentral, satSun), Central body apparent radius, Sun apparent radius}
     * @exception OrekitException if the trajectory is inside the Earth
     */
    private double[] getEclipseAngles(final Vector3D position,
                                      final Frame frame,
                                      final AbsoluteDate date)
        throws OrekitException {
        final double[] angle = new double[3];

        final Vector3D satSunVector = sun.getPVCoordinates(date, frame).getPosition().subtract(position);

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

    /** This class defines the umbra entry/exit detector. */
    private class UmbraDetector extends AbstractDetector<UmbraDetector> {

        /** Serializable UID. */
        private static final long serialVersionUID = 20141228L;

        /** Build a new instance. */
        UmbraDetector() {
            super(60.0, 1.0e-3, DEFAULT_MAX_ITER, new EventHandler<UmbraDetector>() {

                /** {@inheritDoc} */
                public Action eventOccurred(final SpacecraftState s, final UmbraDetector detector,
                                            final boolean increasing) {
                    return Action.RESET_DERIVATIVES;
                }

                /** {@inheritDoc} */
                @Override
                public SpacecraftState resetState(final UmbraDetector detector, final SpacecraftState oldState) {
                    return oldState;
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
                              final int maxIter, final EventHandler<UmbraDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected UmbraDetector create(final double newMaxCheck, final double newThreshold,
                                       final int newMaxIter, final EventHandler<UmbraDetector> newHandler) {
            return new UmbraDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
         * the Earth's apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         * @exception OrekitException if sun or spacecraft position cannot be computed
         */
        public double g(final SpacecraftState s) throws OrekitException {
            final double[] angle = getEclipseAngles(s.getPVCoordinates().getPosition(),
                                                    s.getFrame(), s.getDate());
            return angle[0] - angle[1] + angle[2];
        }

    }

    /** This class defines the penumbra entry/exit detector. */
    private class PenumbraDetector extends AbstractDetector<PenumbraDetector> {

        /** Serializable UID. */
        private static final long serialVersionUID = 20141228L;

        /** Build a new instance. */
        PenumbraDetector() {
            super(60.0, 1.0e-3, DEFAULT_MAX_ITER, new EventHandler<PenumbraDetector>() {

                /** {@inheritDoc} */
                public Action eventOccurred(final SpacecraftState s, final PenumbraDetector detector,
                                            final boolean increasing) {
                    return Action.RESET_DERIVATIVES;
                }

                /** {@inheritDoc} */
                @Override
                public SpacecraftState resetState(final PenumbraDetector detector, final SpacecraftState oldState) {
                    return oldState;
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
                                 final int maxIter, final EventHandler<PenumbraDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /** {@inheritDoc} */
        @Override
        protected PenumbraDetector create(final double newMaxCheck, final double newThreshold,
                                          final int newMaxIter, final EventHandler<PenumbraDetector> newHandler) {
            return new PenumbraDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
         * the sum of the Earth's and Sun's apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         * @exception OrekitException if sun or spacecraft position cannot be computed
         */
        public double g(final SpacecraftState s) throws OrekitException {
            final double[] angle = getEclipseAngles(s.getPVCoordinates().getPosition(),
                                                    s.getFrame(), s.getDate());
            return angle[0] - angle[1] - angle[2];
        }

    }

}
