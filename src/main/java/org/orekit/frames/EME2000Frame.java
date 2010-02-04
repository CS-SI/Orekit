/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.frames;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

/** EME2000 frame : mean equator at J2000.0.
 * <p>This frame was the standard inertial reference prior to GCRF. It was defined
 * using Lieske precession-nutation model for Earth. This frame has been superseded
 * by GCRF which is implicitly defined from a few hundred quasars coordinates.<p>
 * <p>The transformation between GCRF and EME2000 is a constant rotation bias.</p>
 * @version $Revision$ $Date$
 * @author Luc Maisonobe
 */
class EME2000Frame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = -1045789793339869819L;

    /** Radians per arcsecond. */
    private static final double RADIANS_PER_ARC_SECOND = Math.PI / (180.0 * 3600.0);

    /** Obliquity of the ecliptic. */
    private static final double EPSILON_0 = 84381.44 * RADIANS_PER_ARC_SECOND;

    /** Bias in longitude. */
    private static final double D_PSI_B = -0.041775 * RADIANS_PER_ARC_SECOND;

    /** Bias in obliquity. */
    private static final double D_EPSILON_B = -0.0068192 * RADIANS_PER_ARC_SECOND;

    /** Right Ascension of the 2000 equinox in ICRS frame. */
    private static final double ALPHA_0 = -0.0146 * RADIANS_PER_ARC_SECOND;

    /** Simple constructor.
     * @param name name of the frame
     */
    protected EME2000Frame(final String name) {

        super(FramesFactory.getGCRF(), null, name, true);

        // build the bias transform
        final Rotation r1 = new Rotation(Vector3D.PLUS_I, D_EPSILON_B);
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -D_PSI_B * Math.sin(EPSILON_0));
        final Rotation r3 = new Rotation(Vector3D.PLUS_K, -ALPHA_0);
        final Rotation bias = r1.applyTo(r2.applyTo(r3));

        // store the bias transform
        setTransform(new Transform(bias));

    }

}
