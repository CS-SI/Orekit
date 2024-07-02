/* Contributed in the public domain.
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
package org.orekit.files.ccsds.definitions;

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.CelestialBody;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * A reference frame created from the {@code REF_FRAME} and {@code CENTER_NAME} is a CCSDS
 * OPM, OMM, or OEM file.
 *
 * @author Evan Ward
 */
public class ModifiedFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 20170619L;

    /** Reference frame used to create this frame. */
    private final CelestialBodyFrame refFrame;

    /** Value of the CENTER_NAME keyword in the ODM file used to create this frame. */
    private final String centerName;

    /**
     * Create a CCSDS reference frame by changing the origin of an existing frame.
     *
     * @param frame      the existing frame that specifies the orientation.
     * @param refFrame   the reference frame used to create this frame.
     * @param body       the new origin.
     * @param centerName the value of the {@code CENTER_NAME} key word used to create
     *                   {@code body}.
     */
    public ModifiedFrame(final Frame frame,
                         final CelestialBodyFrame refFrame,
                         final CelestialBody body,
                         final String centerName) {
        super(frame, new OriginTransformProvider(body, frame),
              body.getName() + "/" + frame.getName(), frame.isPseudoInertial());
        this.refFrame   = refFrame;
        this.centerName = centerName;
    }

    /**
     * Get the CCSDS reference frame.
     *
     * @return the reference frame used to create this frame.
     */
    public CelestialBodyFrame getRefFrame() {
        return refFrame;
    }

    /**
     * Get the CCSDS center name.
     *
     * @return the value of the {@code CENTER_NAME} key word used to specify the origin of
     * this frame.
     */
    public String getCenterName() {
        return centerName;
    }

    /** Transform provider for {@link ModifiedFrame}. */
    private static class OriginTransformProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170619L;

        /** The new origin. */
        private final CelestialBody body;

        /** The original frame, specifying the orientation. */
        private final Frame frame;

        /**
         * Create a transform provider to change the origin of an existing frame.
         *
         * @param frame the existing frame that specifies the orientation.
         * @param body  the new origin.
         */
        OriginTransformProvider(final CelestialBody body, final Frame frame) {
            this.body = body;
            this.frame = frame;
        }

        @Override
        public Transform getTransform(final AbsoluteDate date) {
            return new Transform(date, body.getPVCoordinates(date, frame).negate());
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(
                final FieldAbsoluteDate<T> date) {
            return new FieldTransform<>(
                    date,
                    body.getPVCoordinates(date, frame).negate());
        }

    }

}
