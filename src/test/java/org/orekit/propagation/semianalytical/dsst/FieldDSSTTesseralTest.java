package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class FieldDSSTTesseralTest {

    @Test
    public void testGetMeanElementRate() throws OrekitException {
        doTestGetMeanElementRate(Decimal64Field.getInstance());
    }
    
    private <T extends RealFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field)
        throws OrekitException {
        
        final T zero = field.getZero();
        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 4);
        
        final Frame frame = FramesFactory.getEME2000();
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());
        
        // a  = 2655989.0 m
        // ey = 0.0041543085910249414
        // ex = 2.719455286199036E-4
        // hy = 0.3960084733107685
        // hx = -0.3412974060023717
        // lM = 8.566537840341699 rad
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(2.655989E7),
                                                                zero.add(2.719455286199036E-4),
                                                                zero.add(0.0041543085910249414),
                                                                zero.add(-0.3412974060023717),
                                                                zero.add(0.3960084733107685),
                                                                zero.add(8.566537840341699),
                                                                PositionAngle.TRUE,
                                                                frame,
                                                                initDate,
                                                                zero.add(3.986004415E14));
        
        final T mass = zero.add(1000.0);
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, mass);
        
        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);
        
        // Initialize force model
        tesseral.initialize(new AuxiliaryElements(orbit.toOrbit(), 1),
                            true, tesseral.getParameters());

        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);
        
        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);
        
        final T[] daidt = tesseral.getMeanElementRate(state, auxiliaryElements, tesseral.getParameters(field));
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assert.assertEquals(7.120011500375922E-5,   elements[0].getReal(), 6.0e-19);
        Assert.assertEquals(-1.109767646425212E-11, elements[1].getReal(), 2.0e-26);
        Assert.assertEquals(2.3036711391089307E-11, elements[2].getReal(), 1.5e-26);
        Assert.assertEquals(2.499304852807308E-12,  elements[3].getReal(), 1.0e-27);
        Assert.assertEquals(1.3899097178558372E-13, elements[4].getReal(), 3.0e-27);
        Assert.assertEquals(5.795522421338584E-12,  elements[5].getReal(), 1.0e-26);
        
    }
    
    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException, OrekitException {
        doTestShortPeriodTerms(Decimal64Field.getInstance());
    }

    @SuppressWarnings("unchecked")
    private <T extends RealFieldElement<T>> void doTestShortPeriodTerms(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
        
        final T zero = field.getZero();
        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        int earthDegree = 36;
        int earthOrder  = 36;
        int eccPower    = 4;
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(earthDegree, earthOrder);
        final org.orekit.frames.Frame earthFrame =
                FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame
        final DSSTForceModel force =
                new DSSTTesseral(earthFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                 earthDegree, earthOrder, eccPower, earthDegree + eccPower,
                                 earthDegree, earthOrder, eccPower);

        TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, "2015-07-01", tai);
        Frame eci = FramesFactory.getGCRF();
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(
                        zero.add(7120000.0), zero.add(1.0e-3), zero.add(FastMath.toRadians(60.0)),
                        zero.add(FastMath.toRadians(120.0)), zero.add(FastMath.toRadians(47.0)),
                        zero.add(FastMath.toRadians(12.0)),
                PositionAngle.TRUE, eci, initialDate, zero.add(Constants.EIGEN5C_EARTH_MU));
        
        final FieldSpacecraftState<T> meanState = new FieldSpacecraftState<>(orbit);
        
        //Create the auxiliary object
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(orbit, 1);
       
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();

        force.registerAttitudeProvider(null);
        shortPeriodTerms.addAll(force.initialize(aux, false, force.getParameters(field)));
        force.updateShortPeriodTerms(force.getParameters(field), meanState);
        
        T[] y = MathArrays.buildArray(field, 6);
        Arrays.fill(y, zero);
        for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
            final T[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] = y[i].add(shortPeriodic[i]);
            }
        }
        
        Assert.assertEquals(-72.9028792607815,     y[0].getReal(), 1.5e-12);
        Assert.assertEquals(2.1249447786897624E-6, y[1].getReal(), 2.0e-19);
        Assert.assertEquals(-6.974560212491233E-6, y[2].getReal(), 4.0e-18);
        Assert.assertEquals(-1.997990379590397E-6, y[3].getReal(), 1.5e-18);
        Assert.assertEquals(9.602513303108225E-6,  y[4].getReal(), 1.2e-18);
        Assert.assertEquals(4.538526372438945E-5,  y[5].getReal(), 5.5e-18);
    }

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
    
}
