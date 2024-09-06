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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Test for deprecated functions of (Field)AuxiliaryElements.
 * 
 * @deprecated should be removed in 13.0
 * @since 12.2
 */
public class DSSTZonalTesseralDeprecatedFunctionsTest {

    @Test
    @Deprecated
    void testAuxiliaryElementsDeprecatedFunctions() {
       
        // GIVEN
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential");
        
        final EquinoctialOrbit orbit = new EquinoctialOrbit(context.initialOrbit);
        
        final Field<Binary64> field = Binary64Field.getInstance();
        final FieldEquinoctialOrbit<Binary64> fieldOrbit = new FieldEquinoctialOrbit<>(field, orbit);
        
        // WHEN
        final AuxiliaryElements aux = new AuxiliaryElements(context.initialOrbit, 1);
        final FieldAuxiliaryElements<Binary64> fieldAux = new FieldAuxiliaryElements<>(fieldOrbit, 1);
        
        // THEN
        Assertions.assertEquals(aux.getVectorF().getZ(), aux.getAlpha(), 0.);
        Assertions.assertEquals(aux.getVectorG().getZ(), aux.getBeta(), 0.);
        Assertions.assertEquals(aux.getVectorW().getZ(), aux.getGamma(), 0.);
        
        Assertions.assertEquals(fieldAux.getVectorF().getZ().getReal(), fieldAux.getAlpha().getReal(), 0.);
        Assertions.assertEquals(fieldAux.getVectorG().getZ().getReal(), fieldAux.getBeta().getReal(), 0.);
        Assertions.assertEquals(fieldAux.getVectorW().getZ().getReal(), fieldAux.getGamma().getReal(), 0.);
    }
    
    @Test
    @Deprecated
    void testDSSTZonalContextDeprecatedFunctions() {
       
        // GIVEN
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential");
        
        final EquinoctialOrbit orbit = new EquinoctialOrbit(context.initialOrbit);
        
        final Field<Binary64> field = Binary64Field.getInstance();
        final FieldEquinoctialOrbit<Binary64> fieldOrbit = new FieldEquinoctialOrbit<>(field, orbit);
        
        final AuxiliaryElements aux = new AuxiliaryElements(context.initialOrbit, 1);
        final FieldAuxiliaryElements<Binary64> fieldAux = new FieldAuxiliaryElements<>(fieldOrbit, 1);
        
        final UnnormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getUnnormalizedProvider(2, 0);

        // WHEN
        final DSSTZonalContext zonalContext = new DSSTZonalContext(aux, gravity, new double[] {gravity.getMu()});
        final FieldDSSTZonalContext<Binary64> fieldZonalContext = new FieldDSSTZonalContext<>(fieldAux, gravity,
                        new Binary64[] {field.getZero().newInstance(gravity.getMu())});

        // THEN
        Assertions.assertEquals(zonalContext.getX(), zonalContext.getChi(), 0.);
        Assertions.assertEquals(zonalContext.getXX(), zonalContext.getChi2(), 0.);
        Assertions.assertEquals(zonalContext.getXXX(), zonalContext.getChi3(), 0.);
        Assertions.assertEquals(zonalContext.getM2aoA(), -zonalContext.getAx2oA(), 0.);
        Assertions.assertEquals(zonalContext.getMCo2AB(), -zonalContext.getCo2AB(), 0.);
        
        Assertions.assertEquals(fieldZonalContext.getX().getReal(), fieldZonalContext.getChi().getReal(), 0.);
        Assertions.assertEquals(fieldZonalContext.getXX().getReal(), fieldZonalContext.getChi2().getReal(), 0.);
        Assertions.assertEquals(fieldZonalContext.getXXX().getReal(), fieldZonalContext.getChi3().getReal(), 0.);
        Assertions.assertEquals(fieldZonalContext.getM2aoA().getReal(), -fieldZonalContext.getAx2oA().getReal(), 0.);
        Assertions.assertEquals(fieldZonalContext.getMCo2AB().getReal(), -fieldZonalContext.getCo2AB().getReal(), 0.);
    }

    @Test
    @Deprecated
    void testDSSTTesseralContextDeprecatedFunctions() {
       
        // GIVEN
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential");
        
        final EquinoctialOrbit orbit = new EquinoctialOrbit(context.initialOrbit);
        
        final Field<Binary64> field = Binary64Field.getInstance();
        final FieldEquinoctialOrbit<Binary64> fieldOrbit = new FieldEquinoctialOrbit<>(field, orbit);
        
        final AuxiliaryElements aux = new AuxiliaryElements(context.initialOrbit, 1);
        final FieldAuxiliaryElements<Binary64> fieldAux = new FieldAuxiliaryElements<>(fieldOrbit, 1);
        
        final UnnormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // WHEN
        final DSSTTesseralContext tesseralContext = new DSSTTesseralContext(aux, itrf, gravity, gravity.getMaxDegree(),
                                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                                         new double[] {gravity.getMu()});
        final FieldDSSTTesseralContext<Binary64> fieldTesseralContext =
                        new FieldDSSTTesseralContext<>(fieldAux, itrf, gravity, gravity.getMaxDegree(),
                                        Constants.WGS84_EARTH_ANGULAR_VELOCITY,           
                                        new Binary64[] {field.getZero().newInstance(gravity.getMu())});
        
        // THEN
        Assertions.assertEquals(tesseralContext.getMoa(), tesseralContext.getMuoa(), 0.);        
        Assertions.assertEquals(fieldTesseralContext.getMoa().getReal(), fieldTesseralContext.getMuoa().getReal(), 0.);
    }
}
