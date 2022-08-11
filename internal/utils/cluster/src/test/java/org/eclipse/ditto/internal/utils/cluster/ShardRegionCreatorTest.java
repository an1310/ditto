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
package org.eclipse.ditto.internal.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.eclipse.ditto.base.model.headers.DittoHeaders;
import org.eclipse.ditto.things.model.ThingId;
import org.eclipse.ditto.things.model.signals.commands.modify.DeleteThing;
import org.junit.After;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ShardRegion;
import akka.japi.pf.ReceiveBuilder;
import akka.testkit.TestKit;
import scala.concurrent.duration.Duration;

/**
 * Tests {@link ShardRegionCreator}.
 */
public final class ShardRegionCreatorTest {

    private static final Config CONFIG = ConfigFactory.load("shard-region-test");

    private final ActorSystem system1 = ActorSystem.create("system", CONFIG);
    private final ActorSystem system2 = ActorSystem.create("system", CONFIG);

    @After
    public void terminateActorSystems() {
        system1.terminate();
        system2.terminate();
    }

    @Test
    public void testHandOffMessage() throws Exception {
        new TestKit(system2) {{
            // GIVEN: 2 actor systems form a cluster with shard regions started on both
            final var latch = new CountDownLatch(2);
            final var cluster1 = Cluster.get(system1);
            final var cluster2 = Cluster.get(system2);
            cluster1.registerOnMemberUp(latch::countDown);
            cluster2.registerOnMemberUp(latch::countDown);
            cluster1.join(cluster1.selfAddress());
            cluster2.join(cluster1.selfAddress());
            latch.await();

            final var props = Props.create(MessageForwarder.class, testActor());
            final var shardName = "shard";
            final var role = "dc-default";
            final var extractor = new DummyExtractor();
            final var shard1 = ShardRegionCreator.start(system1, shardName, props, extractor, role);
            final var shard2 = ShardRegionCreator.start(system2, shardName, props, extractor, role);
            final var proxy1 = ClusterSharding.get(system1).startProxy(shardName, Optional.of(role), extractor);

            // GIVEN: a sharded actor is started
            final var signal = DeleteThing.of(ThingId.of("thing:id"), DittoHeaders.empty());
            proxy1.tell(signal, testActor());
            final var firstShardedActor = expectMsgClass(ActorRef.class);
            expectMsgClass(DeleteThing.class);

            final var startedInSystem1 = isShardedActorIn(firstShardedActor, system1);
            final var startedInSystem2 = isShardedActorIn(firstShardedActor, system2);
            assertThat(startedInSystem1)
                    .describedAs("Sharded actor should start in exactly 1 actor system")
                    .isNotEqualTo(startedInSystem2);

            // WHEN: the shard region containing the started actor is shut down
            final var shardOfFirstActor = startedInSystem1 ? shard1 : shard2;
            shardOfFirstActor.tell(ShardRegion.GracefulShutdown$.MODULE$, ActorRef.noSender());

            // THEN: the sharded actor receives the hand-off message
            expectMsgClass(StopShardedActor.class);

            // THEN: the next message to the sharded actor is buffered
            proxy1.tell(signal, testActor());

            // WHEN: the sharded actor stops
            firstShardedActor.tell(PoisonPill.getInstance(), testActor());

            // THEN: a new sharded actor for the same entity starts in the remaining shard region
            final var activeSystem = startedInSystem1 ? system2 : system1;
            final var secondShardedActor = expectMsgClass(Duration.apply(10, "s"), ActorRef.class);
            assertThat(isShardedActorIn(secondShardedActor, activeSystem)).isTrue();

            // THEN: the buffered message is processed by the new sharded actor
            expectMsgClass(DeleteThing.class);
        }};
    }

    private static boolean isShardedActorIn(final ActorRef shardedActor, final ActorSystem system) {
        final var relativePath =
                shardedActor.path().elements().drop(1).reduce((x, y) -> x + "/" + y).toString();
        return ActorSelection.apply(system.systemImpl().systemGuardian(), relativePath)
                .resolveOne(java.time.Duration.ofSeconds(10))
                .thenApply(result -> true)
                .exceptionally(error -> false)
                .toCompletableFuture()
                .join();
    }

    private static final class DummyExtractor implements ShardRegion.MessageExtractor {

        @Override
        public String entityId(final Object message) {
            return "myId0";
        }

        @Override
        public Object entityMessage(final Object message) {
            return message;
        }

        @Override
        public String shardId(final Object message) {
            return "myShard0";
        }
    }

    private static final class MessageForwarder extends AbstractActor {

        private final ActorRef receiver;

        private MessageForwarder(final ActorRef receiver) {
            this.receiver = receiver;
        }

        @Override
        public void preStart() {
            receiver.tell(getSelf(), getSelf());
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .matchAny(message -> receiver.tell(message, getSelf()))
                    .build();
        }
    }
}