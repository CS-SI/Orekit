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

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Greenwich True Of Date Frame, also known as True of Date Rotating frame (TDR)
 * or Greenwich Rotating Coordinate frame (GCR).
 * <p> This frame handles the sidereal time according to IAU-82 model.</p>
 * <p> Its parent frame is the {@link TODProvider}.</p>
 * <p> The pole motion is not applied here.</p>
 * @author Pascal Parraud
 * @author Thierry Ceolin
 */
public class GTODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130922L;

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** GMST function. */
    private final TimeFunction<DerivativeStructure> gmstFunction;

    /** GAST function. */
    private final TimeFunction<DerivativeStructure> gastFunction;

    /** Simple constructor.
     * @param conventions conventions to apply
     * @param eopHistory EOP history
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    protected GTODProvider(final IERSConventions conventions, final EOPHistory eopHistory)
        throws OrekitException {
        final UT1Scale ut1 = TimeScalesFactory.getUT1(eopHistory);
        this.eopHistory    = eopHistory;
        this.gmstFunction  = conventions.getGMSTFunction(ut1);
        this.gastFunction  = conventions.getGASTFunction(ut1, eopHistory);
    }

    /** Get the EOP history.
     * @return EOP history
     */
    EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** Get the transform from TOD at specified date.
     * <p>The update considers the Earth rotation from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // compute Greenwich apparent sidereal time, in radians
        final double gast = gastFunction.value(date).getValue();

        // compute true angular rotation of Earth, in rad/s
        final double lod = (eopHistory == null) ? 0.0 : eopHistory.getLOD(date);
        final double omp = AVE * (1 - lod / Constants.JULIAN_DAY);
        final Vector3D rotationRate = new Vector3D(omp, Vector3D.PLUS_K);

        // set up the transform from parent TOD
        return new Transform(date, new Rotation(Vector3D.PLUS_K, -gast), rotationRate);

    }

    /** Get the Greenwich mean sidereal time, in radians.
     * @param date current date
     * @return Greenwich mean sidereal time, in radians
     * @exception OrekitException if UT1 time scale cannot be retrieved
     * @deprecated as of 6.1, replaced by {@link IERSConventions#getGMSTFunction(UT1Scale)}
     */
    @Deprecated
    public double getGMST(final AbsoluteDate date) throws OrekitException {
        return gmstFunction.value(date).getValue();
    }

    /** Get the Greenwich apparent sidereal time, in radians.
     * @param date current date
     * @return Greenwich apparent sidereal time, in radians
     * @exception OrekitException if UT1 time scale cannot be retrieved
     * @deprecated as of 6.1, replaced by {@link IERSConventions#getGASTFunction(UT1Scale)}
     */
    @Deprecated
    public double getGAST(final AbsoluteDate date) throws OrekitException {
        return gastFunction.value(date).getValue();
    }

}
