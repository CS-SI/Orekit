/* Contributed in the public domain.
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
package org.orekit.files.ccsds.definitions;

import org.orekit.errors.OrekitException;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.time.AbsoluteDate;

/**
 * An interface for creating an Orekit {@link Frame} from the specification in a CCSDS NDM
 * file. Note that CCSDS uses "frame" to mean only orientation, while Orekit uses "frame"
 * to mean origin and orientation. Some NDM files provide different information, so there
 * are several methods in the interface:
 *
 * <ul>
 *     <li>{@link #buildCcsdsFrame(FrameFacade, AbsoluteDate)} for when only an
 *         orientation is provided. E.g. covariance section of an OEM.
 *     <li>{@link #buildCcsdsFrame(BodyFacade, FrameFacade, AbsoluteDate)} for when a
 *         center and orientation are provided. E.g. in the trajectory section of an OEM.
 * </ul>
 *
 * <p>Notes for implementors: Orekit will shortcut frame transformations if frames are
 * {@code ==}. So for best performance, memoize created frames, similar to how {@link
 * Frames} is implemented. Also, {@link EphemerisSegment#getInertialFrame()} uses the
 * closest frame ancestor by default, so it is better to do translations first, then
 * rotations.
 *
 * @author Evan M. Ward
 * @since 13.1.5
 */
public interface CcsdsFrameMapper {

    /**
     * Create an Orekit {@link Frame} from the alignment specified in a CCSDS NDM.
     *
     * @param orientation the attitude of the returned frame.
     * @param frameEpoch  the epoch of the returned frame, if not intrinsic to the
     *                    definition of the reference frame. May be {@code null} if not
     *                    specified in the file. Many frames will ignore this value.
     * @return a {@link Frame} with the given orientation. Never {@code null}.
     * @throws OrekitException if a frame cannot be constructed for the given
     *                         orientation.
     * @since 13.1.5
     */
    Frame buildCcsdsFrame(FrameFacade orientation,
                          AbsoluteDate frameEpoch);

    /**
     * Create an Orekit {@link Frame} from the center, alignment, and epoch specified in a
     * CCSDS NDM.
     *
     * @param center      the origin of the returned frame.
     * @param orientation the attitude of the returned frame.
     * @param frameEpoch  the epoch of the returned frame, if not intrinsic to the
     *                    definition of the reference frame. May be {@code null} if not
     *                    specified in the file. Many frames will ignore this value.
     * @return a {@link Frame} with the given center and orientation. Never {@code null}.
     * @throws OrekitException if a frame cannot be constructed for the given center and
     *                         orientation.
     * @since 13.1.5
     */
    Frame buildCcsdsFrame(BodyFacade center,
                          FrameFacade orientation,
                          AbsoluteDate frameEpoch);

}
