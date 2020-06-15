/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.services.connectivity.messaging.validation;

import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.ditto.model.connectivity.Connection;
import org.eclipse.ditto.services.connectivity.messaging.ClientActorPropsFactory;
import org.eclipse.ditto.signals.commands.connectivity.ConnectivityCommand;
import org.eclipse.ditto.signals.commands.connectivity.ConnectivityCommandInterceptor;
import org.eclipse.ditto.signals.commands.connectivity.modify.CreateConnection;
import org.eclipse.ditto.signals.commands.connectivity.modify.ModifyConnection;
import org.eclipse.ditto.signals.commands.connectivity.modify.OpenConnection;
import org.eclipse.ditto.signals.commands.connectivity.modify.TestConnection;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * Checks if the given {@link ConnectivityCommand} is valid by trying to create the client actor props.
 */
public final class DittoConnectivityCommandValidator implements ConnectivityCommandInterceptor {

    private final ClientActorPropsFactory propsFactory;
    private final ActorRef conciergeForwarder;
    private final ActorRef connectionActor;
    private final ConnectionValidator connectionValidator;
    private final ActorSystem actorSystem;

    public DittoConnectivityCommandValidator(
            final ClientActorPropsFactory propsFactory,
            final ActorRef conciergeForwarder,
            final ActorRef connectionActor,
            final ConnectionValidator connectionValidator,
            final ActorSystem actorSystem) {
        this.propsFactory = propsFactory;
        this.conciergeForwarder = conciergeForwarder;
        this.connectionActor = connectionActor;
        this.connectionValidator = connectionValidator;
        this.actorSystem = actorSystem;
    }

    @Override
    public void accept(final ConnectivityCommand<?> command, final Supplier<Connection> connectionSupplier) {
        switch (command.getType()) {
            case CreateConnection.TYPE:
            case TestConnection.TYPE:
            case ModifyConnection.TYPE:
                resolveConnection(connectionSupplier)
                        .ifPresentOrElse(connection -> {
                                    connectionValidator.validate(connection, command.getDittoHeaders(), actorSystem);
                                    propsFactory.getActorPropsForType(connection, conciergeForwarder, connectionActor);
                                },
                                // should never happen
                                handleNullConnection(command));
                break;
            case OpenConnection.TYPE:
                resolveConnection(connectionSupplier).ifPresentOrElse(c -> connectionValidator.validate(c,
                        command.getDittoHeaders(), actorSystem), handleNullConnection(command));
                break;
            default: // nothing to validate for other commands
        }
    }

    @Nonnull
    private Runnable handleNullConnection(final ConnectivityCommand<?> command) {
        return () -> {
            throw new IllegalStateException("connection=null for " + command);
        };
    }

    private Optional<Connection> resolveConnection(@Nullable Supplier<Connection> connectionSupplier) {
        return Optional.ofNullable(connectionSupplier).map(Supplier::get);
    }
}
