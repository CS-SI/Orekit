/* Copyright 2024-2025 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv;

import java.io.IOException;

/**
 * Writer class that outputs {@link IIRVMessage} data to an output stream.
 *
 * @author Nick LaFarge
 * @see IIRVFileWriter
 * @since 13.0
 */
public class StreamingIIRVFileWriter {

    /** Output stream. */
    private final Appendable writer;

    /** Setting for when message metadata terms appear in the created IIRV message. */
    private final IIRVMessage.IncludeMessageMetadata includeMessageMetadataSetting;

    /**
     * Create an IIRV writer that streams data to the given output stream.
     *
     * @param writer                        the output stream for the IIRV file.
     * @param includeMessageMetadataSetting Setting for when message metadata terms appear in the created IIRV message
     */
    public StreamingIIRVFileWriter(final Appendable writer, final IIRVMessage.IncludeMessageMetadata includeMessageMetadataSetting) {
        this.writer = writer;
        this.includeMessageMetadataSetting = includeMessageMetadataSetting;
    }

    /**
     * Write the passed in {@link IIRVMessage} using the passed in {@link Appendable}.
     *
     * @param iirvMessage a an {@link IIRVMessage} instance populated with {@link IIRVVector} terms.
     * @throws IOException if any buffer writing operations fail
     */
    public void writeIIRVMessage(final IIRVMessage iirvMessage) throws IOException {
        writer.append(iirvMessage.toMessageString(includeMessageMetadataSetting));
    }

    /**
     * Gets the setting for when message metadata terms appear in the created IIRV message.
     *
     * @return setting for when message metadata terms appear in the created IIRV message
     */
    public IIRVMessage.IncludeMessageMetadata getIncludeMessageMetadataSetting() {
        return includeMessageMetadataSetting;
    }
}
