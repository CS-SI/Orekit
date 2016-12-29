/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.models.earth;


import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


public class SaastamoinenModelTest {

    private static double epsilon = 1e-6;

    @Test
    public void testFixedElevation() throws OrekitException {
        Utils.setDataRoot("atmosphere");
        SaastamoinenModel model = SaastamoinenModel.getStandardModel();
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing height of the station
        for (double height = 0; height < 5000; height += 100) {
            final double delay = model.pathDelay(FastMath.toRadians(5), height);
            Assert.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void testFixedHeight() throws OrekitException {
        Utils.setDataRoot("atmosphere");
        SaastamoinenModel model = SaastamoinenModel.getStandardModel();
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.pathDelay(FastMath.toRadians(elev), 350);
            Assert.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void NoFile() {
        Utils.setDataRoot("atmosphere");
        try {
            new SaastamoinenModel(273.16 + 18, 1013.25, 0.5, "^non-existent-file$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals("non-existent-file", oe.getParts()[0]);
        }
    }

    @Test
    public void testSerialization()
      throws OrekitException, IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Utils.setDataRoot("atmosphere");
        SaastamoinenModel model = SaastamoinenModel.getStandardModel();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(model);

        Assert.assertTrue(bos.size() > 1400);
        Assert.assertTrue(bos.size() < 1500);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        SaastamoinenModel deserialized  = (SaastamoinenModel) ois.readObject();

        double[] heights = new double[] {
            0.0, 250.0, 500.0, 750.0, 1000.0, 1250.0, 1500.0, 1750.0, 2000.0, 2250.0, 2500.0, 2750.0, 3000.0, 3250.0,
            3500.0, 3750.0, 4000.0, 4250.0, 4500.0, 4750.0, 5000.0
        };
        double[] elevations = new double[] {
            FastMath.toRadians(10.0), FastMath.toRadians(15.0), FastMath.toRadians(20.0),
            FastMath.toRadians(25.0), FastMath.toRadians(30.0), FastMath.toRadians(35.0),
            FastMath.toRadians(40.0), FastMath.toRadians(45.0), FastMath.toRadians(50.0),
            FastMath.toRadians(55.0), FastMath.toRadians(60.0), FastMath.toRadians(65.0),
            FastMath.toRadians(70.0), FastMath.toRadians(75.0), FastMath.toRadians(80.0),
            FastMath.toRadians(85.0), FastMath.toRadians(90.0)
        };
        for (int h = 0; h < heights.length; h++) {
            for (int e = 0; e < elevations.length; e++) {
                double height = heights[h];
                double elevation = elevations[e];
                double expectedValue = model.pathDelay(elevation, height);
                double actualValue = deserialized.pathDelay(elevation, height);
                assertEquals("For height=" + height + " elevation = " + elevation + " precision not met",
                             expectedValue, actualValue, epsilon);
            }
        }

    }

    @Test
    public void compareDefaultAndLoaded() throws OrekitException {
        Utils.setDataRoot("atmosphere");
        SaastamoinenModel defaultModel = new SaastamoinenModel(273.16 + 18, 1013.25, 0.5, null);
        SaastamoinenModel loadedModel  = new SaastamoinenModel(273.16 + 18, 1013.25, 0.5, SaastamoinenModel.DELTA_R_FILE_NAME);
        double[] heights = new double[] {
            0.0, 250.0, 500.0, 750.0, 1000.0, 1250.0, 1500.0, 1750.0, 2000.0, 2250.0, 2500.0, 2750.0, 3000.0, 3250.0,
            3500.0, 3750.0, 4000.0, 4250.0, 4500.0, 4750.0, 5000.0
        };
        double[] elevations = new double[] {
            FastMath.toRadians(10.0), FastMath.toRadians(15.0), FastMath.toRadians(20.0),
            FastMath.toRadians(25.0), FastMath.toRadians(30.0), FastMath.toRadians(35.0),
            FastMath.toRadians(40.0), FastMath.toRadians(45.0), FastMath.toRadians(50.0),
            FastMath.toRadians(55.0), FastMath.toRadians(60.0), FastMath.toRadians(65.0),
            FastMath.toRadians(70.0), FastMath.toRadians(75.0), FastMath.toRadians(80.0),
            FastMath.toRadians(85.0), FastMath.toRadians(90.0)
        };
        for (int h = 0; h < heights.length; h++) {
            for (int e = 0; e < elevations.length; e++) {
                double height = heights[h];
                double elevation = elevations[e];
                double expectedValue = defaultModel.pathDelay(elevation, height);
                double actualValue = loadedModel.pathDelay(elevation, height);
                assertEquals("For height=" + height + " elevation = " +
                             FastMath.toDegrees(elevation) + " precision not met",
                             expectedValue, actualValue, epsilon);
            }
        }
    }

    @Test
    public void compareExpectedValues() throws OrekitException {
        Utils.setDataRoot("atmosphere");
        SaastamoinenModel model = SaastamoinenModel.getStandardModel();
        double[] heights = new double[] {
            0.0, 250.0, 500.0, 750.0, 1000.0, 1250.0, 1500.0, 1750.0, 2000.0, 2250.0, 2500.0, 2750.0, 3000.0, 3250.0,
            3500.0, 3750.0, 4000.0, 4250.0, 4500.0, 4750.0, 5000.0
        };
        double[] elevations = new double[] {
            FastMath.toRadians(10.0), FastMath.toRadians(15.0), FastMath.toRadians(20.0),
            FastMath.toRadians(25.0), FastMath.toRadians(30.0), FastMath.toRadians(35.0),
            FastMath.toRadians(40.0), FastMath.toRadians(45.0), FastMath.toRadians(50.0),
            FastMath.toRadians(55.0), FastMath.toRadians(60.0), FastMath.toRadians(65.0),
            FastMath.toRadians(70.0), FastMath.toRadians(75.0), FastMath.toRadians(80.0),
            FastMath.toRadians(85.0), FastMath.toRadians(90.0)
        };
        double[][] expectedValues = new double[][] {
            {
                13.517414068807756, 9.204443522241771, 7.0029750138616835, 5.681588299211439, 4.8090544808193805,
                4.196707503563898, 3.7474156937027994, 3.408088733958258, 3.1468182787091985, 2.943369134588668,
                2.784381959210485, 2.6607786449639343, 2.5662805210588195, 2.496526411065139, 2.4485331622035984,
                2.420362989334734, 2.4109238764096896
            },
            {
                13.004543691989646, 8.85635958366884, 6.7385672069739835, 5.467398852004842, 4.627733401816586,
                4.038498891518074, 3.606157486803878, 3.279627416773643, 3.0282066103153564, 2.8324244661904134,
                2.6794262487842104, 2.56047665894495, 2.4695340768552505, 2.402401959167246, 2.356209754788866,
                2.329092816778967, 2.3200003434082928
            },
            {
                12.531363728988735, 8.534904937493899, 6.494310751299656, 5.269517663702665, 4.460196658874199,
                3.892306407739391, 3.475621589928269, 3.1609130970914956, 2.9185920283074447, 2.729893581384905,
                2.582428928493012, 2.4677793389442715, 2.380122124673593, 2.3154128046624067, 2.2708848382055904,
                2.2447411392156, 2.2359689784370986
            },
            {
                12.09063673875363, 8.235209195291242, 6.266604991324561, 5.084562550097057, 4.303522687504001,
                3.755568409663704, 3.353518885593948, 3.0498713508982442, 2.8160742841783275, 2.6340206738065692,
                2.4917559301110956, 2.3811563829818176, 2.2966034070866277, 2.234194258980332, 2.191259347593356,
                2.1660645619383274, 2.1576326612520003
            },
            {
                11.67727244511714, 7.953871771343415, 6.052791636499061, 4.910850401669602, 4.156351680934609,
                3.6271143683534492, 3.2388081756428404, 2.9455492155570195, 2.7197591598098305, 2.5439482552015917,
                2.4065694710150236, 2.2997761077823076, 2.218141111456481, 2.157894809849032, 2.1164586386563973,
                2.0921576169523175, 2.0840478264673044
            },
            {
                11.286432636721148, 7.688212194124222, 5.850570088713712, 4.747059102172618, 4.017661723553029,
                3.506085459183713, 3.1307362765144857, 2.8472616350388877, 2.6290036896596245, 2.459056474904878,
                2.3262584011701235, 2.223024699820715, 2.1441094810550236, 2.085868912585518, 2.0458105091517886,
                2.022315205377469, 2.0144705937765144
            },
            {
                10.915292255872545, 7.435769456080562, 5.6583502024136285, 4.591362033342799, 3.8858133055608204,
                3.391020479976734, 3.027986150306355, 2.7538117534167346, 2.5427137172039873, 2.3783406833254412,
                2.249897295933348, 2.1500476936060946, 2.0737181577274097, 2.0173844567055803, 1.978635920228296,
                1.9559066302818124, 1.9483141307804097
            },
            {
                10.560838722447652, 7.194507733765155, 5.474676340193435, 4.442196283017724, 3.759852141271757,
                3.281095182764508, 2.9298266864995415, 2.6645376694677676, 2.4602800254909183, 2.3012323491024245,
                2.1769492208902093, 2.080332602007238, 2.0064732734566384, 1.9519612730357911, 1.9144640973301363,
                1.8924666054100323, 1.8851149566358782
            },
            {
                10.221303680396087, 6.9632552008410915, 5.298576794548074, 4.299160340784349, 3.6390721146637293,
                3.175686404496119, 2.8356974323630437, 2.5789272031073223, 2.381228081225778, 2.2272865154903485,
                2.106992477081925, 2.0134758868645615, 1.9419852149640153, 1.8892200435803028, 1.8529228069725603,
                1.8316270449308856, 1.8245063513318645
            },
            {
                9.894629211990411, 6.740704058398638, 5.129125614634508, 4.161737017461952, 3.5228709097629975,
                3.0742748111390386, 2.7451382632192436, 2.4965641131187946, 2.305174987172354, 2.156146000844558,
                2.039689834231423, 1.949155743414788, 1.8799439192071106, 1.8288593407337934, 1.7937165420599064,
                1.7730958998798743, 1.7661974033814984
            },
            {
                9.579864280378851, 6.526143322382175, 4.965721065018929, 4.029207163135991, 3.4108058435845767,
                2.9764687866827866, 2.6577964388561925, 2.417125714868741, 2.2318215659378273, 2.087530132722662,
                1.9747751921856533, 1.8871174620329965, 1.820103416896286, 1.770639660836324, 1.7366102498117952,
                1.7166407238790062, 1.709956524792288
            },
            {
                9.274887896199909, 6.318551036055798, 4.807695974007752, 3.901070574943598, 3.302472329989199,
                2.881925175414427, 2.5733712370891264, 2.3403420235759187, 2.1609208036663294, 2.021209397507298,
                1.9120324913823707, 1.8271553141955852, 1.7622658032319802, 1.7143688314998151, 1.681415677692901,
                1.662075550126913, 1.6555984999945992
            },
            {
                8.979896361522131, 6.117657835260275, 4.654740323946152, 3.777036627543426, 3.197606518234132,
                2.7904044409768964, 2.4916434285107703, 2.266010367769514, 2.0922834230246177, 1.9570053034359607,
                1.851291869170061, 1.7691062591782465, 1.706273315177691, 1.6598930167213255, 1.627981703939375,
                1.6092508503238145, 1.6029743261170657
            },
            {
                8.69399537567174, 5.922971478822413, 4.5066379960473855, 3.6569305214071046, 3.0956861718504136,
                2.701448680145781, 2.412205508374445, 2.193765237935503, 2.025580422503155, 1.8946215440529706,
                1.7922869252688411, 1.7127316699243513, 1.6519133992903239, 1.6070243276625142, 1.5761438889147779,
                1.5580245420477885, 1.5519632546752067
            },
            {
                8.416815377025097, 5.734136251055505, 4.362963429392922, 3.5404077519897172, 2.996794592537471,
                2.615133166436779, 2.3351235507871273, 2.1236617698358717, 1.9608543092876316, 1.8340865056076205,
                1.735030640851246, 1.658028018024244, 1.5991650567003197, 1.555723443805895, 1.5258438192326151,
                1.5083184020062343, 1.5024665667687351
            },
            {
                8.1478867312736, 5.550837062543208, 4.223478184395355, 3.4272753528502906, 2.9007686780115858,
                2.531315719772937, 2.2602706846943197, 2.055584632739777, 1.8979986259230126, 1.7753006325382452,
                1.679428848769866, 1.60490532174873, 1.5479415024951926, 1.5059059371968162, 1.4769986856932136,
                1.4600505675451787, 1.4544027112557927
            },
            {
                7.886816833128103, 5.372810504566989, 4.087982930125399, 3.3173720077392095, 2.807472077886753,
                2.449877480332473, 2.187540848323867, 1.9894374123646559, 1.8369243760154002, 1.718180698301642,
                1.6254028270926364, 1.5532883580952295, 1.4981701861499142, 1.457501227681867, 1.4295392613913276,
                1.4131526030534576, 1.4077035129433761
            },
            {
                7.632757849842542, 5.199465832889435, 3.956158045029617, 3.2103200618121055, 2.7169983015566497,
                2.370922636048298, 2.1170374348830436, 1.9253162741272756, 1.7777165574325338, 1.6627979450991903,
                1.5730082599508786, 1.5032159322097494, 1.4498720192717967, 1.4105115179325056, 1.3834483541077152,
                1.3675873164708097, 1.3623112195283247
            },
            {
                7.385939247167236, 5.03097891384785, 3.8280091975097514, 3.1062430912053003, 2.629039083023718,
                2.294159790631063, 2.048490000181759, 1.8629731967599608, 1.720149999888605, 1.608950046027118,
                1.5220654734302106, 1.454530760098946, 1.402911820651357, 1.364823439078982, 1.3386341212762891,
                1.3232841113878862, 1.3181762050118586
            },
            {
                7.146111766814672, 4.867182513816216, 3.7034098358166165, 3.005038679030282, 2.5435078557931674,
                2.219513482041795, 1.981831207440737, 1.8023469685072222, 1.664168201116958, 1.5565841619968868,
                1.472524488341563, 1.407185083983383, 1.3572435292187972, 1.320392181006258, 1.295052611935796,
                1.2801995392223198, 1.2752551861465835
            },
            {
                6.913054711276373, 4.707928561310275, 3.58224790888857, 2.906616143639732, 2.460327972424674,
                2.14691689491338, 1.9170014355353862, 1.7433833914442447, 1.6097211330539392, 1.5056535083847744,
                1.4243410522646305, 1.3611366183164608, 1.3128263617229614, 1.277178068079704, 1.2526649111609156,
                1.238295129863562, 1.2335098392123367
            }
        };

        for (int h = 0; h < heights.length; h++) {
            for (int e = 0; e < elevations.length; e++) {
                double height = heights[h];
                double elevation = elevations[e];
                double expectedValue = expectedValues[h][e];
                double actualValue = model.pathDelay(elevation, height);
                assertEquals("For height=" + height + " elevation = " +
                             FastMath.toDegrees(elevation) + " precision not met",
                             expectedValue, actualValue, epsilon);
            }
        }
    }

}
