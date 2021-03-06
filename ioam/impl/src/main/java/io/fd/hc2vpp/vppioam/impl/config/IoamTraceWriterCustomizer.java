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

package io.fd.hc2vpp.vppioam.impl.config;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.vppioam.impl.util.FutureJVppIoamtraceCustomizer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.ioamtrace.dto.TraceProfileAdd;
import io.fd.jvpp.ioamtrace.dto.TraceProfileAddReply;
import io.fd.jvpp.ioamtrace.dto.TraceProfileDel;
import io.fd.jvpp.ioamtrace.dto.TraceProfileDelReply;
import io.fd.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfigKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer customizer responsible for Ioam Trace create/delete.
 */
public class IoamTraceWriterCustomizer extends FutureJVppIoamtraceCustomizer
        implements ListWriterCustomizer<TraceConfig, TraceConfigKey>, ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IoamTraceWriterCustomizer.class);

    public IoamTraceWriterCustomizer(@Nonnull FutureJVppIoamtrace futureJVppIoamtrace) {
        super(futureJVppIoamtrace);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<TraceConfig> id,
                                       @Nonnull final TraceConfig dataCurr,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        try {
            addTraceConfig(dataCurr, id);
        } catch (Exception exCreate) {
            LOG.error("Add Trace Configuration failed", exCreate);
            throw new WriteFailedException.CreateFailedException(id, dataCurr, exCreate);
        }

        LOG.debug("Trace config added iid={}, added {}", id, dataCurr);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<TraceConfig> id,
                                        @Nonnull final TraceConfig dataBefore,
                                        @Nonnull final TraceConfig dataAfter,
                                        @Nonnull final WriteContext ctx) throws WriteFailedException {
        try {
            deleteTraceConfig(id);
            addTraceConfig(dataAfter, id);
        } catch (Exception exUpdate) {
            LOG.error("Update Trace Configuration failed", exUpdate);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, exUpdate);
        }

        LOG.debug("Trace config updated {}", dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<TraceConfig> id,
                                        @Nonnull final TraceConfig dataBefore,
                                        @Nonnull final WriteContext ctx) throws WriteFailedException {
        try {
            deleteTraceConfig(id);
        } catch (Exception exDelete) {
            LOG.error("Delete Trace Configuration failed", exDelete);
            throw new WriteFailedException.DeleteFailedException(id, exDelete);
        }

        LOG.debug("Trace config deleted:iid={} dataBefore={}", id, dataBefore);
    }

    public TraceProfileAddReply addTraceConfig(TraceConfig traceConfig,
            final InstanceIdentifier<TraceConfig> id) throws WriteFailedException {

        TraceProfileAdd traceProfileAdd = new TraceProfileAdd();
        traceProfileAdd.traceType = traceConfig.getTraceType().byteValue(); //trace type
        traceProfileAdd.numElts = traceConfig.getTraceNumElt().byteValue();  //num of elts
        traceProfileAdd.traceTsp = (byte) traceConfig.getTraceTsp().getIntValue(); // tsp
        traceProfileAdd.appData = traceConfig.getTraceAppData().intValue(); // appdata
        traceProfileAdd.nodeId = traceConfig.getNodeId().intValue(); // nodeid

        /* Write to VPP */
        final TraceProfileAddReply reply = getReplyForWrite((getFutureJVppIoamtrace()
                                                            .traceProfileAdd(traceProfileAdd)
                                                            .toCompletableFuture()), id);
        return reply;
    }

    public TraceProfileDelReply deleteTraceConfig(final InstanceIdentifier<TraceConfig> id)
        throws WriteFailedException {
        TraceProfileDel del = new TraceProfileDel();

        /* Write to VPP */
        TraceProfileDelReply reply = getReplyForWrite(getFutureJVppIoamtrace().traceProfileDel(del)
            .toCompletableFuture(), id);

        return reply;
    }
}
