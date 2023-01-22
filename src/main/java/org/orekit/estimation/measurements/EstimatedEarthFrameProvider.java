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
package org.orekit.estimation.measurements;

import java.io.Serializable;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.StaticTransform;
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
     * @since 9.1
     */
    public EstimatedEarthFrameProvider(final UT1Scale baseUT1) {

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
    public Transform getTransform(final AbsoluteDate date) {

        // take parametric prime meridian shift into account
        final double theta    = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver);
        final double thetaDot = primeMeridianDriftDriver.getValue();
        final Transform meridianShift =
                        new Transform(date,
                                      new Rotation(Vector3D.PLUS_K, theta, RotationConvention.FRAME_TRANSFORM),
                                      new Vector3D(0, 0, thetaDot));

        // take parametric pole shift into account
        final double xpNeg     = -linearModel(date, polarOffsetXDriver, polarDriftXDriver);
        final double ypNeg     = -linearModel(date, polarOffsetYDriver, polarDriftYDriver);
        final double xpNegDot  = -polarDriftXDriver.getValue();
        final double ypNegDot  = -polarDriftYDriver.getValue();
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
    public StaticTransform getStaticTransform(final AbsoluteDate date) {

        // take parametric prime meridian shift into account
        final double theta    = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver);
        final StaticTransform meridianShift = StaticTransform.of(
                date,
                new Rotation(Vector3D.PLUS_K, theta, RotationConvention.FRAME_TRANSFORM)
        );

        // take parametric pole shift into account
        final double xpNeg     = -linearModel(date, polarOffsetXDriver, polarDriftXDriver);
        final double ypNeg     = -linearModel(date, polarOffsetYDriver, polarDriftYDriver);
        final StaticTransform poleShift = StaticTransform.compose(
                date,
                StaticTransform.of(
                        date,
                        new Rotation(Vector3D.PLUS_J, xpNeg, RotationConvention.FRAME_TRANSFORM)),
                StaticTransform.of(
                        date,
                        new Rotation(Vector3D.PLUS_I, ypNeg, RotationConvention.FRAME_TRANSFORM)));

        return StaticTransform.compose(date, meridianShift, poleShift);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        final T zero = date.getField().getZero();

        // prime meridian shift parameters
        final T theta    = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver);
        final T thetaDot = zero.add(primeMeridianDriftDriver.getValue());

        // pole shift parameters
        final T xpNeg    = linearModel(date, polarOffsetXDriver, polarDriftXDriver).negate();
        final T ypNeg    = linearModel(date, polarOffsetYDriver, polarDriftYDriver).negate();
        final T xpNegDot = zero.subtract(polarDriftXDriver.getValue());
        final T ypNegDot = zero.subtract(polarDriftYDriver.getValue());

        return getTransform(date, theta, thetaDot, xpNeg, xpNegDot, ypNeg, ypNegDot);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {

        // take parametric prime meridian shift into account
        final T theta    = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver);
        final FieldStaticTransform<T> meridianShift = FieldStaticTransform.of(
                date,
                new FieldRotation<>(FieldVector3D.getPlusK(date.getField()), theta, RotationConvention.FRAME_TRANSFORM)
        );

        // take parametric pole shift into account
        final T xpNeg     = linearModel(date, polarOffsetXDriver, polarDriftXDriver).negate();
        final T ypNeg     = linearModel(date, polarOffsetYDriver, polarDriftYDriver).negate();
        final FieldStaticTransform<T> poleShift = FieldStaticTransform.compose(
                date,
                FieldStaticTransform.of(
                        date,
                        new FieldRotation<>(FieldVector3D.getPlusJ(date.getField()), xpNeg, RotationConvention.FRAME_TRANSFORM)),
                FieldStaticTransform.of(
                        date,
                        new FieldRotation<>(FieldVector3D.getPlusI(date.getField()), ypNeg, RotationConvention.FRAME_TRANSFORM)));

        return FieldStaticTransform.compose(date, meridianShift, poleShift);

    }

    /** Get the transform with derivatives.
     * @param date date of the transform
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the estimated parameters in derivatives computations
     * @return computed transform with derivatives
     * @since 10.2
     */
    public FieldTransform<Gradient> getTransform(final FieldAbsoluteDate<Gradient> date,
                                                 final int freeParameters,
                                                 final Map<String, Integer> indices) {

        // prime meridian shift parameters
        final Gradient theta    = linearModel(freeParameters, date,
                                              primeMeridianOffsetDriver, primeMeridianDriftDriver,
                                              indices);
        final Gradient thetaDot = primeMeridianDriftDriver.getValue(freeParameters, indices, date.toAbsoluteDate());

        // pole shift parameters
        final Gradient xpNeg    = linearModel(freeParameters, date,
                                                         polarOffsetXDriver, polarDriftXDriver, indices).negate();
        final Gradient ypNeg    = linearModel(freeParameters, date,
                                                         polarOffsetYDriver, polarDriftYDriver, indices).negate();
        final Gradient xpNegDot = polarDriftXDriver.getValue(freeParameters, indices, date.toAbsoluteDate()).negate();
        final Gradient ypNegDot = polarDriftYDriver.getValue(freeParameters, indices, date.toAbsoluteDate()).negate();

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
     */
    private <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date,
                                                                           final T theta, final T thetaDot,
                                                                           final T xpNeg, final T xpNegDot,
                                                                           final T ypNeg, final T ypNegDot) {

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
     */
    private double linearModel(final AbsoluteDate date,
                               final ParameterDriver offsetDriver, final ParameterDriver driftDriver) {
        if (offsetDriver.getReferenceDate() == null) {
            throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                      offsetDriver.getName());
        }
        final double dt     = date.durationFrom(offsetDriver.getReferenceDate());
        final double offset = offsetDriver.getValue();
        final double drift  = driftDriver.getValue();
        return dt * drift + offset;
    }

    /** Evaluate a parametric linear model.
     * @param date current date
     * @param offsetDriver driver for the offset parameter
     * @param driftDriver driver for the drift parameter
     * @return current value of the linear model
     * @param <T> type of the filed elements
     */
    private <T extends CalculusFieldElement<T>> T linearModel(final FieldAbsoluteDate<T> date,
                                                          final ParameterDriver offsetDriver,
                                                          final ParameterDriver driftDriver) {
        if (offsetDriver.getReferenceDate() == null) {
            throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                      offsetDriver.getName());
        }
        final T dt          = date.durationFrom(offsetDriver.getReferenceDate());
        final double offset = offsetDriver.getValue();
        final double drift  = driftDriver.getValue();
        return dt.multiply(drift).add(offset);
    }

    /** Evaluate a parametric linear model.
     * @param freeParameters total number of free parameters in the gradient
     * @param date current date
     * @param offsetDriver driver for the offset parameter
     * @param driftDriver driver for the drift parameter
     * @param indices indices of the estimated parameters in derivatives computations
     * @return current value of the linear model
     * @since 10.2
     */
    private Gradient linearModel(final int freeParameters, final FieldAbsoluteDate<Gradient> date,
                                 final ParameterDriver offsetDriver, final ParameterDriver driftDriver,
                                 final Map<String, Integer> indices) {
        if (offsetDriver.getReferenceDate() == null) {
            throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                      offsetDriver.getName());
        }
        final Gradient dt     = date.durationFrom(offsetDriver.getReferenceDate());
        final Gradient offset = offsetDriver.getValue(freeParameters, indices, date.toAbsoluteDate());
        final Gradient drift  = driftDriver.getValue(freeParameters, indices, date.toAbsoluteDate());
        return dt.multiply(drift).add(offset);
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
        public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
            final T dut1 = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver).divide(EARTH_ANGULAR_VELOCITY);
            return baseUT1.offsetFromTAI(date).add(dut1);
        }

        /** {@inheritDoc} */
        @Override
        public double offsetFromTAI(final AbsoluteDate date) {
            final double dut1 = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver) / EARTH_ANGULAR_VELOCITY;
            return baseUT1.offsetFromTAI(date) + dut1;
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
