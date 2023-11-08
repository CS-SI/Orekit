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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * Attitude law defined by fixed Roll, Pitch and Yaw angles (in any order)
 * with respect to a local orbital frame.

 * <p>
 * The attitude provider is defined as a rotation offset from some local orbital frame.
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class LofOffset implements AttitudeProvider {

    /** Local Orbital Frame. */
    private final LOF lof;

    /** Rotation from local orbital frame.  */
    private final Rotation offset;

    /** Inertial frame with respect to which orbit should be computed. */
    private final Frame inertialFrame;

    /** Create a LOF-aligned attitude.
     * <p>
     * Calling this constructor is equivalent to call
     * {@code LofOffset(inertialFrame, LOF, RotationOrder.XYZ, 0, 0, 0)}
     * </p>
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @param lof local orbital frame
     */
    public LofOffset(final Frame inertialFrame, final LOF lof) {
        this(inertialFrame, lof, RotationOrder.XYZ, 0, 0, 0);
    }

    /** Creates new instance.
     * <p>
     * An important thing to note is that the rotation order and angles signs used here
     * are compliant with an <em>attitude</em> definition, i.e. they correspond to
     * a frame that rotate in a field of fixed vectors. So to retrieve the angles
     * provided here from the Hipparchus underlying rotation, one has to either use the
     * {@link RotationConvention#VECTOR_OPERATOR} and <em>revert</em> the rotation, or
     * to use {@link RotationConvention#FRAME_TRANSFORM} as in the following code snippet:
     * </p>
     * <pre>
     *   LofOffset law          = new LofOffset(inertial, LOF, order, alpha1, alpha2, alpha3);
     *   Rotation  offsetAtt    = law.getAttitude(orbit).getRotation();
     *   Rotation  alignedAtt   = new LofOffset(inertial, LOF).getAttitude(orbit).getRotation();
     *   Rotation  offsetProper = offsetAtt.compose(alignedAtt.revert(), RotationConvention.VECTOR_OPERATOR);
     *
     *   // note the call to revert and the conventions in the following statement
     *   double[] anglesV = offsetProper.revert().getAngles(order, RotationConvention.VECTOR_OPERATOR);
     *   System.out.format(Locale.US, "%f == %f%n", alpha1, anglesV[0]);
     *   System.out.format(Locale.US, "%f == %f%n", alpha2, anglesV[1]);
     *   System.out.format(Locale.US, "%f == %f%n", alpha3, anglesV[2]);
     *
     *   // note the conventions in the following statement
     *   double[] anglesF = offsetProper.getAngles(order, RotationConvention.FRAME_TRANSFORM);
     *   System.out.format(Locale.US, "%f == %f%n", alpha1, anglesF[0]);
     *   System.out.format(Locale.US, "%f == %f%n", alpha2, anglesF[1]);
     *   System.out.format(Locale.US, "%f == %f%n", alpha3, anglesF[2]);
     * </pre>
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @param lof local orbital frame
     * @param order order of rotations to use for (alpha1, alpha2, alpha3) composition
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     */
    public LofOffset(final Frame inertialFrame, final LOF lof,
                     final RotationOrder order, final double alpha1,
                     final double alpha2, final double alpha3) {
        this.lof    = lof;
        this.offset = new Rotation(order, RotationConvention.VECTOR_OPERATOR, alpha1, alpha2, alpha3).revert();
        if (!inertialFrame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME,
                                      inertialFrame.getName());
        }
        this.inertialFrame = inertialFrame;
    }


    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {

        // construction of the local orbital frame, using PV from inertial frame
        final PVCoordinates pv = pvProv.getPVCoordinates(date, inertialFrame);
        final Transform inertialToLof = lof.transformFromInertial(date, pv);

        // take into account the specified start frame (which may not be an inertial one)
        final Transform frameToInertial = frame.getTransformTo(inertialFrame, date);
        final Transform frameToLof = new Transform(date, frameToInertial, inertialToLof);

        // compose with offset rotation
        return new Attitude(date, frame,
                            offset.compose(frameToLof.getRotation(), RotationConvention.VECTOR_OPERATOR),
                            offset.applyTo(frameToLof.getRotationRate()),
                            offset.applyTo(frameToLof.getRotationAcceleration()));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                            final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {

        // construction of the local orbital frame, using PV from inertial frame
        final FieldPVCoordinates<T> pv = pvProv.getPVCoordinates(date, inertialFrame);
        final FieldTransform<T> inertialToLof = lof.transformFromInertial(date, pv);

        // take into account the specified start frame (which may not be an inertial one)
        final FieldTransform<T> frameToInertial = frame.getTransformTo(inertialFrame, date);
        final FieldTransform<T> frameToLof = new FieldTransform<>(date, frameToInertial, inertialToLof);

        // compose with offset rotation
        return new FieldAttitude<>(date, frame,
                                   frameToLof.getRotation().compose(offset, RotationConvention.FRAME_TRANSFORM),
                                   FieldRotation.applyTo(offset, frameToLof.getRotationRate()),
                                   FieldRotation.applyTo(offset, frameToLof.getRotationAcceleration()));

    }

    /** {@inheritDoc} */
    @Override
    public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        // construction of the local orbital frame, using PV from inertial frame
        final PVCoordinates pv = pvProv.getPVCoordinates(date, inertialFrame);
        final Rotation inertialToLof = lof.rotationFromInertial(date, pv);

        // take into account the specified start frame (which may not be an inertial one)
        final RotationConvention rotationConvention = RotationConvention.FRAME_TRANSFORM;
        final Rotation frameToInertial = frame.getStaticTransformTo(inertialFrame, date).getRotation();
        final Rotation frameToLof = frameToInertial.compose(inertialToLof, rotationConvention);

        // compose with offset rotation
        return frameToLof.compose(offset, rotationConvention);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                    final FieldAbsoluteDate<T> date,
                                                                                    final Frame frame) {
        // construction of the local orbital frame, using PV from inertial frame
        final FieldPVCoordinates<T> pv = pvProv.getPVCoordinates(date, inertialFrame);
        final Field<T> field = date.getField();
        final FieldRotation<T> inertialToLof = lof.rotationFromInertial(field, date, pv);

        // take into account the specified start frame (which may not be an inertial one)
        final RotationConvention rotationConvention = RotationConvention.FRAME_TRANSFORM;
        final FieldRotation<T> frameToInertial = frame.getStaticTransformTo(inertialFrame, date).getRotation();
        final FieldRotation<T> frameToLof = frameToInertial.compose(inertialToLof, rotationConvention);

        // compose with offset rotation
        return frameToLof.compose(offset, rotationConvention);
    }
}
