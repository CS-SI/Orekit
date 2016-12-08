/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.attitudes;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
/**
 * Attitude law defined by fixed Roll, Pitch and Yaw angles (in any order)
 * with respect to a local orbital frame.

 * <p>
 * The attitude provider is defined as a rotation offset from some local orbital frame.
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class FieldLofOffset<T extends RealFieldElement<T>> implements FieldAttitudeProvider<T> {


    /** Type of Local Orbital Frame. */
    private LOFType type;

    /** Rotation from local orbital frame.  */
    private final FieldRotation<T> offset;

    /** Inertial frame with respect to which orbit should be computed. */
    private final Frame inertialFrame;

    /** Create a LOF-aligned attitude.
     * <p>
     * Calling this constructor is equivalent to call
     * {@code LofOffset(inertialFrame, LOFType, RotationOrder.XYZ, 0, 0, 0)}
     * </p>
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @param type type of Local Orbital Frame
     * @param field field used as default
     * @exception OrekitException if inertialFrame is not a pseudo-inertial frame
     */
    public FieldLofOffset(final Frame inertialFrame, final LOFType type, final Field<T> field) throws OrekitException {
        this(inertialFrame, type, RotationOrder.XYZ, field.getZero(), field.getZero(), field.getZero());
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
     *   LofOffset law          = new LofOffset(inertial, lofType, order, alpha1, alpha2, alpha3);
     *   Rotation  offsetAtt    = law.getAttitude(orbit).getRotation();
     *   Rotation  alignedAtt   = new LofOffset(inertial, lofType).getAttitude(orbit).getRotation();
     *   Rotation  offsetProper = offsetAtt.compose(alignedAtt.revert(), RotationConvention.VECTOR_OPERATOR);
     *
     *   // note the call to revert and the conventions in the following statement
     *   double[] anglesV = offsetProper.revert().getAngles(order, RotationConvention.VECTOR_OPERATOR);
     *   System.out.println(alpha1 + " == " + anglesV[0]);
     *   System.out.println(alpha2 + " == " + anglesV[1]);
     *   System.out.println(alpha3 + " == " + anglesV[2]);
     *
     *   // note the conventions in the following statement
     *   double[] anglesF = offsetProper.getAngles(order, RotationConvention.FRAME_TRANSFORM);
     *   System.out.println(alpha1 + " == " + anglesF[0]);
     *   System.out.println(alpha2 + " == " + anglesF[1]);
     *   System.out.println(alpha3 + " == " + anglesF[2]);
     * </pre>
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @param type type of Local Orbital Frame
     * @param order order of rotations to use for (alpha1, alpha2, alpha3) composition
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     * @exception OrekitException if inertialFrame is not a pseudo-inertial frame
     */
    public FieldLofOffset(final Frame inertialFrame, final LOFType type,
                          final RotationOrder order, final T alpha1,
                          final T alpha2, final T alpha3) throws OrekitException {
        this.type = type;
        this.offset = new FieldRotation<T>(order, RotationConvention.VECTOR_OPERATOR, alpha1, alpha2, alpha3).revert();
        if (!inertialFrame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME,
                                      inertialFrame.getName());
        }
        this.inertialFrame = inertialFrame;

    }


    /** {@inheritDoc} */ //TODO TO ASK
    public FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                final FieldAbsoluteDate<T> date, final Frame frame)
        throws OrekitException {

        // construction of the local orbital frame, using PV from inertial frame
        final FieldPVCoordinates<T> pv = pvProv.getPVCoordinates(date, inertialFrame);
        final Transform inertialToLof = type.transformFromInertial(date.toAbsoluteDate(), pv.toPVCoordinates());

        // take into account the specified start frame (which may not be an inertial one)
        final Transform frameToInertial = frame.getTransformTo(inertialFrame, date.toAbsoluteDate());
        final Transform frameToLof = new Transform(date.toAbsoluteDate(), frameToInertial, inertialToLof);

        // compose with offset rotation
        return new FieldAttitude<T>(date, frame,
                            offset.compose(frameToLof.getRotation(), RotationConvention.VECTOR_OPERATOR),
                            offset.applyTo(frameToLof.getRotationRate()),
                            offset.applyTo(frameToLof.getRotationAcceleration()));

    }


}
