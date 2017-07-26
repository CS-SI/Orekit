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

import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Class modeling a ground station that can perform some measurements.
 * <p>
 * This class adds a position offset parameter to a base {@link TopocentricFrame
 * topocentric frame}.
 * </p>
 * <p>
 * Since 9.0, this class also adds parameters for an additional polar motion
 * and an additional prime meridian orientation. Since these parameters will
 * have the same name for all ground stations, they will be managed consistently
 * and allow to estimate Earth orientation precisely (this is needed for precise
 * orbit determination). The polar motion and prime meridian orientation will
 * be applied <em>after</em> regular Earth orientation parameters, so the value
 * of the estimated parameters will be correction to EOP, they will not be the
 * complete EOP values by themselves. Basically, this means that for Earth, the
 * following transforms are applied in order, between inertial frame and ground
 * station frame (for non-Earth based ground stations, different precession nutation
 * models and associated planet oritentation parameters would be applied, if available):
 * </p>
 * <ol>
 *   <li>precession/nutation, as theoretical model plus celestial pole EOP parameters</li>
 *   <li>body rotation, as theoretical model plus prime meridian EOP parameters</li>
 *   <li>polar motion, which is only from EOP parameters (no theoretical models)</li>
 *   <li>additional body rotation, controlled by {@link #getPrimeMeridianOffsetDriver()} and {@link #getPrimeMeridianDriftDriver()}</li>
 *   <li>additional polar motion, controlled by {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()},
 *   {@link #getPolarOffsetYDriver()} and {@link #getPolarDriftYDriver()}</li>
 *   <li>station position offset, controlled by {@link #getEastOffsetDriver()},
 *   {@link #getNorthOffsetDriver()} and {@link #getZenithOffsetDriver()}</li>
 * </ol>
 * @author Luc Maisonobe
 * @since 8.0
 */
public class GroundStation {

    /** Suffix for ground station position offset parameter name. */
    public static final String OFFSET_SUFFIX = "-offset";

    /** Suffix for ground station intermediate frame name. */
    public static final String INTERMEDIATE_SUFFIX = "-intermediate";

    /** Offsets scaling factor.
     * <p>
     * We use a power of 2 (in fact really 1.0 here) to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double OFFSET_SCALE = FastMath.scalb(1.0, 0);

    /** Angular scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double ANGULAR_SCALE = FastMath.scalb(1.0, -22);

    /** Base frame associated with the station. */
    private final TopocentricFrame baseFrame;

    /** Driver for position offset along the East axis. */
    private final ParameterDriver eastOffsetDriver;

    /** Driver for position offset along the North axis. */
    private final ParameterDriver northOffsetDriver;

    /** Driver for position offset along the zenith axis. */
    private final ParameterDriver zenithOffsetDriver;

    /** Driver for prime meridian offset. */
    private final ParameterDriver primeMeridianOffsetDriver;

    /** Driver for prime meridian drift. */
    private final ParameterDriver primeMeridianDriftDriver;

    /** Driver for pole offset along X. */
    private final ParameterDriver polarOffsetXDriver;

    /** Driver for pole drift along X. */
    private final ParameterDriver polarDriftXDriver;

    /** Driver for pole offset along Y. */
    private final ParameterDriver polarOffsetYDriver;

    /** Driver for pole drift along Y. */
    private final ParameterDriver polarDriftYDriver;

    /** Simple constructor.
     * <p>
     * The initial values for the pole and prime meridian parametric linear models
     * ({@link #getPrimeMeridianOffsetDriver()}, {@link #getPrimeMeridianDriftDriver()},
     * {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()},
     * {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()}) are set to 0.
     * The initial values for the station offset model ({@link #getEastOffsetDriver()},
     * {@link #getNorthOffsetDriver()}, {@link #getZenithOffsetDriver()}) are set to 0.
     * This implies that as long as these values are not changed, the offset frame is
     * the same as the {@link #getBaseFrame() base frame}. As soon as some of these models
     * are changed, the offset frame moves away from the {@link #getBaseFrame() base frame}.
     * </p>
     * @param baseFrame base frame associated with the station, without *any* parametric
     * model (no station offset, no polar motion, no meridian shift)
     * @exception OrekitException if some frame transforms cannot be computed
     * or if the ground station is not defined on a {@link OneAxisEllipsoid ellipsoid}.
     */
    public GroundStation(final TopocentricFrame baseFrame)
        throws OrekitException {

        this.baseFrame = baseFrame;

        this.eastOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-East",
                                                    0.0, OFFSET_SCALE,
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.northOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-North",
                                                     0.0, OFFSET_SCALE,
                                                     Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.zenithOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-Zenith",
                                                      0.0, OFFSET_SCALE,
                                                      Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

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

    }

    /** Get a driver allowing to change station position along East axis.
     * @return driver for station position offset along East axis
     */
    public ParameterDriver getEastOffsetDriver() {
        return eastOffsetDriver;
    }

    /** Get a driver allowing to change station position along North axis.
     * @return driver for station position offset along North axis
     */
    public ParameterDriver getNorthOffsetDriver() {
        return northOffsetDriver;
    }

    /** Get a driver allowing to change station position along Zenith axis.
     * @return driver for station position offset along Zenith axis
     */
    public ParameterDriver getZenithOffsetDriver() {
        return zenithOffsetDriver;
    }

    /** Get a driver allowing to add a prime meridian rotation.
     * <p>
     * The parameter is an angle in radians. In order to convert this
     * value to a DUT1 in seconds, the value must be divided by
     * {@code ave = 7.292115146706979e-5} (which is the nominal Angular Velocity
     * of Earth from the TIRF model).
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
     * {@code ave = 7.292115146706979e-5} (which is the nominal Angular Velocity
     * of Earth from the TIRF model).
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

    /** Get the base frame associated with the station.
     * <p>
     * The base frame corresponds to a null position offset, null
     * polar motion, null meridian shift
     * </p>
     * @return base frame associated with the station
     */
    public TopocentricFrame getBaseFrame() {
        return baseFrame;
    }

    /** Get the geodetic point at the center of the offset frame.
     * @return geodetic point at the center of the offset frame
     * @exception OrekitException if frames transforms cannot be computed
     */
    public GeodeticPoint getOffsetGeodeticPoint()
        throws OrekitException {

        // take station offset into account
        final double    x          = parametricModel(eastOffsetDriver);
        final double    y          = parametricModel(northOffsetDriver);
        final double    z          = parametricModel(zenithOffsetDriver);
        final BodyShape baseShape  = baseFrame.getParentShape();
        final Transform baseToBody = baseFrame.getTransformTo(baseShape.getBodyFrame(), (AbsoluteDate) null);
        final Vector3D  origin     = baseToBody.transformPosition(new Vector3D(x, y, z));

        return baseShape.transform(origin, baseShape.getBodyFrame(), null);

    }

    /** Get the transform between offset frame and inertial frame.
     * <p>
     * The offset frame takes the <em>current</em> position offset,
     * polar motion and the meridian shift into account. The frame
     * returned is disconnected from later changes in the parameters.
     * When the {@link ParameterDriver parameters} managing these
     * offsets are changed, the method must be called again to retrieve
     * a new offset frame.
     * </p>
     * @param inertial inertial frame to transform to
     * @param date date of the transform
     * @return offset frame defining vectors
     * @exception OrekitException if offset frame cannot be computed for current offset values
     */
    public Transform getOffsetToInertial(final Frame inertial, final AbsoluteDate date)
        throws OrekitException {

        // take parametric prime meridian shift into account
        final double theta    = linearModel(date, primeMeridianOffsetDriver, primeMeridianDriftDriver);
        final double thetaDot = parametricModel(primeMeridianDriftDriver);
        final Transform meridianShift =
                        new Transform(date,
                                      new Rotation(Vector3D.PLUS_K, -theta, RotationConvention.FRAME_TRANSFORM),
                                      new Vector3D(-thetaDot, Vector3D.PLUS_K));

        // take parametric pole shift into account
        final double xp     = linearModel(date, polarOffsetXDriver, polarDriftXDriver);
        final double yp     = linearModel(date, polarOffsetYDriver, polarDriftYDriver);
        final double xpDot  = parametricModel(polarDriftXDriver);
        final double ypDot  = parametricModel(polarDriftYDriver);
        final Transform poleShift =
                        new Transform(date,
                                      new Transform(date,
                                                    new Rotation(Vector3D.PLUS_I, yp, RotationConvention.FRAME_TRANSFORM),
                                                    new Vector3D(ypDot, 0.0, 0.0)),
                                      new Transform(date,
                                                    new Rotation(Vector3D.PLUS_J, xp, RotationConvention.FRAME_TRANSFORM),
                                                    new Vector3D(0.0, xpDot, 0.0)));

        // take station offset into account
        final double    x          = parametricModel(eastOffsetDriver);
        final double    y          = parametricModel(northOffsetDriver);
        final double    z          = parametricModel(zenithOffsetDriver);
        final BodyShape baseShape  = baseFrame.getParentShape();
        final Transform baseToBody = baseFrame.getTransformTo(baseShape.getBodyFrame(), (AbsoluteDate) null);

        final Vector3D      origin   = baseToBody.transformPosition(new Vector3D(x, y, z));
        final GeodeticPoint originGP = baseShape.transform(origin, baseShape.getBodyFrame(), date);
        final Transform offsetToIntermediate =
                        new Transform(date,
                                      new Transform(date,
                                                    new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K,
                                                                 originGP.getEast(), originGP.getZenith()),
                                                    Vector3D.ZERO),
                                      new Transform(date, origin));

        // combine all transforms together
        final Transform intermediateToBody = new Transform(date, poleShift, meridianShift);
        final Transform bodyToInert        = baseFrame.getParent().getTransformTo(inertial, date);

        return new Transform(date, offsetToIntermediate, new Transform(date, intermediateToBody, bodyToInert));

    }

    /** Get the transform between offset frame and inertial frame with derivatives.
     * <p>
     * As the East and North vector are not well defined at pole, the derivatives
     * of these two vectors diverge to infinity as we get closer to the pole.
     * So this method should not be used for stations less than 0.0001 degree from
     * either poles.
     * </p>
     * @param inertial inertial frame to transform to
     * @param date date of the transform
     * @param factory factory for the derivatives
     * @param indices indices of the estimated parameters in derivatives computations
     * @return offset frame defining vectors with derivatives
     * @exception OrekitException if some frame transforms cannot be computed
     * @since 9.0
     */
    public FieldTransform<DerivativeStructure> getOffsetToInertial(final Frame inertial,
                                                                   final FieldAbsoluteDate<DerivativeStructure> date,
                                                                   final DSFactory factory,
                                                                   final Map<String, Integer> indices)
        throws OrekitException {

        final Field<DerivativeStructure>         field = date.getField();
        final FieldVector3D<DerivativeStructure> zero  = FieldVector3D.getZero(field);
        final FieldVector3D<DerivativeStructure> plusI = FieldVector3D.getPlusI(field);
        final FieldVector3D<DerivativeStructure> plusJ = FieldVector3D.getPlusJ(field);
        final FieldVector3D<DerivativeStructure> plusK = FieldVector3D.getPlusK(field);

        // take parametric prime meridian shift into account
        final DerivativeStructure theta    = linearModel(factory, date,
                                                         primeMeridianOffsetDriver, primeMeridianDriftDriver,
                                                         indices);
        final DerivativeStructure thetaDot = parametricModel(factory, primeMeridianDriftDriver, indices);
        final FieldTransform<DerivativeStructure> meridianShift =
                        new FieldTransform<>(date,
                                             new FieldRotation<>(plusK, theta.negate(), RotationConvention.FRAME_TRANSFORM),
                                             new FieldVector3D<>(thetaDot.negate(), plusK));

        // take parametric pole shift into account
        final DerivativeStructure xp    = linearModel(factory, date,
                                                      polarOffsetXDriver, polarDriftXDriver, indices);
        final DerivativeStructure yp    = linearModel(factory, date,
                                                      polarOffsetYDriver, polarDriftYDriver, indices);
        final DerivativeStructure xpDot = parametricModel(factory, polarDriftXDriver, indices);
        final DerivativeStructure ypDot = parametricModel(factory, polarDriftYDriver, indices);
        final FieldTransform<DerivativeStructure> poleShift =
                        new FieldTransform<>(date,
                                             new FieldTransform<>(date,
                                                             new FieldRotation<>(plusI, yp, RotationConvention.FRAME_TRANSFORM),
                                                             new FieldVector3D<>(ypDot, field.getZero(), field.getZero())),
                                             new FieldTransform<>(date,
                                                             new FieldRotation<>(plusJ, xp, RotationConvention.FRAME_TRANSFORM),
                                                             new FieldVector3D<>(field.getZero(), xpDot, field.getZero())));

        // take station offset into account
        final DerivativeStructure  x          = parametricModel(factory, eastOffsetDriver,   indices);
        final DerivativeStructure  y          = parametricModel(factory, northOffsetDriver,  indices);
        final DerivativeStructure  z          = parametricModel(factory, zenithOffsetDriver, indices);
        final BodyShape            baseShape  = baseFrame.getParentShape();
        final Transform            baseToBody = baseFrame.getTransformTo(baseShape.getBodyFrame(), (AbsoluteDate) null);

        final FieldVector3D<DerivativeStructure>      origin   = baseToBody.transformPosition(new FieldVector3D<>(x, y, z));
        final FieldGeodeticPoint<DerivativeStructure> originGP = baseShape.transform(origin, baseShape.getBodyFrame(), date);
        final FieldTransform<DerivativeStructure> offsetToIntermediate =
                        new FieldTransform<>(date,
                                             new FieldTransform<>(date,
                                                                  new FieldRotation<>(plusI, plusK,
                                                                                      originGP.getEast(), originGP.getZenith()),
                                                                  zero),
                                             new FieldTransform<>(date, origin));

        // combine all transforms together
        final FieldTransform<DerivativeStructure> intermediateToBody = new FieldTransform<>(date, poleShift, meridianShift);
        final FieldTransform<DerivativeStructure> bodyToInert        = baseFrame.getParent().getTransformTo(inertial, date);

        return new FieldTransform<>(date, offsetToIntermediate, new FieldTransform<>(date, intermediateToBody, bodyToInert));

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

}
