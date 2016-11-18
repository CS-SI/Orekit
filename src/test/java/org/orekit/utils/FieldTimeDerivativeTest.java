package org.orekit.utils;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;

public class FieldTimeDerivativeTest {


    @Test
    public void testEverything() {
        double value = 1.5;
        DerivativeStructure DSa = new DerivativeStructure(1,3,0,value);
        DerivativeStructure DSb = DSa;
        Decimal64 zero = new Decimal64(0);
        FieldTimeDerivative<Decimal64> TDa = new FieldTimeDerivative<Decimal64>(zero.add(value), zero.add(1.0), zero);
        FieldTimeDerivative<Decimal64> TDb = TDa;
        TDb = TDa.add(0);
        DSb = DSa.add(0);

        Assert.assertTrue(equals(DSa,TDa));
        Assert.assertTrue(equals(DSb,TDb));


        TDb = TDb.abs();
        DSb = DSb.abs();


//

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.add(0);
        DSb = DSb.add(0);

        Assert.assertTrue(equals(DSb,TDb));

    //    TDb = TDb.acos();
      //  DSb = DSb.acos();



        TDb = TDb.sqrt();
        DSb = DSb.sqrt();

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.divide(10.0);
        DSb = DSb.divide(10.0);

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.abs();
        DSb = DSb.abs();

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.subtract(10.0);
        DSb = DSb.subtract(10.0);

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.exp();
        DSb = DSb.exp();

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.floor();
        DSb = DSb.floor();

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.divide(10.0);
        DSb = DSb.divide(10.0);

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.divide(10.0);
        DSb = DSb.divide(10.0);

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.divide(10.0);
        DSb = DSb.divide(10.0);

        Assert.assertTrue(equals(DSb,TDb));

        TDb = TDb.divide(10.0);
        DSb = DSb.divide(10.0);

        Assert.assertTrue(equals(DSb,TDb));

    }


    private boolean equals(DerivativeStructure DS, FieldTimeDerivative<Decimal64> TD){

        return ((FastMath.abs(TD.getReal() - DS.getReal()) < 1e-9) &&
                (FastMath.abs(TD.getVelocity().getReal() - DS.getPartialDerivative(1)) < 1e-9)  &&
                (FastMath.abs(TD.getAcceleration().getReal() - DS.getPartialDerivative(2)) <1e-9) );
    }

}


