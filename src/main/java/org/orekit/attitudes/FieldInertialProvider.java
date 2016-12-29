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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;


/**
 * This class handles an inertial attitude provider.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class FieldInertialProvider<T extends RealFieldElement<T>> implements FieldAttitudeProvider<T> {

    /** Fixed satellite frame. */
    private final Frame satelliteFrame;

    /** Creates new instance.
     * @param rotation rotation from EME2000 to the desired satellite frame
     */
    public FieldInertialProvider(final FieldRotation<T> rotation) {
        satelliteFrame =
            new Frame(FramesFactory.getEME2000(), new Transform(AbsoluteDate.J2000_EPOCH, rotation.toRotation()),
                      null, false);
    }
    /** Dummy attitude provider, perfectly aligned with the EME2000 frame.
     * @param field field used by default=
     * */
    public FieldInertialProvider(final Field<T> field) {
        this(new FieldRotation<T>(field.getOne(), field.getZero(), field.getZero(), field.getZero(), false));
    };
    /** Dummy attitude provider, perfectly aligned with the EME2000 frame.
     * @param field field used by default
     * @return FieldRotation<T> instance of EME2000_ALIGNED
     * */
    public final FieldInertialProvider<T> EME2000_ALIGNED(final Field<T> field) {
        final FieldRotation<T> FR = new FieldRotation<T>(field.getOne(), field.getZero(), field.getZero(), field.getZero(), false);

        return new FieldInertialProvider<T>(FR);
    };

    /** {@inheritDoc} */
    public FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                final FieldAbsoluteDate<T> date, final Frame frame)
        throws OrekitException {
        final Field<T> field = pvProv.getPVCoordinates(date, frame).getPosition().getAlpha().getField();
        final Transform t = frame.getTransformTo(satelliteFrame, date.toAbsoluteDate());
        return new FieldAttitude<T>(date, frame, t.getRotation(), t.getRotationRate(), t.getRotationAcceleration(), field);
    }
}
