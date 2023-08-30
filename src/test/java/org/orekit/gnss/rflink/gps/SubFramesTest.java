/* Copyright 2023 Thales Alenia Space
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
package org.orekit.gnss.rflink.gps;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.metric.parser.HexadecimalSequenceEncodedMessage;

/** This class aims at validating parsing of GPS RF link messages.
 * @author Luc Maisonobe
 */
public class SubFramesTest {

    @Test
    public void testWrongPreamble() {
        try {
            SubFrame.parse(new HexadecimalSequenceEncodedMessage("8c0308d40008c3c7978c9dcda8a6533a6f2ca78c052ab26a21a7f17a01ce0526c4732e9bb840"));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_GNSS_DATA, oe.getSpecifier());
            Assertions.assertEquals(0x8c, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testWrongSvId() {
        try {
            // replaced ID 57 = (111001) base 2 with ID 9 = (001001) base 2 and updated parity
            SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540008c3c4978c940da8a66c3a6f2c5b8c0529426a21a8317a01f10526c48f2e9bbbb0"));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_GNSS_DATA, oe.getSpecifier());
            Assertions.assertEquals(9, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testParityError() {
        try {
            SubFrame.parse(new HexadecimalSequenceEncodedMessage("8c0308540008c3e7978c9dcda8a6533a6f2ca78c052ab26a21a7f17a01ce0526c4732e9bb840"));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.GNSS_PARITY_ERROR, oe.getSpecifier());
            Assertions.assertEquals(0x1, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testSubFrame1() {
        SubFrame1 sf = (SubFrame1) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b030854068892030900060162f57126c4c680d21f5fdfcc40a7978070bc00ffd4307a443c40"));
        Assertions.assertEquals(139,     sf.getPreamble());
        Assertions.assertEquals(194,     sf.getMessage());
        Assertions.assertEquals(0,       sf.getIntegrityStatus());
        Assertions.assertEquals(5016,    sf.getTow());
        Assertions.assertEquals(0,       sf.getAlert());
        Assertions.assertEquals(1,       sf.getAntiSpoofing());
        Assertions.assertEquals(1,       sf.getId());
        Assertions.assertEquals(194,     sf.getWeekNumber());
        Assertions.assertEquals(1,       sf.getCaOrPFlag());
        Assertions.assertEquals(0,       sf.getUraIndex());
        Assertions.assertEquals(0,       sf.getSvHealth());
        Assertions.assertEquals(94,      sf.getIODC());
        Assertions.assertEquals(0,       sf.getL2PDataFlag());
        Assertions.assertEquals(363477,  sf.getReserved04());
        Assertions.assertEquals(2540742, sf.getReserved05());
        Assertions.assertEquals(3442647, sf.getReserved06());
        Assertions.assertEquals(64708,   sf.getReserved07());
        Assertions.assertEquals(10,      sf.getTGD());
        Assertions.assertEquals(450,     sf.getTOC());
        Assertions.assertEquals(0.0,                   sf.getAF2(), 1.0e-15);
        Assertions.assertEquals(7.445578376064077E-9,  sf.getAF1(), 1.0e-15);
        Assertions.assertEquals(2.3320456966757774E-4, sf.getAF0(), 1.0e-15);
    }

    @Test
    public void testSubFrame2() {
        SubFrame2 sf = (SubFrame2) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540d2ea746b030f34bb0912d308f53300904199380148d3fca68580d378bd40e11fd40"));
        Assertions.assertEquals(139,                  sf.getPreamble());
        Assertions.assertEquals(194,                  sf.getMessage());
        Assertions.assertEquals(0,                    sf.getIntegrityStatus());
        Assertions.assertEquals(10122,                sf.getTow());
        Assertions.assertEquals(0,                    sf.getAlert());
        Assertions.assertEquals(1,                    sf.getAntiSpoofing());
        Assertions.assertEquals(2,                    sf.getId());
        Assertions.assertEquals(107,                  sf.getIODE());
        Assertions.assertEquals(24.46875,             sf.getCrs(), 1.0e-15);
        Assertions.assertEquals(4.275178078327134E-9, sf.getDeltaN(), 1.0e-15);
        Assertions.assertEquals(1.6736267155104847,   sf.getM0(), 1.0e-15);
        Assertions.assertEquals(3.3764149269486122E-6,sf.getCuc(), 1.0e-15);
        Assertions.assertEquals(0.01214603427797556,  sf.getE(), 1.0e-15);
        Assertions.assertEquals(3.822370874232414E-4, sf.getCus(), 1.0e-15);
        Assertions.assertEquals(-3038.34787940979,    sf.getSqrtA(), 1.0e-15);
        Assertions.assertEquals(14400,                sf.getToe());
        Assertions.assertEquals(0,                    sf.getFitInterval());
        Assertions.assertEquals(27900,                sf.getAODO());
    }

    @Test
    public void testSubFrame3() {
        SubFrame3 sf = (SubFrame3) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540326b70ffe3cceee041fc9004128d12d58220319126b123f7db4ffa35bbd7be89080"));
        Assertions.assertEquals(139,                  sf.getPreamble());
        Assertions.assertEquals(194,                  sf.getMessage());
        Assertions.assertEquals(0,                    sf.getIntegrityStatus());
        Assertions.assertEquals(2418,                 sf.getTow());
        Assertions.assertEquals(0,                    sf.getAlert());
        Assertions.assertEquals(1,                    sf.getAntiSpoofing());
        Assertions.assertEquals(3,                    sf.getId());
        Assertions.assertEquals(1.220162957906723E-4, sf.getCic(),             1.0e-15);
        Assertions.assertEquals(-1.2586250585571226,  sf.getUppercaseOmega0(), 1.0e-15);
        Assertions.assertEquals(1.210719347000122E-7, sf.getCis(),             1.0e-15);
        Assertions.assertEquals(0.9889704584978043,   sf.getI0(),              1.0e-15);
        Assertions.assertEquals(396.53125,            sf.getCrc(),             1.0e-15);
        Assertions.assertEquals(0.9396583427359628,   sf.getLowercaseOmega(),  1.0e-15);
        Assertions.assertEquals(5.983641742696371E-6, sf.getOmegaDot(),        1.0e-15);
        Assertions.assertEquals(94,                   sf.getIODE());
        Assertions.assertEquals(5.71773816674512E-9,  sf.getIDot(),            1.0e-15);
    }

    @Test
    public void testSubFrame4Page01() {
        SubFrame4A0 page01 = (SubFrame4A0) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540008c3c7978c9dcda8a6533a6f2ca78c052ab26a21a7f17a01ce0526c4732e9bb840"));
        Assertions.assertEquals(     139, page01.getPreamble());
        Assertions.assertEquals(     194, page01.getMessage());
        Assertions.assertEquals(       0, page01.getIntegrityStatus());
        Assertions.assertEquals(      24, page01.getTow());
        Assertions.assertEquals(       0, page01.getAlert());
        Assertions.assertEquals(       1, page01.getAntiSpoofing());
        Assertions.assertEquals(       4, page01.getId());
        Assertions.assertEquals(       1, page01.getDataId());
        Assertions.assertEquals(      57, page01.getSvId());
        Assertions.assertEquals(   30921, page01.getReserved03());
        Assertions.assertEquals( 3580569, page01.getReserved04());
        Assertions.assertEquals( 3829548, page01.getReserved05());
        Assertions.assertEquals(14877002, page01.getReserved06());
        Assertions.assertEquals( 2531866, page01.getReserved07());
        Assertions.assertEquals(12969991, page01.getReserved08());
        Assertions.assertEquals(       5, page01.getReservedA09());
        Assertions.assertEquals(    9924, page01.getReservedB09());
        Assertions.assertEquals( 3336635, page01.getReserved10());
        Assertions.assertFalse(page01.hasParityErrors());

        // raw 30 bits words
        final int word01 = 0x8b030854 >>> 2;
        final int word02 = 0x40008c3c & 0x3FFFFFFF;
        final int word03 = 0x7978c9dc >>> 2;
        final int word04 = 0xcda8a653 & 0x3FFFFFFF;
        final int word05 = 0x3a6f2ca7 >>> 2;
        final int word06 = 0x78c052ab & 0x3FFFFFFF;
        final int word07 = 0x26a21a7f >>> 2;
        final int word08 = 0xf17a01ce & 0x3FFFFFFF;
        final int word09 = 0x0526c473 >>> 2;
        final int word10 = 0x32e9bb84 & 0x3FFFFFFF;

        Assertions.assertEquals((word01 >>>  8) &   0x3FFF, page01.getMessage());
        Assertions.assertEquals((word02 >>>  8) &      0x7, page01.getId());
        Assertions.assertEquals((word03 >>>  6) &   0xFFFF, page01.getReserved03());
        Assertions.assertEquals((word04 >>>  6) & 0xFFFFFF, page01.getReserved04());
        Assertions.assertEquals((word05 >>>  6) & 0xFFFFFF, page01.getReserved05());
        Assertions.assertEquals((word06 >>>  6) & 0xFFFFFF, page01.getReserved06());
        Assertions.assertEquals((word07 >>>  6) & 0xFFFFFF, page01.getReserved07());
        Assertions.assertEquals((word08 >>>  6) & 0xFFFFFF, page01.getReserved08());
        Assertions.assertEquals((word09 >>> 22) &     0xFF, page01.getReservedA09());
        Assertions.assertEquals((word09 >>>  6) &   0xFFFF, page01.getReservedB09());
        Assertions.assertEquals((word10 >>>  8) & 0x3FFFFF, page01.getReserved10());

    }

    @Test
    public void testSubFrame4Page02() {
        SubFrameAlmanac page02 = (SubFrameAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540012c68595a43dc90200abfd4c00ce843061a7649d8a4a4b16351ee15418d4009d00"));
        Assertions.assertEquals(                  139, page02.getPreamble());
        Assertions.assertEquals(                  194, page02.getMessage());
        Assertions.assertEquals(                    0, page02.getIntegrityStatus());
        Assertions.assertEquals(                   54, page02.getTow());
        Assertions.assertEquals(                    0, page02.getAlert());
        Assertions.assertEquals(                    1, page02.getAntiSpoofing());
        Assertions.assertEquals(                    4, page02.getId());
        Assertions.assertEquals(                    1, page02.getDataId());
        Assertions.assertEquals(                   25, page02.getSvId());
        Assertions.assertEquals(  0.01101827621459961, page02.getE(),               1.0e-15);
        Assertions.assertEquals(               147456, page02.getToaA());
        Assertions.assertEquals( 0.012283830527990513, page02.getDeltai(),          1.0e-15);
        Assertions.assertEquals( 7.411051557187493E-7, page02.getOmegaDot(),        1.0e-15);
        Assertions.assertEquals(                    0, page02.getSvHealth());
        Assertions.assertEquals(        5153.51171875, page02.getSqrtA(),           1.0e-15);
        Assertions.assertEquals(   2.9032354083909366, page02.getUppercaseOmega0(), 1.0e-15);
        Assertions.assertEquals(    1.010542800638158, page02.getLowercaseOmega(),  1.0e-15);
        Assertions.assertEquals(   0.7579138416051279, page02.getM0(),              1.0e-15);
        Assertions.assertEquals(4.0531158447265625E-4, page02.getAF0(),             1.0e-15);
        Assertions.assertEquals(3.637978807091713E-12, page02.getAF1(),             1.0e-15);
    }

    @Test
    public void testSubFrame4Page03() {
        SubFrameAlmanac page03 = (SubFrameAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b030854001cc0c5a3f0ad493ede0afd37008e84318fd742311b44484c0dfedaab307c003f80"));
        Assertions.assertEquals( 84, page03.getTow());
        Assertions.assertEquals(  4, page03.getId());
        Assertions.assertEquals(  1, page03.getDataId());
        Assertions.assertEquals( 26, page03.getSvId());
    }

    @Test
    public void testSubFrame4Page04() {
        SubFrameAlmanac page04 = (SubFrameAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540026cc05b5a184c9047207fd6000068433dd5a13e92c06f96b70b6cad1344bfeedc0"));
        Assertions.assertEquals(114, page04.getTow());
        Assertions.assertEquals(  4, page04.getId());
        Assertions.assertEquals(  1, page04.getDataId());
        Assertions.assertEquals( 27, page04.getSvId());
    }

    @Test
    public void testSubFrame4Page05() {
        // page 5 should be almanac for PRN 28, but here holds a dummy almanac (SV 0)
        SubFrameDummyAlmanac page05 = (SubFrameDummyAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540030c2040aaaab2aaaaabcaaaaaaf2aaaaabcaaaaaaf2aaaaabcaaaaaaf2aaaaabc0"));
        Assertions.assertEquals(144, page05.getTow());
        Assertions.assertEquals(  4, page05.getId());
        Assertions.assertEquals(  1, page05.getDataId());
        Assertions.assertEquals(  0, page05.getSvId());
    }

    @Test
    public void testSubFrame4Page06() {
        SubFrame4A0 page06 = (SubFrame4A0) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b030854003acbc7978c9dcda8a6533a6f2ca78c052ab26a21a7f17a01ce0526c47183039880"));
        Assertions.assertEquals(174, page06.getTow());
        Assertions.assertEquals(  4, page06.getId());
        Assertions.assertEquals(  1, page06.getDataId());
        Assertions.assertEquals( 57, page06.getSvId());
    }

    @Test
    public void testSubFrame4Page07() {
        SubFrameAlmanac page07 = (SubFrameAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540044cb05d113640905defcfd65003a84320e6a46fe0098ef2fc4b41fe2e2d3ffbb00"));
        Assertions.assertEquals(204, page07.getTow());
        Assertions.assertEquals(  4, page07.getId());
        Assertions.assertEquals(  1, page07.getDataId());
        Assertions.assertEquals( 29, page07.getSvId());
    }

    @Test
    public void testSubFrame4Page08() {
        SubFrameAlmanac page08 = (SubFrameAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b030854004ec2c5e31142893ed016fd3500f284330ae4c83f4e64d436513d0410dae4007640"));
        Assertions.assertEquals(234, page08.getTow());
        Assertions.assertEquals(  4, page08.getId());
        Assertions.assertEquals(  1, page08.getDataId());
        Assertions.assertEquals( 30, page08.getSvId());
    }

    @Test
    public void testSubFrame4Page09() {
        SubFrameAlmanac page09 = (SubFrameAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540058ccc5f585130901e8b9fd4600e284323814cf61d504f39dc92ad34b1f94002c00"));
        Assertions.assertEquals(264, page09.getTow());
        Assertions.assertEquals(  4, page09.getId());
        Assertions.assertEquals(  1, page09.getDataId());
        Assertions.assertEquals( 31, page09.getSvId());
    }

    @Test
    public void testSubFrame4Page10() {
        SubFrameAlmanac page10 = (SubFrameAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540062c0060343d0c902b213fd60000684349462101d9b28b1abdad379494b4ffe0800"));
        Assertions.assertEquals(294, page10.getTow());
        Assertions.assertEquals(  4, page10.getId());
        Assertions.assertEquals(  1, page10.getDataId());
        Assertions.assertEquals( 32, page10.getSvId());
    }

    @Test
    public void testSubFrame4Page11() {
        SubFrame4A0 page11 = (SubFrame4A0) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b030854006cc647978c9dcda8a6533a6f2ca78c052ab26a21a7f17a01ce0526c473fddb08c0"));
        Assertions.assertEquals(324, page11.getTow());
        Assertions.assertEquals(  4, page11.getId());
        Assertions.assertEquals(  1, page11.getDataId());
        Assertions.assertEquals( 57, page11.getSvId());
    }

    @Test
    public void testSubFrame4Page12() {
        SubFrame4A1 page12 = (SubFrame4A1) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540076c307e04887f64206ee7d8a2d7572d5e09e14d671b724005fe9910ba732a8f2c0"));
        Assertions.assertEquals(     139, page12.getPreamble());
        Assertions.assertEquals(     194, page12.getMessage());
        Assertions.assertEquals(       0, page12.getIntegrityStatus());
        Assertions.assertEquals(     354, page12.getTow());
        Assertions.assertEquals(       0, page12.getAlert());
        Assertions.assertEquals(       1, page12.getAntiSpoofing());
        Assertions.assertEquals(       4, page12.getId());
        Assertions.assertEquals(       1, page12.getDataId());
        Assertions.assertEquals(      62, page12.getSvId());
        Assertions.assertEquals(    1160, page12.getReserved03());
        Assertions.assertEquals(14223387, page12.getReserved04());
        Assertions.assertEquals( 8227373, page12.getReserved05());
        Assertions.assertEquals( 6075768, page12.getReserved06());
        Assertions.assertEquals(14765415, page12.getReserved07());
        Assertions.assertEquals(14454785, page12.getReserved08());
        Assertions.assertEquals(     233, page12.getReservedA09());
        Assertions.assertEquals(   37131, page12.getReservedB09());
        Assertions.assertEquals( 3353231, page12.getReserved10());
    }

    @Test
    public void testSubFrame4Page13() {
        SubFrame4C page13 = (SubFrame4C) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540080ce4747026e640a3f104b3e59089759d1957cd4b951ec3140e6f502c059fa8380"));
        Assertions.assertEquals(139, page13.getPreamble());
        Assertions.assertEquals(194, page13.getMessage());
        Assertions.assertEquals(  0, page13.getIntegrityStatus());
        Assertions.assertEquals(384, page13.getTow());
        Assertions.assertEquals(  0, page13.getAlert());
        Assertions.assertEquals(  1, page13.getAntiSpoofing());
        Assertions.assertEquals(  4, page13.getId());
        Assertions.assertEquals(  1, page13.getDataId());
        Assertions.assertEquals( 52, page13.getSvId());
        Assertions.assertEquals( 48, page13.getERD( 1));
        Assertions.assertEquals(  9, page13.getERD( 2));
        Assertions.assertEquals( 41, page13.getERD( 3));
        Assertions.assertEquals(  9, page13.getERD( 4));
        Assertions.assertEquals(  0, page13.getERD( 5));
        Assertions.assertEquals( 40, page13.getERD( 6));
        Assertions.assertEquals( 63, page13.getERD( 7));
        Assertions.assertEquals(  4, page13.getERD( 8));
        Assertions.assertEquals(  4, page13.getERD( 9));
        Assertions.assertEquals( 44, page13.getERD(10));
        Assertions.assertEquals( 62, page13.getERD(11));
        Assertions.assertEquals( 22, page13.getERD(12));
        Assertions.assertEquals( 18, page13.getERD(13));
        Assertions.assertEquals(  2, page13.getERD(14));
        Assertions.assertEquals( 23, page13.getERD(15));
        Assertions.assertEquals( 22, page13.getERD(16));
        Assertions.assertEquals( 29, page13.getERD(17));
        Assertions.assertEquals(  5, page13.getERD(18));
        Assertions.assertEquals(  5, page13.getERD(19));
        Assertions.assertEquals( 31, page13.getERD(20));
        Assertions.assertEquals( 13, page13.getERD(21));
        Assertions.assertEquals( 18, page13.getERD(22));
        Assertions.assertEquals( 52, page13.getERD(23));
        Assertions.assertEquals(  4, page13.getERD(24));
        Assertions.assertEquals( 30, page13.getERD(25));
        Assertions.assertEquals( 48, page13.getERD(26));
        Assertions.assertEquals( 49, page13.getERD(27));
        Assertions.assertEquals( 30, page13.getERD(28));
        Assertions.assertEquals( 14, page13.getERD(29));
        Assertions.assertEquals( 27, page13.getERD(30));
    }

    @Test
    public void testSubFrame4Page14() {
        SubFrame4B page14 = (SubFrame4B) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b030854008ac7875aaaac6aaaaaaaaaaaaa56aaaaaaaaaaaaa56aaaaaaaaaaaaa56aaaaa5c0"));
        Assertions.assertEquals(     139, page14.getPreamble());
        Assertions.assertEquals(     194, page14.getMessage());
        Assertions.assertEquals(       0, page14.getIntegrityStatus());
        Assertions.assertEquals(     414, page14.getTow());
        Assertions.assertEquals(       0, page14.getAlert());
        Assertions.assertEquals(       1, page14.getAntiSpoofing());
        Assertions.assertEquals(       4, page14.getId());
        Assertions.assertEquals(       1, page14.getDataId());
        Assertions.assertEquals(      53, page14.getSvId());
        Assertions.assertEquals(   43690, page14.getReserved03());
        Assertions.assertEquals(11184810, page14.getReserved04());
        Assertions.assertEquals(11184810, page14.getReserved05());
        Assertions.assertEquals(11184810, page14.getReserved06());
        Assertions.assertEquals(11184810, page14.getReserved07());
        Assertions.assertEquals(11184810, page14.getReserved08());
        Assertions.assertEquals(11184810, page14.getReserved09());
        Assertions.assertEquals( 2796202, page14.getReserved10());
    }

    @Test
    public void testSubFrame4Page15() {
        SubFrame4B page15 = (SubFrame4B) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540094cd476aaaa9aaaaabcd3f494828e2dfabec3504c536fcf2a194c3eb3d2833efc0"));
        Assertions.assertEquals(444, page15.getTow());
        Assertions.assertEquals(  4, page15.getId());
        Assertions.assertEquals(  1, page15.getDataId());
        Assertions.assertEquals( 54, page15.getSvId());
    }

    @Test
    public void testSubFrame4Page16() {
        SubFrame4A0 page16 = (SubFrame4A0) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b030854009ec487978c9dcda8a6533a6f2ca78c052ab26a21a7f17a01ce0526c4705db5bb00"));
        Assertions.assertEquals(474, page16.getTow());
        Assertions.assertEquals(  4, page16.getId());
        Assertions.assertEquals(  1, page16.getDataId());
        Assertions.assertEquals( 57, page16.getSvId());
    }

    @Test
    public void testSubFrame4Page17() {
        SubFrame4B page17 = (SubFrame4B) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400a8c30772053a958dcaf3f84938693528c3c504838551d08c0b2239494d5d080580"));
        Assertions.assertEquals(504, page17.getTow());
        Assertions.assertEquals(  4, page17.getId());
        Assertions.assertEquals(  1, page17.getDataId());
        Assertions.assertEquals( 55, page17.getSvId());
    }

    @Test
    public void testSubFrame4Page18() {
        SubFrame4D page18 = (SubFrame4D) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400b2c647811ff03fc08ff7f301051fffffea4ffffff0ffc930b7128907c048000f00"));
        Assertions.assertEquals(                   139, page18.getPreamble());
        Assertions.assertEquals(                   194, page18.getMessage());
        Assertions.assertEquals(                     0, page18.getIntegrityStatus());
        Assertions.assertEquals(                   534, page18.getTow());
        Assertions.assertEquals(                     0, page18.getAlert());
        Assertions.assertEquals(                     1, page18.getAntiSpoofing());
        Assertions.assertEquals(                     4, page18.getId());
        Assertions.assertEquals(                     1, page18.getDataId());
        Assertions.assertEquals(                    56, page18.getSvId());
        Assertions.assertEquals( 1.5832483768463135E-8, page18.getAlpha0(), 1.0e-15);
        Assertions.assertEquals(  6.047563327615444E-7, page18.getAlpha1(), 1.0e-15);
        Assertions.assertEquals(  1.539999355602027E-6, page18.getAlpha2(), 1.0e-15);
        Assertions.assertEquals(  3.844682506704244E-9, page18.getAlpha3(), 1.0e-15);
        Assertions.assertEquals(              129024.0, page18.getBeta0(),  1.0e-15);
        Assertions.assertEquals(    1267290.9695821572, page18.getBeta1(),  1.0e-15);
        Assertions.assertEquals(     6640.185091184218, page18.getBeta2(),  1.0e-15);
        Assertions.assertEquals(    10568.182803070738, page18.getBeta3(),  1.0e-15);
        Assertions.assertEquals( 1.4901155864777138E-8, page18.getA1(),     1.0e-15);
        Assertions.assertEquals(-9.313225746154785E-10, page18.getA0(),     1.0e-15);
        Assertions.assertEquals(                147456, page18.getTot());
        Assertions.assertEquals(                   194, page18.getWeekNumberT());
        Assertions.assertEquals(                    18, page18.getDeltaTLs());
        Assertions.assertEquals(                   137, page18.getWeekNumberLsf());
        Assertions.assertEquals(                     7, page18.getDn());
        Assertions.assertEquals(                    18, page18.getDeltaTLsf());
        Assertions.assertEquals(                     0, page18.getReserved10());
    }

    @Test
    public void testSubFrame4Page19() {
        SubFrame4A1 page19 = (SubFrame4A1) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400bcc007a8462455cd2b586377bd0162ada731d15f824cc2805767a31bedb12b2780"));
        Assertions.assertEquals(564, page19.getTow());
        Assertions.assertEquals(  4, page19.getId());
        Assertions.assertEquals(  1, page19.getDataId());
        Assertions.assertEquals( 58, page19.getSvId());
    }

    @Test
    public void testSubFrame4Page20() {
        SubFrame4A1 page20 = (SubFrame4A1) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400c6cf47b1ef74c48f76b6634b680ce21fc6dc1e416b04c2c0749266de7d218ad940"));
        Assertions.assertEquals(594, page20.getTow());
        Assertions.assertEquals(  4, page20.getId());
        Assertions.assertEquals(  1, page20.getDataId());
        Assertions.assertEquals( 59, page20.getSvId());
    }

    @Test
    public void testSubFrame4Page21() {
        SubFrame4A0 page21 = (SubFrame4A0) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400d0c147978c9dcda8a6533a6f2ca78c052ab26a21a7f17a01ce0526c473e1a50a00"));
        Assertions.assertEquals(624, page21.getTow());
        Assertions.assertEquals(  4, page21.getId());
        Assertions.assertEquals(  1, page21.getDataId());
        Assertions.assertEquals( 57, page21.getSvId());
    }

    @Test
    public void testSubFrame4Page22() {
        SubFrame4A1 page22 = (SubFrame4A1) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400dac887cba28a19cb45c2edb526417155cea9358a10b72e804cb02d07310876ac40"));
        Assertions.assertEquals(654, page22.getTow());
        Assertions.assertEquals(  4, page22.getId());
        Assertions.assertEquals(  1, page22.getDataId());
        Assertions.assertEquals( 60, page22.getSvId());
    }

    @Test
    public void testSubFrame4Page23() {
        SubFrame4A1 page23 = (SubFrame4A1) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400e4cbc7d1a0d4b815fad54cf28e58bd3fb3cbc178da9482c064cfae562eebf580c0"));
        Assertions.assertEquals(684, page23.getTow());
        Assertions.assertEquals(  4, page23.getId());
        Assertions.assertEquals(  1, page23.getDataId());
        Assertions.assertEquals( 61, page23.getSvId());
    }

    @Test
    public void testSubFrame4Page24() {
        SubFrame4A1 page24 = (SubFrame4A1) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400eec207e04887f64206ee7d8a2d7572d5e09e14d671b724005fe9a10aceaaaaad00"));
        Assertions.assertEquals(714, page24.getTow());
        Assertions.assertEquals(  4, page24.getId());
        Assertions.assertEquals(  1, page24.getDataId());
        Assertions.assertEquals( 62, page24.getSvId());
    }

    @Test
    public void testSubFrame4Page25() {
        SubFrame4E page25 = (SubFrame4E) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085400f8cc07fb9bcbeaeaeeffca9ca94ab266663cbbbbcd6aeac025000fc04000000000"));
        Assertions.assertEquals(139, page25.getPreamble());
        Assertions.assertEquals(194, page25.getMessage());
        Assertions.assertEquals(  0, page25.getIntegrityStatus());
        Assertions.assertEquals(744, page25.getTow());
        Assertions.assertEquals(  0, page25.getAlert());
        Assertions.assertEquals(  1, page25.getAntiSpoofing());
        Assertions.assertEquals(  4, page25.getId());
        Assertions.assertEquals(  1, page25.getDataId());
        Assertions.assertEquals( 63, page25.getSvId());
        Assertions.assertEquals( 11, page25.getAntiSpoofing( 1));
        Assertions.assertEquals(  9, page25.getAntiSpoofing( 2));
        Assertions.assertEquals( 11, page25.getAntiSpoofing( 3));
        Assertions.assertEquals( 12, page25.getAntiSpoofing( 4));
        Assertions.assertEquals( 10, page25.getAntiSpoofing( 5));
        Assertions.assertEquals( 11, page25.getAntiSpoofing( 6));
        Assertions.assertEquals( 10, page25.getAntiSpoofing( 7));
        Assertions.assertEquals( 11, page25.getAntiSpoofing( 8));
        Assertions.assertEquals( 11, page25.getAntiSpoofing( 9));
        Assertions.assertEquals( 11, page25.getAntiSpoofing(10));
        Assertions.assertEquals( 12, page25.getAntiSpoofing(11));
        Assertions.assertEquals( 10, page25.getAntiSpoofing(12));
        Assertions.assertEquals(  9, page25.getAntiSpoofing(13));
        Assertions.assertEquals( 12, page25.getAntiSpoofing(14));
        Assertions.assertEquals( 10, page25.getAntiSpoofing(15));
        Assertions.assertEquals(  9, page25.getAntiSpoofing(16));
        Assertions.assertEquals( 10, page25.getAntiSpoofing(17));
        Assertions.assertEquals( 12, page25.getAntiSpoofing(18));
        Assertions.assertEquals(  9, page25.getAntiSpoofing(19));
        Assertions.assertEquals(  9, page25.getAntiSpoofing(20));
        Assertions.assertEquals(  9, page25.getAntiSpoofing(21));
        Assertions.assertEquals(  9, page25.getAntiSpoofing(22));
        Assertions.assertEquals( 12, page25.getAntiSpoofing(23));
        Assertions.assertEquals( 11, page25.getAntiSpoofing(24));
        Assertions.assertEquals( 11, page25.getAntiSpoofing(25));
        Assertions.assertEquals( 11, page25.getAntiSpoofing(26));
        Assertions.assertEquals( 11, page25.getAntiSpoofing(27));
        Assertions.assertEquals( 12, page25.getAntiSpoofing(28));
        Assertions.assertEquals( 10, page25.getAntiSpoofing(29));
        Assertions.assertEquals( 11, page25.getAntiSpoofing(30));
        Assertions.assertEquals( 10, page25.getAntiSpoofing(31));
        Assertions.assertEquals( 11, page25.getAntiSpoofing(32));
        Assertions.assertEquals(  0, page25.getReserved8());
        Assertions.assertEquals(  0, page25.getSvHealth(1));
        Assertions.assertEquals(  0, page25.getSvHealth(2));
        Assertions.assertEquals( 63, page25.getSvHealth(3));
        Assertions.assertEquals(  0, page25.getSvHealth(4));
        Assertions.assertEquals(  0, page25.getSvHealth(5));
        Assertions.assertEquals(  0, page25.getSvHealth(6));
        Assertions.assertEquals(  0, page25.getSvHealth(7));
        Assertions.assertEquals(  0, page25.getSvHealth(8));
        Assertions.assertEquals(  0, page25.getReserved10());
    }

    @Test
    public void testSubFrame4Page26() {
        SubFrame4A0 page26 = (SubFrame4A0) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b0308540102cd07978c9dcda8a6533a6f2ca78c052ab26a21a7f17a01ce0526c4732e9bb840"));
        Assertions.assertEquals(774, page26.getTow());
        Assertions.assertEquals(  4, page26.getId());
        Assertions.assertEquals(  1, page26.getDataId());
        Assertions.assertEquals( 57, page26.getSvId());
    }

    @Test
    public void testSubFrame5Almanac() {
        SubFrameAlmanac almanac = (SubFrameAlmanac) SubFrame.parse(new HexadecimalSequenceEncodedMessage("8b03085412acda043242dece45a69afd4300de8436811f6b0e290a9631922b9fd70b38007040"));
        Assertions.assertEquals(               14340,   almanac.getTow());
        Assertions.assertEquals(                   5,   almanac.getId());
        Assertions.assertEquals(                   1,   almanac.getDataId());
        Assertions.assertEquals(                   3,   almanac.getSvId());
        Assertions.assertEquals(                   3,   almanac.getPRN());
        Assertions.assertEquals(              233472,   almanac.getToaA());
        Assertions.assertEquals( 0.03467036265119664,   almanac.getDeltai(),          1.0e-15);
        Assertions.assertEquals(7.410022942913008e-7,   almanac.getOmegaDot(),        1.0e-15);
        Assertions.assertEquals(                   0,   almanac.getSvHealth());
        Assertions.assertEquals(         5153.703125,   almanac.getSqrtA(),           1.0e-15);
        Assertions.assertEquals(   6.054706808373034,   almanac.getUppercaseOmega0(), 1.0e-15);
        Assertions.assertEquals(   1.039346136184126,   almanac.getLowercaseOmega(),  1.0e-15);
        Assertions.assertEquals(    1.07070323515602,   almanac.getM0(),              1.0e-15);
        Assertions.assertEquals( 0.0015783309936523438, almanac.getAF0(),             1.0e-15);
        Assertions.assertEquals(                   0.0, almanac.getAF1(),             1.0e-15);
    }

}
