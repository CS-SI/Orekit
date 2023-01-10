/* Copyright 2002-2023 CS GROUP
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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModifiedLambdaMethodTest extends AbstractLambdaMethodTest {

    protected AbstractLambdaMethod buildReducer() {
        return new ModifiedLambdaMethod();
    }

    protected RealMatrix buildCovariance(AbstractLambdaMethod reducer) {
        RealMatrix Z  = getZMatrix(reducer);
        RealMatrix Zt = Z.transpose();
        return Zt.multiply(getLow(reducer).transposeMultiply(getDiag(reducer))).
                        multiply(getLow(reducer)).multiply(Z);
    }

   private RealMatrix getZMatrix(final AbstractLambdaMethod reducer) {
        final int n = reducer.getSize();
        RealMatrix Z = MatrixUtils.createRealMatrix(n,n);
        for(int i = 0; i<n; i++) {
            for(int j = 0; j<n ;j++) {
                Z.setEntry(i, j, reducer.getZInverseTransformationReference()[reducer.zIndex(i,j)]);
            }
        }
        return(Z);
   }

   @Test
   public void testReduction() {

        RandomGenerator random = new Well19937a(0xf824c33093974ee5l);
        for (int k = 0; k < 1000; ++k) {
            // generate random test data
            final int        n           = FastMath.max(2, 1 + random.nextInt(20));

            final RealMatrix covariance  = createRandomSymmetricPositiveDefiniteMatrix(n, random);
            final int[]      indirection = createRandomIndirectionArray(n, random);

            // perform ILS resolution test
            doTestReduction(random, indirection, covariance);

        }
    }

    private void doTestReduction(final RandomGenerator random,
                                   final int[] indirection, final RealMatrix covariance) {
        final double[] floatAmbiguities = new double[indirection.length];
        for (int i = 0; i < floatAmbiguities.length; ++i) {
            floatAmbiguities[i] = 2 * random.nextDouble() - 1.0;
        }
        RealMatrix filteredCovariance = filterCovariance(covariance, indirection);
        final AbstractLambdaMethod reducer = buildReducer();
        initializeProblem(reducer, floatAmbiguities, indirection, covariance, 2);
        reducer.ltdlDecomposition();
        reducer.reduction();

        //Rebuilt the covariance
        RealMatrix Z                  = this.getZMatrix(reducer);
        RealMatrix RebuiltCov         = Z.transposeMultiply(getLow(reducer).transposeMultiply(getDiag(reducer))).
                                        multiply(getLow(reducer)).multiply(Z);

        //Check the covariance are the same
        double norm                   = filteredCovariance.subtract(RebuiltCov).getNorm1();
        Assertions.assertEquals(0.0, norm, 1e-11);

        //Check the floatAmbiguities have been well transform by Z transformation
        RealMatrix a = MatrixUtils.createColumnRealMatrix(floatAmbiguities);
        RealMatrix invZ = new QRDecomposer(1.0e-10).
                        decompose(Z).
                        getInverse();
        RealMatrix zRef = invZ.transposeMultiply(a);
        double[] zComputed = getDecorrelated(reducer);
        for(int i= 0; i<zComputed.length; i++) {
            Assertions.assertEquals(zRef.getEntry(i, 0),zComputed[i],1e-6);
        }
    }
}


