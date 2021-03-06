/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fd.hc2vpp.acl.write.request;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.acl.dto.AclInterfaceSetAclList;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multi-assignment single-request taking advantage from acl_interface_set_acl_list api
 */
public class AclInterfaceAssignmentRequest implements JvppReplyConsumer, ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(AclInterfaceAssignmentRequest.class);

    private final MappingContext mappingContext;
    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface>
            identifier;
    private List<String> inputAclNames = Collections.emptyList();
    private List<String> outputAclNames = Collections.emptyList();
    private AclContextManager standardAclContext;
    private NamingContext interfaceContext;


    private AclInterfaceAssignmentRequest(final MappingContext mappingContext) {
        this.mappingContext = checkNotNull(mappingContext, "Mapping context cannot be null");
    }

    public static AclInterfaceAssignmentRequest create(@Nonnull final MappingContext mappingContext) {
        return new AclInterfaceAssignmentRequest(mappingContext);
    }

    public AclInterfaceAssignmentRequest identifier(
            @Nonnull final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface> identifier) {
        this.identifier = identifier;
        return this;
    }

    public AclInterfaceAssignmentRequest inputAclNames(@Nonnull final List<String> inputAclNames) {
        checkNotNull(inputAclNames, "Input ACL names cannot be null");
        this.inputAclNames = ImmutableList.copyOf(inputAclNames);
        return this;
    }

    public AclInterfaceAssignmentRequest outputAclNames(@Nonnull final List<String> outputAclNames) {
        checkNotNull(outputAclNames, "Output ACL names cannot be null");
        this.outputAclNames = ImmutableList.copyOf(outputAclNames);
        return this;
    }

    public AclInterfaceAssignmentRequest standardAclContext(@Nonnull final AclContextManager standardAclContext) {
        this.standardAclContext = standardAclContext;
        return this;
    }

    public AclInterfaceAssignmentRequest interfaceContext(@Nonnull final NamingContext interfaceContext) {
        this.interfaceContext = interfaceContext;
        return this;
    }

    private void checkValidRequest() {
        checkNotNull(identifier, "Identifier cannot be null");
        checkNotNull(standardAclContext, "ACL context cannot be null");
        checkNotNull(interfaceContext, "Interface context cannot be null");
    }

    public void executeAsCreate(@Nonnull final FutureJVppAclFacade api) throws WriteFailedException {
        checkValidRequest();
        final String interfaceName = identifier.firstKeyOf(Interface.class).getInterfaceId();

        // locking on mapping context, to prevent modifying of mappings (for both contexts) during binding/execution of request
        synchronized (mappingContext) {
            LOG.debug(
                    "Executing acl interface assignment write request for interface={}, input ACL's={},output ACL's={}",
                    interfaceName, inputAclNames, outputAclNames);

            getReplyForWrite(api.aclInterfaceSetAclList(createRequest(interfaceName)).toCompletableFuture(),
                    identifier);
            LOG.debug(
                    "Acl interface assignment write request for interface={}, input ACL's={},output ACL's={} successfully passed",
                    interfaceName, inputAclNames, outputAclNames);
        }
    }

    public void executeAsUpdate(@Nonnull final FutureJVppAclFacade api,
                                final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface before,
                                final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface after)
            throws WriteFailedException {
        checkValidRequest();
        final String interfaceName = identifier.firstKeyOf(Interface.class).getInterfaceId();

        // locking on mapping context, to prevent modifying of mappings (for both contexts) during binding/execution of request
        synchronized (mappingContext) {
            LOG.debug(
                    "Executing acl interface assignment update request for interface={}, input ACL's={},output ACL's={}",
                    interfaceName, inputAclNames, outputAclNames);

            getReplyForUpdate(api.aclInterfaceSetAclList(createRequest(interfaceName)).toCompletableFuture(),
                    identifier, before, after);
            LOG.debug(
                    "Acl interface assignment update request for interface={}, input ACL's={},output ACL's={} successfully passed",
                    interfaceName, inputAclNames, outputAclNames);
        }
    }

    public void executeAsDelete(@Nonnull final FutureJVppAclFacade api) throws WriteFailedException {
        checkValidRequest();
        final String interfaceName = identifier.firstKeyOf(Interface.class).getInterfaceId();

        // locking on mapping context, to prevent modifying of mappings (for both contexts) during binding/execution of request
        synchronized (mappingContext) {
            LOG.debug(
                    "Executing acl interface assignment delete request for interface={}, input ACL's={},output ACL's={}",
                    interfaceName, inputAclNames, outputAclNames);

            // remove all ACLs, just in case they were set by AclInterfaceAssignmentRequest user
            inputAclNames = Collections.emptyList();
            outputAclNames = Collections.emptyList();

            getReplyForDelete(api.aclInterfaceSetAclList(createRequest(interfaceName)).toCompletableFuture(),
                    identifier);
            LOG.debug(
                    "Acl interface assignment delete request for interface={}, input ACL's={},output ACL's={} successfully passed",
                    interfaceName, inputAclNames, outputAclNames);
        }
    }

    // synchronized on higher layer
    private AclInterfaceSetAclList createRequest(final String interfaceName) {

        AclInterfaceSetAclList request = new AclInterfaceSetAclList();
        request.swIfIndex = interfaceContext.getIndex(interfaceName, mappingContext);
        // FIXME (HC2VPP-201): possible overflow
        request.nInput = (byte) inputAclNames.size();
        request.count = (byte) (inputAclNames.size() + outputAclNames.size());
        request.acls = Stream.concat(inputAclNames.stream(), outputAclNames.stream())
                .mapToInt(aclName -> standardAclContext.getAclIndex(aclName, mappingContext))
                .toArray();
        return request;
    }
}
