/* Copyright 2022-2025 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.troposphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.HeightDependentPressureTemperatureHumidityConverter;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;


public class ModifiedHopfieldModelTest extends AbstractPathDelayTest<ModifiedHopfieldModel> {

    private static final double epsilon = 1e-6;

    private double[][] expectedValues;

    private double[] elevations;

    private double[] heights;

    @Override
    protected ModifiedHopfieldModel buildTroposphericModel(final PressureTemperatureHumidityProvider provider) {
        return new ModifiedHopfieldModel(provider);
    }

    @Test
    @Override
    public void testFixedHeight() {
        doTestFixedHeight(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER);
    }

    @Test
    @Override
    public void testFieldFixedHeight() {
        doTestFieldFixedHeight(Binary64Field.getInstance(),
                               TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER);
    }

    @Test
    @Override
    public void testFixedElevation() {
        doTestFixedElevation(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER);
    }

    @Test
    @Override
    public void testFieldFixedElevation() {
        doTestFieldFixedElevation(Binary64Field.getInstance(),
                                  TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER);
    }

    @Test
    @Override
    public void testDelay() {
        doTestDelay(defaultDate, defaultPoint,
                    new TrackingCoordinates(FastMath.toRadians(192), FastMath.toRadians(5), 1.4e6),
                    TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER,
                    2.31371, 0.00942, 23.62185, 0.10438, 23.72623);
    }

    @Test
    @Override
    public void testFieldDelay() {
        doTestDelay(Binary64Field.getInstance(),
                    defaultDate, defaultPoint,
                    new TrackingCoordinates(FastMath.toRadians(192), FastMath.toRadians(5), 1.4e6),
                    TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER,
                    2.31371, 0.00942, 23.62185, 0.10438, 23.72623);
    }

    @Test
    public void testNegativeHeight() {
        Utils.setDataRoot("atmosphere");
        ModifiedHopfieldModel model = new ModifiedHopfieldModel(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER);
        final double height = -500.0;
        for (double elevation = 0; elevation < FastMath.PI; elevation += 0.1) {
            Assertions.assertEquals(model.pathDelay(new TrackingCoordinates(0.0, elevation, 0.0),
                                                    new GeodeticPoint(0.0, 0.0, 0.0),
                                                    null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                    model.pathDelay(new TrackingCoordinates(0.0, elevation, 0.0),
                                                    new GeodeticPoint(0.0, 0.0, height),
                                                    null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                    1.e-10);
        }
    }

    @Test
    public void testFieldNegativeHeight() {
        doTestFieldNegativeHeight(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldNegativeHeight(final Field<T> field) {
        final T zero = field.getZero();
        Utils.setDataRoot("atmosphere");
        ModifiedHopfieldModel model = buildTroposphericModel(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER);
        final T height = zero.subtract(500.0);
        for (double elevation = 0; elevation < FastMath.PI; elevation += 0.1) {
            Assertions.assertEquals(model.pathDelay(new FieldTrackingCoordinates<>(zero,
                                                                                   zero.newInstance(elevation),
                                                                                   zero),
                                                    new FieldGeodeticPoint<>(zero, zero, zero),
                                                    null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                    model.pathDelay(new FieldTrackingCoordinates<>(zero,
                                                                                   zero.newInstance(elevation),
                                                                                   zero),
                                                    new FieldGeodeticPoint<>(zero, zero, height),
                                                    null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                    1.e-10);
        }
    }

    @Test
    public void compareExpectedValues() {
        Utils.setDataRoot("atmosphere");
        // the reference values have been computed by our own implementation,
        // but checked against the plot in Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007
        // by making a screenshot of figure 5.4 and using it as the background of a gnuplot plot,
        // twicking the scales and offset to ensure the elevation scales at 0° and 90° line up
        // as well as the delay scale at 0m and 40m
        final HeightDependentPressureTemperatureHumidityConverter converter =
                        new HeightDependentPressureTemperatureHumidityConverter(new CIPM2007());
        final PressureTemperatureHumidityProvider provider =
            converter.getProvider(TroposphericModelUtils.STANDARD_ATMOSPHERE);
        ModifiedHopfieldModel model = buildTroposphericModel(provider);

        for (int h = 0; h < heights.length; h++) {
            for (int e = 0; e < elevations.length; e++) {
                double height = heights[h];
                double elevation = elevations[e];
                double expectedValue = expectedValues[h][e];
                final GeodeticPoint location = new GeodeticPoint(0.0, 0.0, height);
                final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
                double actualValue = model.pathDelay(new TrackingCoordinates(0.0, elevation, 0.0),
                                                     location,
                                                     null, date).getDelay();
                Assertions.assertEquals(expectedValue, actualValue, epsilon, "For height=" + height + " elevation = " +
                        FastMath.toDegrees(elevation) + " precision not met");
            }
        }
    }

    @Test
    public void compareFieldExpectedValues() {
        doCompareFieldExpectedValues(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCompareFieldExpectedValues(final Field<T> field) {
        final T zero = field.getZero();
        Utils.setDataRoot("atmosphere");
        // the reference values have been computed by our own implementation,
        // but checked against the plot in Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007
        // by making a screenshot of figure 5.4 and using it as the background of a gnuplot plot,
        // twicking the scales and offset to ensure the elevation scales at 0° and 90° line up
        // as well as the delay scale at 0m and 40m
        final HeightDependentPressureTemperatureHumidityConverter converter =
                        new HeightDependentPressureTemperatureHumidityConverter(new CIPM2007());
        final PressureTemperatureHumidityProvider provider = new PressureTemperatureHumidityProvider() {

            /** {@inheritDoc} */
            @Override
            public PressureTemperatureHumidity getWeatherParameters(final GeodeticPoint location,
                                                                    final AbsoluteDate date) {
                return converter.convert(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER.
                                         getWeatherParameters(location, date),
                                         location.getAltitude());
            }

            /** {@inheritDoc} */
            @Override
            public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T> getWeatherParameters(
                final FieldGeodeticPoint<T> location, final FieldAbsoluteDate<T> date) {
                return converter.convert(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER.
                                         getWeatherParameters(location, date),
                                         location.getAltitude());
            }
        };
        ModifiedHopfieldModel model = new ModifiedHopfieldModel(provider);

        for (int h = 0; h < heights.length; h++) {
            for (int e = 0; e < elevations.length; e++) {
                T height = zero.newInstance(heights[h]);
                T elevation = zero.newInstance(elevations[e]);
                double expectedValue = expectedValues[h][e];
                FieldGeodeticPoint<T> location = new FieldGeodeticPoint<>(zero, zero, height);
                FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
                T actualValue = model.pathDelay(new FieldTrackingCoordinates<>(zero, elevation, zero),
                                                location,
                                                null, date).getDelay();
                Assertions.assertEquals(expectedValue, actualValue.getReal(), epsilon, "For height=" + height + " elevation = " +
                        FastMath.toDegrees(elevation.getReal()) + " precision not met");
            }
        }
    }

    @BeforeEach
    public void setUp() {
        super.setUp();
        heights = new double[] {
            0.0, 250.0, 500.0, 750.0, 1000.0, 1250.0, 1500.0, 1750.0, 2000.0, 2250.0, 2500.0, 2750.0, 3000.0, 3250.0,
            3500.0, 3750.0, 4000.0, 4250.0, 4500.0, 4750.0, 5000.0
        };

        elevations = new double[] {
            FastMath.toRadians(10.0), FastMath.toRadians(15.0), FastMath.toRadians(20.0),
            FastMath.toRadians(25.0), FastMath.toRadians(30.0), FastMath.toRadians(35.0),
            FastMath.toRadians(40.0), FastMath.toRadians(45.0), FastMath.toRadians(50.0),
            FastMath.toRadians(55.0), FastMath.toRadians(60.0), FastMath.toRadians(65.0),
            FastMath.toRadians(70.0), FastMath.toRadians(75.0), FastMath.toRadians(80.0),
            FastMath.toRadians(85.0), FastMath.toRadians(90.0)
        };

        expectedValues = new double[][] {
            { 12.934879975754736, 8.84084160477251, 6.736022203196621, 5.46898001730666, 4.630745784092203, 
               4.041035315294115, 3.608411951861711, 3.2817269682153354, 3.030241489495224, 2.8344622441649374, 
               2.6815195803527967, 2.56266799580085, 2.4718587456496492, 2.4048902896027595, 2.358888630010781, 
               2.3319868666687267, 2.3231329225099335 },
            { 12.561159287653568, 8.5845772339206, 6.540524901381437, 5.310160285259242, 4.496223800666109, 
               3.9236205924564462, 3.503553629933834, 3.1863534038999624, 2.942171043169153, 2.7520781316145233, 
               2.6035781540118803, 2.488179277049209, 2.4000082578504416, 2.334985490083572, 2.29032039704188, 
               2.2642002736876043, 2.255603586487864 },
            { 12.196271109246876, 8.334396708338184, 6.349676953558442, 5.1551208397950665, 4.364905266082483, 
               3.8090027338029726, 3.401193538602676, 3.0932523859733427, 2.8561993139217607, 2.671657358487666, 
               2.5274942798666755, 2.415465885996259, 2.3298702638610886, 2.2667468414729446, 2.223386478825016, 
               2.1980293767510024, 2.189683818548383 },
            { 11.840056851277243, 8.090190002026562, 6.1633940364070305, 5.003793024325134, 4.236731955221294, 
               3.697130880280773, 3.301286235350814, 3.0023825684445966, 2.7722881127132357, 2.593164195299694, 
               2.4532341509182074, 2.344495510225485, 2.2614135935595785, 2.2001440162361927, 2.158057126701398, 
               2.1334447657599136, 2.1253443200250293 },
            { 11.492359892250795, 7.85184847179859, 5.981592891895236, 4.856109051837414, 4.111646381469352, 
               3.5879548184769834, 3.203786854867139, 2.9137031306992824, 2.6903997358985885, 2.516563366789527, 
               2.3807643900137903, 2.275236248208002, 2.1946074732175376, 2.135147072538955, 2.09430297036501, 
               2.0704174046704082, 2.062556164891846 },
            { 11.153025565763668, 7.619264848064081, 5.804191320080653, 4.7120019989793684, 3.9895917916751586, 
               3.4814249761974767, 3.1086511050880046, 2.8271737738921936, 2.6104969618924763, 2.4418200487968793, 
               2.310052046890637, 2.2076566064757457, 2.1294215227726028, 2.0717264515927147, 2.0320950152583372, 
               2.0089186289188268, 2.0012907971982328 },
            { 10.821901147844857, 7.392333225629629, 5.631108171923848, 4.57140580015122, 3.870512161113126, 
               3.377492418053854, 3.0158352632458802, 2.74275471734699, 2.5325430478403774, 2.3688998651454702, 
               2.2410645952252195, 2.1417254968001207, 2.0658257531059148, 2.0098529750053538, 1.9714046399731924, 
               1.9489201428519454, 1.9415200285087917 },
            { 10.498835844315545, 7.1709490545137635, 5.462263342114509, 4.434255241609992, 3.7543521884571724, 
               3.2761088410589285, 2.925296171925621, 2.6604066949628806, 2.456501726296164, 2.2977688845324415, 
               2.173769929688361, 2.0774122333761604, 2.0037905633251, 1.9494978421369638, 1.9122035936566382, 
               1.8903940171622173, 1.8832160353482224 },
            { 10.183680778166208, 6.955009130777359, 5.297577761910658, 4.30048595558501, 3.641057290764192, 
               3.1772265702307525, 2.8369912351285764, 2.5800909516284145, 2.3823372019064784, 2.2283936174238486, 
               2.108136363006301, 2.0146865300124164, 1.9432867380526482, 1.8906326274610628, 1.8544639934221097, 
               1.8333126863281592, 1.8263513566513527 },
            {  9.8762889769513, 6.744411587369776, 5.136973391991192, 4.17003441440436, 3.5305735984673015, 
               3.080798554205539, 2.750878414344576, 2.501769239642544, 2.3100141481017182, 2.1607410129566422, 
               2.0441326230277275, 1.9535184973264808, 1.8842854447198623, 1.8332292779310844, 1.798158321765943, 
               1.7776489460600666, 1.770898891218422 },
            {  9.576515360202594, 6.539055884991308, 4.980373215321884, 4.042837924633299, 3.42284795037922, 
                2.9867783608594287, 2.666916224632045, 2.425403815143106, 2.2394977037939823, 2.0947784558469777, 
                1.9817278497969246, 1.89387863994656, 1.8267582308665125, 1.777260110352495, 1.743259423989196, 
                1.7233759507509971, 1.7168318951757036 },
            {  9.284216726861493, 6.338842802972065, 4.8277012300356, 3.9188346212243026, 3.317827888706016, 
               2.8951201729394875, 2.585063730706338, 2.350957434542875, 2.1707534700819777, 2.0304737633052126, 
               1.9208915926332406, 1.835737853718858, 1.7706770214462317, 1.7226978087605191, 1.6897405056251327, 
               1.6704672109333, 1.664123979441577 },
            {  8.999251742730987, 6.143674430167964, 4.6788824423264685, 3.797963461679377, 3.2154616540713117, 
               2.8057787837040173, 2.505280543036565, 2.278393350973342, 2.103747506963166, 1.9677951819575752, 
               1.861593807216871, 1.7790674229211891, 1.7160141161378446, 1.6695154218036077, 1.6375751298722576, 
               1.6188965907406763, 1.6127491071981621 },
            {  8.721480927948047, 5.95345415587411, 4.533842859358816, 3.680164220224668, 3.115698180551306, 
               2.7187095925724325, 2.4275268139509656, 2.2076753107363376, 2.0384463300531492, 1.9067113847747645, 
               1.8038048526812085, 1.723839017482765, 1.6627421866627106, 1.6176863601327756, 1.586737215033144, 
               1.568638305375983, 1.5626815913686267 },
            {  8.450766644477026, 5.768086660756171, 4.392509482191019, 3.5653774819978135, 3.0184870907207904, 
               2.6338686007848495, 2.3517632337511234, 2.1387675497637897, 1.9748169073126105, 1.8471914680074653, 
               1.7474954887117944, 1.6700246902104292, 1.6108342741082338, 1.5671843937969427, 1.5372010319591705, 
               1.5196669185848175, 1.513896092100304 },
            {  8.18697308362473, 5.587477907800089, 4.25481029871462, 3.4535446372483163, 2.923778690710489, 
               2.5512124070717315, 2.277951026835166, 2.0716347900856587, 1.9128266557818219, 1.789204948129171, 
               1.6926368726521528, 1.6175968740213034, 1.5602637862576487, 1.5179836496443941, 1.4889412015012302, 
               1.4719573401350756, 1.4663676142537276 },
            {  7.929966253578143, 5.411535133280728, 4.120674276609269, 3.344607875551149, 2.8315239652758417, 
               2.470698203333716, 2.206051947830202, 2.006242236306332, 1.8524434383230521, 1.732721758786236, 
               1.6392005566165015, 1.5665283791822082, 1.511004494926278, 1.470058608730549, 1.4419326919666187, 
               1.4254848233025974, 1.4200715048977355 },
            {  7.6796139669652295, 5.240166837750182, 3.9900313563137013, 3.2385101800340683, 2.7416745728777223, 
               2.3922837703318565, 2.136028277734145, 1.94255557208964, 1.7936355603709304, 1.677712247755512, 
               1.587158484609674, 1.51679239055582, 1.4630305333043425, 1.4233841037320645, 1.3961508165822356, 
               1.3802249623630023, 1.374983450810773 },
            {  7.435785828440213, 5.073282777045855, 3.862812444013299, 3.135195321618908, 2.6541828407752037, 
               2.315927473388706, 2.0678428200672396, 1.8805409566527334, 1.7363717666909935, 1.6241471739096365, 
               1.536482989654237, 1.4683624648538693, 1.416316393306523, 1.3779353163675905, 1.3515712309641663, 
               1.3361536900899693, 1.3310794759885491 },
            {  7.198353222293529, 4.910793953319438, 3.7389494046447243, 3.034607853277241, 2.5690017601307655, 
               2.2415882581002715, 2.0014588970334697, 1.820165021268985, 1.6806212381467098, 1.5719977041902182, 
               1.4871467909251757, 1.4212125278974417, 1.3708369229284505, 1.3336877748252136, 1.3081699305939638, 
               1.2932472752599524, 1.2883359391581959 },
            {  6.967189300087932, 4.75261260608708, 3.618375054918015, 2.936693104300784, 2.486084981128291, 
               2.1692256460593353, 1.9368403456921452, 1.7613948657802356, 1.6263535884750422, 1.5212354105891261, 
               1.4391229908922085, 1.3753168718846802, 1.3265673236102633, 1.2906173511967862, 1.2659232483016118, 
               1.2514823201637046, 1.246729531299119 },
        };
    }

}
