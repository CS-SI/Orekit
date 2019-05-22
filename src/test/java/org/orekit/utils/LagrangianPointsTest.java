package org.orekit.utils;

import org.junit.Assert;
import org.junit.Test;

public class LagrangianPointsTest {

    @Test
    public void testList() {
        Assert.assertEquals(5, LagrangianPoints.values().length);
    }

}
