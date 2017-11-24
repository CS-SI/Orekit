/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import java.io.Serializable;
import java.util.Map;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.UT1Scale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

/** Class modeling an Earth frame whose Earth Orientation Parameters can be estimated.
 * <p>
 * This class adds parameters for an additional polar motion
 * and an additional prime meridian orientation on top of an underlying regular Earth
 * frame like {@link org.orekit.frames.FramesFactory#getITRF(IERSConventions, boolean) ITRF}.
 * The polar motion and prime meridian orientation are applied <em>after</em> regular Earth
 * orientation parameters, so the value of the estimated parameters will be correction to EOP,
 * they will not be the complete EOP values by themselves. Basically, this means that for
 * Earth, the following transforms are applied in order, between inertial frame and this frame:
 * </p>
 * <ol>
 *   <li>precession/nutation, as theoretical model plus celestial pole EOP parameters</li>
 *   <li>body rotation, as theoretical model plus prime meridian EOP parameters</li>
 *   <li>polar motion, which is only from EOP parameters (no theoretical models)</li>
 *   <li>additional body rotation, controlled by {@link #getPrimeMeridianOffsetDriver()} and {@link #getPrimeMeridianDriftDriver()}</li>
 *   <li>additional polar motion, controlled by {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()},
 *   {@link #getPolarOffsetYDriver()} and {@link #getPolarDriftYDriver()}</li>
 * </ol>
 * @author Luc Maisonobe
 * @since 9.1
 */
public class EstimatedEarthFrameProvider implements TransformProvider {

    /** Earth Angular Velocity, in rad/s, from TIRF model. */
    public static final double EARTH_ANGULAR_VELOCITY = 7.292115146706979e-5;

    /** Serializable UID. */
    private static final long serialVersionUID = 20170922L;

    /** Angular scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double ANGULAR_SCALE = FastMath.scalb(1.0, -22);

    /** Underlying raw UT1. */
    private final UT1Scale baseUT1;

    /** Estimated UT1. */
    private final transient UT1Scale estimatedUT1;

    /** Driver for prime meridian offset. */
    private final transient ParameterDriver primeMeridianOffsetDriver;

    /** Driver for prime meridian drift. */
    private final transient ParameterDriver primeMeridianDriftDriver;

    /** Driver for pole offset along X. */
    private final transient ParameterDriver polarOffsetXDriver;

    /** Driver for pole drift along X. */
    private final transient ParameterDriver polarDriftXDriver;

    /** Driver for pole offset along Y. */
    private final transient ParameterDriver polarOffsetYDriver;

    /** Driver for pole drift along Y. */
    private final transient ParameterDriver polarDriftYDriver;

