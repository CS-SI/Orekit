/* Copyright 2022 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/**
 * Zeis model for J2-squared second-order terms.
 *
 * @see "ZEIS, Eric and CEFOLA, P. Computerized algebraic utilities for the
 *      construction of nonsingular satellite theories. Journal of Guidance and
 *      Control, 1980, vol. 3, no 1, p. 48-54."
 *
 * @see "SAN-JUAN, Juan F., LÓPEZ, Rosario, et CEFOLA, Paul J. A Second-Order
 *      Closed-Form $$ J_2 $$ Model for the Draper Semi-Analytical Satellite
 *      Theory. The Journal of the Astronautical Sciences, 2022, p. 1-27."
 *
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class ZeisModel implements J2SquaredModel {

    /**
     * Retrograde factor I.
     * <p>
     * DSST model needs equinoctial orbit as internal representation. Classical
     * equinoctial elements have discontinuities when inclination is close to zero.
     * In this representation, I = +1. <br>
     * To avoid this discontinuity, another representation exists and equinoctial
     * elements can be expressed in a different way, called "retrograde" orbit. This
     * implies I = -1. <br>
     * As Orekit doesn't implement the retrograde orbit, I is always set to +1. But
     * for the sake of consistency with the theory, the retrograde factor has been
     * kept in the formulas.
     * </p>
     */
    private static final int I = 1;

    /** Constructor. */
    public ZeisModel() {
        // Nothing to do...
    }

    /** {@inheritDoc}. */
    @Override
    public double[] computeMeanEquinoctialSecondOrderTerms(final DSSTJ2SquaredClosedFormContext context) {

        // Auxiliary elements
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

        // Zeis constant
        final double c2z = computeC2Z(context);

        // Useful terms
        final double s2mf    = 19.0 * context.getS2() - 15.0;
        final double s2pIcmo = context.getS2() + I * context.getC() - 1.0;
        final double s4mts2  = 19.0 * context.getS2() * context.getS2() - 30.0 * context.getS2() + 12.0;

        // Second-order terms (Ref [2] Eq. 37)
        final double deltaA =  0.0;
        final double deltaK = -c2z * auxiliaryElements.getH() * s2mf * s2pIcmo;
        final double deltaH =  c2z * auxiliaryElements.getK() * s2mf * s2pIcmo;
        final double deltaQ = -c2z * context.getC() * auxiliaryElements.getP() * s2mf;
        final double deltaP =  c2z * context.getC() * auxiliaryElements.getQ() * s2mf;
        final double deltaM =  0.5 * c2z * (2.0 * s2mf * s2pIcmo + 5.0 * s4mts2 * context.getEta());

        // Return
        return new double[] { deltaA, deltaK, deltaH, deltaQ, deltaP, deltaM };

    }

    /** {@inheritDoc}. */
    @Override
    public <T extends CalculusFieldElement<T>> T[] computeMeanEquinoctialSecondOrderTerms(final FieldDSSTJ2SquaredClosedFormContext<T> context) {

        // Auxiliary elements
        final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

        // Field
        final Field<T> field = auxiliaryElements.getDate().getField();

        // Zeis constant
        final T c2z = computeC2Z(context);

        // Useful terms
        final T s2mf    = context.getS2().multiply(19.0).subtract(15.0);
        final T s2pIcmo = context.getS2().add(context.getC().multiply(I)).subtract(1.0);
        final T s4mts2  = context.getS2().multiply(context.getS2()).multiply(19.0).subtract(context.getS2().multiply(30.0)).add(12.0);

        // Second-order terms (Ref [2] Eq. 37)
        final T deltaA = field.getZero();
        final T deltaK = c2z.multiply(auxiliaryElements.getH()).multiply(s2mf).multiply(s2pIcmo).negate();
        final T deltaH = c2z.multiply(auxiliaryElements.getK()).multiply(s2mf).multiply(s2pIcmo);
        final T deltaQ = c2z.multiply(context.getC()).multiply(auxiliaryElements.getP()).multiply(s2mf).negate();
        final T deltaP = c2z.multiply(context.getC()).multiply(auxiliaryElements.getQ()).multiply(s2mf);
        final T deltaM = c2z.multiply(0.5).multiply(s2mf.multiply(s2pIcmo).multiply(2.0).add(s4mts2.multiply(context.getEta()).multiply(5.0)));

        // Return
        final T[] terms = MathArrays.buildArray(field, 6);
        terms[0] = deltaA;
        terms[1] = deltaK;
        terms[2] = deltaH;
        terms[3] = deltaQ;
        terms[4] = deltaP;
        terms[5] = deltaM;
        return terms;

    }

    /**
     * Get the value of the Zeis constant.
     *
     * @param context model context
     * @return the value of the Zeis constant
     */
    public double computeC2Z(final DSSTJ2SquaredClosedFormContext context) {
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();
        return 0.75 * context.getAlpha4() * auxiliaryElements.getMeanMotion() / (context.getA4() * context.getEta());
    }

    /**
     * Get the value of the Zeis constant.
     *
     * @param context model context
     * @param <T> type of the elements
     * @return the value of the Zeis constant
     */
    public <T extends CalculusFieldElement<T>> T computeC2Z(final FieldDSSTJ2SquaredClosedFormContext<T> context) {
        final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();
        return auxiliaryElements.getMeanMotion().multiply(context.getAlpha4()).multiply(0.75).divide(context.getA4().multiply(context.getEta()));
    }

}
