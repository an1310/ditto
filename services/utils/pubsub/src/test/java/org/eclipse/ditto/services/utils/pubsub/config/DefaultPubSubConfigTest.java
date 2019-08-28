/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.utils.pubsub.config;

import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

import java.time.Duration;

import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.data.Percentage;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Tests {@link org.eclipse.ditto.services.utils.pubsub.config.DefaultPubSubConfig}.
 */
public final class DefaultPubSubConfigTest {

    private static Config pubSubTestConf;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @BeforeClass
    public static void initTestFixture() {
        pubSubTestConf = ConfigFactory.load("pubsub-test.conf");
    }

    @Test
    public void assertImmutability() {
        assertInstancesOf(DefaultPubSubConfig.class, areImmutable());
    }

    @Test
    public void testHashCodeAndEquals() {
        EqualsVerifier.forClass(DefaultPubSubConfig.class).verify();
    }

    @Test
    public void underTestReturnsDefaultValuesIfBaseConfigWasEmpty() {
        final PubSubConfig underTest = DefaultPubSubConfig.of(ConfigFactory.empty());

        softly.assertThat(underTest.getSeed())
                .as(PubSubConfig.ConfigValue.SEED.getConfigPath())
                .startsWith("Lorem ipsum");

        softly.assertThat(underTest.getHashFamilySize())
                .as(PubSubConfig.ConfigValue.HASH_FAMILY_SIZE.getConfigPath())
                .isEqualTo(10);

        softly.assertThat(underTest.getRestartDelay())
                .as(PubSubConfig.ConfigValue.RESTART_DELAY.getConfigPath())
                .isEqualTo(Duration.ofSeconds(10L));

        softly.assertThat(underTest.getUpdateInterval())
                .as(PubSubConfig.ConfigValue.UPDATE_INTERVAL.getConfigPath())
                .isEqualTo(Duration.ofSeconds(3L));

        softly.assertThat(underTest.getForceUpdateProbability())
                .as(PubSubConfig.ConfigValue.FORCE_UPDATE_PROBABILITY.getConfigPath())
                .isCloseTo(0.01, Percentage.withPercentage(1.0));
    }

    @Test
    public void underTestReturnsValuesOfConfigFile() {
        final PubSubConfig underTest = DefaultPubSubConfig.of(pubSubTestConf);

        softly.assertThat(underTest.getSeed())
                .as(PubSubConfig.ConfigValue.SEED.getConfigPath())
                .startsWith("Two households");

        softly.assertThat(underTest.getHashFamilySize())
                .as(PubSubConfig.ConfigValue.HASH_FAMILY_SIZE.getConfigPath())
                .isEqualTo(11);

        softly.assertThat(underTest.getRestartDelay())
                .as(PubSubConfig.ConfigValue.RESTART_DELAY.getConfigPath())
                .isEqualTo(Duration.ofSeconds(11L));

        softly.assertThat(underTest.getUpdateInterval())
                .as(PubSubConfig.ConfigValue.UPDATE_INTERVAL.getConfigPath())
                .isEqualTo(Duration.ofSeconds(4L));

        softly.assertThat(underTest.getForceUpdateProbability())
                .as(PubSubConfig.ConfigValue.FORCE_UPDATE_PROBABILITY.getConfigPath())
                .isCloseTo(0.011, Percentage.withPercentage(1.0));
    }

}