    /** Build an estimated Earth frame.
     * <p>
     * The initial values for the pole and prime meridian parametric linear models
     * ({@link #getPrimeMeridianOffsetDriver()}, {@link #getPrimeMeridianDriftDriver()},
     * {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()},
     * {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()}) are set to 0.
     * </p>
     * @param baseUT1 underlying base UT1
     * @exception OrekitException if scales are too close to zero (never happens)
     * @since 9.1
     */
    public EstimatedEarthFrameProvider(final UT1Scale baseUT1)
        throws OrekitException {

        this.primeMeridianOffsetDriver = new ParameterDriver("prime-meridian-offset",
                                                             0.0, ANGULAR_SCALE,
                                                            -FastMath.PI, FastMath.PI);

        this.primeMeridianDriftDriver = new ParameterDriver("prime-meridian-drift",
                                                            0.0, ANGULAR_SCALE,
                                                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.polarOffsetXDriver = new ParameterDriver("polar-offset-X",
                                                      0.0, ANGULAR_SCALE,
                                                      -FastMath.PI, FastMath.PI);

        this.polarDriftXDriver = new ParameterDriver("polar-drift-X",
                                                     0.0, ANGULAR_SCALE,
                                                     Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.polarOffsetYDriver = new ParameterDriver("polar-offset-Y",
                                                      0.0, ANGULAR_SCALE,
                                                      -FastMath.PI, FastMath.PI);

        this.polarDriftYDriver = new ParameterDriver("polar-drift-Y",
                                                     0.0, ANGULAR_SCALE,
                                                     Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.baseUT1      = baseUT1;
        this.estimatedUT1 = new EstimatedUT1Scale();

    }

    /** Get a driver allowing to add a prime meridian rotation.
     * <p>
     * The parameter is an angle in radians. In order to convert this
     * value to a DUT1 in seconds, the value must be divided by
     * {@link #EARTH_ANGULAR_VELOCITY} (nominal Angular Velocity of Earth).
     * </p>
     * @return driver for prime meridian rotation
     */
    public ParameterDriver getPrimeMeridianOffsetDriver() {
        return primeMeridianOffsetDriver;
    }

    /** Get a driver allowing to add a prime meridian rotation rate.
     * <p>
     * The parameter is an angle rate in radians per second. In order to convert this
     * value to a LOD in seconds, the value must be multiplied by -86400 and divided by
     * {@link #EARTH_ANGULAR_VELOCITY} (nominal Angular Velocity of Earth).
     * </p>
     * @return driver for prime meridian rotation rate
     */
    public ParameterDriver getPrimeMeridianDriftDriver() {
        return primeMeridianDriftDriver;
    }

    /** Get a driver allowing to add a polar offset along X.
     * <p>
     * The parameter is an angle in radians
     * </p>
     * @return driver for polar offset along X
     */
    public ParameterDriver getPolarOffsetXDriver() {
        return polarOffsetXDriver;
    }

    /** Get a driver allowing to add a polar drift along X.
     * <p>
     * The parameter is an angle rate in radians per second
     * </p>
     * @return driver for polar drift along X
     */
    public ParameterDriver getPolarDriftXDriver() {
        return polarDriftXDriver;
    }

    /** Get a driver allowing to add a polar offset along Y.
     * <p>
     * The parameter is an angle in radians
     * </p>
     * @return driver for polar offset along Y
     */
    public ParameterDriver getPolarOffsetYDriver() {
        return polarOffsetYDriver;
    }

    /** Get a driver allowing to add a polar drift along Y.
     * <p>
     * The parameter is an angle rate in radians per second
     * </p>
     * @return driver for polar drift along Y
     */
    public ParameterDriver getPolarDriftYDriver() {
        return polarDriftYDriver;
    }

    /** Get the estimated UT1 time scale.
     * @return estimated UT1 time scale
     */
    public UT1Scale getEstimatedUT1() {
        return estimatedUT1;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date)
        throws OrekitException {

        // take parametric prime meridian shift into account
        final double theta    = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver);
        final double thetaDot = parametricModel(primeMeridianDriftDriver);
        final Transform meridianShift =
                        new Transform(date,
                                      new Rotation(Vector3D.PLUS_K, theta, RotationConvention.FRAME_TRANSFORM),
                                      new Vector3D(0, 0, thetaDot));

        // take parametric pole shift into account
        final double xpNeg     = -linearModel(date, polarOffsetXDriver, polarDriftXDriver);
        final double ypNeg     = -linearModel(date, polarOffsetYDriver, polarDriftYDriver);
        final double xpNegDot  = -parametricModel(polarDriftXDriver);
        final double ypNegDot  = -parametricModel(polarDriftYDriver);
        final Transform poleShift =
                        new Transform(date,
                                      new Transform(date,
                                                    new Rotation(Vector3D.PLUS_J, xpNeg, RotationConvention.FRAME_TRANSFORM),
                                                    new Vector3D(0.0, xpNegDot, 0.0)),
                                      new Transform(date,
                                                    new Rotation(Vector3D.PLUS_I, ypNeg, RotationConvention.FRAME_TRANSFORM),
                                                    new Vector3D(ypNegDot, 0.0, 0.0)));

        return new Transform(date, meridianShift, poleShift);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date)
        throws OrekitException {

        final T zero = date.getField().getZero();

        // prime meridian shift parameters
        final T theta    = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver);
        final T thetaDot = zero.add(parametricModel(primeMeridianDriftDriver));

        // pole shift parameters
        final T xpNeg    = linearModel(date, polarOffsetXDriver, polarDriftXDriver).negate();
        final T ypNeg    = linearModel(date, polarOffsetYDriver, polarDriftYDriver).negate();
        final T xpNegDot = zero.subtract(parametricModel(polarDriftXDriver));
        final T ypNegDot = zero.subtract(parametricModel(polarDriftYDriver));

        return getTransform(date, theta, thetaDot, xpNeg, xpNegDot, ypNeg, ypNegDot);

    }

    /** Get the transform with derivatives.
     * @param date date of the transform
     * @param factory factory for the derivatives
     * @param indices indices of the estimated parameters in derivatives computations
     * @return computed transform with derivatives
     * @exception OrekitException if some frame transforms cannot be computed
     */
    public FieldTransform<DerivativeStructure> getTransform(final FieldAbsoluteDate<DerivativeStructure> date,
                                                            final DSFactory factory,
                                                            final Map<String, Integer> indices)
        throws OrekitException {

        // prime meridian shift parameters
        final DerivativeStructure theta    = linearModel(factory, date,
                                                         primeMeridianOffsetDriver, primeMeridianDriftDriver,
                                                         indices);
        final DerivativeStructure thetaDot = parametricModel(factory, primeMeridianDriftDriver, indices);

        // pole shift parameters
        final DerivativeStructure xpNeg    = linearModel(factory, date,
                                                         polarOffsetXDriver, polarDriftXDriver, indices).negate();
        final DerivativeStructure ypNeg    = linearModel(factory, date,
                                                         polarOffsetYDriver, polarDriftYDriver, indices).negate();
        final DerivativeStructure xpNegDot = parametricModel(factory, polarDriftXDriver, indices).negate();
        final DerivativeStructure ypNegDot = parametricModel(factory, polarDriftYDriver, indices).negate();

        return getTransform(date, theta, thetaDot, xpNeg, xpNegDot, ypNeg, ypNegDot);

    }

    /** Get the transform with derivatives.
     * @param date date of the transform
     * @param theta angle of the prime meridian
     * @param thetaDot angular rate of the prime meridian
     * @param xpNeg opposite of the angle of the pole motion along X
     * @param xpNegDot opposite of the angular rate of the pole motion along X
     * @param ypNeg opposite of the angle of the pole motion along Y
     * @param ypNegDot opposite of the angular rate of the pole motion along Y
     * @param <T> type of the field elements
     * @return computed transform with derivatives
     * @exception OrekitException if some frame transforms cannot be computed
     */
    private <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date,
                                                                           final T theta, final T thetaDot,
                                                                           final T xpNeg, final T xpNegDot,
                                                                           final T ypNeg, final T ypNegDot)
        throws OrekitException {

        final T                zero  = date.getField().getZero();
        final FieldVector3D<T> plusI = FieldVector3D.getPlusI(date.getField());
        final FieldVector3D<T> plusJ = FieldVector3D.getPlusJ(date.getField());
        final FieldVector3D<T> plusK = FieldVector3D.getPlusK(date.getField());

        // take parametric prime meridian shift into account
        final FieldTransform<T> meridianShift =
                        new FieldTransform<>(date,
                                             new FieldRotation<>(plusK, theta, RotationConvention.FRAME_TRANSFORM),
                                             new FieldVector3D<>(zero, zero, thetaDot));

        // take parametric pole shift into account
        final FieldTransform<T> poleShift =
                        new FieldTransform<>(date,
                                      new FieldTransform<>(date,
                                                           new FieldRotation<>(plusJ, xpNeg, RotationConvention.FRAME_TRANSFORM),
                                                           new FieldVector3D<>(zero, xpNegDot, zero)),
                                      new FieldTransform<>(date,
                                                           new FieldRotation<>(plusI, ypNeg, RotationConvention.FRAME_TRANSFORM),
                                                           new FieldVector3D<>(ypNegDot, zero, zero)));

        return new FieldTransform<>(date, meridianShift, poleShift);

    }

