/* Copyright 2002-2013 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.NutationFunction;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Celestial Intermediate Reference Frame 2000.
 * <p>This provider includes precession effects according to either the IAU 2006 precession
 * (also known as Nicole Capitaines's P03 precession theory) and IAU 2000A_R06 nutation
 * for IERS 2010 conventions or the IAU 2000A precession-nutation model for IERS 2003
 * conventions. These models replaced the older IAU-76 precession (Lieske) and IAU-80
 * theory of nutation (Wahr) which were used in the classical equinox-based paradigm.
 * It <strong>must</strong> be used with the Earth Rotation Angle (REA) defined by
 * Capitaine's model and <strong>not</strong> IAU-82 sidereal
 * time which is consistent with the older models only.</p>
 * <p>Its parent frame is the GCRF frame.<p>
 */
class CIRFProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130728L;

   /** Generator for fundamental nutation arguments. */
    private final FundamentalNutationArguments nutationArguments;

    /** Pole position (X). */
    private final NutationFunction xFunction;

    /** Pole position (Y). */
    private final NutationFunction yFunction;

    /** Pole position (S + XY/2). */
    private final NutationFunction sxy2Function;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     * @see Frame
     */
    public CIRFProvider(final IERSConventions conventions)
        throws OrekitException {

        // load the nutation model
        nutationArguments = conventions.getNutationArguments();
        xFunction         = conventions.getXFunction();
        yFunction         = conventions.getYFunction();
        sxy2Function      = conventions.getSXY2XFunction();

    }

    /** Get the transform from GCRF to CIRF2000 at the specified date.
     * <p>The transform considers the nutation and precession effects from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        final BodiesElements elements = nutationArguments.evaluateAll(date);

        // position of the Celestial Intermediate Pole (CIP)
        final double xCurrent =    xFunction.value(elements);
        final double yCurrent =    yFunction.value(elements);

        // position of the Celestial Intermediate Origin (CIO)
        final double sCurrent = sxy2Function.value(elements) - xCurrent * yCurrent / 2;

        // set up the bias, precession and nutation rotation
        final double x2Py2  = xCurrent * xCurrent + yCurrent * yCurrent;
        final double zP1    = 1 + FastMath.sqrt(1 - x2Py2);
        final double r      = FastMath.sqrt(x2Py2);
        final double sPe2   = 0.5 * (sCurrent + FastMath.atan2(yCurrent, xCurrent));
        final double cos    = FastMath.cos(sPe2);
        final double sin    = FastMath.sin(sPe2);
        final double xPr    = xCurrent + r;
        final double xPrCos = xPr * cos;
        final double xPrSin = xPr * sin;
        final double yCos   = yCurrent * cos;
        final double ySin   = yCurrent * sin;
        final Rotation bpn  = new Rotation(zP1 * (xPrCos + ySin), -r * (yCos + xPrSin),
                                           r * (xPrCos - ySin), zP1 * (yCos - xPrSin),
                                           true);

        return new Transform(date, bpn, Vector3D.ZERO);

    }

}
