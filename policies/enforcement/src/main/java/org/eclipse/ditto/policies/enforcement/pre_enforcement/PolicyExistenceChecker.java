/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.policies.enforcement.pre_enforcement;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.eclipse.ditto.base.model.entity.id.EntityId;
import org.eclipse.ditto.base.model.entity.id.WithEntityId;
import org.eclipse.ditto.base.model.signals.Signal;
import org.eclipse.ditto.internal.utils.cache.entry.Entry;
import org.eclipse.ditto.internal.utils.cacheloaders.EnforcementCacheKey;
import org.eclipse.ditto.internal.utils.cluster.ShardRegionProxyActorFactory;
import org.eclipse.ditto.internal.utils.cluster.config.DefaultClusterConfig;
import org.eclipse.ditto.internal.utils.config.DefaultScopedConfig;
import org.eclipse.ditto.policies.api.PoliciesMessagingConstants;
import org.eclipse.ditto.policies.enforcement.config.DefaultEnforcementConfig;
import org.eclipse.ditto.policies.enforcement.config.EnforcementConfig;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * Checks the existence of the entity from a Policy command.
 *
 * @since 3.0.0
 */
public final class PolicyExistenceChecker implements ExistenceChecker {

    public static final String ENFORCEMENT_CACHE_DISPATCHER = "enforcement-cache-dispatcher";

    private final AsyncCacheLoader<EnforcementCacheKey, Entry<EnforcementCacheKey>> policyIdCache;
    private final ActorSystem actorSystem;

    public PolicyExistenceChecker(final ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        final var enforcementConfig = DefaultEnforcementConfig.of(
                DefaultScopedConfig.dittoScoped(actorSystem.settings().config()));
        policyIdCache = getPolicyIdCache(actorSystem, enforcementConfig);
    }

    private AsyncCacheLoader<EnforcementCacheKey, Entry<EnforcementCacheKey>> getPolicyIdCache(
            final ActorSystem actorSystem,
            final EnforcementConfig enforcementConfig) {

        final var clusterConfig = DefaultClusterConfig.of(actorSystem.settings().config().getConfig("ditto.cluster"));
        final ShardRegionProxyActorFactory shardRegionProxyActorFactory =
                ShardRegionProxyActorFactory.newInstance(actorSystem, clusterConfig);

        final ActorRef policiesShardRegion = shardRegionProxyActorFactory.getShardRegionProxyActor(
                PoliciesMessagingConstants.CLUSTER_ROLE, PoliciesMessagingConstants.SHARD_REGION);

        return new PreEnforcementPolicyIdCacheLoader(enforcementConfig.getAskWithRetryConfig(),
                actorSystem.getScheduler(),
                policiesShardRegion);
    }

    @Override
    public CompletionStage<Boolean> checkExistence(final Signal<?> signal) {
        final Optional<EntityId> entityIdOptional = WithEntityId.getEntityIdOfType(EntityId.class, signal);

        try {
            return policyIdCache.asyncLoad(EnforcementCacheKey.of(
                                    entityIdOptional.orElseThrow(() -> getWrongEntityException(entityIdOptional, signal))),
                            actorSystem.dispatchers().lookup(ENFORCEMENT_CACHE_DISPATCHER))
                    .thenApply(Entry::exists);
        } catch (final Exception e) {
            throw new IllegalStateException("Could not load policyId via policyIdCacheLoader", e);
        }
    }

    private static IllegalArgumentException getWrongEntityException(final Optional<EntityId> entityIdOptional,
            final Signal<?> signal) {

        final String message =
                String.format("ExistenceChecker: unknown entity type or empty ID <%s:%s> for signal <%s>",
                        entityIdOptional.map(EntityId::getEntityType).map(Objects::toString).orElse(""),
                        entityIdOptional.map(Objects::toString).orElse(""), signal);
        return new IllegalArgumentException(message);
    }

}
