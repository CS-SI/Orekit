/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.forces;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.ThirdBody;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

/** Sun model.
 * The position model is the Newcomb theory.
 * @author &Eacute;douard Delente
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class Sun extends ThirdBody {

    /** Serializable UID. */
    private static final long serialVersionUID = 6780721457181916297L;

    /** Reference date. */
    private static final AbsoluteDate REFERENCE_DATE =
        new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH, 864000000.0);

    /** Transform from Veis1950 to J2000. */
    private final Transform transform;

    /** Simple constructor.
     */
    public Sun() {
        super(6.96e8, 1.32712440e20);
        Transform t;
        try {
            t  = Frame.getVeis1950().getTransformTo(Frame.getJ2000(), REFERENCE_DATE);
        } catch (OrekitException e) {
            // should not happen
            t = Transform.IDENTITY;
        }
        transform = t;
    }

    /** Gets the position of the Sun in the selected Frame.
     * <p>The position model is the Newcomb theory
     * as used in the MSLIB library.</p>
     * @param date date
     * @param frame the frame where to define the position
     * @return position of the sun (m) in the J2000 Frame
     * @exception OrekitException if a frame conversion cannot be computed
     */
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        final double t = date.minus(REFERENCE_DATE) / 86400.0;
        final double f = Math.toRadians(225.768 + 13.2293505 * t);
        final double d = Math.toRadians(11.786 + 12.190749 * t);
        final double xlp = Math.toRadians(134.003 + 0.9856 * t);
        final double g = Math.toRadians(282.551 + 0.000047 * t);
        final double e = Math.toRadians(23.44223 - 3.5626E-07 * t);
        final double ce = Math.cos(e);
        final double se = Math.sin(e);
        final double rot = 0.6119022e-6 * date.minus(AbsoluteDate.FIFTIES_EPOCH) / 86400.0;
        final double cr = Math.cos(rot);
        final double sr = Math.sin(rot);

        // Newcomb's theory
        final double cl = (99972.0 * Math.cos(xlp + g) +
                           1671.0 * Math.cos(xlp + xlp + g) -
                           1678.0 * Math.cos(g) +
                           32.0 * Math.cos(3.0 * xlp + g) +
                           Math.cos(4.0 * xlp + g) +
                           2.0 * Math.cos(xlp + d + g) -
                           4.0 * Math.cos(g - xlp) -
                           2.0 * Math.cos(xlp - d + g) +
                           4.0 * Math.cos(f - d) -
                           4.0 * Math.cos(xlp + xlp - f + d + g + g)) * 1.0e-5;

        final double sl = (99972.0 * Math.sin(xlp + g) +
                           1671.0 * Math.sin(xlp + xlp + g) -
                           1678.0 * Math.sin(g) +
                           32.0 * Math.sin(3.0 * xlp + g) +
                           Math.sin(4.0 * xlp + g) +
                           2.0 * Math.sin(xlp + d + g) -
                           4.0 * Math.sin(g - xlp) -
                           2.0 * Math.sin(xlp - d + g) +
                           4.0 * Math.sin(f - d) -
                           4.0 * Math.sin(xlp + xlp - f + d + g + g)) * 1.0e-5;

        final double q = Math.sqrt(cl * cl + sl * sl);
        final double sx = cl / q;
        final double sy = sl * ce / q;
        final Vector3D centralSun =
            transform.transformVector(new Vector3D(sx * cr + sy * sr,
                                                   sy * cr - sx * sr,
                                                   sl * se / q));
        final double dasr = 1672.2 * Math.cos(xlp) +
                            28.0 * Math.cos(xlp + xlp) -
                            0.35 * Math.cos(d);

        final Vector3D posInJ2000 =
            new Vector3D(1000.0 * 149597870.0 / (1.0 + 1.E-05 * dasr), centralSun);

        return Frame.getJ2000().getTransformTo(frame, date).transformPosition(posInJ2000);

    }

}
