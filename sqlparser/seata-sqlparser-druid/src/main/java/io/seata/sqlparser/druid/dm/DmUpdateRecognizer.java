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
package io.seata.sqlparser.druid.dm;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.sqlparser.util.ColumnUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The type dm update recognizer.
 *
 */
public class DmUpdateRecognizer extends BaseDmRecognizer implements SQLUpdateRecognizer {

    private SQLUpdateStatement ast;

    /**
     * Instantiates a new My sql update recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public DmUpdateRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (SQLUpdateStatement)ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.UPDATE;
    }

    @Override
    public List<String> getUpdateColumns() {
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<String> list = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            SQLExpr expr = updateSetItem.getColumn();
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr)expr).getName());
            } else if (expr instanceof SQLPropertyExpr) {
                // This is alias case, like UPDATE xxx_tbl a SET a.name = ? WHERE a.id = ?
                SQLExpr owner = ((SQLPropertyExpr)expr).getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    list.add(((SQLIdentifierExpr)owner).getName() + "." + ((SQLPropertyExpr)expr).getName());
                    //This is table Field Full path, like update xxx_database.xxx_tbl set xxx_database.xxx_tbl.xxx_field...
                } else if (((SQLPropertyExpr) expr).getOwnernName().split("\\.").length > 1) {
                    list.add(((SQLPropertyExpr)expr).getOwnernName()  + "." + ((SQLPropertyExpr)expr).getName());
                }
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public List<Object> getUpdateValues() {
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<Object> list = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            SQLExpr expr = updateSetItem.getValue();
            if (expr instanceof SQLValuableExpr) {
                list.add(((SQLValuableExpr)expr).getValue());
            } else if (expr instanceof SQLVariantRefExpr) {
                list.add(new VMarker());
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public String getWhereCondition(final ParametersHolder parametersHolder,
        final ArrayList<List<Object>> paramAppenderList) {
        SQLExpr where = ast.getWhere();
        return super.getWhereCondition(where, parametersHolder, paramAppenderList);
    }

    @Override
    public String getWhereCondition() {
        SQLExpr where = ast.getWhere();
        return super.getWhereCondition(where);
    }

    @Override
    public String getTableAlias() {
        return ast.getTableSource().getAlias();
    }

    @Override
    public String getTableName() {
        StringBuilder sb = new StringBuilder();
        OracleOutputVisitor visitor = new OracleOutputVisitor(sb) {

            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }

            @Override
            public boolean visit(SQLJoinTableSource x) {
                throw new NotSupportYetException("not support the syntax of update with join table");
            }
        };
        SQLTableSource tableSource = ast.getTableSource();
        if (tableSource instanceof SQLExprTableSource) {
            visitor.visit((SQLExprTableSource) tableSource);
        } else if (tableSource instanceof SQLJoinTableSource) {
            visitor.visit((SQLJoinTableSource) tableSource);
        } else {
            throw new NotSupportYetException("not support the syntax of update with unknow");
        }
        return sb.toString();
    }

    @Override
    public String getLimitCondition() {
        return null;
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override
    public String getOrderByCondition() {
        return null;
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override
    public List<String> getUpdateColumnsUnEscape() {
        List<String> updateColumns = getUpdateColumns();
        return ColumnUtils.delEscape(updateColumns, getDbType());
    }

    @Override
    protected SQLStatement getAst() {
        return this.ast;
    }
}
