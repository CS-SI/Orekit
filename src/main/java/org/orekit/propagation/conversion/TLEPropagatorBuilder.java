/* Copyright 2002-2014 CS Systèmes d'Information
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
package org.orekit.propagation.conversion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Builder for TLEPropagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class TLEPropagatorBuilder extends AbstractParameterizable
                                  implements PropagatorBuilder {

    /** Parameter name for B* coefficient. */
    public static final String B_STAR = "BSTAR";

    /** Satellite number. */
    private final int satelliteNumber;

    /** Classification (U for unclassified). */
    private final char classification;

    /** Launch year (all digits). */
    private final int launchYear;

    /** Launch number. */
    private final int launchNumber;

    /** Launch piece. */
    private final String launchPiece;

    /** Element number. */
    private final int elementNumber;

    /** Revolution number at epoch. */
    private final int revolutionNumberAtEpoch;

    /** Central attraction coefficient (m³/s²). */
    private final double mu;

    /** TEME frame. */
    private final Frame frame;

    /** List of the free parameters names. */
    private Collection<String> freeParameters;

    /** Ballistic coefficient. */
    private double bStar;

    /** Build a new instance.
     * @param satelliteNumber satellite number
     * @param classification classification (U for unclassified)
     * @param launchYear launch year (all digits)
     * @param launchNumber launch number
     * @param launchPiece launch piece
     * @param elementNumber element number
     * @param revolutionNumberAtEpoch revolution number at epoch
     * @throws OrekitException if the TEME frame cannot be set
     */
    public TLEPropagatorBuilder(final int satelliteNumber,
                                final char classification,
                                final int launchYear,
                                final int launchNumber,
                                final String launchPiece,
                                final int elementNumber,
                                final int revolutionNumberAtEpoch) throws OrekitException {
        super(B_STAR);
        this.satelliteNumber         = satelliteNumber;
        this.classification          = classification;
        this.launchYear              = launchYear;
        this.launchNumber            = launchNumber;
        this.launchPiece             = launchPiece;
        this.elementNumber           = elementNumber;
        this.revolutionNumberAtEpoch = revolutionNumberAtEpoch;
        this.bStar                   = 0.0;
        this.mu                      = TLEPropagator.getMU();
        this.frame                   = FramesFactory.getTEME();
    }

    /** {@inheritDoc} */
    public Propagator buildPropagator(final AbsoluteDate date, final double[] parameters)
        throws OrekitException {

        if (parameters.length != (freeParameters.size() + 6)) {
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.DIMENSIONS_MISMATCH);
        }

        final KeplerianOrbit orb = new KeplerianOrbit(new PVCoordinates(new Vector3D(parameters[0],
                                                                                     parameters[1],
                                                                                     parameters[2]),
                                                                        new Vector3D(parameters[3],
                                                                                     parameters[4],
                                                                                     parameters[5])),
                                                      frame, date, mu);

        final Iterator<String> freeItr = freeParameters.iterator();
        for (int i = 6; i < parameters.length; i++) {
            final String free = freeItr.next();
            for (String available : getParametersNames()) {
                if (free.equals(available)) {
                    setParameter(free, parameters[i]);
                }
            }
        }

        final TLE tle = new TLE(satelliteNumber, classification, launchYear, launchNumber, launchPiece,
                                TLE.DEFAULT, elementNumber, date,
                                orb.getKeplerianMeanMotion(), 0.0, 0.0,
                                orb.getE(), MathUtils.normalizeAngle(orb.getI(), FastMath.PI),
                                MathUtils.normalizeAngle(orb.getPerigeeArgument(), FastMath.PI),
                                MathUtils.normalizeAngle(orb.getRightAscensionOfAscendingNode(), FastMath.PI),
                                MathUtils.normalizeAngle(orb.getMeanAnomaly(), FastMath.PI),
                                revolutionNumberAtEpoch, bStar);

        return TLEPropagator.selectExtrapolator(tle);
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    public void setFreeParameters(final Collection<String> parameters)
        throws IllegalArgumentException {
        freeParameters = new ArrayList<String>();
        for (String name : parameters) {
            complainIfNotSupported(name);
        }
        freeParameters.addAll(parameters);
    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        return bStar;
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        bStar = value * 1.e-4;
    }

}
