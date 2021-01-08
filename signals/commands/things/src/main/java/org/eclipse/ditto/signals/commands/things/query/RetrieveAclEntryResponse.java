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
package org.eclipse.ditto.signals.commands.things.query;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonField;
import org.eclipse.ditto.json.JsonFieldDefinition;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonObjectBuilder;
import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.model.base.common.HttpStatus;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.FieldType;
import org.eclipse.ditto.model.base.json.JsonParsableCommandResponse;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;
import org.eclipse.ditto.model.things.AccessControlListModelFactory;
import org.eclipse.ditto.model.things.AclEntry;
import org.eclipse.ditto.model.things.ThingId;
import org.eclipse.ditto.model.things.ThingsModelFactory;
import org.eclipse.ditto.signals.commands.base.AbstractCommandResponse;
import org.eclipse.ditto.signals.commands.base.CommandResponseJsonDeserializer;
import org.eclipse.ditto.signals.commands.things.ThingCommandResponse;

/**
 * Response to a {@link RetrieveAclEntry} command.
 *
 * @deprecated AccessControlLists belong to deprecated API version 1. Use API version 2 with policies instead.
 */
@Deprecated
@Immutable
@JsonParsableCommandResponse(type = RetrieveAclEntryResponse.TYPE)
public final class RetrieveAclEntryResponse extends AbstractCommandResponse<RetrieveAclEntryResponse>
        implements ThingQueryCommandResponse<RetrieveAclEntryResponse> {

    /**
     * Type of this response.
     */
    public static final String TYPE = TYPE_PREFIX + RetrieveAclEntry.NAME;

    static final JsonFieldDefinition<String> JSON_ACL_ENTRY_SUBJECT =
            JsonFactory.newStringFieldDefinition("aclEntrySubject", FieldType.REGULAR, JsonSchemaVersion.V_1);

    static final JsonFieldDefinition<JsonObject> JSON_ACL_ENTRY_PERMISSIONS =
            JsonFactory.newJsonObjectFieldDefinition("aclEntryPermissions", FieldType.REGULAR, JsonSchemaVersion.V_1);

    private final ThingId thingId;
    private final String aclEntrySubject;
    private final JsonObject aclEntryPermissions;

    private RetrieveAclEntryResponse(final ThingId thingId,
            final HttpStatus statusCode,
            final String aclEntrySubject,
            final JsonObject aclEntryPermissions,
            final DittoHeaders dittoHeaders) {

        super(TYPE, statusCode, dittoHeaders);
        this.thingId = checkNotNull(thingId, "thing ID");
        this.aclEntrySubject = checkNotNull(aclEntrySubject, "AclEntry Subject");
        this.aclEntryPermissions = checkNotNull(aclEntryPermissions, "AclEntry Permissions");
    }

    /**
     * Creates a response to a {@link RetrieveAclEntry} command.
     *
     * @param thingId the Thing ID of the retrieved acl entry.
     * @param aclEntry the retrieved AclEntry.
     * @param dittoHeaders the headers of the preceding command.
     * @return the response.
     * @throws NullPointerException if any argument is {@code null}.
     * @deprecated Thing ID is now typed. Use
     * {@link #of(org.eclipse.ditto.model.things.ThingId, org.eclipse.ditto.model.things.AclEntry, org.eclipse.ditto.model.base.headers.DittoHeaders)}
     * instead.
     */
    @Deprecated
    public static RetrieveAclEntryResponse of(final String thingId, final AclEntry aclEntry,
            final DittoHeaders dittoHeaders) {

        return of(ThingId.of(thingId), aclEntry, dittoHeaders);
    }

    /**
     * Creates a response to a {@link RetrieveAclEntry} command.
     *
     * @param thingId the Thing ID of the retrieved acl entry.
     * @param aclEntry the retrieved AclEntry.
     * @param dittoHeaders the headers of the preceding command.
     * @return the response.
     * @throws NullPointerException if any argument is {@code null}.
     */
    public static RetrieveAclEntryResponse of(final ThingId thingId, final AclEntry aclEntry,
            final DittoHeaders dittoHeaders) {

        checkNotNull(aclEntry, "AclEntry");
        return new RetrieveAclEntryResponse(thingId,
                HttpStatus.OK,
                aclEntry.getAuthorizationSubject().getId(),
                aclEntry.getPermissions()
                        .toJson(dittoHeaders.getSchemaVersion().orElse(aclEntry.getLatestSchemaVersion())),
                dittoHeaders);
    }

    /**
     * Creates a response to a {@link RetrieveAclEntry} command from a JSON string.
     *
     * @param jsonString the JSON string of which the response is to be created.
     * @param dittoHeaders the headers of the preceding command.
     * @return the response.
     * @throws NullPointerException if {@code jsonString} is {@code null}.
     * @throws IllegalArgumentException if {@code jsonString} is empty.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonString} was not in the expected
     * format.
     */
    public static RetrieveAclEntryResponse fromJson(final String jsonString, final DittoHeaders dittoHeaders) {
        return fromJson(JsonFactory.newObject(jsonString), dittoHeaders);
    }

    /**
     * Creates a response to a {@link RetrieveAclEntry} command from a JSON object.
     *
     * @param jsonObject the JSON object of which the response is to be created.
     * @param dittoHeaders the headers of the preceding command.
     * @return the response.
     * @throws NullPointerException if {@code jsonObject} is {@code null}.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonObject} was not in the expected
     * format.
     */
    public static RetrieveAclEntryResponse fromJson(final JsonObject jsonObject, final DittoHeaders dittoHeaders) {
        return new CommandResponseJsonDeserializer<RetrieveAclEntryResponse>(TYPE, jsonObject).deserialize(
                httpStatus -> {

                    final String extractedThingId =
                            jsonObject.getValueOrThrow(ThingCommandResponse.JsonFields.JSON_THING_ID);
                    final ThingId thingId = ThingId.of(extractedThingId);
                    final String aclEntrySubject = jsonObject.getValueOrThrow(JSON_ACL_ENTRY_SUBJECT);
                    final JsonObject aclEntryPermissions = jsonObject.getValueOrThrow(JSON_ACL_ENTRY_PERMISSIONS);
                    final AclEntry extractedAclEntry =
                            ThingsModelFactory.newAclEntry(aclEntrySubject, aclEntryPermissions);

                    return of(thingId, extractedAclEntry, dittoHeaders);
                });
    }

    @Override
    public ThingId getThingEntityId() {
        return thingId;
    }

    /**
     * Returns the retrieved AclEntry.
     *
     * @return the retrieved AclEntry.
     */
    public AclEntry getAclEntry() {
        return AccessControlListModelFactory.newAclEntry(aclEntrySubject, aclEntryPermissions);
    }

    @Override
    public JsonValue getEntity(final JsonSchemaVersion schemaVersion) {
        return aclEntryPermissions;
    }

    @Override
    public RetrieveAclEntryResponse setEntity(final JsonValue entity) {
        checkNotNull(entity, "entity");
        final AclEntry entry = AccessControlListModelFactory.newAclEntry(aclEntrySubject, entity.asObject());
        return of(thingId, entry, getDittoHeaders());
    }

    @Override
    public RetrieveAclEntryResponse setDittoHeaders(final DittoHeaders dittoHeaders) {
        return of(thingId, getAclEntry(), dittoHeaders);
    }

    @Override
    public JsonPointer getResourcePath() {
        final String path = "/acl/" + aclEntrySubject;
        return JsonPointer.of(path);
    }

    @Override
    protected void appendPayload(final JsonObjectBuilder jsonObjectBuilder, final JsonSchemaVersion schemaVersion,
            final Predicate<JsonField> thePredicate) {

        final Predicate<JsonField> predicate = schemaVersion.and(thePredicate);
        jsonObjectBuilder.set(ThingCommandResponse.JsonFields.JSON_THING_ID, thingId.toString(), predicate);
        jsonObjectBuilder.set(JSON_ACL_ENTRY_SUBJECT, aclEntrySubject, predicate);
        jsonObjectBuilder.set(JSON_ACL_ENTRY_PERMISSIONS, aclEntryPermissions, predicate);
    }

    /**
     * RetrieveAclEntryResponse is only available in JsonSchemaVersion V_1.
     *
     * @return the supported JsonSchemaVersions of RetrieveAclEntryResponse.
     */
    @Override
    public JsonSchemaVersion[] getSupportedSchemaVersions() {
        return new JsonSchemaVersion[]{JsonSchemaVersion.V_1};
    }

    @Override
    protected boolean canEqual(@Nullable final Object other) {
        return other instanceof RetrieveAclEntryResponse;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RetrieveAclEntryResponse that = (RetrieveAclEntryResponse) o;
        return that.canEqual(this) &&
                Objects.equals(thingId, that.thingId) &&
                Objects.equals(aclEntrySubject, that.aclEntrySubject) &&
                Objects.equals(aclEntryPermissions, that.aclEntryPermissions) &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), thingId, aclEntrySubject, aclEntryPermissions);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + super.toString() + ", thingId=" + thingId + ", aclEntrySubject=" +
                aclEntrySubject + ", aclEntryPermissions=" + aclEntryPermissions + "]";
    }

}
