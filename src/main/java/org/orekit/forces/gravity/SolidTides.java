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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.ode.UnknownParameterException;
import org.apache.commons.math3.util.Precision;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
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
     * @param sun Sun celestial body
     * @param moon Moon celestial body
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     */
    public SolidTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final TideSystem centralTideSystem, final IERSConventions conventions,
                      final CelestialBody sun, final CelestialBody moon)
        throws OrekitException {
        final String resourceName = conventions.getLoveNumbersModel();
        final TidesField tidesField =
                new TidesField(resourceName, centralBodyFrame, ae, mu, centralTideSystem,
                               sun, moon);
        attractionModel = new HolmesFeatherstoneAttractionModel(centralBodyFrame,
                                                                tidesField);
    }

    /** {@inheritDoc} */
    @Override
    public double getParameter(final String name)
        throws UnknownParameterException {
        complainIfNotSupported(name);
        return Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void setParameter(final String name, final double value)
        throws UnknownParameterException {
        complainIfNotSupported(name);
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
        // delegate to underlying attraction model
        return attractionModel.accelerationDerivatives(s, paramName);
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        // delegate to underlying attraction model
        return attractionModel.getEventsDetectors();
    }

    /** Gravity field correction due to tides. */
    private static class TidesField implements NormalizedSphericalHarmonicsProvider {

        /** Real part of the nominal Love numbers. */
        private final double[][] loveReal;

        /** Imaginary part of the nominal Love numbers. */
        private final double[][] loveImaginary;

        /** Time-dependent part of the Love numbers. */
        private final double[][] lovePlus;

        /** Rotating body frame. */
        private final Frame centralBodyFrame;

        /** Central body reference radius. */
        private final double ae;

        /** Central body attraction coefficient. */
        private final double mu;

        /** Tide system used in the central attraction model. */
        private final TideSystem centralTideSystem;

        /** Sun celestial body. */
        private final CelestialBody sun;

        /** Moon celestial body. */
        private final CelestialBody moon;

        /** Date offset of cached coefficients. */
        private double cachedOffset;

        /** Cached cnm. */
        private double[][] cachedCnm;

        /** Cached snm. */
        private double[][] cachedSnm;

        /** Simple constructor.
         * @param name of the model
         * @param centralBodyFrame rotating body frame
         * @param ae central body reference radius
         * @param mu central body attraction coefficient
         * @param centralTideSystem tide system used in the central attraction model
         * @param sun Sun celestial body
         * @param moon Moon celestial body
         * @exception OrekitException if the Love numbers embedded in the
         * library cannot be read
         */
        public TidesField(final String name, final Frame centralBodyFrame,
                          final double ae, final double mu,
                          final TideSystem centralTideSystem,
                          final CelestialBody sun, final CelestialBody moon)
            throws OrekitException {

            this.centralBodyFrame  = centralBodyFrame;
            this.ae                = ae;
            this.mu                = mu;
            this.centralTideSystem = centralTideSystem;
            this.sun               = sun;
            this.moon              = moon;

            this.loveReal          = new double[2][3];
            this.loveImaginary     = new double[2][3];
            this.lovePlus          = new double[2][3];
            this.cachedOffset      = Double.NaN;
            this.cachedCnm         = new double[2][3];
            this.cachedSnm         = new double[2][3];

            InputStream stream = null;
            try {

                stream = SolidTides.class.getResourceAsStream(name);
                if (stream == null) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, name);
                }

                // setup the reader
                final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                String line = reader.readLine().trim();
                int lineNumber = 1;

                // look for the Love numbers
                while (line != null) {

                    if (!(line.isEmpty() || line.startsWith("#"))) {
                        final String[] fields = line.split("\\p{Space}+");
                        if (fields.length != 5) {
                            // this should never happen with files embedded within Orekit
                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      lineNumber, name, line);
                        }
                        final int n = Integer.parseInt(fields[0]);
                        final int m = Integer.parseInt(fields[1]);
                        if ((n < 2) || (n > 3) || (m < 0) || (m > n)) {
                            // this should never happen with files embedded within Orekit
                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      lineNumber, name, line);

                        }
                        loveReal[n - 2][m]      = Double.parseDouble(fields[2]);
                        loveImaginary[n - 2][m] = Double.parseDouble(fields[3]);
                        lovePlus[n - 2][m]      = Double.parseDouble(fields[4]);
                    }

                    // next line
                    lineNumber++;
                    line = reader.readLine().trim();

                }

            } catch (IOException ioe) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException ioe) {
                    // ignored here
                }
            }
        }


        /** {@inheritDoc} */
        @Override
        public int getMaxDegree() {
            return 4;
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxOrder() {
            return 4;
        }

        /** {@inheritDoc} */
        @Override
        public double getMu() {
            return mu;
        }

        /** {@inheritDoc} */
        @Override
        public double getAe() {
            return ae;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getReferenceDate() {
            return AbsoluteDate.J2000_EPOCH;
        }

        /** {@inheritDoc} */
        @Override
        public double getOffset(final AbsoluteDate date) {
            return date.durationFrom(AbsoluteDate.J2000_EPOCH);
        }

        /** {@inheritDoc} */
        @Override
        public TideSystem getTideSystem() {
            // not really used here, but for consistency we can state that either
            // we add the permanent tide or it was already in the central attraction
            return TideSystem.ZERO_TIDE;
        }

        /** {@inheritDoc} */
        @Override
        public double getNormalizedCnm(final double dateOffset, final int n, final int m)
            throws OrekitException {
            if (!Precision.equals(dateOffset, cachedOffset, 1)) {
                fillCache(dateOffset);
            }
            return cachedCnm[n][m];
        }

        /** {@inheritDoc} */
        @Override
        public double getNormalizedSnm(final double dateOffset, final int n, final int m)
            throws OrekitException {
            if (!Precision.equals(dateOffset, cachedOffset, 1)) {
                fillCache(dateOffset);
            }
            return cachedSnm[n][m];
        }

        /** Fill the cache.
         * @param offset date offset from J2000.0
         * @exception OrekitException if coefficients cannot be computed
         */
        private void fillCache(final double offset) throws OrekitException {

            cachedOffset = offset;
            for (int i = 0; i < cachedCnm.length; ++i) {
                Arrays.fill(cachedCnm[i], 0.0);
                Arrays.fill(cachedSnm[i], 0.0);
            }

            final AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, offset);

            // step 1: frequency independent part
            // equation 6.6 in IERS conventions 2010
            for (final CelestialBody body : Arrays.asList(sun, moon)) {

                // geocentric coordinates of tide-generating body
                final Vector3D position =
                        body.getPVCoordinates(date, centralBodyFrame).getPosition();
                final double r      = position.getNorm();
                final double phi    = position.getDelta();
                final double lambda = position.getAlpha();

                for (int n = 2; n < 4; ++n) {
                    for (int m = 0; m <= n; ++m) {
                        // TODO: compute step 1
                        final double pnm = Double.NaN;
                    }
                }

            }

            // step 2: frequency dependent corrections
            // equations 6.8a and 6.8b in IERS conventions 2010
            // TODO: compute step 2

            if (centralTideSystem == TideSystem.ZERO_TIDE) {
                // step 3: remove permanent tide which is already considered
                // in the central body gravitaty field
                // equations 6.13 and 6.14 in IERS conventions 2010
                // TODO: compute step 3
            }

        }

    }

}
