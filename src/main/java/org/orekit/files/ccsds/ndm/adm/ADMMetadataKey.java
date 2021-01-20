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
package org.orekit.files.ccsds.ndm.adm;

import org.orekit.files.ccsds.ndm.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseEvent;


/** Keys for {@link ADMMetadata ADM metadata} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ADMMetadataKey {

    /** Comment entry. */
    COMMENT((event, context, metadata) -> event.processAsFreeTextString(metadata::addComment)),

    /** Object name entry. */
    OBJECT_NAME((event, context, metadata) -> event.processAsNormalizedString(metadata::setObjectName)),

    /** Object ID entry. */
    OBJECT_ID((event, context, metadata) -> event.processAsNormalizedString(metadata::setObjectID)),

    /** Center name entry. */
    CENTER_NAME((event, context, metadata) -> {
        metadata.setCenterName(event.getNormalizedContent(), context.getDataContext().getCelestialBodies());
    }),

    /** Time system entry. */
    TIME_SYSTEM((event, context, metadata) -> event.processAsTimeScale(metadata::setTimeSystem));

    /** Processing method. */
    private final MetadataEntryProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    ADMMetadataKey(final MetadataEntryProcessor processor) {
        this.processor = processor;
    }

    /** Process one event.
     * @param event event to process
     * @param context parsing context
     * @param metadata metadata to fill
     */
    public void process(final ParseEvent event, final ParsingContext context, final ADMMetadata metadata) {
        processor.process(event, context, metadata);
    }

    /** Interface for processing one event. */
    interface MetadataEntryProcessor {
        /** Process one event.
         * @param event event to process
         * @param context parsing context
         * @param metadata metadata to fill
         */
        void process(ParseEvent event, ParsingContext context, ADMMetadata metadata);
    }

}
