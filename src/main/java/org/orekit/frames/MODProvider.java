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
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.NutationFunction;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Mean Equator, Mean Equinox Frame.
 * <p>This frame handles precession effects according to to selected IERS conventions.</p>
 * <p>Its parent frame is the GCRF frame.<p>
 * <p>It is sometimes called Mean of Date (MoD) frame.<p>
 * @author Pascal Parraud
 */
class MODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130729L;

    /** Generator for fundamental nutation arguments. */
    private final FundamentalNutationArguments nutationArguments;

    /** Function computing the precession angle &zeta;<sub>A</sub>. */
    private final NutationFunction precessionZeta;

    /** Function computing the precession angle &theta;<sub>A</sub>. */
    private final NutationFunction precessionTheta;

    /** Function computing the precession angle z<sub>A</sub>. */
    private final NutationFunction precessionZ;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @exception OrekitException if IERS conventions tables cannot be read
     */
    public MODProvider(final IERSConventions conventions) throws OrekitException {
        this.nutationArguments = conventions.getNutationArguments();
        this.precessionZeta    = conventions.getPrecessionZetaFunction();
        this.precessionTheta   = conventions.getPrecessionThetaFunction();
        this.precessionZ       = conventions.getPrecessionZFunction();
    }

    /** Get the transfrom from parent frame.
     * <p>The update considers the precession effects.</p>
     * @param date new value of the date
     * @return transform at the specified date
     */
    public Transform getTransform(final AbsoluteDate date) {

        // compute fundamental nutation arguments arguments
        final BodiesElements arguments = nutationArguments.evaluateAll(date);

        // compute the zeta precession angle
        final double zeta = precessionZeta.value(arguments);

        // compute the theta precession angle
        final double theta = precessionTheta.value(arguments);

        // compute the z precession angle
        final double z = precessionZ.value(arguments);

        // elementary rotations for precession
        final Rotation r1 = new Rotation(Vector3D.PLUS_K,  z);
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -theta);
        final Rotation r3 = new Rotation(Vector3D.PLUS_K,  zeta);

        // complete precession
        final Rotation precession = r1.applyTo(r2.applyTo(r3));

        // set up the transform from parent GCRF
        return new Transform(date, precession);

    }

}
