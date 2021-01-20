/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.hipparchus.util.FastMath;
import org.orekit.files.ccsds.ndm.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.EventType;
import org.orekit.files.ccsds.utils.lexical.ParseEvent;

/** Keys for {@link APMData APM Euler angles} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum APMEulerKey {

    /** Block wrapping element in XML files. */
    eulerElementsThree((event, context, data) -> true),

    /** Rotation angles wrapping element in XML files. */
    rotationAngles((event, context, data) -> {
        data.setInRotationAngles(event.getType() == EventType.START);
        return true;
    }),

    /** Rotation rates wrapping element in XML files. */
    rotationRates((event, context, data) -> true),

    /** First rotation angle or first rotation rate. */
    rotation1((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            if (data.inRotationAngles()) {
                data.setRotationAngle(0, FastMath.toRadians(event.getContentAsDouble()));
            } else {
                data.setRotationRate(0, FastMath.toRadians(event.getContentAsDouble()));
            }
        }
        return true;
    }),

    /** Second rotation angle or second rotation rate. */
    rotation2((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            if (data.inRotationAngles()) {
                data.setRotationAngle(1, FastMath.toRadians(event.getContentAsDouble()));
            } else {
                data.setRotationRate(1, FastMath.toRadians(event.getContentAsDouble()));
            }
        }
        return true;
    }),

    /** Third rotation angle or third rotation rate. */
    rotation3((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            if (data.inRotationAngles()) {
                data.setRotationAngle(2, FastMath.toRadians(event.getContentAsDouble()));
            } else {
                data.setRotationRate(2, FastMath.toRadians(event.getContentAsDouble()));
            }
        }
        return true;
    }),

    /** Comment entry. */
    COMMENT((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            if (data.getEulerFrameAString() == null) {
                // we are still at block start, we accept comments
                event.processAsFreeTextString(data::addComment);
                return false;
            } else {
                // we have already processed some content in the block
                // the comment belongs to the next block
                return false;
            }
        }
        return true;
    }),

    /** First reference frame entry. */
    EULER_FRAME_A((event, context, data) -> {
        event.processAsNormalizedString(data::setEulerFrameAString);
        return true;
    }),

    /** Second reference frame entry. */
    EULER_FRAME_B((event, context, data) -> {
        event.processAsNormalizedString(data::setEulerFrameBString);
        return true;
    }),

    /** Rotation direction entry. */
    EULER_DIR((event, context, data) -> {
        event.processAsNormalizedString(data::setEulerDirection);
        return true;
    }),

    /** Rotation sequence entry. */
    EULER_ROT_SEQ((event, context, data) -> {
        event.processAsNormalizedString(data::setEulerRotSeq);
        return true;
    }),

    /** Reference frame for rate entry. */
    RATE_FRAME((event, context, data) -> {
        event.processAsNormalizedString(data::setRateFrameString);
        return true;
    }),

    /** X body rotation angle entry. */
    X_ANGLE((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            data.setRotationAngle(0, FastMath.toRadians(event.getContentAsDouble()));
        }
        return true;
    }),

    /** Y body rotation angle entry. */
    Y_ANGLE((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            data.setRotationAngle(1, FastMath.toRadians(event.getContentAsDouble()));
        }
        return true;
    }),

    /** Z body rotation angle entry. */
    Z_ANGLE((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            data.setRotationAngle(2, FastMath.toRadians(event.getContentAsDouble()));
        }
        return true;
    }),

    /** X body rotation rate entry. */
    X_RATE((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            data.setRotationRate(0, FastMath.toRadians(event.getContentAsDouble()));
        }
        return true;
    }),

    /** Y body rotation rate entry. */
    Y_RATE((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            data.setRotationRate(1, FastMath.toRadians(event.getContentAsDouble()));
        }
        return true;
    }),

    /** Z body rotation rate entry. */
    Z_RATE((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            data.setRotationRate(2, FastMath.toRadians(event.getContentAsDouble()));
        }
        return true;
    });

    /** Processing method. */
    private final EulerEntryProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    APMEulerKey(final EulerEntryProcessor processor) {
        this.processor = processor;
    }

    /** Process one event.
     * @param event event to process
     * @param context parsing context
     * @param data data to fill
     * @return true of event was accepted
     */
    public boolean process(final ParseEvent event, final ParsingContext context, final APMEuler data) {
        return processor.process(event, context, data);
    }

    /** Interface for processing one event. */
    interface EulerEntryProcessor {
        /** Process one event.
         * @param event event to process
         * @param context parsing context
         * @param data data to fill
         * @return true of event was accepted
         */
        boolean process(ParseEvent event, ParsingContext context, APMEuler data);
    }

}