    /** Evaluate a parametric linear model.
     * @param date current date
     * @param offsetDriver driver for the offset parameter
     * @param driftDriver driver for the drift parameter
     * @return current value of the linear model
     * @exception OrekitException if reference date has not been set for the
     * offset driver
     */
    private double linearModel(final AbsoluteDate date,
                               final ParameterDriver offsetDriver, final ParameterDriver driftDriver)
        throws OrekitException {
        if (offsetDriver.getReferenceDate() == null) {
            throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                      offsetDriver.getName());
        }
        final double dt     = date.durationFrom(offsetDriver.getReferenceDate());
        final double offset = parametricModel(offsetDriver);
        final double drift  = parametricModel(driftDriver);
        return dt * drift + offset;
    }

    /** Evaluate a parametric linear model.
     * @param date current date
     * @param offsetDriver driver for the offset parameter
     * @param driftDriver driver for the drift parameter
     * @return current value of the linear model
     * @exception OrekitException if reference date has not been set for the
     * offset driver
     * @param <T> type of the filed elements
     */
    private <T extends RealFieldElement<T>> T linearModel(final FieldAbsoluteDate<T> date,
                                                          final ParameterDriver offsetDriver,
                                                          final ParameterDriver driftDriver)
        throws OrekitException {
        if (offsetDriver.getReferenceDate() == null) {
            throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                      offsetDriver.getName());
        }
        final T dt          = date.durationFrom(offsetDriver.getReferenceDate());
        final double offset = parametricModel(offsetDriver);
        final double drift  = parametricModel(driftDriver);
        return dt.multiply(drift).add(offset);
    }

    /** Evaluate a parametric linear model.
     * @param factory factory for the derivatives
     * @param date current date
     * @param offsetDriver driver for the offset parameter
     * @param driftDriver driver for the drift parameter
     * @param indices indices of the estimated parameters in derivatives computations
     * @return current value of the linear model
     * @exception OrekitException if reference date has not been set for the
     * offset driver
     */
    private DerivativeStructure linearModel(final DSFactory factory, final FieldAbsoluteDate<DerivativeStructure> date,
                                            final ParameterDriver offsetDriver, final ParameterDriver driftDriver,
                                            final Map<String, Integer> indices)
        throws OrekitException {
        if (offsetDriver.getReferenceDate() == null) {
            throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                      offsetDriver.getName());
        }
        final DerivativeStructure dt     = date.durationFrom(offsetDriver.getReferenceDate());
        final DerivativeStructure offset = parametricModel(factory, offsetDriver, indices);
        final DerivativeStructure drift  = parametricModel(factory, driftDriver, indices);
        return dt.multiply(drift).add(offset);
    }

    /** Evaluate a parametric model.
     * @param driver driver managing the parameter
     * @return value of the parametric model
     */
    private double parametricModel(final ParameterDriver driver) {
        return driver.getValue();
    }

    /** Evaluate a parametric model.
     * @param factory factory for the derivatives
     * @param driver driver managing the parameter
     * @param indices indices of the estimated parameters in derivatives computations
     * @return value of the parametric model
     */
    private DerivativeStructure parametricModel(final DSFactory factory, final ParameterDriver driver,
                                                final Map<String, Integer> indices) {
        final Integer index = indices.get(driver.getName());
        return (index == null) ?
               factory.constant(driver.getValue()) :
               factory.variable(index, driver.getValue());
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes the files supported names, the ephemeris type
     * and the body name.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(baseUT1,
                                      primeMeridianOffsetDriver.getValue(),
                                      primeMeridianDriftDriver.getValue(),
                                      polarOffsetXDriver.getValue(),
                                      polarDriftXDriver.getValue(),
                                      polarOffsetYDriver.getValue(),
                                      polarDriftYDriver.getValue());
    }

    /** Local time scale for estimated UT1. */
    private class EstimatedUT1Scale extends UT1Scale {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170922L;

        /** Simple constructor.
         */
        EstimatedUT1Scale() {
            super(baseUT1.getEOPHistory(), baseUT1.getUTCScale());
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
            try {
                final T dut1 = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver).divide(EARTH_ANGULAR_VELOCITY);
                return baseUT1.offsetFromTAI(date).add(dut1);
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        @Override
        public double offsetFromTAI(final AbsoluteDate date) throws OrekitExceptionWrapper {
            try {
                final double dut1 = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver) / EARTH_ANGULAR_VELOCITY;
                return baseUT1.offsetFromTAI(date) + dut1;
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return baseUT1.getName() + "/estimated";
        }

    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20171124L;

        /** Underlying raw UT1. */
        private final UT1Scale baseUT1;

        /** Current prime meridian offset. */
        private final double primeMeridianOffset;

        /** Current prime meridian drift. */
        private final double primeMeridianDrift;

        /** Current pole offset along X. */
        private final double polarOffsetX;

        /** Current pole drift along X. */
        private final double polarDriftX;

        /** Current pole offset along Y. */
        private final double polarOffsetY;

        /** Current pole drift along Y. */
        private final double polarDriftY;

        /** Simple constructor.
         * @param baseUT1 underlying raw UT1
         * @param primeMeridianOffset current prime meridian offset
         * @param primeMeridianDrift current prime meridian drift
         * @param polarOffsetX current pole offset along X
         * @param polarDriftX current pole drift along X
         * @param polarOffsetY current pole offset along Y
         * @param polarDriftY current pole drift along Y
         */
        DataTransferObject(final  UT1Scale baseUT1,
                           final double primeMeridianOffset, final double primeMeridianDrift,
                           final double polarOffsetX,        final double polarDriftX,
                           final double polarOffsetY,        final double polarDriftY) {
            this.baseUT1             = baseUT1;
            this.primeMeridianOffset = primeMeridianOffset;
            this.primeMeridianDrift  = primeMeridianDrift;
            this.polarOffsetX        = polarOffsetX;
            this.polarDriftX         = polarDriftX;
            this.polarOffsetY        = polarOffsetY;
            this.polarDriftY         = polarDriftY;
        }

        /** Replace the deserialized data transfer object with a {@link EstimatedEarthFrameProvider}.
         * @return replacement {@link EstimatedEarthFrameProvider}
         */
        private Object readResolve() {
            try {
                final EstimatedEarthFrameProvider provider = new EstimatedEarthFrameProvider(baseUT1);
                provider.getPrimeMeridianOffsetDriver().setValue(primeMeridianOffset);
                provider.getPrimeMeridianDriftDriver().setValue(primeMeridianDrift);
                provider.getPolarOffsetXDriver().setValue(polarOffsetX);
                provider.getPolarDriftXDriver().setValue(polarDriftX);
                provider.getPolarOffsetYDriver().setValue(polarOffsetY);
                provider.getPolarDriftYDriver().setValue(polarDriftY);
                return provider;
            } catch (OrekitException oe) {
                // this should never happen as values already come from previous drivers
                throw new OrekitInternalError(oe);
            }
        }

    }

}
