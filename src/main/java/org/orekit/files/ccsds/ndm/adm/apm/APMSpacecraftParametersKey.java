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

/** Keys for {@link APMSpacecraftParameters APM spacecraft parameters} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum APMSpacecraftParametersKey {

    /** Block wrapping element in XML files. */
    spacecraftParameters((event, context, data) -> true),

    /** Comment entry. */
    COMMENT((event, context, data) -> {
        if (event.getType() == EventType.ENTRY) {
            if (data.getInertiaRefFrameString() == null) {
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

    /** Inertia reference frame entry. */
    INERTIA_REF_FRAME((event, context, data) -> {
        event.processAsNormalizedString(data::setInertiaRefFrameString);
        return true;
    }),

    /** 1-axis moment of inertia entry. */
    I11((event, context, data) -> {
        event.processAsDouble(data::setI11);
        return true;
    }),

    /** 2-axis moment of inertia entry. */
    I22((event, context, data) -> {
        event.processAsDouble(data::setI22);
        return true;
    }),

    /** 3-axis moment of inertia entry. */
    I33((event, context, data) -> {
        event.processAsDouble(data::setI33);
        return true;
    }),

    /** 1-axis / 2-axis inertia cross product entry. */
    I12((event, context, data) -> {
        event.processAsDouble(data::setI12);
        return true;
    }),

    /** 1-axis / 3-axis inertia cross product entry. */
    I13((event, context, data) -> {
        event.processAsDouble(data::setI13);
        return true;
    }),

    /** 2-axis / 3-axis inertia cross product entry. */
    I23((event, context, data) -> {
        event.processAsDouble(data::setI23);
        return true;
    });

    /** Processing method. */
    private final SpacecraftEntryProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    APMSpacecraftParametersKey(final SpacecraftEntryProcessor processor) {
        this.processor = processor;
    }

    /** Process one event.
     * @param event event to process
     * @param context parsing context
     * @param data data to fill
     * @return true of event was accepted
     */
    public boolean process(final ParseEvent event, final ParsingContext context, final APMSpacecraftParameters data) {
        return processor.process(event, context, data);
    }

    /** Interface for processing one event. */
    interface SpacecraftEntryProcessor {
        /** Process one event.
         * @param event event to process
         * @param context parsing context
         * @param data data to fill
         * @return true of event was accepted
         */
        boolean process(ParseEvent event, ParsingContext context, APMSpacecraftParameters data);
    }

}
