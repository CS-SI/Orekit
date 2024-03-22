/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.gnss;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.LazyLoadedFrames;
import org.orekit.utils.IERSConventions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility for IGS files.
 * @author Luc Maisonobe
 * @since 12.1
 *
 */
public class IGSUtils {

    /** Pattern for frame names with year.
     * @since 12.1
     */
    private static final Pattern
        FRAME_WITH_YEAR = Pattern.compile("(?:ITR|ITRF|IGS|SLR)([0-9]{2})");

    /** Private constructor for a utility class.
     */
    private IGSUtils() {
        // nothing to do
    }

    /** Default string to {@link Frame} conversion for {@link org.orekit.files.sp3.SP3Parser}
     * or {@link org.orekit.files.rinex.clock.RinexClockParser}.
     *
     * <p>
     * This method uses the {@link DataContext#getDefault() default data context}.
     * If the frame names has a form like IGS##, or ITR##, or SLR##, where ##
     * is a two digits number, then this number will be used to build the
     * appropriate {@link ITRFVersion}. Otherwise (for example if name is
     * UNDEF or WGS84), then a default {@link
     * org.orekit.frames.Frames#getITRF(IERSConventions, boolean) ITRF}
     * will be created.
     * </p>
     *
     * @param name of the frame.
     * @return ITRF based on 2010 conventions,
     * with tidal effects considered during EOP interpolation
     * @since 12.1
     */
    @DefaultDataContext
    public static Frame guessFrame(final String name) {
        final LazyLoadedFrames frames = DataContext.getDefault().getFrames();
        final Matcher matcher = FRAME_WITH_YEAR.matcher(name);
        if (matcher.matches()) {
            // this is a frame of the form IGS14, or ITR20, or SLR08, or similar
            final int yy = Integer.parseInt(matcher.group(1));
            return frames.getITRF(ITRFVersion.getITRFVersion(yy),
                                  IERSConventions.IERS_2010, false);
        } else {
            // unkonwn frame 'maybe UNDEF or WGS84
            // we use a default ITRF
            return frames.getITRF(IERSConventions.IERS_2010, false);
        }
    }

}
