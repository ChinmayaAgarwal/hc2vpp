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

package io.fd.hc2vpp.vpp.classifier.write.acl.egress;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.write.acl.common.IetfAclWriter;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp._interface.acl.rev170315.VppInterfaceAclAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.MixedAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.ietf.acl.base.attributes.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.ietf.acl.base.attributes.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.vpp.acl.attributes.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.vpp.acl.attributes.ietf.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.vpp.acl.attributes.ietf.acl.EgressBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IetfAclCustomizerTest extends WriterCustomizerTest {
    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Egress> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME)).augmentation(
            VppInterfaceAclAugmentation.class).child(IetfAcl.class).child(Egress.class);
    private static final String ACL_NAME = "acl1";
    private static final Class<? extends AclBase> ACL_TYPE = MixedAcl.class;

    @Mock
    private IetfAclWriter aclWriter;
    private IetfAclCustomizer customizer;

    private static Egress acl(final InterfaceMode mode) {
        return new EgressBuilder().setAccessLists(
            new AccessListsBuilder().setAcl(
                Collections.singletonList(new AclBuilder()
                    .setName(ACL_NAME)
                    .setType(ACL_TYPE)
                    .build())
            ).setMode(mode)
                .build()
        ).build();
    }

    @Override
    protected void setUpTest() {
        customizer = new IetfAclCustomizer(aclWriter, new NamingContext("prefix", IFC_TEST_INSTANCE));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
    }

    private void verifyWrite(final AccessLists accessLists) throws WriteFailedException {
        verify(aclWriter)
            .write(IID, IF_INDEX, accessLists.getAcl(), accessLists.getDefaultAction(), accessLists.getMode(),
                writeContext, mappingContext);
    }

    private void verifyDelete() throws WriteFailedException {
        verify(aclWriter).deleteAcl(IID, IF_INDEX, mappingContext);
    }

    @Test
    public void testWriteL3() throws Exception {
        customizer.writeCurrentAttributes(IID, acl(InterfaceMode.L3), writeContext);
        verifyZeroInteractions(aclWriter);
    }

    @Test
    public void testWriteL2() throws Exception {
        final Egress acl = acl(InterfaceMode.L2);
        customizer.writeCurrentAttributes(IID, acl, writeContext);
        verifyWrite(acl.getAccessLists());
    }

    @Test
    public void testUpdate() throws Exception {
        final Egress aclBefore = acl(InterfaceMode.L3);
        final Egress aclAfter = acl(InterfaceMode.L2);
        customizer.updateCurrentAttributes(IID, aclBefore, aclAfter, writeContext);
        verifyDelete();
        verifyWrite(aclAfter.getAccessLists());
    }

    @Test
    public void testDelete() throws Exception {
        final Egress acl = acl(InterfaceMode.L2);
        customizer.deleteCurrentAttributes(IID, acl, writeContext);
        verifyDelete();
    }
}