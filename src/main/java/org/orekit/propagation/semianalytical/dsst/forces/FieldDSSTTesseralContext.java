/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.FieldHansenTesseralLinear;

/** This class is a container for the field attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral DSSTTesseral}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements) initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class FieldDSSTTesseralContext<T extends RealFieldElement<T>> extends FieldForceModelContext<T> {

    /** Retrograde factor I.
     *  <p>
     *  DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination
     *  is close to zero. In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial
     *  elements can be expressed in a different way, called "retrograde" orbit.
     *  This implies I = -1. <br>
     *  As Orekit doesn't implement the retrograde orbit, I is always set to +1.
     *  But for the sake of consistency with the theory, the retrograde factor
     *  has been kept in the formulas.
     *  </p>
     */
    private static final int I = 1;

    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in seconds.
     */
    private static final double MIN_PERIOD_IN_SECONDS = 864000.;

    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in satellite revolutions.
     */
    private static final double MIN_PERIOD_IN_SAT_REV = 10.;

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private T chi;

    /** &Chi;². */
    private T chi2;

    /** Central body rotation angle θ. */
    private T theta;

    // Common factors from equinoctial coefficients
    /** 2 * a / A .*/
    private T ax2oA;

    /** 1 / (A * B) .*/
    private T ooAB;

    /** B / A .*/
    private T BoA;

    /** B / (A * (1 + B)) .*/
    private T BoABpo;

    /** C / (2 * A * B) .*/
    private T Co2AB;

    /** μ / a .*/
    private T moa;

    /** R / a .*/
    private T roa;

    /** ecc². */
    private T e2;

    /** Maximum power of the eccentricity to use in summation over s. */
    private int maxEccPow;

    /** Maximum power of the eccentricity to use in Hansen coefficient Kernel expansion. */
    private int maxHansen;

    /** Keplerian period. */
    private T orbitPeriod;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegree;

    /** Maximal order to consider for harmonics potential. */
    private final int maxOrder;

    /** A two dimensional array that contains the objects needed to build the Hansen coefficients. <br/>
     * The indexes are s + maxDegree and j */
    private FieldHansenTesseralLinear<T>[][] hansenObjects;

    /** Ratio of satellite period to central body rotation period. */
    private T ratio;

    /** Factory for the DerivativeStructure instances. */
    private final FDSFactory<T> factory;

    /** List of resonant orders. */
    private final List<Integer> resOrders;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param meanOnly create only the objects required for the mean contribution
     * @param centralBodyFrame rotating body frame
     * @param provider provider for spherical harmonics
     * @param maxFrequencyShortPeriodics maximum value for j
     //* @param resOrders list of resonant orders
     * @param bodyPeriod central body rotation period (seconds)
     * @throws OrekitException if some specific error occurs
     */
    @SuppressWarnings("unchecked")
    public FieldDSSTTesseralContext(final FieldAuxiliaryElements<T> auxiliaryElements,
                                    final boolean meanOnly,
                                    final Frame centralBodyFrame,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final int maxFrequencyShortPeriodics,
                                    //final List<Integer> resOrders,
                                    final double bodyPeriod)
        throws OrekitException {

        super(auxiliaryElements);

        final Field<T> field = auxiliaryElements.getDate().getField();
        final T zero = field.getZero();

        this.maxEccPow = 0;
        this.maxHansen = 0;
        this.maxOrder = provider.getMaxOrder();
        this.maxDegree = provider.getMaxDegree();
        this.resOrders = new ArrayList<Integer>();

        this.factory = new FDSFactory<>(field, 1, 1);

        // Eccentricity square
        e2 = auxiliaryElements.getEcc().multiply(auxiliaryElements.getEcc());

        // Central body rotation angle from equation 2.7.1-(3)(4).
        final FieldTransform<T> t = centralBodyFrame.getTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());
        final FieldVector3D<T> xB = t.transformVector(FieldVector3D.getPlusI(field));
        final FieldVector3D<T> yB = t.transformVector(FieldVector3D.getPlusJ(field));
        theta = FastMath.atan2(auxiliaryElements.getVectorF().dotProduct(yB).negate().add((auxiliaryElements.getVectorG().dotProduct(xB)).multiply(I)),
                               auxiliaryElements.getVectorF().dotProduct(xB).add(auxiliaryElements.getVectorG().dotProduct(yB).multiply(I)));

        // Common factors from equinoctial coefficients
        // 2 * a / A
        ax2oA  = auxiliaryElements.getSma().divide(auxiliaryElements.getA()).multiply(2.);
        // B / A
        BoA  = auxiliaryElements.getB().divide(auxiliaryElements.getA());
        // 1 / AB
        ooAB = auxiliaryElements.getA().multiply(auxiliaryElements.getB()).reciprocal();
        // C / 2AB
        Co2AB = auxiliaryElements.getC().multiply(ooAB).divide(2.);
        // B / (A * (1 + B))
        BoABpo = BoA.divide(auxiliaryElements.getB().add(1.));
        // &mu / a
        moa = auxiliaryElements.getSma().divide(provider.getMu()).reciprocal();
        // R / a
        roa = auxiliaryElements.getSma().divide(provider.getAe()).reciprocal();

        // &Chi; = 1 / B
        chi = auxiliaryElements.getB().reciprocal();
        chi2 = chi.multiply(chi);

        // Set the highest power of the eccentricity in the analytical power
        // series expansion for the averaged high order resonant central body
        // spherical harmonic perturbation
        final T e = auxiliaryElements.getEcc();
        if (e.getReal() <= 0.005) {
            maxEccPow = 3;
        } else if (e.getReal() <= 0.02) {
            maxEccPow = 4;
        } else if (e.getReal() <= 0.1) {
            maxEccPow = 7;
        } else if (e.getReal() <= 0.2) {
            maxEccPow = 10;
        } else if (e.getReal() <= 0.3) {
            maxEccPow = 12;
        } else if (e.getReal() <= 0.4) {
            maxEccPow = 15;
        } else {
            maxEccPow = 20;
        }

        // Set the maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
        maxHansen = maxEccPow / 2;

        // Keplerian period
        orbitPeriod = auxiliaryElements.getKeplerianPeriod();

        // Ratio of satellite to central body periods to define resonant terms
        ratio = orbitPeriod.divide(bodyPeriod);

        // Compute natural resonant terms
        final T tolerance = FastMath.max(zero.add(MIN_PERIOD_IN_SAT_REV),
                                                   orbitPeriod.divide(MIN_PERIOD_IN_SECONDS).reciprocal()).reciprocal();

        // Search the resonant orders in the tesseral harmonic field
        resOrders.clear();
        for (int m = 1; m <= maxOrder; m++) {
            final T resonance = ratio.multiply(m);
            final int jComputedRes = (int) FastMath.round(resonance);
            if (jComputedRes > 0 && jComputedRes <= maxFrequencyShortPeriodics && FastMath.abs(resonance.subtract(jComputedRes)).getReal() <= tolerance.getReal()) {
                // Store each resonant index and order
                this.resOrders.add(m);
            }
        }

        //Allocate the two dimensional array
        final int rows     = 2 * maxDegree + 1;
        final int columns  = maxFrequencyShortPeriodics + 1;
        //context.setHansenObjects(rows, columns);
        this.hansenObjects = new FieldHansenTesseralLinear[rows][columns];

        if (meanOnly) {
            // loop through the resonant orders
            for (int m : resOrders) {
                //Compute the corresponding j term
                final int j = FastMath.max(1, (int) FastMath.round(ratio.multiply(m)));

                //Compute the sMin and sMax values
                final int sMin = FastMath.min(maxEccPow - j, maxDegree);
                final int sMax = FastMath.min(maxEccPow + j, maxDegree);

                //loop through the s values
                for (int s = 0; s <= sMax; s++) {
                    //Compute the n0 value
                    final int n0 = FastMath.max(FastMath.max(2, m), s);

                    //Create the object for the pair j, s
                    //context.getHansenObjects()[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, context.getMaxHansen());
                    this.hansenObjects[s + maxDegree][j] = new FieldHansenTesseralLinear<>(maxDegree, s, j, n0, maxHansen, field);

                    if (s > 0 && s <= sMin) {
                        //Also create the object for the pair j, -s
                        //context.getHansenObjects()[maxDegree - s][j] = new HansenTesseralLinear(maxDegree, -s, j, n0, context.getMaxHansen());
                        this.hansenObjects[maxDegree - s][j] =  new FieldHansenTesseralLinear<>(maxDegree, -s, j, n0, maxHansen, field);
                    }
                }
            }
        } else {
            // create all objects
            for (int j = 0; j <= maxFrequencyShortPeriodics; j++) {
                for (int s = -maxDegree; s <= maxDegree; s++) {
                    //Compute the n0 value
                    final int n0 = FastMath.max(2, FastMath.abs(s));

                    //context.getHansenObjects()[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, context.getMaxHansen());
                    this.hansenObjects[s + maxDegree][j] = new FieldHansenTesseralLinear<>(maxDegree, s, j, n0, maxHansen, field);
                }
            }
        }

    }

    /** Compute init values for hansen objects.
     * @param rows number of rows of the hansen matrix
     * @param columns columns number of columns of the hansen matrix
     */
    public void computeHansenObjectsInitValues(final int rows, final int columns) {
        hansenObjects[rows][columns].computeInitValues(e2, chi, chi2);
    }

    /** Get hansen object.
     * @return hansenObjects
     */
    public FieldHansenTesseralLinear<T>[][] getHansenObjects() {
        return hansenObjects;
    }

    /** Get the list of resonant orders.
     * @return resOrders
     */
    public List<Integer> getResOrders() {
        return resOrders;
    }

    /** Get ecc².
     * @return e2
     */
    public T getE2() {
        return e2;
    }

    /** Get Central body rotation angle θ.
     * @return theta
     */
    public T getTheta() {
        return theta;
    }

    /** Get ax2oA = 2 * a / A .
     * @return ax2oA
     */
    public T getAx2oA() {
        return ax2oA;
    }

    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return chi
     */
    public T getChi() {
        return chi;
    }

    /** Get &Chi;².
     * @return chi2
     */
    public T getChi2() {
        return chi2;
    }

    /** Get B / A.
     * @return BoA
     */
    public T getBoA() {
        return BoA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public T getOoAB() {
        return ooAB;
    }

    /** Get Co2AB = C / 2AB.
     * @return Co2AB
     */
    public T getCo2AB() {
        return Co2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public T getBoABpo() {
        return BoABpo;
    }

    /** Get μ / a .
     * @return moa
     */
    public T getMoa() {
        return moa;
    }

    /** Get roa = R / a.
     * @return roa
     */
    public T getRoa() {
        return roa;
    }

    /** Get the maximum power of the eccentricity to use in summation over s.
     * @return roa
     */
    public int getMaxEccPow() {
        return maxEccPow;
    }

    /** Get the maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
     * @return maxHansen
     */
    public int getMaxHansen() {
        return maxHansen;
    }

    /** Get keplerian period.
     * @return orbitPeriod
     */
    public T getOrbitPeriod() {
        return orbitPeriod;
    }

    /** Factory for the DerivativeStructure instances.
     * @return factory
     */
    public FDSFactory<T> getFactory() {
        return factory;
    }


    /** Get the ratio of satellite period to central body rotation period.
     * @return ratio
     */
    public T getRatio() {
        return ratio;
    }

}
