/* Copyright 2022-2025 Romain Serra
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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.conversion.ClassicalRungeKuttaFieldIntegratorBuilder;
import org.orekit.propagation.conversion.DormandPrince54FieldIntegratorBuilder;
import org.orekit.propagation.conversion.DormandPrince853FieldIntegratorBuilder;
import org.orekit.propagation.conversion.FieldExplicitRungeKuttaIntegratorBuilder;
import org.orekit.propagation.conversion.LutherFieldIntegratorBuilder;
import org.orekit.propagation.conversion.MidpointFieldIntegratorBuilder;

/**
 * Factory for some common schemes.
 *
 * @author Romain Serra
 * @since 13.0
 * @see ShootingPropagationSettings
 */
public class ShootingIntegrationSettingsFactory {

    /**
     * Private constructor.
     */
    private ShootingIntegrationSettingsFactory() {
        // factory class
    }

    /**
     * Returns shooting integration settings according to the midpoint Runge Kutta scheme.
     * @param step default step-size
     * @return integration settings
     */
    public static ShootingIntegrationSettings getMidpointIntegratorSettings(final double step) {
        return new ShootingIntegrationSettings() {
            @Override
            public <T extends CalculusFieldElement<T>> FieldExplicitRungeKuttaIntegratorBuilder<T> getFieldIntegratorBuilder(final Field<T> field) {
                return new MidpointFieldIntegratorBuilder<>(field.getZero().newInstance(step));
            }
        };
    }

    /**
     * Returns shooting integration settings according to the classical Runge Kutta scheme.
     * @param step default step-size
     * @return integration settings
     */
    public static ShootingIntegrationSettings getClassicalRungeKuttaIntegratorSettings(final double step) {
        return new ShootingIntegrationSettings() {
            @Override
            public <T extends CalculusFieldElement<T>> FieldExplicitRungeKuttaIntegratorBuilder<T> getFieldIntegratorBuilder(final Field<T> field) {
                return new ClassicalRungeKuttaFieldIntegratorBuilder<>(field.getZero().newInstance(step));
            }
        };
    }

    /**
     * Returns shooting integration settings according to the Luther Runge Kutta scheme.
     * @param step default step-size
     * @return integration settings
     */
    public static ShootingIntegrationSettings getLutherIntegratorSettings(final double step) {
        return new ShootingIntegrationSettings() {
            @Override
            public <T extends CalculusFieldElement<T>> FieldExplicitRungeKuttaIntegratorBuilder<T> getFieldIntegratorBuilder(final Field<T> field) {
                return new LutherFieldIntegratorBuilder<>(field.getZero().newInstance(step));
            }
        };
    }

    /**
     * Returns shooting integration settings according to the Dormand Prince 5(4) scheme.
     * @param minStep minimum step-size
     * @param maxStep maximum step-size
     * @param toleranceProvider tolerance provider
     * @return integration settings
     */
    public static ShootingIntegrationSettings getDormandPrince54IntegratorSettings(final double minStep,
                                                                                   final double maxStep,
                                                                                   final ToleranceProvider toleranceProvider) {
        return new ShootingIntegrationSettings() {
            @Override
            public <T extends CalculusFieldElement<T>> FieldExplicitRungeKuttaIntegratorBuilder<T> getFieldIntegratorBuilder(final Field<T> field) {
                return new DormandPrince54FieldIntegratorBuilder<>(minStep, maxStep, toleranceProvider);
            }
        };
    }

    /**
     * Returns shooting integration settings according to the Dormand Prince 8(53) scheme.
     * @param minStep minimum step-size
     * @param maxStep maximum step-size
     * @param toleranceProvider tolerance provider
     * @return integration settings
     */
    public static ShootingIntegrationSettings getDormandPrince853IntegratorSettings(final double minStep,
                                                                                    final double maxStep,
                                                                                    final ToleranceProvider toleranceProvider) {
        return new ShootingIntegrationSettings() {
            @Override
            public <T extends CalculusFieldElement<T>> FieldExplicitRungeKuttaIntegratorBuilder<T> getFieldIntegratorBuilder(final Field<T> field) {
                return new DormandPrince853FieldIntegratorBuilder<>(minStep, maxStep, toleranceProvider);
            }
        };
    }
}
