/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.v3po.write;

import io.fd.hc2vpp.v3po.read.cache.InterfaceStatisticsManager;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces.Statistics;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfacesStatisticsCustomizer implements WriterCustomizer<Statistics> {

    private final InterfaceStatisticsManager statisticsManager;

    public InterfacesStatisticsCustomizer(final InterfaceStatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Statistics> instanceIdentifier,
                                       @Nonnull final Statistics statisticsCollection,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        enableDisableStatistics(statisticsCollection);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Statistics> id,
                                        @Nonnull final Statistics dataBefore,
                                        @Nonnull final Statistics dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        enableDisableStatistics(dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Statistics> instanceIdentifier,
                                        @Nonnull final Statistics statisticsCollection,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        statisticsManager.disableStatistics();
    }

    private void enableDisableStatistics(final Statistics statsCollect) {
        if (statsCollect != null) {
            if (statsCollect.isEnabled()) {
                statisticsManager.enableStatistics();
                return;
            }
        }
        statisticsManager.disableStatistics();
    }
}
