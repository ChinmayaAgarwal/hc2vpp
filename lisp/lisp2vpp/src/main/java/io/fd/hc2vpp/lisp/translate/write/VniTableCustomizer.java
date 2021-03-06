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

package io.fd.hc2vpp.lisp.translate.write;

import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * This customizer serves only as a check if user is not trying to define VniTable <br>
 * without mapping to vrf/bd
 */
public class VniTableCustomizer extends CheckedLispCustomizer implements ListWriterCustomizer<VniTable, VniTableKey> {

    public VniTableCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                              @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJvpp, lispStateCheckService);
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<VniTable> id, VniTable dataAfter, WriteContext writeContext)
            throws WriteFailedException {
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        checkAtLeastOnChildExists(id, writeContext, false);
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<VniTable> id, VniTable dataBefore, WriteContext writeContext)
            throws WriteFailedException {
        lispStateCheckService.checkLispEnabledBefore(writeContext);
        checkAtLeastOnChildExists(id, writeContext, true);
    }

    private void checkAtLeastOnChildExists(final InstanceIdentifier<VniTable> id, final WriteContext writeContext,
                                           final boolean before) {

        Optional<VniTable> optData;
        final InstanceIdentifier<VniTable> trimmedId = RWUtils.cutId(id, InstanceIdentifier.create(VniTable.class));
        if (before) {
            optData = writeContext.readBefore(trimmedId);
        } else {
            optData = writeContext.readAfter(trimmedId);
        }

        checkState(optData.isPresent(), "Illegal after-write state");

        final VniTable dataAfter = optData.get();
        checkState(dataAfter.getVrfSubtable() != null || dataAfter.getBridgeDomainSubtable() != null,
                "At least one of VrfSubtable/BridgeDomainSubtable must be defined");
    }
}
