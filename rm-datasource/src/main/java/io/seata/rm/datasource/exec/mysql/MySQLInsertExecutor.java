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
package io.seata.rm.datasource.exec.mysql;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.BaseInsertExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.sqlparser.struct.ColumnMeta;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type My sql insert executor.
 *
 */
@LoadLevel(name = JdbcConstants.MYSQL, scope = Scope.PROTOTYPE)
public class MySQLInsertExecutor extends BaseInsertExecutor implements Defaultable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * the modify for test
     */
    public static final String ERR_SQL_STATE = "S1009";

    /**
     * The cache of auto increment step of database
     * the key is the db's resource id
     * the value is the step
     */
    public static final Map<String, BigDecimal> RESOURCE_ID_STEP_CACHE = new ConcurrentHashMap<>(8);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public MySQLInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                               SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public Map<String,List<Object>> getPkValues() throws SQLException {
        Map<String,List<Object>> pkValuesMap = null;
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
        boolean isContainsPk = containsPK();
        //when there is only one pk in the table
        if (pkColumnNameList.size() == 1) {
            if (isContainsPk) {
                pkValuesMap = getPkValuesByColumn();
            }
            else if (containsColumns()) {
                pkValuesMap = getPkValuesByAuto();
            }
            else {
                pkValuesMap = getPkValuesByColumn();
            }
        } else {
            //when there is multiple pk in the table
            //1,all pk columns are filled value.
            //2,the auto increment pk column value is null, and other pk value are not null.
            pkValuesMap = getPkValuesByColumn();
            for (String columnName:pkColumnNameList) {
                if (!pkValuesMap.containsKey(columnName)) {
                    ColumnMeta pkColumnMeta = getTableMeta().getColumnMeta(columnName);
                    if (Objects.nonNull(pkColumnMeta) && pkColumnMeta.isAutoincrement()) {
                        //3,the auto increment pk column is not exits in sql, and other pk are exits also the value is not null.
                        pkValuesMap.putAll(getPkValuesByAuto());
                    }
                }
            }
        }
        return pkValuesMap;
    }

    /**
     * the modify for test
     */
    public Map<String, List<Object>> getPkValuesByAuto() throws SQLException {
        // PK is just auto generated
        Map<String, List<Object>> pkValuesMap = new HashMap<>(8);
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        String autoColumnName = null;
        for (Map.Entry<String, ColumnMeta> entry : pkMetaMap.entrySet()) {
            if (entry.getValue().isAutoincrement()) {
                autoColumnName = entry.getKey();
                break;
            }
        }
        if (StringUtils.isBlank(autoColumnName)) {
            throw new ShouldNeverHappenException("auto increment column not exist");
        }

        ResultSet genKeys = null;
        boolean isManualCloseResultSet = false;
        try {
            genKeys = statementProxy.getGeneratedKeys();
        } catch (SQLException e) {
            // java.sql.SQLException: Generated keys not requested. You need to
            // specify Statement.RETURN_GENERATED_KEYS to
            // Statement.executeUpdate() or Connection.prepareStatement().
            if (ERR_SQL_STATE.equalsIgnoreCase(e.getSQLState())) {
                logger.error("Fail to get auto-generated keys, use 'SELECT LAST_INSERT_ID()' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.");
                int updateCount = statementProxy.getUpdateCount();
                try {
                    genKeys = statementProxy.getTargetStatement().executeQuery("SELECT LAST_INSERT_ID()");
                    // If there is batch insert
                    // do auto increment base LAST_INSERT_ID and variable `auto_increment_increment`
                    if (updateCount > 1 && canAutoIncrement(pkMetaMap)) {
                        genKeys.next();
                        BigDecimal firstId = new BigDecimal(genKeys.getString(1));
                        return autoGeneratePks(firstId, autoColumnName, updateCount);
                    } else {
                        isManualCloseResultSet = true;
                    }
                } finally {
                    if (!isManualCloseResultSet) {
                        IOUtil.close(genKeys);
                    }
                }
            } else {
                throw e;
            }
        }
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        try {
            genKeys.beforeFirst();
        } catch (SQLException e) {
            logger.warn("Fail to reset ResultSet cursor. can not get primary key value");
        } finally {
            if (isManualCloseResultSet) {
                IOUtil.close(genKeys);
            }
        }
        pkValuesMap.put(autoColumnName, pkValues);
        return pkValuesMap;
    }

    @Override
    public Map<String,List<Object>> getPkValuesByColumn() throws SQLException {
        Map<String,List<Object>> pkValuesMap  = parsePkValuesFromStatement();
        Set<String> keySet = new HashSet<>(pkValuesMap.keySet());
        //auto increment
        for (String pkKey:keySet) {
            List<Object> pkValues = pkValuesMap.get(pkKey);
            // pk auto generated while single insert primary key is expression
            if (pkValues.size() == 1 && (pkValues.get(0) instanceof SqlMethodExpr)) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
            // pk auto generated while column exists and value is null
            else if (!pkValues.isEmpty() && pkValues.get(0) instanceof Null) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
        }
        return pkValuesMap;
    }

    @Deprecated
    @SuppressWarnings("lgtm[java/database-resource-leak]")
    @Override
    public List<Object> getPkValuesByDefault() throws SQLException {
        // mysql default keyword the logic not support. (sample: insert into test(id, name) values(default, 'xx'))
        throw new NotSupportYetException();
    }

    @SuppressWarnings("lgtm[java/database-resource-leak]")
    @Override
    public List<Object> getPkValuesByDefault(String pkKey) throws SQLException {
        // mysql default keyword the logic not support. (sample: insert into test(id, name) values(default, 'xx'))
        throw new NotSupportYetException();
    }

    protected Map<String, List<Object>> autoGeneratePks(BigDecimal cursor, String autoColumnName, Integer updateCount) throws SQLException {
        BigDecimal step = BigDecimal.ONE;
        String resourceId = statementProxy.getConnectionProxy().getDataSourceProxy().getResourceId();
        if (RESOURCE_ID_STEP_CACHE.containsKey(resourceId)) {
            step = RESOURCE_ID_STEP_CACHE.get(resourceId);
        } else {
            ResultSet increment = null;
            try {
                increment = statementProxy.getTargetStatement().executeQuery("SHOW VARIABLES LIKE 'auto_increment_increment'");

                increment.next();
                step = new BigDecimal(increment.getString(2));
                RESOURCE_ID_STEP_CACHE.put(resourceId, step);
            } finally {
                IOUtil.close(increment);
            }
        }

        List<Object> pkValues = new ArrayList<>();
        for (int i = 0; i < updateCount; i++) {
            pkValues.add(cursor);
            cursor = cursor.add(step);
        }

        Map<String, List<Object>> pkValuesMap = new HashMap<>(1, 1.001f);
        pkValuesMap.put(autoColumnName,pkValues);
        return pkValuesMap;
    }

    protected boolean canAutoIncrement(Map<String, ColumnMeta> primaryKeyMap) {
        if (primaryKeyMap.size() != 1) {
            return false;
        }

        for (ColumnMeta pk : primaryKeyMap.values()) {
            return pk.isAutoincrement();
        }
        return false;
    }

}
