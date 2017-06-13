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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.Ellipse;
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
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a ground station that can perform some measurements.
 * <p>
 * This class adds a position offset parameter to a base {@link TopocentricFrame
 * topocentric frame}.
 * </p>
 * @author Luc Maisonobe
 * @since 8.0
 */
public class GroundStation {

    /** Suffix for ground station position offset parameter name. */
    public static final String OFFSET_SUFFIX = "-offset";

    /** Offsets scaling factor.
     * <p>
     * We use a power of 2 (in fact really 1.0 here) to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double OFFSET_SCALE = FastMath.scalb(1.0, 0);

    /** Base frame associated with the station. */
    private final TopocentricFrame baseFrame;

    /** Drivers for position offset along the East axis. */
    private final ParameterDriver eastOffsetDriver;

    /** Drivers for position offset along the North axis. */
    private final ParameterDriver northOffsetDriver;

    /** Drivers for position offset along the zenith axis. */
    private final ParameterDriver zenithOffsetDriver;

    /** Offset frame associated with the station, taking offset parameter into account. */
    private TopocentricFrame offsetFrame;

    /** Simple constructor.
     * @param baseFrame base frame associated with the station
     * @exception OrekitException if some frame transforms cannot be computed
     */
    public GroundStation(final TopocentricFrame baseFrame)
        throws OrekitException {

        this.baseFrame = baseFrame;

        final ParameterObserver resettingObserver = new ParameterObserver() {
            /** {@inheritDoc} */
            @Override
            public void valueChanged(final double previousValue, final ParameterDriver driver) {
                offsetFrame = null;
            }
        };

        this.eastOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-East",
                                                    0.0, OFFSET_SCALE,
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.eastOffsetDriver.addObserver(resettingObserver);

        this.northOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-North",
                                                     0.0, OFFSET_SCALE,
                                                     Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.northOffsetDriver.addObserver(resettingObserver);

        this.zenithOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-Zenith",
                                                      0.0, OFFSET_SCALE,
                                                      Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.zenithOffsetDriver.addObserver(resettingObserver);

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

    /** Get the base frame associated with the station.
     * <p>
     * The base frame corresponds to a null position offset
     * </p>
     * @return base frame associated with the station
     */
    public TopocentricFrame getBaseFrame() {
        return baseFrame;
    }

    /** Get the offset frame associated with the station.
     * <p>
     * The offset frame takes the position offset into account
     * </p>
     * @return offset frame associated with the station
     * @exception OrekitException if offset frame cannot be computed for current offset values
     */
    public TopocentricFrame getOffsetFrame() throws OrekitException {
        if (offsetFrame == null) {
            // lazy evaluation of offset frame, in body frame
            final BodyShape bodyShape    = baseFrame.getParentShape();
            final Frame     bodyFrame    = bodyShape.getBodyFrame();
            final Transform baseToBody   = baseFrame.getTransformTo(bodyFrame, (AbsoluteDate) null);
            final double    x            = eastOffsetDriver.getValue();
            final double    y            = northOffsetDriver.getValue();
            final double    z            = zenithOffsetDriver.getValue();
            final Vector3D  origin       = baseToBody.transformPosition(new Vector3D(x, y, z));
            final GeodeticPoint originGP = bodyShape.transform(origin, bodyFrame, null);

            // create a new topocentric frame at parameterized origin
            offsetFrame = new TopocentricFrame(bodyShape, originGP,
                                               baseFrame.getName() + OFFSET_SUFFIX);

        }
        return offsetFrame;
    }

    /** Compute propagation delay on a link leg (either downlink or uplink).
     * @param adjustableEmitterPV position/velocity of emitter that may be adjusted
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate},
     * in the same frame as {@code adjustableEmitterPV}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public double signalTimeOfFlight(final TimeStampedPVCoordinates adjustableEmitterPV,
                                     final Vector3D receiverPosition,
                                     final AbsoluteDate signalArrivalDate) {

        // initialize emission date search loop assuming the state is already correct
        // this will be true for all but the first orbit determination iteration,
        // and even for the first iteration the loop will converge very fast
        final double offset = signalArrivalDate.durationFrom(adjustableEmitterPV.getDate());
        double delay = offset;

        // search signal transit date, computing the signal travel in inertial frame
        final double cReciprocal = 1.0 / Constants.SPEED_OF_LIGHT;
        double delta;
        int count = 0;
        do {
            final double previous   = delay;
            final Vector3D transitP = adjustableEmitterPV.shiftedBy(offset - delay).getPosition();
            delay                   = receiverPosition.distance(transitP) * cReciprocal;
            delta                   = FastMath.abs(delay - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(delay));

        return delay;

    }

    /** Compute propagation delay on a link leg (either downlink or uplink).
     * @param adjustableEmitterPV position/velocity of emitter that may be adjusted
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate},
     * in the same frame as {@code adjustableEmitterPV}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @return <em>positive</em> delay between signal emission and signal reception dates
     * @param <T> the type of the components
     */
    public <T extends RealFieldElement<T>> T signalTimeOfFlight(final TimeStampedFieldPVCoordinates<T> adjustableEmitterPV,
                                                                final FieldVector3D<T> receiverPosition,
                                                                final FieldAbsoluteDate<T> signalArrivalDate) {

        // Initialize emission date search loop assuming the emitter PV is almost correct
        // this will be true for all but the first orbit determination iteration,
        // and even for the first iteration the loop will converge extremely fast
        final T offset = signalArrivalDate.durationFrom(adjustableEmitterPV.getDate());
        T delay = offset;

        // search signal transit date, computing the signal travel in the frame shared by emitter and receiver
        final double cReciprocal = 1.0 / Constants.SPEED_OF_LIGHT;
        double delta;
        int count = 0;
        do {
            final double previous           = delay.getReal();
            final FieldVector3D<T> transitP = adjustableEmitterPV.shiftedBy(delay.negate().add(offset)).getPosition();
            delay                           = receiverPosition.distance(transitP).multiply(cReciprocal);
            delta                           = FastMath.abs(delay.getReal() - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(delay.getReal()));

        return delay;

    }

    /** Get the transform between offset frame and inertial frame with derivatives.
     * <p>
     * Note that this method works only if the ground station is defined on
     * an {@link OneAxisEllipsoid ellipsoid} body. For any other body shape,
     * the method will throw an exception.
     * </p>
     * <p>
     * As the East and North vector are not well defined at pole, the derivatives
     * of these two vectors diverge to infinity as we get closer to the pole.
     * So this method should not be used for stations less than 0.0001 degree from
     * either poles.
     * </p>
     * @param inertial inertial frame to transform to
     * @param date date of the transform
     * @param factory factory for the derivatives
     * @param eastOffsetIndex index of the East offset in the set of
     * free parameters in derivatives computations (negative if not used)
     * @param northOffsetIndex index of the North offset in the set of
     * free parameters in derivatives computations (negative if not used)
     * @param zenithOffsetIndex index of the Zenith offset in the set of
     * free parameters in derivatives computations (negative if not used)
     * @return offset frame defining vectors with derivatives
     * @exception OrekitException if some frame transforms cannot be computed
     * or if the ground station is not defined on a {@link OneAxisEllipsoid ellipsoid}.
     * @since 9.0
     */
    public FieldTransform<DerivativeStructure> getOffsetToInertial(final Frame inertial,
                                                                   final FieldAbsoluteDate<DerivativeStructure> date,
                                                                   final DSFactory factory,
                                                                   final int eastOffsetIndex,
                                                                   final int northOffsetIndex,
                                                                   final int zenithOffsetIndex)
        throws OrekitException {

        final Field<DerivativeStructure> field = factory.getDerivativeField();
        final TopocentricFrame frame  = getOffsetFrame();
        final Frame bodyFrame = baseFrame.getParent();

        // offset frame origin
        final Transform offsetToBody = frame.getTransformTo(bodyFrame, (AbsoluteDate) null);
        final Vector3D  offsetOrigin = offsetToBody.transformPosition(Vector3D.ZERO);
        final DerivativeStructure eastZDS = eastOffsetIndex < 0 ?
                                            factory.constant(0.0) :
                                            factory.variable(eastOffsetIndex,   0.0);
        final FieldVector3D<DerivativeStructure> zeroEast = new FieldVector3D<>(eastZDS, baseFrame.getEast());
        final DerivativeStructure northZDS = northOffsetIndex < 0 ?
                                             factory.constant(0.0) :
                                             factory.variable(northOffsetIndex,   0.0);
        final FieldVector3D<DerivativeStructure> zeroNorth = new FieldVector3D<>(northZDS, baseFrame.getNorth());
        final DerivativeStructure zenithZDS = zenithOffsetIndex < 0 ?
                                              factory.constant(0.0) :
                                              factory.variable(zenithOffsetIndex,   0.0);
        final FieldVector3D<DerivativeStructure> zeroZenith = new FieldVector3D<>(zenithZDS, baseFrame.getZenith());
        final FieldVector3D<DerivativeStructure> offsetOriginDS =
                zeroEast.add(zeroNorth).add(zeroZenith).add(offsetOrigin);

        // vectors changes due to offset in the meridian plane
        // (we are in fact only interested in the derivatives parts, not the values)
        final Vector3D meridianCenter = centerOfCurvature(offsetOrigin, frame.getEast());
        final FieldVector3D<DerivativeStructure> meridianCenterToOffset =
                        zeroNorth.add(offsetOrigin).subtract(meridianCenter);
        final FieldVector3D<DerivativeStructure> meridianZ = meridianCenterToOffset.normalize();
        FieldVector3D<DerivativeStructure>       meridianE = FieldVector3D.crossProduct(Vector3D.PLUS_K, meridianZ);
        if (meridianE.getNormSq().getValue() < Precision.SAFE_MIN) {
            // this should never happen, this case is present only for the sake of defensive programming
            meridianE = FieldVector3D.getPlusJ(field);
        } else {
            meridianE = meridianE.normalize();
        }

        // vectors changes due to offset in the transverse plane
        // (we are in fact only interested in the derivatives parts, not the values)
        final Vector3D transverseCenter = centerOfCurvature(offsetOrigin, frame.getNorth());
        final FieldVector3D<DerivativeStructure> transverseCenterToOffset =
                        zeroEast.add(offsetOrigin).subtract(transverseCenter);
        final FieldVector3D<DerivativeStructure> transverseZ = transverseCenterToOffset.normalize();
        FieldVector3D<DerivativeStructure>       transverseE = FieldVector3D.crossProduct(Vector3D.PLUS_K, transverseZ);
        if (transverseE.getNormSq().getValue() < Precision.SAFE_MIN) {
            // this should never happen, this case is present only for the sake of defensive programming
            transverseE = FieldVector3D.getPlusJ(field);
        } else {
            transverseE = transverseE.normalize();
        }

        final FieldVector3D<DerivativeStructure> eastDS   = combine(frame.getEast(),   meridianE, transverseE);
        final FieldVector3D<DerivativeStructure> zenithDS = combine(frame.getZenith(), meridianZ, transverseZ);

        final FieldVector3D<DerivativeStructure> plusI =  FieldVector3D.getPlusI(date.getField());
        final FieldVector3D<DerivativeStructure> plusK =  FieldVector3D.getPlusK(date.getField());
        final FieldTransform<DerivativeStructure> offsetToBodyDS =
                        new FieldTransform<>(date,
                                             new FieldTransform<>(date,
                                                                  new FieldRotation<>(plusI, plusK, eastDS, zenithDS),
                                                                  FieldVector3D.getZero(field)),
                                             new FieldTransform<>(date, offsetOriginDS));

        final FieldTransform<DerivativeStructure> bodyToInertDS = bodyFrame.getTransformTo(inertial, date);

        return new FieldTransform<>(date, offsetToBodyDS, bodyToInertDS);

    }

    /** Get the center of curvature of the ellipsoid below a point.
     * @param point point under which we want the center of curvature
     * @param normal normal to the plane into which we want the center of curvature
     * @return center of curvature of the ellipsoid surface, below the point and
     * in the specified plane
     * @exception OrekitException if some frame transforms cannot be computed
     * or if the ground station is not defined on a {@link OneAxisEllipsoid ellipsoid}.
     */
    private Vector3D centerOfCurvature(final Vector3D point, final Vector3D normal)
        throws OrekitException {

        // get the ellipsoid
        if (!(baseFrame.getParentShape() instanceof OneAxisEllipsoid)) {
            throw new OrekitException(OrekitMessages.BODY_SHAPE_IS_NOT_AN_ELLIPSOID);
        }
        final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) baseFrame.getParentShape();

        // set up a plane section containing the point and orthogonal to the specified normal vector
        final Ellipse section = ellipsoid.getPlaneSection(point, normal);

        // compute center of curvature in the 2D ellipse
        final Vector2D centerOfCurvature = section.getCenterOfCurvature(section.toPlane(point));

        // convert back to 3D
        return section.toSpace(centerOfCurvature);

    }

    /** Combine a vector and additive derivatives.
     * @param v vector value
     * @param d1 vector derivative (values will be ignored)
     * @param d2 vector derivative (values will be ignored)
     * @return combined vector
     */
    private FieldVector3D<DerivativeStructure> combine(final Vector3D v,
                                                       final FieldVector3D<DerivativeStructure> d1,
                                                       final FieldVector3D<DerivativeStructure> d2) {

        // combine value and derivatives for all coordinates
        final double[] x = d1.getX().add(d2.getX()).getAllDerivatives();
        x[0] = v.getX();
        final double[] y = d1.getY().add(d2.getY()).getAllDerivatives();
        y[0] = v.getY();
        final double[] z = d1.getZ().add(d2.getZ()).getAllDerivatives();
        z[0] = v.getZ();

        // build the combined vector
        return new FieldVector3D<>(d1.getX().getFactory().build(x),
                                   d1.getX().getFactory().build(y),
                                   d1.getX().getFactory().build(z));

    }

}
