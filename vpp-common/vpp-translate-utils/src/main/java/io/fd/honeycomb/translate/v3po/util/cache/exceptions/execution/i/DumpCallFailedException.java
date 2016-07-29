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

package io.fd.honeycomb.translate.v3po.util.cache.exceptions.execution.i;

import io.fd.honeycomb.translate.v3po.util.cache.exceptions.execution.DumpExecutionFailedException;
import org.openvpp.jvpp.VppBaseCallException;

/**
 * Wrapper exception for {@link org.openvpp.jvpp.VppBaseCallException} during dumping
 */
public class DumpCallFailedException extends DumpExecutionFailedException {

    public DumpCallFailedException(String message, VppBaseCallException cause) {
        super(message, cause);
    }

    public static final DumpCallFailedException wrapFailedCallException(String message, VppBaseCallException cause) {
        return new DumpCallFailedException(message, cause);
    }
}