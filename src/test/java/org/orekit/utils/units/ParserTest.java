/* Copyright 2002-2021 CS GROUP
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
package org.orekit.utils.units;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Unit tests for {@link Parser}.
 *
 * @author Luc Maisonobe
 */
public class ParserTest {

    @Test
    public void testNotAUnit() {
        Assert.assertNull(Parser.buildTree("n/a"));
    }

    @Test
    public void testOne() {
        final ParseTree tree = Parser.buildTree("1");
        Assert.assertEquals(1, tree.getFactor());
        Assert.assertEquals(1, tree.getTerms().size());
        checkTerm(tree.getTerms().get(0), "1", 1, 1);
    }

    @Test
    public void testOneNotComposite() {
        expectFailure("m.1/s");
    }

    @Test
    public void testNumber() {
        final ParseTree tree = Parser.buildTree("#/y");
        Assert.assertEquals(1, tree.getFactor());
        Assert.assertEquals(2, tree.getTerms().size());
        checkTerm(tree.getTerms().get(0), "#",  1, 1);
        checkTerm(tree.getTerms().get(1), "y", -1, 1);
    }

    @Test
    public void testIntegerPrefix() {
        final ParseTree tree = Parser.buildTree("2rev/d²");
        Assert.assertEquals(2, tree.getFactor());
        Assert.assertEquals(2, tree.getTerms().size());
        checkTerm(tree.getTerms().get(0), "rev", 1, 1);
        checkTerm(tree.getTerms().get(1), "d",  -2, 1);
    }

    @Test
    public void testSquareRoot() {
        final ParseTree tree = Parser.buildTree("abcd/√ef");
        Assert.assertEquals(1, tree.getFactor());
        Assert.assertEquals(2, tree.getTerms().size());
        checkTerm(tree.getTerms().get(0), "abcd", 1, 1);
        checkTerm(tree.getTerms().get(1), "ef", -1, 2);
    }

    @Test
    public void testChain() {
        final ParseTree tree = Parser.buildTree("kg.m^(3/4)·s⁻¹");
        Assert.assertEquals(1, tree.getFactor());
        Assert.assertEquals(3, tree.getTerms().size());
        checkTerm(tree.getTerms().get(0), "kg", 1, 1);
        checkTerm(tree.getTerms().get(1), "m",  3, 4);
        checkTerm(tree.getTerms().get(2), "s", -1, 1);
    }

    @Test
    public void testExponents() {
        final ParseTree tree = Parser.buildTree("µas^⅖/(h**(2)×m)³");
        Assert.assertEquals(1, tree.getFactor());
        Assert.assertEquals(3, tree.getTerms().size());
        checkTerm(tree.getTerms().get(0), "µas", 2, 5);
        checkTerm(tree.getTerms().get(1), "h",  -6, 1);
        checkTerm(tree.getTerms().get(2), "m",  -3, 1);
    }

    @Test
    public void testCompoundInSquareRoot() {
        final ParseTree tree = Parser.buildTree("km/√(kg.s)");
        Assert.assertEquals(1, tree.getFactor());
        Assert.assertEquals(3, tree.getTerms().size());
        checkTerm(tree.getTerms().get(0), "km",  1, 1);
        checkTerm(tree.getTerms().get(1), "kg", -1, 2);
        checkTerm(tree.getTerms().get(2), "s",  -1, 2);
    }

    @Test
    public void testLeftAssociativity() {
        final ParseTree tree1 = Parser.buildTree("(kg/m)/s²");
        Assert.assertEquals(1, tree1.getFactor());
        Assert.assertEquals(3, tree1.getTerms().size());
        checkTerm(tree1.getTerms().get(0), "kg",  1, 1);
        checkTerm(tree1.getTerms().get(1), "m",  -1, 1);
        checkTerm(tree1.getTerms().get(2), "s",  -2, 1);
        final ParseTree tree2 = Parser.buildTree("kg/(m/s²)");
        Assert.assertEquals(1, tree2.getFactor());
        Assert.assertEquals(3, tree2.getTerms().size());
        checkTerm(tree2.getTerms().get(0), "kg",  1, 1);
        checkTerm(tree2.getTerms().get(1), "m",  -1, 1);
        checkTerm(tree2.getTerms().get(2), "s",   2, 1);
        final ParseTree tree3 = Parser.buildTree("kg/m/s²");
        Assert.assertEquals(1, tree3.getFactor());
        Assert.assertEquals(3, tree3.getTerms().size());
        checkTerm(tree3.getTerms().get(0), "kg",  1, 1);
        checkTerm(tree3.getTerms().get(1), "m",  -1, 1);
        checkTerm(tree3.getTerms().get(2), "s",  -2, 1);
    }

    @Test
    public void testCcsdsRoot() {
        final ParseTree tree1 = Parser.buildTree("km**0.5/s");
        Assert.assertEquals(1, tree1.getFactor());
        Assert.assertEquals(2, tree1.getTerms().size());
        checkTerm(tree1.getTerms().get(0), "km",  1, 2);
        checkTerm(tree1.getTerms().get(1), "s",  -1, 1);
        final ParseTree tree2 = Parser.buildTree("km/s**0.5");
        Assert.assertEquals(1, tree2.getFactor());
        Assert.assertEquals(2, tree2.getTerms().size());
        checkTerm(tree2.getTerms().get(0), "km",  1, 1);
        checkTerm(tree2.getTerms().get(1), "s",  -1, 2);
    }

    @Test
    public void testEmpty() {
        expectFailure("");
    }

    @Test
    public void testIncompleteExponent1() {
        expectFailure("m.g^(2/)");
    }

    @Test
    public void testIncompleteExponent2() {
        expectFailure("m.g^(2m)");
    }

    @Test
    public void testMissingClosingParenthesis() {
        expectFailure("m.(W");
    }

    @Test
    public void testGarbageOnInput() {
        expectFailure("kg+s");
    }

    @Test
    public void testSpuriousFactor() {
        expectFailure("kg/3s");
    }

    @Test
    public void testMissingUnit() {
        expectFailure("km/√");
    }

    @Test
    public void testRootAndPower() {
        expectFailure("km/√d³");
    }

    @Test
    public void testRootAndParenthesisedPower() {
        final ParseTree tree = Parser.buildTree("km/√(d³)");
        Assert.assertEquals(2, tree.getTerms().size());
        checkTerm(tree.getTerms().get(0), "km",  1, 1);
        checkTerm(tree.getTerms().get(1), "d",  -3, 2);
    }

    private void checkTerm(final PowerTerm term, final String base, final int numerator, final int denominator) {
        Assert.assertEquals(base,        term.getBase().toString());
        Assert.assertEquals(numerator,   term.getExponent().getNumerator());
        Assert.assertEquals(denominator, term.getExponent().getDenominator());
    }

    private void expectFailure(final String unitSpecification) {
        try {
            Parser.buildTree(unitSpecification);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_UNIT, oe.getSpecifier());
            Assert.assertEquals(unitSpecification, oe.getParts()[0]);
        }
    }

}
