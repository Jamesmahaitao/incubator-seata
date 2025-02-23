/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.core.protocol.transaction;

import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A unit test for {@link GlobalBeginResponse}
 *
 **/
public class GlobalBeginResponseTest {
    private final String XID = "test_xid";
    private final String EXTRA_DATA = "test_extra_data";
    private final ResultCode RESULT_CODE = ResultCode.Success;

    @Test
    public void testGetSetXid() {
        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setXid(XID);
        Assertions.assertEquals(XID, globalBeginResponse.getXid());
    }

    @Test
    public void testGetSetExtraData() {
        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setExtraData(EXTRA_DATA);
        Assertions.assertEquals(EXTRA_DATA, globalBeginResponse.getExtraData());
    }

    @Test
    public void testGetTypeCode() {
        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        Assertions.assertEquals(MessageType.TYPE_GLOBAL_BEGIN_RESULT, globalBeginResponse.getTypeCode());
    }

}
