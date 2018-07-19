package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
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
    
    @Test
    @SuppressWarnings("unchecked")
    public void testShortPeriodTermsDerivatives() throws OrekitException {
        
        // Initial spacecraft state
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                       TimeScalesFactory.getUTC());

        final Orbit orbit = new EquinoctialOrbit(42164000,
                                                 10e-3,
                                                 10e-3,
                                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3), 0.1,
                                                 PositionAngle.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);
        
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;
       
        final SpacecraftState meanState = new SpacecraftState(orbit);
        
        // Force model
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(4, 4);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final DSSTForceModel tesseral =
                        new DSSTTesseral(earthFrame,
                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                         4, 4, 4, 8, 4, 4, 2);
        
        final double[] parameters = tesseral.getParameters();
                        
        // Converter for derivatives
        final DSSTDSConverter converter = new DSSTDSConverter(meanState, InertialProvider.EME2000_ALIGNED);
        
        // Field parameters
        final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(tesseral);
        final DerivativeStructure[] dsParameters                = converter.getParameters(dsState, tesseral);
        
        final FieldAuxiliaryElements<DerivativeStructure> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);
        
        // Zero
        final DerivativeStructure zero = dsState.getDate().getField().getZero();
        
        // Compute state Jacobian using directly the method
        final List<FieldShortPeriodTerms<DerivativeStructure>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<DerivativeStructure>>();
        shortPeriodTerms.addAll(tesseral.initialize(fieldAuxiliaryElements, false, dsParameters));
        tesseral.updateShortPeriodTerms(dsParameters, dsState);
        final DerivativeStructure[] shortPeriod = new DerivativeStructure[6];
        Arrays.fill(shortPeriod, zero);
        for (final FieldShortPeriodTerms<DerivativeStructure> spt : shortPeriodTerms) {
            final DerivativeStructure[] spVariation = spt.value(dsState.getOrbit());
            for (int i = 0; i < spVariation .length; i++) {
                shortPeriod[i] = shortPeriod[i].add(spVariation[i]);
            }
        }
        
        final double[][] shortPeriodJacobian = new double[6][6];
      
        final double[] derivativesASP  = shortPeriod[0].getAllDerivatives();
        final double[] derivativesExSP = shortPeriod[1].getAllDerivatives();
        final double[] derivativesEySP = shortPeriod[2].getAllDerivatives();
        final double[] derivativesHxSP = shortPeriod[3].getAllDerivatives();
        final double[] derivativesHySP = shortPeriod[4].getAllDerivatives();
        final double[] derivativesLSP  = shortPeriod[5].getAllDerivatives();

        // Update Jacobian with respect to state
        addToRow(derivativesASP,  0, shortPeriodJacobian);
        addToRow(derivativesExSP, 1, shortPeriodJacobian);
        addToRow(derivativesEySP, 2, shortPeriodJacobian);
        addToRow(derivativesHxSP, 3, shortPeriodJacobian);
        addToRow(derivativesHySP, 4, shortPeriodJacobian);
        addToRow(derivativesLSP,  5, shortPeriodJacobian);
        
        // Compute reference state Jacobian using finite differences
        double[][] shortPeriodJacobianRef = new double[6][6];
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; i++) {
            
            SpacecraftState stateM4 = shiftState(meanState, orbitType, -4 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsM4 = new AuxiliaryElements(stateM4.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsM4 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsM4.addAll(tesseral.initialize(auxiliaryElementsM4, false, parameters));
            tesseral.updateShortPeriodTerms(parameters, stateM4);
            final double[] shortPeriodM4 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsM4) {
                final double[] spVariation = spt.value(stateM4.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodM4[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateM3 = shiftState(meanState, orbitType, -3 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsM3 = new AuxiliaryElements(stateM3.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsM3 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsM3.addAll(tesseral.initialize(auxiliaryElementsM3, false, parameters));
            tesseral.updateShortPeriodTerms(parameters, stateM3);
            final double[] shortPeriodM3 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsM3) {
                final double[] spVariation = spt.value(stateM3.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodM3[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateM2 = shiftState(meanState, orbitType, -2 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsM2 = new AuxiliaryElements(stateM2.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsM2 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsM2.addAll(tesseral.initialize(auxiliaryElementsM2, false, parameters));
            tesseral.updateShortPeriodTerms(parameters, stateM2);
            final double[] shortPeriodM2 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsM2) {
                final double[] spVariation = spt.value(stateM2.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodM2[j] += spVariation[j];
                }
            }
 
            SpacecraftState stateM1 = shiftState(meanState, orbitType, -1 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsM1 = new AuxiliaryElements(stateM1.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsM1 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsM1.addAll(tesseral.initialize(auxiliaryElementsM1, false, parameters));
            tesseral.updateShortPeriodTerms(parameters, stateM1);
            final double[] shortPeriodM1 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsM1) {
                final double[] spVariation = spt.value(stateM1.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodM1[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateP1 = shiftState(meanState, orbitType, 1 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsP1 = new AuxiliaryElements(stateP1.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsP1 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsP1.addAll(tesseral.initialize(auxiliaryElementsP1, false, parameters));
            tesseral.updateShortPeriodTerms(parameters, stateP1);
            final double[] shortPeriodP1 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsP1) {
                final double[] spVariation = spt.value(stateP1.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodP1[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateP2 = shiftState(meanState, orbitType, 2 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsP2 = new AuxiliaryElements(stateP2.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsP2 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsP2.addAll(tesseral.initialize(auxiliaryElementsP2, false, parameters));
            tesseral.updateShortPeriodTerms(parameters, stateP2);
            final double[] shortPeriodP2 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsP2) {
                final double[] spVariation = spt.value(stateP2.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodP2[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateP3 = shiftState(meanState, orbitType, 3 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsP3 = new AuxiliaryElements(stateP3.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsP3 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsP3.addAll(tesseral.initialize(auxiliaryElementsP3, false, parameters));
            tesseral.updateShortPeriodTerms(parameters, stateP3);
            final double[] shortPeriodP3 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsP3) {
                final double[] spVariation = spt.value(stateP3.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodP3[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateP4 = shiftState(meanState, orbitType, 4 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsP4 = new AuxiliaryElements(stateP4.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsP4 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsP4.addAll(tesseral.initialize(auxiliaryElementsP4, false, parameters));
            tesseral.updateShortPeriodTerms(parameters, stateP4);
            final double[] shortPeriodP4 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsP4) {
                final double[] spVariation = spt.value(stateP4.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodP4[j] += spVariation[j];
                }
            }
            
            fillJacobianColumn(shortPeriodJacobianRef, i, orbitType, steps[i],
                               shortPeriodM4, shortPeriodM3, shortPeriodM2, shortPeriodM1,
                               shortPeriodP1, shortPeriodP2, shortPeriodP3, shortPeriodP4);
            
        }
        
        for (int m = 0; m < 6; ++m) {
            for (int n = 0; n < 6; ++n) {
                double error = FastMath.abs((shortPeriodJacobian[m][n] - shortPeriodJacobianRef[m][n]) / shortPeriodJacobianRef[m][n]);
                Assert.assertEquals(0, error, 7.6e-10);
            }
        }

    }
    
    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, double h,
                                    double[] M4h, double[] M3h,
                                    double[] M2h, double[] M1h,
                                    double[] P1h, double[] P2h,
                                    double[] P3h, double[] P4h) {
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (P4h[i] - M4h[i]) +
                                    32 * (P3h[i] - M3h[i]) -
                                   168 * (P2h[i] - M2h[i]) +
                                   672 * (P1h[i] - M1h[i])) / (840 * h);
        }
    }
 
    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType);
        array[0][column] += delta;

        return arrayToState(array, orbitType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType) {
          double[][] array = new double[2][6];

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngle.MEAN, array[0], array[1]);
          return array;
      }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType,
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
          EquinoctialOrbit orbit = (EquinoctialOrbit) orbitType.mapArrayToOrbit(array[0], array[1], PositionAngle.MEAN, date, mu, frame);
          return new SpacecraftState(orbit, attitude);
    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for ex, 2 for ey, 3 for hx, 4 for hy, 5 for l)
     * @param jacobian Jacobian of short period terms with respect to state
     */
    private void addToRow(final double[] derivatives, final int index,
                          final double[][] jacobian) {

        for (int i = 0; i < 6; i++) {
            jacobian[index][i] += derivatives[i + 1];
        }

    }

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
    
}
