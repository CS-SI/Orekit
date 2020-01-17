/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.FieldAbsoluteDate;

public class FieldAbsolutePVCoordinatesTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void TestPVOnlyConstructor() {
    	doTestPVOnlyConstructor(Decimal64Field.getInstance());
    }
    
    @Test
    public void testPVCoordinatesCopyConstructor() {
    	doTestPVCoordinatesCopyConstructor(Decimal64Field.getInstance());
    }
    
    @Test
    public void testLinearConstructors() {
    	doTestLinearConstructors(Decimal64Field.getInstance());
    }
    
    @Test
    public void testToDerivativeStructureVector2() {
    	doTestToDerivativeStructureVector2(Decimal64Field.getInstance());
    }  
    
    @Test
    public void testShift() {
    	doTestShift(Decimal64Field.getInstance());
    }
    
    @Test
    public void testToString() {
    	doTestToString(Decimal64Field.getInstance());
    }
    
    @Test
    public void testInterpolatePolynomialPVA() {
    	doTestInterpolatePolynomialPVA(Decimal64Field.getInstance());
    }
    
    @Test
    public void testInterpolatePolynomialPV() {
    	doTestInterpolatePolynomialPV(Decimal64Field.getInstance());
    }
       
    @Test
    public void testInterpolatePolynomialPositionOnly() {
    	doTestInterpolatePolynomialPositionOnly(Decimal64Field.getInstance());
    }
    
    @Test
    public void testInterpolateNonPolynomial() {
    	doTestInterpolateNonPolynomial(Decimal64Field.getInstance());
    }
    
    @Test
    public void testSamePV() {
    	doTestSamePV(Decimal64Field.getInstance());
    }
    
    @Test
    public void testTaylorProvider() {
    	doTestTaylorProvider(Decimal64Field.getInstance());
    }
    
    private <T extends RealFieldElement<T>> void doTestPVOnlyConstructor(Field<T> field) {
        //setup
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        final T one = field.getOne();
        FieldVector3D<T> p = new FieldVector3D<>(one, one.multiply(2.0), one.multiply(3.0));
        FieldVector3D<T> v = new FieldVector3D<>(one.multiply(4.0), one.multiply(5.0), one.multiply(6.0));

        //action
        FieldAbsolutePVCoordinates<T> actual = new FieldAbsolutePVCoordinates<>(frame, date, p, v);

        //verify
        Assert.assertEquals(date, actual.getDate());
        Assert.assertEquals(1.0, actual.getPosition().getX().getReal(), 0.0);
        Assert.assertEquals(2.0, actual.getPosition().getY().getReal(), 0.0);
        Assert.assertEquals(3.0, actual.getPosition().getZ().getReal(), 0.0);
        Assert.assertEquals(4.0, actual.getVelocity().getX().getReal(), 0.0);
        Assert.assertEquals(5.0, actual.getVelocity().getY().getReal(), 0.0);
        Assert.assertEquals(6.0, actual.getVelocity().getZ().getReal(), 0.0);
        Assert.assertEquals(FieldVector3D.getZero(field), actual.getAcceleration());
    }
    
    private <T extends RealFieldElement<T>> void doTestPVCoordinatesCopyConstructor(Field<T> field) {
        //setup
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        final T one = field.getOne();
        FieldPVCoordinates<T> pv = new FieldPVCoordinates<>(new FieldVector3D<>(one, one.multiply(2), one.multiply(3)), new FieldVector3D<>(one.multiply(4), one.multiply(5), one.multiply(6)));

        //action
        FieldAbsolutePVCoordinates<T> actual = new FieldAbsolutePVCoordinates<>(frame, date, pv);

        //verify
        Assert.assertEquals(date, actual.getDate());
        Assert.assertEquals(1.0, actual.getPosition().getX().getReal(), 0.0);
        Assert.assertEquals(2.0, actual.getPosition().getY().getReal(), 0.0);
        Assert.assertEquals(3.0, actual.getPosition().getZ().getReal(), 0.0);
        Assert.assertEquals(4.0, actual.getVelocity().getX().getReal(), 0.0);
        Assert.assertEquals(5.0, actual.getVelocity().getY().getReal(), 0.0);
        Assert.assertEquals(6.0, actual.getVelocity().getZ().getReal(), 0.0);
        Assert.assertEquals(FieldVector3D.getZero(field), actual.getAcceleration());
    }
    
    private <T extends RealFieldElement<T>> void doTestLinearConstructors(Field<T> field) {
        Frame frame = FramesFactory.getEME2000();
        final T one = field.getOne();
        FieldAbsolutePVCoordinates<T> pv1 = new FieldAbsolutePVCoordinates<>(frame,FieldAbsoluteDate.getCCSDSEpoch(field),
                                                              new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0)),
                                                              new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                                                              new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0)));
        FieldAbsolutePVCoordinates<T> pv2 = new FieldAbsolutePVCoordinates<>(frame,FieldAbsoluteDate.getFiftiesEpoch(field),
        													  new FieldVector3D<>(one.multiply(2.0), one.multiply(0.2), one.multiply(20.0)),
        													  new FieldVector3D<>(one.multiply(-2.0), one.multiply(-0.2), one.multiply(-20.0)),
        													  new FieldVector3D<>(one.multiply(20.0), one.multiply(-2.0), one.multiply(-200.0)));
        FieldAbsolutePVCoordinates<T> pv3 = new FieldAbsolutePVCoordinates<>(frame,FieldAbsoluteDate.getGalileoEpoch(field),
				  											  new FieldVector3D<>(one.multiply(3.0), one.multiply(0.3), one.multiply(30.0)),
				  											  new FieldVector3D<>(one.multiply(-3.0), one.multiply(-0.3), one.multiply(-30.0)),
				  											  new FieldVector3D<>(one.multiply(30.0), one.multiply(-3.0), one.multiply(-300.0)));
        FieldAbsolutePVCoordinates<T> pv4 = new FieldAbsolutePVCoordinates<>(frame,FieldAbsoluteDate.getJulianEpoch(field),
				  											  new FieldVector3D<>(one.multiply(4.0), one.multiply(0.4), one.multiply(40.0)),
				  											  new FieldVector3D<>(one.multiply(-4.0), one.multiply(-0.4), one.multiply(-40.0)),
				  											  new FieldVector3D<>(one.multiply(40.0), one.multiply(-4.0), one.multiply(-400.0)));
        checkPV(pv4, new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJulianEpoch(field), one.multiply(4.0), pv1), 1.0e-15);
        checkPV(pv2, new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getFiftiesEpoch(field), pv1, pv3), 1.0e-15);
        checkPV(pv3, new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getGalileoEpoch(field), one, pv1, one, pv2), 1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(2.0), pv4),
                new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv1, one, pv2, one, pv3),
                1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv3),
                new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(3.0), pv1, one, pv2, one, pv4),
                1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(5.0), pv4),
                new FieldAbsolutePVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field), one.multiply(4.0), pv1, one.multiply(3.0), pv2, one.multiply(2.0), pv3, one, pv4),
                1.0e-15);
    }

   private <T extends RealFieldElement<T>> void doTestToDerivativeStructureVector2(Field<T> field) {
    	final T one = field.getOne();
    	FieldVector3D<FieldDerivativeStructure<T>> fv =
                new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                          FieldAbsoluteDate.getGalileoEpoch(field),
                                          new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0)),
                                          new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                                          new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0))).toDerivativeStructureVector(2);
     	
        Assert.assertEquals(1, fv.getX().getFreeParameters());
        Assert.assertEquals(2, fv.getX().getOrder());
        Assert.assertEquals(   1.0, fv.getX().getReal(), 1.0e-10);
        Assert.assertEquals(   0.1, fv.getY().getReal(), 1.0e-10);
        Assert.assertEquals(  10.0, fv.getZ().getReal(), 1.0e-10);
        Assert.assertEquals(  -1.0, fv.getX().getPartialDerivative(1).getReal(), 1.0e-15);
        Assert.assertEquals(  -0.1, fv.getY().getPartialDerivative(1).getReal(), 1.0e-15);
        Assert.assertEquals( -10.0, fv.getZ().getPartialDerivative(1).getReal(), 1.0e-15);
        Assert.assertEquals(  10.0, fv.getX().getPartialDerivative(2).getReal(), 1.0e-15);
        Assert.assertEquals(  -1.0, fv.getY().getPartialDerivative(2).getReal(), 1.0e-15);
        Assert.assertEquals(-100.0, fv.getZ().getPartialDerivative(2).getReal(), 1.0e-15);
        checkPV(new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                          FieldAbsoluteDate.getGalileoEpoch(field),
                                          new FieldVector3D<>(one,  one.multiply(0.1), one.multiply(10.0)),
                                          new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10.0)),
                                          new FieldVector3D<>(one.multiply(10.0), one.multiply(-1.0), one.multiply(-100.0))),
        		new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
        				FieldAbsoluteDate.getGalileoEpoch(field), fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            Vector3D p = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                           new Vector3D(-1, -0.1, -10),
                                           new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assert.assertEquals(p.getX(), fv.getX().taylor(dt).getReal(), 1.0e-14);
            Assert.assertEquals(p.getY(), fv.getY().taylor(dt).getReal(), 1.0e-14);
            Assert.assertEquals(p.getZ(), fv.getZ().taylor(dt).getReal(), 1.0e-14);
        }
   }

   
    private <T extends RealFieldElement<T>> void doTestShift(Field<T> field) {
    	final T one = field.getOne();
        FieldVector3D<T> p1 = new FieldVector3D<>(one, one.multiply(0.1), one.multiply(10.0));
        FieldVector3D<T> v1 = new FieldVector3D<>(one.multiply(-1.0), one.multiply(-0.1), one.multiply(-10));
        FieldVector3D<T> a1 = new FieldVector3D<>(one.multiply(10.0), one, one.multiply(100.0));
        FieldVector3D<T> p2 = new FieldVector3D<>(one.multiply(7.0), one.multiply(0.7), one.multiply(70.0));
        FieldVector3D<T> v2 = new FieldVector3D<>(one.multiply(-11.0), one.multiply(-1.1), one.multiply(-110.0));
        FieldVector3D<T> a2 = new FieldVector3D<>(one.multiply(10.0), one, one.multiply(100.0));
        checkPV(new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(), FieldAbsoluteDate.getJ2000Epoch(field), p2, v2, a2),
                new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(), FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(1.0), p1, v1, a1).shiftedBy(one.multiply(-1.0)), 1.0e-15);
        Assert.assertEquals(0.0, FieldAbsolutePVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(new Vector3D(-6, -0.6, -60)).getNorm().getReal(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void doTestToString(Field<T> field) {
    	final T one = field.getOne();
        FieldAbsolutePVCoordinates<T> pv =
            new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(),
                                      FieldAbsoluteDate.getJ2000Epoch(field),
                                      new FieldVector3D<>(one.multiply(1.0),   one.multiply(0.1),  one.multiply(10.0)),
                                      new FieldVector3D<>(one.multiply(-1.0),  one.multiply(-0.1), one.multiply(-10.0)),
                                      new FieldVector3D<>(one.multiply(10.0),  one.multiply(1.0),  one.multiply(100.0)));
        Assert.assertEquals("{2000-01-01T11:58:55.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}", pv.toString());
    }

    
    private <T extends RealFieldElement<T>> void doTestInterpolatePolynomialPVA(Field<T> field) {
    	final T one = field.getOne();
        Random random = new Random(0xfe3945fcb8bf47cel);
        FieldAbsoluteDate<T> t0 = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<FieldAbsolutePVCoordinates<T>> sample = new ArrayList<FieldAbsolutePVCoordinates<T>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<T> position     = new FieldVector3D<>(one.multiply(px.value(dt)), one.multiply(py.value(dt)), one.multiply(pz.value(dt)));
                FieldVector3D<T> velocity     = new FieldVector3D<>(one.multiply(pxDot.value(dt)), one.multiply(pyDot.value(dt)), one.multiply(pzDot.value(dt)));
                FieldVector3D<T> acceleration = new FieldVector3D<>(one.multiply(pxDotDot.value(dt)), one.multiply(pyDotDot.value(dt)), one.multiply(pzDotDot.value(dt)));
                sample.add(new FieldAbsolutePVCoordinates<>(frame, t0.shiftedBy(one.multiply(dt)), position, velocity, acceleration));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsolutePVCoordinates<T> interpolated =
                                FieldAbsolutePVCoordinates.interpolate(frame, t0.shiftedBy(one.multiply(dt)), CartesianDerivativesFilter.USE_PVA, sample.stream());
                FieldVector3D<T> p = interpolated.getPosition();
                FieldVector3D<T> v = interpolated.getVelocity();
                FieldVector3D<T> a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt),       p.getX().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(py.value(dt),       p.getY().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pz.value(dt),       p.getZ().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pxDot.value(dt),    v.getX().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pyDot.value(dt),    v.getY().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pzDot.value(dt),    v.getZ().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 9.0e-15 * a.getNorm().getReal());
                Assert.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 9.0e-15 * a.getNorm().getReal());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 9.0e-15 * a.getNorm().getReal());
            }

        }

    }

    private <T extends RealFieldElement<T>> void doTestInterpolatePolynomialPV(Field<T> field) {
    	final T one = field.getOne();
        Random random = new Random(0xae7771c9933407bdl);
        FieldAbsoluteDate<T> t0 = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<FieldAbsolutePVCoordinates<T>> sample = new ArrayList<FieldAbsolutePVCoordinates<T>>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<T> position = new FieldVector3D<>(one.multiply(px.value(dt)), one.multiply(py.value(dt)), one.multiply(pz.value(dt)));
                FieldVector3D<T> velocity = new FieldVector3D<>(one.multiply(pxDot.value(dt)), one.multiply(pyDot.value(dt)), one.multiply(pzDot.value(dt)));
                sample.add(new FieldAbsolutePVCoordinates<>(frame, t0.shiftedBy(one.multiply(dt)), position, velocity, FieldVector3D.getZero(field)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsolutePVCoordinates<T> interpolated =
                                FieldAbsolutePVCoordinates.interpolate(frame, t0.shiftedBy(dt), CartesianDerivativesFilter.USE_PV, sample.stream());
                FieldVector3D<T> p = interpolated.getPosition();
                FieldVector3D<T> v = interpolated.getVelocity();
                FieldVector3D<T> a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt),       p.getX().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(py.value(dt),       p.getY().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pz.value(dt),       p.getZ().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pxDot.value(dt),    v.getX().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pyDot.value(dt),    v.getY().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pzDot.value(dt),    v.getZ().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assert.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 1.0e-14 * a.getNorm().getReal());
                Assert.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 1.0e-14 * a.getNorm().getReal());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 1.0e-14 * a.getNorm().getReal());
            }

        }

    }

  
    private <T extends RealFieldElement<T>> void doTestInterpolatePolynomialPositionOnly(Field<T> field) {
    	final T one = field.getOne();
        Random random = new Random(0x88740a12e4299003l);
        FieldAbsoluteDate<T> t0 = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<FieldAbsolutePVCoordinates<T>> sample = new ArrayList<FieldAbsolutePVCoordinates<T>>();
            for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                FieldVector3D<T>position = new FieldVector3D<>(one.multiply(px.value(dt)), one.multiply(py.value(dt)), one.multiply(pz.value(dt)));
                sample.add(new FieldAbsolutePVCoordinates<>(frame, t0.shiftedBy(one.multiply(dt)), position, FieldVector3D.getZero(field), FieldVector3D.getZero(field)));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsolutePVCoordinates<T> interpolated =
                                FieldAbsolutePVCoordinates.interpolate(frame, t0.shiftedBy(one.multiply(dt)), CartesianDerivativesFilter.USE_P, sample.stream());
                FieldVector3D<T> p = interpolated.getPosition();
                FieldVector3D<T> v = interpolated.getVelocity();
                FieldVector3D<T> a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt),       p.getX().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(py.value(dt),       p.getY().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pz.value(dt),       p.getZ().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assert.assertEquals(pxDot.value(dt),    v.getX().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assert.assertEquals(pyDot.value(dt),    v.getY().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assert.assertEquals(pzDot.value(dt),    v.getZ().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assert.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 2.0e-13 * a.getNorm().getReal());
                Assert.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 2.0e-13 * a.getNorm().getReal());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 2.0e-13 * a.getNorm().getReal());
            }

        }
    }

    private <T extends RealFieldElement<T>> void doTestInterpolateNonPolynomial(Field<T> field) {
    	final T one = field.getOne();
    	FieldAbsoluteDate<T> t0 = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();

        List<FieldAbsolutePVCoordinates<T>> sample = new ArrayList<FieldAbsolutePVCoordinates<T>>();
        for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
            FieldVector3D<T> position     = new FieldVector3D<>( one.multiply(FastMath.cos(dt)),  one.multiply(FastMath.sin(dt)), one.multiply(0.0));
            FieldVector3D<T> velocity     = new FieldVector3D<>( one.multiply(-FastMath.sin(dt)), one.multiply(FastMath.cos(dt)), one.multiply(0.0));
            FieldVector3D<T> acceleration = new FieldVector3D<>( one.multiply(-FastMath.cos(dt)), one.multiply(-FastMath.sin(dt)), one.multiply(0.0));
            sample.add(new FieldAbsolutePVCoordinates<>(frame, t0.shiftedBy(one.multiply(dt)), position, velocity, acceleration));
        }

        for (double dt = 0; dt < 1.0; dt += 0.01) {
            FieldAbsolutePVCoordinates<T> interpolated =
                            FieldAbsolutePVCoordinates.interpolate(frame, t0.shiftedBy(one.multiply(dt)), CartesianDerivativesFilter.USE_PVA, sample.stream());
            FieldVector3D<T> p = interpolated.getPosition();
            FieldVector3D<T> v = interpolated.getVelocity();
            FieldVector3D<T> a = interpolated.getAcceleration();
            Assert.assertEquals( FastMath.cos(dt),   p.getX().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assert.assertEquals( FastMath.sin(dt),   p.getY().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assert.assertEquals(0,                   p.getZ().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assert.assertEquals(-FastMath.sin(dt),   v.getX().getReal(), 3.0e-9  * v.getNorm().getReal());
            Assert.assertEquals( FastMath.cos(dt),   v.getY().getReal(), 3.0e-9  * v.getNorm().getReal());
            Assert.assertEquals(0,                   v.getZ().getReal(), 3.0e-9  * v.getNorm().getReal());
            Assert.assertEquals(-FastMath.cos(dt),   a.getX().getReal(), 4.0e-8  * a.getNorm().getReal());
            Assert.assertEquals(-FastMath.sin(dt),   a.getY().getReal(), 4.0e-8  * a.getNorm().getReal());
            Assert.assertEquals(0,                   a.getZ().getReal(), 4.0e-8  * a.getNorm().getReal());
        }

    }

    private <T extends RealFieldElement<T>> void doTestSamePV(Field<T> field) {
        //setup
    	final T one = field.getOne();
    	FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        FieldVector3D<T> p = new FieldVector3D<>(one.multiply(1), one.multiply(2), one.multiply(3));
        FieldVector3D<T> v = new FieldVector3D<>(one.multiply(4), one.multiply(5), one.multiply(6));

        //action
        FieldAbsolutePVCoordinates<T> actual = new FieldAbsolutePVCoordinates<>(frame, date, p, v);

        //verify
        assertEquals(actual.getPVCoordinates().toString(), actual.getPVCoordinates(frame).toString());
        assertEquals(actual.getPVCoordinates(frame).toString(), actual.getPVCoordinates(date, frame).toString());
    }

    
    private <T extends RealFieldElement<T>> void doTestTaylorProvider(Field<T> field) {
        //setup
    	final T one = field.getOne();
    	FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame frame = FramesFactory.getEME2000();
        FieldVector3D<T> p = new FieldVector3D<>(one.multiply(1), one.multiply(2), one.multiply(3));
        FieldVector3D<T> v = new FieldVector3D<>(one.multiply(4), one.multiply(5), one.multiply(6));

        //action
        FieldAbsolutePVCoordinates<T> actual = new FieldAbsolutePVCoordinates<>(frame, date, p, v);
        final FieldPVCoordinatesProvider<T> pv = actual.toTaylorProvider();

        //verify
        Assert.assertEquals(actual.getPVCoordinates(date, frame).toString(), pv.getPVCoordinates(date, frame).toString());
    }

    private PolynomialFunction randomPolynomial(int degree, Random random) {
        double[] coeff = new double[ 1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
    }

    private <T extends RealFieldElement<T>> void checkPV(FieldAbsolutePVCoordinates<T> expected, FieldAbsolutePVCoordinates<T> real, double epsilon) {
        Assert.assertEquals(expected.getDate(), real.getDate());
        Assert.assertEquals(expected.getPosition().getX().getReal(),     real.getPosition().getX().getReal(),     epsilon);
        Assert.assertEquals(expected.getPosition().getY().getReal(),     real.getPosition().getY().getReal(),     epsilon);
        Assert.assertEquals(expected.getPosition().getZ().getReal(),     real.getPosition().getZ().getReal(),     epsilon);
        Assert.assertEquals(expected.getVelocity().getX().getReal(),     real.getVelocity().getX().getReal(),     epsilon);
        Assert.assertEquals(expected.getVelocity().getY().getReal(),     real.getVelocity().getY().getReal(),     epsilon);
        Assert.assertEquals(expected.getVelocity().getZ().getReal(),     real.getVelocity().getZ().getReal(),     epsilon);
        Assert.assertEquals(expected.getAcceleration().getX().getReal(), real.getAcceleration().getX().getReal(), epsilon);
        Assert.assertEquals(expected.getAcceleration().getY().getReal(), real.getAcceleration().getY().getReal(), epsilon);
        Assert.assertEquals(expected.getAcceleration().getZ().getReal(), real.getAcceleration().getZ().getReal(), epsilon);
    }

}
