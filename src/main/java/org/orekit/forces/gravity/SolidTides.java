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
package org.orekit.forces.gravity;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.ode.UnknownParameterException;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Solid tides force model.
 *
 * @author Luc Maisonobe
 */
public class SolidTides extends AbstractParameterizable implements ForceModel {

    /** Underlying attraction model. */
    private final ForceModel attractionModel;

    /** Simple constructor.
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param conventions IERS conventions used for loading Love numbers
     * @param bodies tide generating bodies (typically Sun and Moon)
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     */
    public SolidTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final TideSystem centralTideSystem, final IERSConventions conventions,
                      final CelestialBody ... bodies)
        throws OrekitException {
        final TidesField tidesField =
                new TidesField(conventions.getLoveNumbersModel(),
                               conventions.getK20FrequencyDependenceModel(),
                               conventions.getK21FrequencyDependenceModel(),
                               conventions.getK22FrequencyDependenceModel(),
                               centralBodyFrame, ae, mu, centralTideSystem, bodies);
        attractionModel = new HolmesFeatherstoneAttractionModel(centralBodyFrame, tidesField);
    }

    /** {@inheritDoc} */
    @Override
    public double getParameter(final String name)
        throws UnknownParameterException {
        // there are no tunable parameters at all in this force model
        throw new UnknownParameterException(name);
    }

    /** {@inheritDoc} */
    @Override
    public void setParameter(final String name, final double value)
        throws UnknownParameterException {
        // there are no tunable parameters at all in this force model
        throw new UnknownParameterException(name);
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s,
                                final TimeDerivativesEquations adder)
        throws OrekitException {
        // delegate to underlying attraction model
        attractionModel.addContribution(s, adder);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date,
                                                                      final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position,
                                                                      final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation,
                                                                      final DerivativeStructure mass)
        throws OrekitException {
        // delegate to underlying attraction model
        return attractionModel.accelerationDerivatives(date, frame, position, velocity, rotation, mass);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s,
                                                                      final String paramName)
        throws OrekitException {
        // this should never be called as there are no tunable parameters
        return attractionModel.accelerationDerivatives(s, paramName);
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        // delegate to underlying attraction model
        return attractionModel.getEventsDetectors();
    }

}
