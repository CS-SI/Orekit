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
package org.orekit.data;

import java.util.zip.GZIPInputStream;

/** Filter for gzip compressed data.
 * @author Luc Maisonobe
 * @since 9.2
 */
public class GzipFilter implements DataFilter {

    /** Suffix for gzip compressed files. */
    private static final String SUFFIX = ".gz";

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public GzipFilter() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public DataSource filter(final DataSource original) {
        final String            oName   = original.getName();
        final DataSource.Opener oOpener = original.getOpener();
        if (oName.endsWith(SUFFIX)) {
            final String                  fName   = oName.substring(0, oName.length() - SUFFIX.length());
            final DataSource.StreamOpener fOpener = () -> new GZIPInputStream(oOpener.openStreamOnce());
            return new DataSource(fName, fOpener);
        } else {
            return original;
        }
    }

}
