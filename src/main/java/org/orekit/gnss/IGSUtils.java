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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.Predefined;
import org.orekit.frames.VersionedITRF;
import org.orekit.utils.IERSConventions;

import java.util.Locale;
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
    private static final Pattern EARTH_FRAME_WITH_YEAR = Pattern.compile("(?:IER|ITR|ITRF|IGS|IGb|SLR)([0-9]{2})");

    /** Pattern for GCRF inertial frame.
     * @since 12.1
     */
    private static final Pattern GCRF_FRAME = Pattern.compile(" *GCRF *");

    /** Pattern for EME2000 inertial frame.
     * @since 12.1
     */
    private static final Pattern EME2000_FRAME = Pattern.compile("EME(?:00|2K)");

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
     * </p>
     * <p>
     * Various frame names are supported:
     * </p>
     * <ul>
     *     <li>IER##, ITR##, ITRF##, IGS##, IGb##, or SLR##, where ## is a two digits number,
     *         the number will be used to build the appropriate {@link ITRFVersion}</li>
     *     <li>GCRF (left or right justified) for GCRF inertial frame</li>
     *     <li>EME00 or EME2K for EME2000 inertial frame</li>
     *     <li>for all other names (for example if name is UNDEF or WGS84),
     *     then a default {@link org.orekit.frames.Frames#getITRF(IERSConventions, boolean) ITRF}
     *     frame will be selected</li>
     * </ul>
     * <p>
     * Note that using inertial frames in classical products like SP3 files is non-standard,
     * it is supported by Orekit, but may not be supported by other programs, so they should
     * be used with caution when writing files.
     * </p>
     *
     * @param name of the frame.
     * @return ITRF based on 2010 conventions,
     * with tidal effects considered during EOP interpolation
     * @since 12.1
     */
    @DefaultDataContext
    public static Frame guessFrame(final String name) {
        return guessFrame(DataContext.getDefault().getFrames(), name);
    }

    /** Default string to {@link Frame} conversion for {@link org.orekit.files.sp3.SP3Parser}
     * or {@link org.orekit.files.rinex.clock.RinexClockParser}.
     *
     * <p>
     * Various frame names are supported:
     * </p>
     * <ul>
     *     <li>IER##, ITR##, ITRF##, IGS##, IGb##, or SLR##, where ## is a two digits number,
     *         the number will be used to build the appropriate {@link ITRFVersion}</li>
     *     <li>GCRF (left or right justified) for GCRF inertial frame</li>
     *     <li>EME00 or EME2K for EME2000 inertial frame</li>
     *     <li>for all other names (for example if name is UNDEF or WGS84),
     *     then a default {@link org.orekit.frames.Frames#getITRF(IERSConventions, boolean) ITRF}
     *     frame will be selected</li>
     * </ul>
     * <p>
     * Note that using inertial frames in classical products like SP3 files is non-standard,
     * it is supported by Orekit, but may not be supported by other programs, so they should
     * be used with caution when writing files.
     * </p>
     *
     * @param frames frames factory
     * @param name of the frame.
     * @return guessed frame
     * @since 12.1
     */
    public static Frame guessFrame(final Frames frames, final String name) {
        final Matcher earthMatcher = EARTH_FRAME_WITH_YEAR.matcher(name);
        if (earthMatcher.matches()) {
            // this is a frame of the form IGS14, or ITR20, or SLR08, or similar
            final int yy = Integer.parseInt(earthMatcher.group(1));
            final ITRFVersion itrfVersion = ITRFVersion.getITRFVersion(yy);
            final IERSConventions conventions =
                itrfVersion.getYear() < 2003 ?
                IERSConventions.IERS_1996 :
                (itrfVersion.getYear() < 2010 ? IERSConventions.IERS_2003 : IERSConventions.IERS_2010);
            return frames.getITRF(itrfVersion, conventions, false);
        } else {
            final Matcher gcrfMatcher = GCRF_FRAME.matcher(name);
            if (gcrfMatcher.matches()) {
                // inertial GCRF frame
                return frames.getGCRF();
            } else {
                final Matcher eme2000Matcher = EME2000_FRAME.matcher(name);
                if (eme2000Matcher.matches()) {
                    // inertial EME2000 frame
                    return frames.getEME2000();
                } else {
                    // unknown frame 'maybe UNDEF or WGS84
                    // we use a default ITRF
                    return frames.getITRF(IERSConventions.IERS_2010, false);
                }
            }
        }
    }

    /** Guess a frame name.
     * <p>
     * If the frame is not compatible with {@link #guessFrame(Frames, String)},
     * an exception will be triggered
     * </p>
     * @param frame frame from which we want the name
     * @return name compatible with {@link #guessFrame(Frames, String)}
     * @since 12.1
     */
    public static String frameName(final Frame frame) {
        if (frame instanceof VersionedITRF) {
            final int yy = ((VersionedITRF) frame).getITRFVersion().getYear() % 100;
            return String.format(Locale.US, "IGS%02d", yy);
        } else if (Predefined.GCRF.getName().equals(frame.getName())) {
            return "GCRF";
        } else if (Predefined.EME2000.getName().equals(frame.getName())) {
            return "EME2K";
        } else {
            throw new OrekitException(OrekitMessages.FRAME_NOT_ALLOWED, frame.getName());
        }
    }

}
