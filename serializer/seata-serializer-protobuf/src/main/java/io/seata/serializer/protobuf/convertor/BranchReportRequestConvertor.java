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

import io.seata.serializer.protobuf.generated.AbstractMessageProto;
import io.seata.serializer.protobuf.generated.AbstractTransactionRequestProto;
import io.seata.serializer.protobuf.generated.BranchReportRequestProto;
import io.seata.serializer.protobuf.generated.BranchStatusProto;
import io.seata.serializer.protobuf.generated.BranchTypeProto;
import io.seata.serializer.protobuf.generated.MessageTypeProto;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchReportRequest;

/**
 */
public class BranchReportRequestConvertor implements PbConvertor<BranchReportRequest, BranchReportRequestProto> {
    @Override
    public BranchReportRequestProto convert2Proto(BranchReportRequest branchReportRequest) {
        final short typeCode = branchReportRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(abstractMessage).build();

        final String applicationData = branchReportRequest.getApplicationData();
        final String resourceId = branchReportRequest.getResourceId();
        BranchReportRequestProto result = BranchReportRequestProto.newBuilder().setAbstractTransactionRequest(
            abstractTransactionRequestProto).setXid(branchReportRequest.getXid()).setBranchId(
            branchReportRequest.getBranchId()).setBranchType(
            BranchTypeProto.valueOf(branchReportRequest.getBranchType().name())).setApplicationData(
            applicationData == null ? "" : applicationData).setResourceId(resourceId == null ? "" : resourceId)
            .setStatus(BranchStatusProto.valueOf(branchReportRequest.getStatus().name())).build();

        return result;
    }

    @Override
    public BranchReportRequest convert2Model(BranchReportRequestProto branchReportRequestProto) {
        BranchReportRequest branchReportRequest = new BranchReportRequest();
        branchReportRequest.setApplicationData(branchReportRequestProto.getApplicationData());
        branchReportRequest.setBranchId(branchReportRequestProto.getBranchId());
        branchReportRequest.setResourceId(branchReportRequestProto.getResourceId());
        branchReportRequest.setXid(branchReportRequestProto.getXid());
        branchReportRequest.setBranchType(BranchType.valueOf(branchReportRequestProto.getBranchType().name()));
        branchReportRequest.setStatus(BranchStatus.valueOf(branchReportRequestProto.getStatus().name()));
        return branchReportRequest;
    }
}
