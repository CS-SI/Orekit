/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.sp3;

import java.io.IOException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.data.DataFilter;
import org.orekit.data.DataSource;
import org.orekit.data.LineOrientedFilteringReader;

/** Filter for some non-official files from CDDIS.
 * <p>
 * Some files produced by UKRI/NERC/British Geological Survey Space Geodesy Facility (SGF)
 * claim to be SP3c but are really SP3d since they have more than 4 comments lines. This
 * filter can be used to parse them.
 * </p>
 * @see <a href="https://forum.orekit.org/t/solved-sp3-precise-orbit-file-is-not-compliant-with-sp3-format-c-extra-comment-line">SP3
 * precise orbit file is not compliant with SP3 format c (extra comment line)</a>
 * @since 12.1
 */
public class NsgfV00Filter implements DataFilter {

    /** Default regular expression for NSGF V00 files. */
    public static final String DEFAULT_V00_PATTERN = ".*nsgf\\.orb\\.[^.]+\\.v00\\.sp3$";

    /** Pattern matching file names to which filtering should be applied. */
    private final Pattern pattern;

    /** Renaming function. */
    private final Function<String, String> renaming;

    /** Simple constructor.
     * @param nameRegexp regular expression matching file names to which filtering should be applied
     * @param renaming function to apply for renaming files (and avoid the filter to be applied in infinite recursion)
     */
    public NsgfV00Filter(final String nameRegexp, final Function<String, String> renaming) {
        this.pattern  = Pattern.compile(nameRegexp);
        this.renaming = renaming;
    }

    /** Simple constructor.
     * <p>
     * This uses {@link #DEFAULT_V00_PATTERN} as the regular expression matching files
     * that must be filtered, and replaces "v00" by "v70" to generate the filtered name.
     * </p>
     */
    public NsgfV00Filter() {
        this(DEFAULT_V00_PATTERN, s -> s.replace("v00", "v70"));
    }

    /** {@inheritDoc} */
    @Override
    public DataSource filter(final DataSource original) throws IOException {
        final Matcher matcher = pattern.matcher(original.getName());
        if (matcher.matches()) {
            // this is a v00 file from NSGF
            // we need to parse it as an SP3d file even if it claims being an SP3c file
            final String oName = original.getName();
            final String fName = renaming.apply(oName);
            return new DataSource(fName,
                                  () -> new LineOrientedFilteringReader(oName, original.getOpener().openReaderOnce()) {

                                      /** {@inheritDoc} */
                                      @Override
                                      protected CharSequence filterLine(final int lineNumber, final String originalLine) {
                                          if (lineNumber == 1 && originalLine.startsWith("#c")) {
                                              // the 'c' format marker appears in the first header line
                                              // we replace it by a 'd' format marker
                                              return "#d" + originalLine.substring(2);
                                          } else {
                                              // don't filter any other lines
                                              return originalLine;
                                          }
                                      }

                                  });
        } else {
            // this is a regular file, no need to filter it
            return original;
        }
    }

}

