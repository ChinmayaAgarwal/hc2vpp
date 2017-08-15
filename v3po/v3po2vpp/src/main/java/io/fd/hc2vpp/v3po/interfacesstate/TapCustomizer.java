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

package io.fd.hc2vpp.v3po.interfacesstate;

import static java.lang.String.format;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceTapDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceTapDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.SwInterfaceTapDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces.state._interface.TapBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TapCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<Tap, TapBuilder>, InterfaceDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TapCustomizer.class);
    // TODO - HC2VPP-213 - used dump cache manager
    public static final String DUMPED_TAPS_CONTEXT_KEY = TapCustomizer.class.getName() + "dumpedTapsDuringGetAllIds";
    private NamingContext interfaceContext;
    private final InterfaceCacheDumpManager dumpManager;

    public TapCustomizer(@Nonnull final FutureJVppCore jvpp,
                         @Nonnull final NamingContext interfaceContext,
                         @Nonnull final InterfaceCacheDumpManager dumpManager) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
        this.dumpManager = dumpManager;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull Tap readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setTap(readValue);
    }

    @Nonnull
    @Override
    public TapBuilder getBuilder(@Nonnull InstanceIdentifier<Tap> id) {
        return new TapBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Tap> id,
                                      @Nonnull final TapBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        final SwInterfaceDetails ifcDetails = dumpManager.getInterfaceDetail(id, ctx, key.getName());

        if (!isInterfaceOfType(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.Tap.class,
                ifcDetails)) {
            return;
        }

        LOG.debug("Reading attributes for tap interface: {}", key.getName());

        @SuppressWarnings("unchecked")
        Map<Integer, SwInterfaceTapDetails> mappedTaps =
                (Map<Integer, SwInterfaceTapDetails>) ctx.getModificationCache().get(DUMPED_TAPS_CONTEXT_KEY);

        if (mappedTaps == null) {
            // Full Tap dump has to be performed here, no filter or anything is here to help so at least we cache it
            final SwInterfaceTapDump request = new SwInterfaceTapDump();
            final CompletionStage<SwInterfaceTapDetailsReplyDump> swInterfaceTapDetailsReplyDumpCompletionStage =
                    getFutureJVpp().swInterfaceTapDump(request);
            final SwInterfaceTapDetailsReplyDump reply =
                    getReplyForRead(swInterfaceTapDetailsReplyDumpCompletionStage.toCompletableFuture(), id);

            if (null == reply || null == reply.swInterfaceTapDetails) {
                mappedTaps = Collections.emptyMap();
            } else {
                final List<SwInterfaceTapDetails> swInterfaceTapDetails = reply.swInterfaceTapDetails;
                // Cache interfaces dump in per-tx context to later be used in readCurrentAttributes
                mappedTaps = swInterfaceTapDetails.stream()
                        .collect(Collectors.toMap(t -> t.swIfIndex, swInterfaceDetails -> swInterfaceDetails));
            }

            ctx.getModificationCache().put(DUMPED_TAPS_CONTEXT_KEY, mappedTaps);
        }

        final SwInterfaceTapDetails swInterfaceTapDetails = mappedTaps.get(index);
        LOG.trace("Tap interface: {} attributes returned from VPP: {}", key.getName(), swInterfaceTapDetails);

        builder.setTapName(toString(swInterfaceTapDetails.devName));

        if (ifcDetails.tag[0] != 0) { // tag supplied
            builder.setTag(toString(ifcDetails.tag));
        }
        LOG.debug("Tap interface: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.Tap> init(
            @Nonnull final InstanceIdentifier<Tap> id, @Nonnull final Tap readValue, @Nonnull final ReadContext ctx) {
        // The MAC address & tag is set from interface details, those details are retrieved from cache
        final InterfaceKey key = id.firstKeyOf(Interface.class);

        final SwInterfaceDetails ifcDetails;
        try {
            ifcDetails = dumpManager.getInterfaceDetail(id, ctx, key.getName());
        } catch (ReadFailedException e) {
            throw new IllegalStateException(format("Unable to read interface %s", key.getName()), e);
        }

        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.TapBuilder()
                        .setMac(new PhysAddress(vppPhysAddrToYang(ifcDetails.l2Address)))
                        .setTapName(readValue.getTapName())
                        .setTag(ifcDetails.tag[0] == 0
                                ? null
                                : toString(ifcDetails.tag))
//                            tapBuilder.setDeviceInstance();
                        .build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.Tap> getCfgId(
            final InstanceIdentifier<Tap> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.Tap.class);
    }
}
