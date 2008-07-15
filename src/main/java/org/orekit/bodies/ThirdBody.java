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
package org.orekit.bodies;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** This class represents an attracting body different from the central one.
 * @author &Eacute;douard Delente
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public abstract class ThirdBody implements CelestialBody, Serializable {

    /** Reference radius. */
    private double radius;

    /** Attraction coefficient. */
    private double mu;

    /** Simple constructor.
     * @param radius reference radius
     * @param mu attraction coefficient
     */
    protected ThirdBody(final double radius, final double mu) {
        this.radius = radius;
        this.mu = mu;
    }

    /** {@inheritDoc} */
    public abstract Vector3D getPosition(AbsoluteDate date, Frame frame)
        throws OrekitException;

    /** Get the reference radius of the body.
     * @return reference radius of the body (m)
     */
    public double getRadius() {
        return radius;
    }

    /** Get the attraction coefficient of the body.
     * @return attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>)
     */
    public double getMu() {
        return mu;
    }

}
