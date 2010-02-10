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

import org.orekit.errors.OrekitException;

/** This class holds an Earth Orientation Parameters entry (IAU2000).
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class EOP2000Entry extends EOPEntry {

    /** Serializable UID. */
    private static final long serialVersionUID = -4943721984721570383L;

    /** X component of pole motion. */
    private final double x;

    /** Y component of pole motion. */
    private final double y;

   /** Simple constructor.
    * @param mjd entry date (modified julian day, 00h00 UTC scale)
    * @param dt UT1-UTC in seconds
    * @param lod length of day
    * @param x X component of pole motion
    * @param y Y component of pole motion
    * @exception OrekitException if UTC time scale cannot be retrieved
    */
    public EOP2000Entry(final int mjd, final double dt, final double lod,
                        final double x, final double y)
        throws OrekitException {
        super(mjd, dt, lod);
        this.x = x;
        this.y = y;
    }

    /** Get the X component of the pole motion.
     * @return X component of pole motion
     */
    public double getX() {
        return x;
    }

    /** Get the Y component of the pole motion.
     * @return Y component of pole motion
     */
    public double getY() {
        return y;
    }

}
