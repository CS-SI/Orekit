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
import org.orekit.utils.ParameterDriver;

/**
 * The Empirical CODE Orbit Model 2 (ECOM2) of the Center for Orbit Determination in Europe (CODE).
 * <p>
 * The drag acceleration is computed as follows :
 * γ = γ<sub>0</sub> + D(u)e<sub>D</sub> + Y(u)e<sub>Y</sub> + B(u)e<sub>B</sub>
 * </p> <p>
 * In the above equation, γ<sub>0</sub> is a selectable a priori model. Since 2013, no
 * a priori model is used for CODE IGS contribution (i.e. γ<sub>0</sub> = 0). Moreover,
 * u denotes the satellite's argument of latitude.
 * </p> <p>
 * D(u), Y(u) and B(u) are three functions of the ECOM2 model that can be represented
 * as Fourier series. The coefficients of the Fourier series are estimated during the
 * estimation process. he ECOM2 model has user-defines upper limits <i>nD</i> and
 * <i>nB</i> for the Fourier series (i.e. <i>nD</i> for D(u) and <i>nB</i> for
 * B(u). Y(u) is defined as a constant value).
 * </p> <p>
 * It exists several configurations to initialize <i>nD</i> and <i>nB</i> values. However,
 * Arnold et al recommend to use <b>D2B1</b> (i.e. <i>nD</i> = 1 and <i>nB</i> = 1) and
 * <b>D4B1</b> (i.e. <i>nD</i> = 2 an <i>nB</i> = 1) configurations. At the opposite, in Arnold paper, it
 * is recommend to not use <b>D2B0</b> (i.e. <i>nD</i> = 1 and <i>nB</i> = 0) configuration.
 * </p>
 * @see "Arnold, Daniel, et al. "CODE’s new solar radiation pressure model for GNSS orbit determination."
 *      Journal of geodesy 89.8 (2015): 775-791."
 * @see "Tzu-Pang tseng and Michael Moore, "Impact of solar radiation pressure mis-modeling on
 *      GNSS satellite orbit determination" IGS Worshop, Wuhan, China, 2018."
 * @author David Soulard
 * @since 10.2
 */
public class ECOM2 extends AbstractForceModel {

    /** Parameter name for ECOM model coefficients enabling Jacobian processing. */
    public static final String ECOM_COEFFICIENT = "ECOM coefficient";

    /** Margin to force recompute lighting ratio derivatives when we are really inside penumbra. */
    private static final double ANGULAR_MARGIN = 1.0e-10;

    /** Minimum value for ECOM2 estimated parameters. */
    private static final double MIN_VALUE = Double.NEGATIVE_INFINITY;

    /** Maximum value for ECOM2 estimated parameters. */
    private static final double MAX_VALUE = Double.POSITIVE_INFINITY;

