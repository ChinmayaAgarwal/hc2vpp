/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.mpls;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.StaticLspConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.InSegment;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.in.segment.type.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.mpls.rev171120.LookupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.mpls.rev171120.StaticLspVppLookupAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Translates {@link StaticLspConfig.Operation#PopAndLookup} operation with {@link LookupType#Mpls} to
 * mpls_route_add_del API.
 *
 * @see <a href="https://git.fd.io/vpp/tree/src/vnet/mpls/mpls.api">mpls_route_add_del</a> definition
 */
final class MplsLookupWriter implements LspWriter {
    private static final byte MPLS_PROTOCOL = (byte) LookupType.Mpls.getIntValue();

    private final FutureJVppCore vppApi;

    MplsLookupWriter(@Nonnull final FutureJVppCore vppApi) {
        this.vppApi = vppApi;
    }

    @Override
    public void write(@Nonnull final InstanceIdentifier<StaticLsp> id, @Nonnull final StaticLsp data,
                      @Nonnull final MappingContext ctx, final boolean isAdd) throws WriteFailedException {
        final Config config = data.getConfig();
        final MplsRouteAddDel request = new MplsRouteAddDel();

        request.mrIsAdd = booleanToByte(isAdd);

        translate(request, config);
        translate(request, config.getAugmentation(StaticLspVppLookupAugmentation.class));

        // default values based on inspecting VPP's CLI and make test code
        request.mrClassifyTableIndex = -1;
        request.mrNextHopProto = MPLS_PROTOCOL;
        request.mrNextHopWeight = 1;
        request.mrNextHop = new byte[0]; // no next hop since we POP
        request.mrNextHopOutLabelStack = new int[0]; // no new labels
        request.mrNextHopSwIfIndex = -1;
        request.mrNextHopViaLabel = MPLS_LABEL_INVALID;

        getReplyForWrite(vppApi.mplsRouteAddDel(request).toCompletableFuture(), id);
    }

    private void translate(@Nonnull final MplsRouteAddDel request, @Nonnull final Config config) {
        final InSegment inSegment = config.getInSegment();
        checkArgument(inSegment != null, "Configuring mpls pop-and-lookup, but in-segment is missing.");

        checkArgument(inSegment.getType() instanceof MplsLabel, "Expecting mpls-label in-segment type, but %s given.",
            inSegment.getType());
        final Long label = ((MplsLabel) inSegment.getType()).getIncomingLabel().getValue();
        request.mrLabel = label.intValue();
    }

    private void translate(@Nonnull final MplsRouteAddDel request,
                           @Nonnull final StaticLspVppLookupAugmentation vppLabelLookup) {
        // MPLS lookup for the last label is not valid operation (there is no next label to lookup),
        // so match only labels without EOS bit set:
        request.mrEos = 0;
        final Long mplsLookupInTable = vppLabelLookup.getLabelLookup().getMplsLookupInTable();
        checkArgument(mplsLookupInTable != null,
            "Configuring pop and mpls lookup, but MPLS lookup table was not given");
        request.mrNextHopTableId = mplsLookupInTable.intValue();
    }
}