/* Copyright 2002-2024 CS GROUP
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

import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link Parser}.
 *
 * @author Luc Maisonobe
 */
class ParserTest {

    @Test
    void testNotAUnit() {
        assertNull(Parser.buildTermsList("n/a"));
    }

    @Test
    void testOne() {
        final List<PowerTerm> terms = Parser.buildTermsList("1");
        assertEquals(1, terms.size());
        checkTerm(terms.get(0), 1, "1", 1, 1);
    }

    @Test
    void testOneCompositeMultiplication() {
        final List<PowerTerm> terms = Parser.buildTermsList("1×2s");
        assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1, "1", 1, 1);
        checkTerm(terms.get(1), 2, "s", 1, 1);
    }

    @Test
    void testOneCompositeDivision() {
        final List<PowerTerm> terms = Parser.buildTermsList("1/2s");
        assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,   "1",  1, 1);
        checkTerm(terms.get(1), 0.5, "s", -1, 1);
    }

    @Test
    void testNumber() {
        final List<PowerTerm> terms = Parser.buildTermsList("#/y");
        assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,  "#", 1, 1);
        checkTerm(terms.get(1), 1, "y", -1, 1);
    }

    @Test
    void testIntegerPrefix() {
        final List<PowerTerm> terms = Parser.buildTermsList("2rev/d²");
        assertEquals(2, terms.size());
        checkTerm(terms.get(0), 2, "rev", 1, 1);
        checkTerm(terms.get(1), 1,  "d", -2, 1);
    }

    @Test
    void testSimpleFactor() {
        final List<PowerTerm> terms = Parser.buildTermsList("kg/3s");
        assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,          "kg", 1, 1);
        checkTerm(terms.get(1), 1.0 / 3.0,  "s", -1, 1);
    }

    @Test
    void testFinalFactor() {
        final List<PowerTerm> terms = Parser.buildTermsList("kg/3");
        assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,          "kg", 1, 1);
        checkTerm(terms.get(1), 1.0 / 3.0,  "1", -1, 1);
    }

    @Test
    void testCompositeFactor() {
        final List<PowerTerm> terms = Parser.buildTermsList("3kg*N/5(s·2A)");
        assertEquals(4, terms.size());
        checkTerm(terms.get(0), 3,          "kg", 1, 1);
        checkTerm(terms.get(1), 1,          "N",  1, 1);
        checkTerm(terms.get(2), 1.0 / 5.0,  "s", -1, 1);
        checkTerm(terms.get(3), 1.0 / 2.0,  "A", -1, 1);
    }

    @Test
    void testSquareRoot() {
        final List<PowerTerm> terms = Parser.buildTermsList("abcd¹/1√ef");
        assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1, "abcd", 1, 1);
        checkTerm(terms.get(1), 1, "ef", -1, 2);
    }

    @Test
    void testChain() {
        final List<PowerTerm> terms = Parser.buildTermsList("kg.m^(3/4)·s⁻¹");
        assertEquals(3, terms.size());
        checkTerm(terms.get(0), 1, "kg", 1, 1);
        checkTerm(terms.get(1), 1,  "m", 3, 4);
        checkTerm(terms.get(2), 1, "s", -1, 1);
    }

    @Test
    void testExponents() {
        final List<PowerTerm> terms = Parser.buildTermsList("µas^⅖/(h**(2)×8m.√A)³");
        assertEquals(4, terms.size());
        checkTerm(terms.get(0), 1,           "µas", 2, 5);
        checkTerm(terms.get(1), 1,            "h", -6, 1);
        checkTerm(terms.get(2), 1.0 / 512.0,  "m", -3, 1);
        checkTerm(terms.get(3), 1,            "A", -3, 2);
    }

    @Test
    void testCompoundInSquareRoot() {
        final List<PowerTerm> terms = Parser.buildTermsList("km/√(kg.s)");
        assertEquals(3, terms.size());
        checkTerm(terms.get(0), 1,  "km", 1, 1);
        checkTerm(terms.get(1), 1, "kg", -1, 2);
        checkTerm(terms.get(2), 1,  "s", -1, 2);
    }

    @Test
    void testLeftAssociativity() {
        final List<PowerTerm> terms1 = Parser.buildTermsList("(kg/m)/s²");
        assertEquals(3, terms1.size());
        checkTerm(terms1.get(0), 1,  "kg", 1, 1);
        checkTerm(terms1.get(1), 1,  "m", -1, 1);
        checkTerm(terms1.get(2), 1,  "s", -2, 1);
        final List<PowerTerm> terms2 = Parser.buildTermsList("kg/(m/s²)");
        assertEquals(3, terms2.size());
        checkTerm(terms2.get(0), 1,  "kg", 1, 1);
        checkTerm(terms2.get(1), 1,  "m", -1, 1);
        checkTerm(terms2.get(2), 1,   "s", 2, 1);
        final List<PowerTerm> terms3 = Parser.buildTermsList("kg/m/s²");
        assertEquals(3, terms3.size());
        checkTerm(terms3.get(0), 1,  "kg", 1, 1);
        checkTerm(terms3.get(1), 1,  "m", -1, 1);
        checkTerm(terms3.get(2), 1,  "s", -2, 1);
    }

    @Test
    void testCcsdsRoot() {
        final List<PowerTerm> terms1 = Parser.buildTermsList("km**0.5/s");
        assertEquals(2, terms1.size());
        checkTerm(terms1.get(0), 1,  "km", 1, 2);
        checkTerm(terms1.get(1), 1,  "s", -1, 1);
        final List<PowerTerm> terms2 = Parser.buildTermsList("km/s**0.5");
        assertEquals(2, terms2.size());
        checkTerm(terms2.get(0), 1,  "km", 1, 1);
        checkTerm(terms2.get(1), 1,  "s", -1, 2);
    }

    @Test
    void testEmpty() {
        expectFailure("");
    }

    @Test
    void testIncompleteExponent1() {
        expectFailure("m.g^(2/)");
    }

    @Test
    void testIncompleteExponent2() {
        expectFailure("m.g^(2m)");
    }

    @Test
    void testMissingClosingParenthesis() {
        expectFailure("m.(W");
    }

    @Test
    void testGarbageOnInput() {
        expectFailure("kg+s");
    }

    @Test
    void testMissingUnit() {
        expectFailure("km/√");
    }

    @Test
    void testRootAndPower() {
        expectFailure("km/√d³");
    }

    @Test
    void testMissingTerm() {
        expectFailure("m/2√");
    }

    @Test
    void testRootAndParenthesisedPower() {
        final List<PowerTerm> terms = Parser.buildTermsList("km/√(d³)");
        assertEquals(2, terms.size());
        checkTerm(terms.get(0), 1,  "km", 1, 1);
        checkTerm(terms.get(1), 1,  "d", -3, 2);
    }

    private void checkTerm(final PowerTerm term, double scale, final String base, final int numerator, final int denominator) {
        assertEquals(scale,       term.getScale(), 1.0e-12);
        assertEquals(base,        term.getBase().toString());
        assertEquals(numerator,   term.getExponent().getNumerator());
        assertEquals(denominator, term.getExponent().getDenominator());
    }

    private void expectFailure(final String unitSpecification) {
        try {
            Parser.buildTermsList(unitSpecification);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNKNOWN_UNIT, oe.getSpecifier());
            assertEquals(unitSpecification, oe.getParts()[0]);
        }
    }

}
