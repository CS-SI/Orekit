/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.data;

import java.io.IOException;
import java.io.InputStream;

/** Container for holding named data that can be {@link DataFilter filtered}.
 * <p>
 * This class is a simple container without any processing methods.
 * </p>
 * @see DataFilter
 * @author Luc Maisonobe
 * @since 9.2
 */
public class NamedData {

    /** Name of the data (file name, zip entry name...). */
    private final String name;

    /** Supplier for data stream. */
    private final StreamOpener streamOpener;

    /** Simple constructor.
     * @param name data name
     * @param streamOpener opener for the data stream
     */
    public NamedData(final String name, final StreamOpener streamOpener) {
        this.name         = name;
        this.streamOpener = streamOpener;
    }

    /** Get the name of the data.
     * @return name of the data
     */
    public String getName() {
        return name;
    }

    /** Get the data stream opener.
     * @return data stream opener
     */
    public StreamOpener getStreamOpener() {
        return streamOpener;
    }

    /** Interface for lazy-opening a stream. */
    public interface StreamOpener {
        /** Open the stream.
         * @return opened stream
         * @exception IOException if stream cannot be opened
         */
        InputStream openStream() throws IOException;

    }

}

