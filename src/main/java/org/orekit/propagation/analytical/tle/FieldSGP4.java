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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;

/** This class contains methods to compute propagated coordinates with the SGP4 model.
 * <p>
 * The user should not bother in this class since it is handled internaly by the
 * {@link TLEPropagator}.
 * </p>
 * <p>This implementation is largely inspired from the paper and source code <a
 * href="https://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack
 * Report #3</a> and is fully compliant with its results and tests cases.</p>
 * @author Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (java translation)
 * @author Thomas Paulet (field translation)
 * @since 11.0
 * @param <T> type of the field elements
 */
public class FieldSGP4<T extends CalculusFieldElement<T>> extends FieldTLEPropagator<T> {

    /** If perige is less than 220 km, some calculus are avoided. */
    private boolean lessThan220;

    /** (1 + eta * cos(M0))³. */
    private T delM0;

    // CHECKSTYLE: stop JavadocVariable check
    private T d2;
    private T d3;
    private T d4;
    private T t3cof;
    private T t4cof;
    private T t5cof;
    private T sinM0;
    private T omgcof;
    private T xmcof;
    private T c5;
    // CHECKSTYLE: resume JavadocVariable check

    /** Constructor for a unique initial TLE.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param initialTLE the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param parameters SGP4 and SDP4 model parameters
     * @see #FieldSGP4(FieldTLE, AttitudeProvider, CalculusFieldElement, Frame, CalculusFieldElement[])
     */
    @DefaultDataContext
    public FieldSGP4(final FieldTLE<T> initialTLE, final AttitudeProvider attitudeProvider,
                final T mass, final T[] parameters) {
        this(initialTLE, attitudeProvider, mass,
                DataContext.getDefault().getFrames().getTEME(), parameters);
    }

    /** Constructor for a unique initial TLE.
     * @param initialTLE the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @param parameters SGP4 and SDP4 model parameters
     */
    public FieldSGP4(final FieldTLE<T> initialTLE,
                final AttitudeProvider attitudeProvider,
                final T mass,
                final Frame teme,
                final T[] parameters) {
        super(initialTLE, attitudeProvider, mass, teme, parameters);
    }

    /** Initialization proper to each propagator (SGP or SDP).
     * @param parameters model parameters
     */
    protected void sxpInitialize(final T[] parameters) {

        final T bStar = parameters[0];
        // For perigee less than 220 kilometers, the equations are truncated to
        // linear variation in sqrt a and quadratic variation in mean anomaly.
        // Also, the c3 term, the delta omega term, and the delta m term are dropped.
        lessThan220 = perige.getReal() < 220;
        if (!lessThan220) {
            final FieldSinCos<T> scM0 = FastMath.sinCos(tle.getMeanAnomaly());
            final T c1sq = c1.multiply(c1);
            delM0 = eta.multiply(scM0.cos()).add(1.0);
            delM0 = delM0.multiply(delM0).multiply(delM0);
            d2 = a0dp.multiply(tsi).multiply(c1sq).multiply(4.0);
            final T temp = d2.multiply(tsi).multiply(c1).divide(3.0);
            d3 = a0dp.multiply(17.0).add(s4).multiply(temp);
            d4 = temp.multiply(0.5).multiply(a0dp).multiply(tsi).multiply(a0dp.multiply(221.0).add(s4.multiply(31.0))).multiply(c1);
            t3cof = d2.add(c1sq.multiply(2));
            t4cof = d3.multiply(3.0).add(c1.multiply(d2.multiply(12.0).add(c1sq.multiply(10)))).multiply(0.25);
            t5cof = d4.multiply(3.0).add(c1.multiply(12.0).multiply(d3)).add(
                    d2.multiply(d2).multiply(6.0)).add(c1sq.multiply(15.0).multiply(d2.multiply(2).add(c1sq))).multiply(0.2);
            sinM0 = scM0.sin();
            if (tle.getE().getReal() < 1e-4) {
                omgcof = c1sq.getField().getZero();
                xmcof = c1sq.getField().getZero();
            } else  {
                final T c3 = coef.multiply(tsi).multiply(xn0dp).multiply(TLEConstants.A3OVK2 * TLEConstants.NORMALIZED_EQUATORIAL_RADIUS).multiply(sini0.divide(tle.getE()));
                xmcof = coef.multiply(bStar).divide(eeta).multiply(-TLEConstants.TWO_THIRD * TLEConstants.NORMALIZED_EQUATORIAL_RADIUS);
                omgcof = bStar.multiply(c3).multiply(FastMath.cos(tle.getPerigeeArgument()));
            }
        }

        c5 = coef1.multiply(2).multiply(a0dp).multiply(beta02).multiply(etasq.add(eeta).multiply(2.75).add(eeta.multiply(etasq)).add(1));
        // initialized
    }

    /** Propagation proper to each propagator (SGP or SDP).
     * @param tSince the offset from initial epoch (min)
     * @param parameters model parameters
     */
    protected void sxpPropagate(final T tSince, final T[] parameters) {

        // Update for secular gravity and atmospheric drag.
        final T bStar = parameters[0];
        final T xmdf = tle.getMeanAnomaly().add(xmdot.multiply(tSince));
        final T omgadf = tle.getPerigeeArgument().add(omgdot.multiply(tSince));
        final T xn0ddf = tle.getRaan().add(xnodot.multiply(tSince));
        omega = omgadf;
        T xmp = xmdf;
        final T tsq = tSince.multiply(tSince);
        xnode = xn0ddf.add(xnodcf.multiply(tsq));
        T tempa = c1.multiply(tSince).negate().add(1.0);
        T tempe = bStar.multiply(c4).multiply(tSince);
        T templ = t2cof.multiply(tsq);

        if (!lessThan220) {
            final T delomg = omgcof.multiply(tSince);
            T delm = eta.multiply(FastMath.cos(xmdf)).add(1.0);
            delm = xmcof.multiply(delm.multiply(delm).multiply(delm).subtract(delM0));
            final T temp = delomg.add(delm);
            xmp = xmdf.add(temp);
            omega = omgadf.subtract(temp);
            final T tcube = tsq.multiply(tSince);
            final T tfour = tSince.multiply(tcube);
            tempa = tempa.subtract(d2.multiply(tsq)).subtract(d3.multiply(tcube)).subtract(d4.multiply(tfour));
            tempe = tempe.add(bStar.multiply(c5).multiply(FastMath.sin(xmp).subtract(sinM0)));
            templ = templ.add(t3cof.multiply(tcube)).add(tfour.multiply(t4cof.add(tSince.multiply(t5cof))));
        }

        a = a0dp.multiply(tempa).multiply(tempa);
        e = tle.getE().subtract(tempe);

        // A highly arbitrary lower limit on e,  of 1e-6:
        if (e.getReal() < 1e-6) {
            e = e.getField().getZero().add(1e-6);
        }

        xl = xmp.add(omega).add(xnode).add(xn0dp.multiply(templ));

        i = tle.getI();

    }

}
