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
package io.seata.serializer.protobuf.convertor;

import io.seata.serializer.protobuf.generated.GlobalCommitRequestProto;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class GlobalCommitRequestConvertorTest {

    @Test
    public void convert2Proto() {

        GlobalCommitRequest globalCommitRequest = new GlobalCommitRequest();
        globalCommitRequest.setExtraData("extraData");
        globalCommitRequest.setXid("xid");

        GlobalCommitRequestConvertor convertor = new GlobalCommitRequestConvertor();
        GlobalCommitRequestProto proto = convertor.convert2Proto(globalCommitRequest);
        GlobalCommitRequest real = convertor.convert2Model(proto);
        assertThat((real.getTypeCode())).isEqualTo(globalCommitRequest.getTypeCode());
        assertThat((real.getXid())).isEqualTo(globalCommitRequest.getXid());
        assertThat((real.getExtraData())).isEqualTo(globalCommitRequest.getExtraData());
    }
}
