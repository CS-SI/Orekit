/* Copyright 2002-2024 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

package org.orekit.estimation.common;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeStampedPVCoordinates;

public class ResultSequentialBatchLeastSquares {

    ParameterDriversList propagatorParameters  ;
    ParameterDriversList measurementsParameters;

    private int numberOfIteration;
    private int numberOfEvaluation;
    private TimeStampedPVCoordinates estimatedPV;
    private RealMatrix covariances;

    private int numberOfIterationSequential;
    private int numberOfEvaluationSequential;
    private TimeStampedPVCoordinates estimatedPVSequential;
    private RealMatrix covariancesSequential;

    ResultSequentialBatchLeastSquares(ParameterDriversList  propagatorParameters,
             ParameterDriversList  measurementsParameters,
             int numberOfIteration, int numberOfEvaluation, TimeStampedPVCoordinates estimatedPV,
             StreamingStatistics posStat, RealMatrix covariances,

             int numberOfIterationSequential, int numberOfEvaluationSequential, TimeStampedPVCoordinates estimatedPVSequential,
             StreamingStatistics posStatSequential, RealMatrix covariancesSequential) {

        // Common objects
        this.propagatorParameters   = propagatorParameters;
        this.measurementsParameters = measurementsParameters;

        // Only BLS
        this.numberOfIteration      = numberOfIteration;
        this.numberOfEvaluation     = numberOfEvaluation;
        this.estimatedPV            = estimatedPV;
        this.covariances            = covariances;

        // Only SBLS
        this.numberOfIterationSequential      = numberOfIterationSequential;
        this.numberOfEvaluationSequential     = numberOfEvaluationSequential;
        this.estimatedPVSequential            = estimatedPVSequential;
        this.covariancesSequential            = covariancesSequential;



    }

    public int getNumberOfIteration() {
        return numberOfIteration;
    }

    public int getNumberOfEvaluation() {
        return numberOfEvaluation;
    }

    public TimeStampedPVCoordinates getEstimatedPV() {
        return estimatedPV;
    }

    public RealMatrix getCovariances() {
        return covariances;
    }

    public ParameterDriversList getPropagatorParameters() {
        return propagatorParameters;
    }

    public ParameterDriversList getMeasurementsParameters() {
        return measurementsParameters;
    }

    public int getNumberOfIterationSequential() {
        return numberOfIterationSequential;
    }

    public int getNumberOfEvaluationSequential() {
        return numberOfEvaluationSequential;
    }

    public TimeStampedPVCoordinates getEstimatedPVSequential() {
        return estimatedPVSequential;
    }

    public RealMatrix getCovariancesSequential() {
        return covariancesSequential;
    }
}