    /** Central body model. */
    private final double equatorialRadius;

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, -22);

    /** Highest order for parameter along eD axis (satellite --> sun direction). */
    private final int nD;

    /** Highest order for parameter along eB axis. */
    private final int nB;

    /** Estimated acceleration coefficients.
     * <p>
     * The 2 * nD first driver are Fourier driver along eD, axis,
     * then along eY, then 2*nB following are along eB axis.
     * </p>
     */
    private final ParameterDriver[] coefs;

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /**
     * Constructor.
     * @param nD truncation rank of Fourier series in D term.
     * @param nB truncation rank of Fourier series in B term.
     * @param value parameters initial value
     * @param sun provide for Sun parameter
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     */
    public ECOM2(final int nD, final int nB, final double value,
                 final ExtendedPVCoordinatesProvider sun, final double equatorialRadius) {
        this.nB = nB;
        this.nD = nD;
        this.coefs = new ParameterDriver[2 * (nD + nB) + 3];
        ParameterDriver driver;
        // Add parameter along eB axis in alphabetical order
        driver = new ParameterDriver(ECOM_COEFFICIENT + " B0", value, SCALE, MIN_VALUE, MAX_VALUE);
        driver.setSelected(true);
        coefs[0] = driver;
        for (int i = 1; i < nB + 1; i++) {
            driver = new ParameterDriver(ECOM_COEFFICIENT + " Bcos" + Integer.toString(i - 1), value, SCALE, MIN_VALUE, MAX_VALUE);
            driver.setSelected(true);
            coefs[i]  = driver;
        }
        for (int i = nB + 1; i < 2 * nB + 1; i++) {
            driver = new ParameterDriver(ECOM_COEFFICIENT + " Bsin" + Integer.toString(i - (nB + 1)), value, SCALE, MIN_VALUE, MAX_VALUE);
            driver.setSelected(true);
            coefs[i] = driver;
        }
        // Add driver along eD axis in alphabetical order
        driver = new ParameterDriver(ECOM_COEFFICIENT + " D0", value, SCALE, MIN_VALUE, MAX_VALUE);
        driver.setSelected(true);
        coefs[2 * nB + 1 ] = driver;
        for (int i = 2 * nB + 2; i < 2 * nB + 2 + nD; i++) {
            driver = new ParameterDriver(ECOM_COEFFICIENT + " Dcos" + Integer.toString(i - (2 * nB + 2)), value, SCALE, MIN_VALUE, MAX_VALUE);
            driver.setSelected(true);
            coefs[i] = driver;
        }
        for (int i = 2 * nB + 2 + nD; i < 2 * (nB + nD) + 2; i++) {
            driver = new ParameterDriver(ECOM_COEFFICIENT + " Dsin" + Integer.toString(i - (2 * nB + nD + 2)), value, SCALE, MIN_VALUE, MAX_VALUE);
            driver.setSelected(true);
            coefs[i] = driver;
        }
        // Add  Y0
        driver = new ParameterDriver(ECOM_COEFFICIENT + " Y0", value, SCALE, MIN_VALUE, MAX_VALUE);
        driver.setSelected(true);
        coefs[2 * (nB + nD) + 2] = driver;
        this.sun = sun;
        this.equatorialRadius = equatorialRadius;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {
        // Build the coordinate system
        final Vector3D Z = s.getPVCoordinates().getMomentum().normalize();
        final Vector3D sunPos = sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition().normalize();
        final Vector3D Y = Z.crossProduct(sunPos);
        final Vector3D X = Y.crossProduct(Z);
        // Build eD, eY, eB vectors
        final Vector3D position = s.getPVCoordinates().getPosition().normalize();
        final Vector3D eD = sunPos.add(-1.0, position);
        final Vector3D eY = position.crossProduct(eD);
        final Vector3D eB = eD.crossProduct(eY);

        // Angular argument difference u_s - u
        final double  delta_u =  FastMath.atan2(position.dotProduct(Y), position.dotProduct(X));

        // Compute B(u)
        double b_u = parameters[0];
        for (int i = 1; i < nB + 1; i++) {
            b_u += parameters[i] * FastMath.cos(2 * i * delta_u) + parameters[i + nB] * FastMath.sin(2 * i * delta_u);
        }
        // Compute D(u)
        double d_u = parameters[2 * nB + 1];
        for (int i = 1; i < nD + 1; i++) {
            d_u += parameters[2 * nB + 1 + i] * FastMath.cos((2 * i - 1) * delta_u) + parameters[2 * nB + 1 + i + nD] * FastMath.sin((2 * i - 1) * delta_u);
        }
        // Return acceleration
        return new Vector3D(d_u, eD, parameters[2 * (nD + nB) + 2], eY, b_u, eB);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s, final T[] parameters) {
        // Build the coordinate system
        final FieldVector3D<T> Z = s.getPVCoordinates().getMomentum().normalize();
        final FieldVector3D<T> sunPos = sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition().normalize();
        final FieldVector3D<T> Y = Z.crossProduct(sunPos);
        final FieldVector3D<T> X = Y.crossProduct(Z);

        // Build eD, eY, eB vectors
        final FieldVector3D<T> position = s.getPVCoordinates().getPosition().normalize();
        final FieldVector3D<T> eD = sunPos.add(-1.0, position);
        final FieldVector3D<T> eY = position.crossProduct(eD);
        final FieldVector3D<T> eB = eD.crossProduct(eY);

        // Angular argument difference u_s - u
        final T  delta_u = FastMath.atan2(position.dotProduct(Y), position.dotProduct(X));

        // Compute B(u)
        T b_u =  parameters[0];
        for (int i = 1; i < nB + 1; i++) {
            b_u = b_u.add(FastMath.cos(delta_u.multiply(2 * i)).multiply(parameters[i])).add(FastMath.sin(delta_u.multiply(2 * i)).multiply(parameters[i + nB]));
        }
        // Compute D(u)
        T d_u = parameters[2 * nB + 1];

        for (int i = 1; i < nD + 1; i++) {
            d_u =  d_u.add(FastMath.cos(delta_u.multiply(2 * i - 1)).multiply(parameters[2 * nB + 1 + i])).add(FastMath.sin(delta_u.multiply(2 * i - 1)).multiply(parameters[2 * nB + 1 + i + nD]));
        }
        // Return the acceleration
        return new FieldVector3D<>(d_u, eD, parameters[2 * (nD + nB) + 2], eY, b_u, eB);
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
        return coefs;
    }

    /**
     * Get the useful angles for eclipse computation.
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

    /**
     * Get the useful angles for eclipse computation.
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

