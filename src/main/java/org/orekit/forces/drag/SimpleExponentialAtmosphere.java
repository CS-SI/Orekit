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
package org.orekit.forces.drag;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/** Simple exponential atmospheric model.
 * <p>This model represents a simple atmosphere with an exponential
 * density and rigidly bound to the underlying rotating body.</p>
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class SimpleExponentialAtmosphere implements Atmosphere {

    /** Serializable UID.*/
    private static final long serialVersionUID = 2772347498196369601L;

    /** Earth shape model. */
    private BodyShape    shape;

    /** Earth reference frame. */
    private Frame        bodyFrame;

    /** Reference density. */
    private double       rho0;

    /** Reference altitude. */
    private double       h0;

    /** Reference altitude scale. */
    private double       hscale;

    /** Create an exponential atmosphere.
     * @param shape body shape model
     * @param bodyFrame body rotation frame
     * @param rho0 Density at the altitude h0
     * @param h0 Altitude of reference (m)
     * @param hscale Scale factor
     */
    public SimpleExponentialAtmosphere(final BodyShape shape, final Frame bodyFrame,
                                       final double rho0, final double h0, final double hscale) {
        this.shape  = shape;
        this.bodyFrame = bodyFrame;
        this.rho0   = rho0;
        this.h0     = h0;
        this.hscale = hscale;
    }

    /** {@inheritDoc} */
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws OrekitException {
        final GeodeticPoint gp = shape.transform(position, frame, date);
        return rho0 * Math.exp((h0 - gp.getAltitude()) / hscale);
    }

    /** {@inheritDoc} */
    public Vector3D getVelocity(final AbsoluteDate date,
                                final Vector3D position, final Frame frame)
        throws OrekitException {
        final Transform bodyToFrame = bodyFrame.getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

}
