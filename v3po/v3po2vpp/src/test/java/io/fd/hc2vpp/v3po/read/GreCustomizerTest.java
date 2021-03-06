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

package io.fd.hc2vpp.v3po.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.read.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.VppInvocationException;
import io.fd.jvpp.core.dto.GreTunnelDetails;
import io.fd.jvpp.core.dto.GreTunnelDetailsReplyDump;
import io.fd.jvpp.core.dto.GreTunnelDump;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import io.fd.jvpp.core.types.GreTunnel;
import io.fd.jvpp.core.types.InterfaceIndex;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Gre;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.GreBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GreCustomizerTest extends ReaderCustomizerTest<Gre, GreBuilder> implements AddressTranslator {

    private static final String IFACE_NAME = "ifc1";
    private static final int IFACE_ID = 0;
    private static final String IFC_CTX_NAME = "ifc-test-instance";

    private NamingContext interfacesContext;
    static final InstanceIdentifier<Gre> IID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(Gre.class);

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

    public GreCustomizerTest() {
        super(Gre.class, VppInterfaceAugmentationBuilder.class);
    }

    @Override
    public void setUp() throws VppInvocationException, ReadFailedException {
        interfacesContext = new NamingContext("gre-tunnel", IFC_CTX_NAME);
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "gre-tunnel4".getBytes();

        when(dumpCacheManager.getInterfaceDetail(IID, ctx, IFACE_NAME)).thenReturn(v);

        final GreTunnelDetailsReplyDump value = new GreTunnelDetailsReplyDump();
        final GreTunnelDetails greTunnelDetails = new GreTunnelDetails();
        greTunnelDetails.tunnel = new GreTunnel();
        greTunnelDetails.tunnel.dst = ipv4AddressToAddress(new Ipv4Address("1.2.3.4"));
        greTunnelDetails.tunnel.src = ipv4AddressToAddress(new Ipv4Address("1.2.3.5"));
        greTunnelDetails.tunnel.outerFibId = 55;
        greTunnelDetails.tunnel.swIfIndex = new InterfaceIndex();
        greTunnelDetails.tunnel.swIfIndex.interfaceindex = 0;
        value.greTunnelDetails = Lists.newArrayList(greTunnelDetails);

        doReturn(future(value)).when(api).greTunnelDump(any(GreTunnelDump.class));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final GreBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        assertEquals(55, builder.getOuterFibId().intValue());

        assertNull(builder.getSrc().getIpv6AddressNoZone());
        assertNotNull(builder.getSrc().getIpv4AddressNoZone());
        assertEquals("1.2.3.5", builder.getSrc().getIpv4AddressNoZone().getValue());

        assertNull(builder.getDst().getIpv6AddressNoZone());
        assertNotNull(builder.getDst().getIpv4AddressNoZone());
        assertEquals("1.2.3.4", builder.getDst().getIpv4AddressNoZone().getValue());

        verify(api).greTunnelDump(any(GreTunnelDump.class));
    }

    @Test
    public void testReadCurrentAttributesWrongType() throws Exception {
        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "tap-2".getBytes();

        when(dumpCacheManager.getInterfaceDetail(IID, ctx, IFACE_NAME)).thenReturn(v);

        final GreBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verifyZeroInteractions(api);
    }

    @Override
    protected ReaderCustomizer<Gre, GreBuilder> initCustomizer() {
        return new GreCustomizer(api, interfacesContext, dumpCacheManager);
    }
}
