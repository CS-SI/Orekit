/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.analytical.tle;

import org.apache.commons.math3.optimization.ConvergenceChecker;
import org.apache.commons.math3.optimization.PointVectorValuePair;
import org.apache.commons.math3.optimization.SimpleVectorValueChecker;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;
import org.orekit.errors.OrekitException;

/** Orbit converter for Two-Lines Elements using differential algorithm.
 * @author Rocca
 * @since 6.0
 */
public class LevenbergMarquardtOrbitConverter extends AbstractTLEFitter {

    /** Maximum number of iterations for fitting. */
    private final int maxIterations;

    /** Simple constructor.
     * @param maxIterations maximum number of iterations for fitting
     * @param satelliteNumber satellite number
     * @param classification classification (U for unclassified)
     * @param launchYear launch year (all digits)
     * @param launchNumber launch number
     * @param launchPiece launch piece
     * @param elementNumber element number
     * @param revolutionNumberAtEpoch revolution number at epoch
     */
    protected LevenbergMarquardtOrbitConverter(final int maxIterations,
                                               final int satelliteNumber, final char classification,
                                               final int launchYear, final int launchNumber, final String launchPiece,
                                               final int elementNumber, final int revolutionNumberAtEpoch) {
        super(satelliteNumber, classification,
              launchYear, launchNumber, launchPiece, elementNumber, revolutionNumberAtEpoch);
        this.maxIterations = maxIterations;
    }

    /** {@inheritDoc} */
    protected double[] fit(final double[] initial) throws OrekitException {

        final ConvergenceChecker<PointVectorValuePair> checker =
                new SimpleVectorValueChecker(-1.0, getPositionTolerance());
        final LevenbergMarquardtOptimizer optimizer =
                new LevenbergMarquardtOptimizer(checker);
        final PointVectorValuePair optimum =
                optimizer.optimize(maxIterations, getPVFunction(),
                                   getTarget(), getWeight(), initial);

        return optimum.getPointRef();

    }

}
