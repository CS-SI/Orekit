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
import org.orekit.bodies.AbstractCelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/** Moon model.
 * The position model is the Brown theory.
 * @author &Eacute;douard Delente
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class Moon extends AbstractCelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -7705996201568232424L;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double GM = 1.32712440017987e20 / 27068700.387534;

    /** Reference date. */
    private static final AbsoluteDate REFERENCE_DATE =
        new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH, 864000000.0);

    /** Creates a new instance of ThirdBody Moon.
     */
    public Moon() {
        super(GM, "Moon-centric (Brown)", Frame.getEME2000());
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // four points finite differences estimation of the velocity
        final double h = 10.0;
        final Vector3D pm2h = getPosition(new AbsoluteDate(date, -2 * h), frame);
        final Vector3D pm1h = getPosition(new AbsoluteDate(date,     -h), frame);
        final Vector3D pp1h = getPosition(new AbsoluteDate(date,      h), frame);
        final Vector3D pp2h = getPosition(new AbsoluteDate(date,  2 * h), frame);
        final double c = 1.0 / (12 * h);
        final Vector3D estimatedV =
            new Vector3D(c, pm2h, -8 * c, pm1h, 8 * c, pp1h, -c, pp2h);

        // combine position and velocity
        return new PVCoordinates(getPosition(date, frame), estimatedV);

    }

    /** Gets the position of the Moon in the selected frame.
     * <p>The position model is the Brown theory as used in the MSLIB library.</p>
     * @param date current date
     * @param frame the frame where to define the position
     * @return position of the Moon wrt the central body (m)
     * @exception OrekitException if a frame conversion cannot be computed
     */
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        final double t   = date.durationFrom(REFERENCE_DATE) / 86400.0;
        final double f   = Math.toRadians(225.768 + 13.2293505 * t);
        final double xl  = Math.toRadians(185.454 + 13.064992 * t);
        final double d   = Math.toRadians(11.786 + 12.190749 * t);
        final double xlp = Math.toRadians(134.003 + 0.9856 * t);
        final double e   = Math.toRadians(23.44223 - 3.5626e-07 * t);
        final double ce  = Math.cos(e);
        final double se  = Math.sin(e);
        final double rot = 0.6119022e-06 * date.durationFrom(AbsoluteDate.FIFTIES_EPOCH) / 86400.0;
        final double cr  = Math.cos(rot);
        final double sr  = Math.sin(rot);

        // Brown's theory
        final double dl = (10976.0 * Math.sin(xl) -
                           2224.0 * Math.sin(xl - d - d) +
                           1149.0 * Math.sin(d + d) +
                           373.0 * Math.sin(xl + xl) -
                           324.0 * Math.sin(xlp) -
                           200.0 * Math.sin(f + f) -
                           103.0 * Math.sin(xl + xl - d - d) -
                           100.0 * Math.sin(xl + xlp - d - d) +
                           93.0 * Math.sin(xl + d + d) -
                           80.0 * Math.sin(xlp - d - d) +
                           72.0 * Math.sin(xl - xlp) -
                           61.0 * Math.sin(d) -
                           53.0 * Math.sin(xl + xlp) +
                           14.0 * Math.sin(xl - xlp - d - d) +
                           19.0 * Math.sin(xl - f - f) -
                           19.0 * Math.sin(xl - 4.0 * d) +
                           17.0 * Math.sin(3.0 * xl) -
                           27.0 * Math.sin(f + f - d - d) -
                           12.0 * Math.sin(xlp + d + d) -
                           22.0 * Math.sin(xl + f + f) -
                           15.0 * Math.sin(xl + xl - 4.0 * d) +
                           7.0 * Math.sin(xl + xl + d + d) +
                           9.0 * Math.sin(xl - d) -
                           6.0 * Math.sin(3.0 * xl - d - d) +
                           7.0 * Math.sin(4.0 * d) +
                           9.0 * Math.sin(xlp + d) +
                           7.0 * Math.sin(xl - xlp + d + d) +
                           5.0 * Math.sin(xl + xl - xlp)) * 1.0e-5;

        final double b = (8950.0 * Math.sin(f) +
                          490.0 * Math.sin(xl + f) +
                          485.0 * Math.sin(xl - f) -
                          302.0 * Math.sin(f - d - d) -
                          97.0 * Math.sin(xl - f - d - d) -
                          81.0 * Math.sin(xl + f - d - d) +
                          57.0 * Math.sin(f + d + d) -
                          14.0 * Math.sin(xlp + f - d - d) +
                          16.0 * Math.sin(xl - f + d + d) +
                          15.0 * Math.sin(xl + xl - f) +
                          30.0 * Math.sin(xl + xl + f) -
                          6.0 * Math.sin(xlp - f + d + d) -
                          7.0 * Math.sin(xl + xl + f - d - d) +
                          7.0 * Math.sin(xl + f + d + d)) * 1.0e-5;

        final double u = Math.toRadians(68.341 + 13.176397 * t) + dl;
        final double cu = Math.cos(u);
        final double su = Math.sin(u);
        final double cb = Math.cos(b);
        final double sb = Math.sin(b);
        final double rx = cu * cb;
        final double ry = su * cb * ce - sb * se;

        final Vector3D centralMoon =
            new Vector3D(rx * cr + ry * sr, ry * cr - rx * sr, sb * ce + su * cb * se);

        final double dasr = (5450.0 * Math.cos(xl) +
                             1002.0 * Math.cos(xl - d - d) +
                             825.0 * Math.cos(d + d) +
                             297.0 * Math.cos(xl + xl) +
                             90.0 * Math.cos(xl + d + d) +
                             56.0 * Math.cos(xlp - d - d) +
                             42.0 * Math.cos(xl + xlp - d - d) +
                             34.0 * Math.cos(xl - xlp) -
                             12.0 * Math.cos(xlp) -
                             29.0 * Math.cos(d) -
                             21.0 * Math.cos(xl - f - f) +
                             18.0 * Math.cos(xl - 4.0 * d) -
                             28.0 * Math.cos(xl + xlp) +
                             11.0 * Math.cos(xl + xl - 4.0 * d) +
                             18.0 * Math.cos(3.0 * xl) -
                             9.0 * Math.cos(xlp + d + d) -
                             7.0 * Math.cos(xl - xlp - d - d) +
                             7.0 * Math.cos(xl - xlp + d + d) -
                             9.0 * Math.cos(xl + xl - d - d) +
                             8.0 * Math.cos(xl + xl + d + d) +
                             8.0 * Math.cos(4.0 * d)) * 1.0e-5;

        final Vector3D posInVeis = new Vector3D(1000.0 * 384389.3 / (1.0 + dasr), centralMoon);

        return Frame.getVeis1950().getTransformTo(frame, date).transformPosition(posInVeis);

    }

}
