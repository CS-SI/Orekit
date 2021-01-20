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

import org.orekit.files.ccsds.ndm.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.EventType;
import org.orekit.files.ccsds.utils.lexical.ParseEvent;

/** Keys for {@link APMQuaternion APM quaternion} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum APMQuaternionKey {

    /** Block wrapping element in XML files. */
    quaternionState((event, context, data) -> true),

    /** Quaternion wrapping element in XML files. */
    quaternion((event, context, data) -> true),

    /** Quaternion wrapping element in XML files. */
    quaternionRate((event, context, data) -> true),

    /** Comment entry. */
    COMMENT((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            if (data.getEpoch() == null) {
                // we are still at block start, we accept comments
                event.processAsFreeTextString(data::addComment);
                return false;
            } else {
                // we have already processed some content in the block
                // the comment belongs to the next block
                return false;
            }
        } return true;
    }),

    /** Epoch entry. */
    EPOCH((event, context, data) -> {
        event.processAsDate(data::setEpoch, context);
        return true;
    }),

    /** First reference frame entry. */
    Q_FRAME_A((event, context, data) -> {
        event.processAsNormalizedString(data::setQuaternionFrameAString);
        return true;
    }),

    /** Second reference frame entry. */
    Q_FRAME_B((event, context, data) -> {
        event.processAsNormalizedString(data::setQuaternionFrameBString);
        return true;
    }),

    /** Rotation direction entry. */
    Q_DIR((event, context, data) -> {
        event.processAsNormalizedString(data::setAttitudeQuaternionDirection);
        return true;
    }),

    /** Scalar part of the quaternion entry. */
    QC((event, context, data) -> {
        event.processAsDouble(data::setQ0);
        return true;
    }),

    /** First component of the vector part of the quaternion entry. */
    Q1((event, context, data) -> {
        event.processAsDouble(data::setQ1);
        return true;
    }),

    /** Second component of the vector part of the quaternion entry. */
    Q2((event, context, data) -> {
        event.processAsDouble(data::setQ2);
        return true;
    }),

    /** Third component of the vector part of the quaternion entry. */
    Q3((event, context, data) -> {
        event.processAsDouble(data::setQ3);
        return true;
    }),

    /** Scalar part of the quaternion derivative entry. */
    QC_DOT((event, context, data) -> {
        event.processAsDouble(data::setQ0Dot);
        return true;
    }),

    /** First component of the vector part of the quaternion derivative entry. */
    Q1_DOT((event, context, data) -> {
        event.processAsDouble(data::setQ1Dot);
        return true;
    }),

    /** Second component of the vector part of the quaternion derivative entry. */
    Q2_DOT((event, context, data) -> {
        event.processAsDouble(data::setQ2Dot);
        return true;
    }),

    /** Third component of the vector part of the quaternion derivative entry. */
    Q3_DOT((event, context, data) -> {
        event.processAsDouble(data::setQ3Dot);
        return true;
    });

    /** Processing method. */
    private final QuaternionEntryProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    APMQuaternionKey(final QuaternionEntryProcessor processor) {
        this.processor = processor;
    }

    /** Process one event.
     * @param event event to process
     * @param context parsing context
     * @param data data to fill
     * @return true of event was accepted
     */
    public boolean process(final ParseEvent event, final ParsingContext context, final APMQuaternion data) {
        return processor.process(event, context, data);
    }

    /** Interface for processing one event. */
    interface QuaternionEntryProcessor {
        /** Process one event.
         * @param event event to process
         * @param context parsing context
         * @param data data to fill
         * @return true of event was accepted
         */
        boolean process(ParseEvent event, ParsingContext context, APMQuaternion data);
    }

}
