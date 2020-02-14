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
package org.orekit.files.ccsds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
 * This class stocks all the information of the Attitude Ephemeris Message (AEM) File parsed
 * by AEMParser. It contains the header and a list of Attitude Ephemerides Blocks each
 * containing metadata and a list of ephemerides data lines and optional covariance matrices.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMFile extends ADMFile {

    /** List of ephemeris blocks. */
    private List<AttitudeEphemeridesBlock> attitudeBlocks;

    /** The name of the reference frame specifying one frame of the transformation. */
    private Frame refFrameA;

    /** The name of the reference frame specifying the second portion of the transformation. */
    private Frame refFrameB;

    /** Rotation direction of the attitude. */
    private AttitudeRotationDirection attitudeDir;

    /** Start of total time span covered by attitude data. */
    private AbsoluteDate startTime;

    /** End of total time span covered by attitude data. */
    private AbsoluteDate stopTime;

    /** Start of useable time span covered by attitude data. */
    private AbsoluteDate useableStartTime;

    /** End of useable time span covered by attitude data. */
    private AbsoluteDate useableStopTime;

    /**
     * AEMFile constructor.
     */
    public AEMFile() {
        attitudeBlocks = new ArrayList<AttitudeEphemeridesBlock>();
    }

    /**
     * Add a block to the list of ephemeris blocks.
     */
    void addAttitudeEphemeridesBlock() {
        attitudeBlocks.add(new AttitudeEphemeridesBlock());
    }

    /**
     * Get the list of attitude ephemerides blocks as an unmodifiable list.
     * @return the list of ephemerides blocks
     */
    public List<AttitudeEphemeridesBlock> getAttitudeEphemeridesBlocks() {
        return Collections.unmodifiableList(attitudeBlocks);
    }

    /**
     * The Attitude Ephemerides Blocks class contain metadata
     * and the list of attitude data lines.
     */
    public class AttitudeEphemeridesBlock {

        /** Meta-data for the block. */
        private ODMMetaData metaData;

    }
}
