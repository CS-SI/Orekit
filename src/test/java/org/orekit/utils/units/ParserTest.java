/* Copyright 2002-2023 CS GROUP
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.util.List;

/**
 * Unit tests for {@link Parser}.
 *
 * @author Luc Maisonobe
 */
public class ParserTest {

    @Test
    public void testNotAUnit() {
        Assertions.assertNull(Parser.buildTermsList("n/a"));
    }

    @Test
    public void testOne() {
        final List<PowerTerm> terms = Parser.buildTermsList("1");
        Assertions.assertEquals(1, terms.size());
        checkTerm(terms.get(0), 1, "1", 1, 1);
    }

    @Test
    public void testOneCompositeMultiplication() {
        final List<PowerTerm> terms = Parser.buildTermsList("1×2s");
        Assertions.assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1, "1", 1, 1);
        checkTerm(terms.get(1), 2, "s", 1, 1);
    }

    @Test
    public void testOneCompositeDivision() {
        final List<PowerTerm> terms = Parser.buildTermsList("1/2s");
        Assertions.assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,   "1",  1, 1);
        checkTerm(terms.get(1), 0.5, "s", -1, 1);
    }

    @Test
    public void testNumber() {
        final List<PowerTerm> terms = Parser.buildTermsList("#/y");
        Assertions.assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,  "#", 1, 1);
        checkTerm(terms.get(1), 1, "y", -1, 1);
    }

    @Test
    public void testIntegerPrefix() {
        final List<PowerTerm> terms = Parser.buildTermsList("2rev/d²");
        Assertions.assertEquals(2, terms.size());
        checkTerm(terms.get(0), 2, "rev", 1, 1);
        checkTerm(terms.get(1), 1,  "d", -2, 1);
    }

    @Test
    public void testSimpleFactor() {
        final List<PowerTerm> terms = Parser.buildTermsList("kg/3s");
        Assertions.assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,          "kg", 1, 1);
        checkTerm(terms.get(1), 1.0 / 3.0,  "s", -1, 1);
    }

    @Test
    public void testFinalFactor() {
        final List<PowerTerm> terms = Parser.buildTermsList("kg/3");
        Assertions.assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,          "kg", 1, 1);
        checkTerm(terms.get(1), 1.0 / 3.0,  "1", -1, 1);
    }

    @Test
    public void testCompositeFactor() {
        final List<PowerTerm> terms = Parser.buildTermsList("3kg*N/5(s·2A)");
        Assertions.assertEquals(4, terms.size());
        checkTerm(terms.get(0), 3,          "kg", 1, 1);
        checkTerm(terms.get(1), 1,          "N",  1, 1);
        checkTerm(terms.get(2), 1.0 / 5.0,  "s", -1, 1);
        checkTerm(terms.get(3), 1.0 / 2.0,  "A", -1, 1);
    }

    @Test
    public void testSquareRoot() {
        final List<PowerTerm> terms = Parser.buildTermsList("abcd¹/1√ef");
        Assertions.assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1, "abcd", 1, 1);
        checkTerm(terms.get(1), 1, "ef", -1, 2);
    }

    @Test
    public void testChain() {
        final List<PowerTerm> terms = Parser.buildTermsList("kg.m^(3/4)·s⁻¹");
        Assertions.assertEquals(3, terms.size());
        checkTerm(terms.get(0), 1, "kg", 1, 1);
        checkTerm(terms.get(1), 1,  "m", 3, 4);
        checkTerm(terms.get(2), 1, "s", -1, 1);
    }

    @Test
    public void testExponents() {
        final List<PowerTerm> terms = Parser.buildTermsList("µas^⅖/(h**(2)×8m.√A)³");
        Assertions.assertEquals(4, terms.size());
        checkTerm(terms.get(0), 1,           "µas", 2, 5);
        checkTerm(terms.get(1), 1,            "h", -6, 1);
        checkTerm(terms.get(2), 1.0 / 512.0,  "m", -3, 1);
        checkTerm(terms.get(3), 1,            "A", -3, 2);
    }

    @Test
    public void testCompoundInSquareRoot() {
        final List<PowerTerm> terms = Parser.buildTermsList("km/√(kg.s)");
        Assertions.assertEquals(3, terms.size());
        checkTerm(terms.get(0), 1,  "km", 1, 1);
        checkTerm(terms.get(1), 1, "kg", -1, 2);
        checkTerm(terms.get(2), 1,  "s", -1, 2);
    }

    @Test
    public void testLeftAssociativity() {
        final List<PowerTerm> terms1 = Parser.buildTermsList("(kg/m)/s²");
        Assertions.assertEquals(3, terms1.size());
        checkTerm(terms1.get(0), 1,  "kg", 1, 1);
        checkTerm(terms1.get(1), 1,  "m", -1, 1);
        checkTerm(terms1.get(2), 1,  "s", -2, 1);
        final List<PowerTerm> terms2 = Parser.buildTermsList("kg/(m/s²)");
        Assertions.assertEquals(3, terms2.size());
        checkTerm(terms2.get(0), 1,  "kg", 1, 1);
        checkTerm(terms2.get(1), 1,  "m", -1, 1);
        checkTerm(terms2.get(2), 1,   "s", 2, 1);
        final List<PowerTerm> terms3 = Parser.buildTermsList("kg/m/s²");
        Assertions.assertEquals(3, terms3.size());
        checkTerm(terms3.get(0), 1,  "kg", 1, 1);
        checkTerm(terms3.get(1), 1,  "m", -1, 1);
        checkTerm(terms3.get(2), 1,  "s", -2, 1);
    }

    @Test
    public void testCcsdsRoot() {
        final List<PowerTerm> terms1 = Parser.buildTermsList("km**0.5/s");
        Assertions.assertEquals(2, terms1.size());
        checkTerm(terms1.get(0), 1,  "km", 1, 2);
        checkTerm(terms1.get(1), 1,  "s", -1, 1);
        final List<PowerTerm> terms2 = Parser.buildTermsList("km/s**0.5");
        Assertions.assertEquals(2, terms2.size());
        checkTerm(terms2.get(0), 1,  "km", 1, 1);
        checkTerm(terms2.get(1), 1,  "s", -1, 2);
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
    public void testMissingUnit() {
        expectFailure("km/√");
    }

    @Test
    public void testRootAndPower() {
        expectFailure("km/√d³");
    }

    @Test
    public void testMissingTerm() {
        expectFailure("m/2√");
    }

    @Test
    public void testRootAndParenthesisedPower() {
        final List<PowerTerm> terms = Parser.buildTermsList("km/√(d³)");
        Assertions.assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,  "km", 1, 1);
        checkTerm(terms.get(1), 1,  "d", -3, 2);
    }

    private void checkTerm(final PowerTerm term, double scale, final String base, final int numerator, final int denominator) {
        Assertions.assertEquals(scale,       term.getScale(), 1.0e-12);
        Assertions.assertEquals(base,        term.getBase().toString());
        Assertions.assertEquals(numerator,   term.getExponent().getNumerator());
        Assertions.assertEquals(denominator, term.getExponent().getDenominator());
    }

    private void expectFailure(final String unitSpecification) {
        try {
            Parser.buildTermsList(unitSpecification);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_UNIT, oe.getSpecifier());
            Assertions.assertEquals(unitSpecification, oe.getParts()[0]);
        }
    }

}
