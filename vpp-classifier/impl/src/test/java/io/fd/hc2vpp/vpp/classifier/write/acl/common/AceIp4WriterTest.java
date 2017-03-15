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

package io.fd.hc2vpp.vpp.classifier.write.acl.common;

import static io.fd.hc2vpp.vpp.classifier.write.acl.common.AclTranslator.VLAN_TAG_LEN;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.InterfaceMode;

public class AceIp4WriterTest {

    private AceIp4Writer writer;
    private PacketHandling action;
    private AceIp aceIp;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        writer = new AceIp4Writer();
        action = new DenyBuilder().setDeny(true).build();
        aceIp = new AceIpBuilder()
            .setProtocol((short) 132)
            .setDscp(new Dscp((short) 11))
            .setAceIpVersion(new AceIpv4Builder()
                .setSourceIpv4Network(new Ipv4Prefix("1.2.3.4/32"))
                .setDestinationIpv4Network(new Ipv4Prefix("1.2.4.5/24"))
                .build())
            .setSourcePortRange(new SourcePortRangeBuilder().setLowerPort(new PortNumber(0x1111)).build())
            .setDestinationPortRange(new DestinationPortRangeBuilder().setLowerPort(new PortNumber(0x2222)).build())
            .build();
    }

    private static void verifyTableRequest(final ClassifyAddDelTable request, final int nextTableIndex,
                                           final int vlanTags, final boolean isL2) {
        assertEquals(1, request.isAdd);
        assertEquals(-1, request.tableIndex);
        assertEquals(1, request.nbuckets);
        assertEquals(nextTableIndex, request.nextTableIndex);
        assertEquals(0, request.skipNVectors);
        assertEquals(AceIp4Writer.MATCH_N_VECTORS, request.matchNVectors);
        assertEquals(AceIp4Writer.TABLE_MEM_SIZE, request.memorySize);

        byte[] expectedMask = new byte[] {
            // L2:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // dscp:
            (byte) 0x00, (byte) 0xfc,
            // protocol:
            0, 0, 0, 0, 0, 0, 0, (byte) 0xff, 0, 0,
            // source address:
            -1, -1, -1, -1,
            // destination address:
            -1, -1, -1, 0,
            // source and destination port:
            -1, -1, -1, -1,
            // padding:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        if (isL2) {
            expectedMask[12] = (byte) 0xff;
            expectedMask[13] = (byte) 0xff;
        }
        AceIpWriterTestUtils
            .assertArrayEqualsWithOffset(expectedMask, expectedMask.length, request.mask, vlanTags * VLAN_TAG_LEN);

    }

    private static void verifySessionRequest(final ClassifyAddDelSession request, final int tableIndex,
                                             final int vlanTags, final boolean isL2) {
        assertEquals(1, request.isAdd);
        assertEquals(tableIndex, request.tableIndex);
        assertEquals(0, request.hitNextIndex);

        byte[] expectedMatch = new byte[] {
            // L2:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // dscp:
            0, (byte) 0x2c,
            // protocol (132):
            0, 0, 0, 0, 0, 0, 0, (byte) 132, 0, 0,
            // source address:
            1, 2, 3, 4,
            // destination address:
            1, 2, 4, 0,
            // source and destination port:
            0x11, 0x11, 0x22, 0x22,
            // padding:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        if (isL2) {
            expectedMatch[12] = (byte) 0x08;
            expectedMatch[13] = (byte) 0x00;
        }
        AceIpWriterTestUtils.assertArrayEqualsWithOffset(expectedMatch, expectedMatch.length, request.match, vlanTags * VLAN_TAG_LEN);

    }

    @Test
    public void testCreateTable() throws Exception {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request = writer.createTable(aceIp, InterfaceMode.L3, nextTableIndex, 0);
        verifyTableRequest(request, nextTableIndex, 0, false);
    }

    @Test
    public void testCreateTableForL2Interface() throws Exception {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request = writer.createTable(aceIp, InterfaceMode.L2, nextTableIndex, 0);
        verifyTableRequest(request, nextTableIndex, 0, true);
    }

    @Test
    public void testCreateTable1VlanTag() throws Exception {
        final int nextTableIndex = 42;
        final int vlanTags = 1;
        final ClassifyAddDelTable request = writer.createTable(aceIp, InterfaceMode.L3, nextTableIndex, vlanTags);
        verifyTableRequest(request, nextTableIndex, vlanTags, false);
    }

    @Test
    public void testCreateTable2VlanTags() throws Exception {
        final int nextTableIndex = 42;
        final int vlanTags = 2;
        final ClassifyAddDelTable request = writer.createTable(aceIp, InterfaceMode.L3, nextTableIndex, vlanTags);
        verifyTableRequest(request, nextTableIndex, vlanTags, false);
    }

    @Test
    public void testCreateClassifySession() throws Exception {
        final int tableIndex = 123;
        final ClassifyAddDelSession request = writer.createSession(action, aceIp, InterfaceMode.L3, tableIndex, 0).get(0);
        verifySessionRequest(request, tableIndex, 0, false);
    }

    @Test
    public void testCreateClassifySessionForL2Interface() throws Exception {
        final int tableIndex = 123;
        final ClassifyAddDelSession request = writer.createSession(action, aceIp, InterfaceMode.L2, tableIndex, 0).get(0);
        verifySessionRequest(request, tableIndex, 0, true);
    }

    @Test
    public void testCreateClassifySession1VlanTag() throws Exception {
        final int tableIndex = 123;
        final int vlanTags = 1;
        final ClassifyAddDelSession request = writer.createSession(action, aceIp, InterfaceMode.L3, tableIndex, vlanTags).get(0);
        verifySessionRequest(request, tableIndex, vlanTags, false);
    }

    @Test
    public void testCreateClassifySession2VlanTags() throws Exception {
        final int tableIndex = 123;
        final int vlanTags = 2;
        final ClassifyAddDelSession request = writer.createSession(action, aceIp, InterfaceMode.L3, tableIndex, vlanTags).get(0);

        verifySessionRequest(request, tableIndex, vlanTags, false);
    }
}