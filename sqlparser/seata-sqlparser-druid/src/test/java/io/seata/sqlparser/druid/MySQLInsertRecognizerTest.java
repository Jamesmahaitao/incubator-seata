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
package io.seata.sqlparser.druid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlOrderingExpr;

import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.mysql.MySQLInsertRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type My sql insert recognizer test.
 *
 */
public class MySQLInsertRecognizerTest extends AbstractRecognizerTest {

    private final int pkIndex = 0;

    /**
     * Insert recognizer test 0.
     */
    @Test
    public void insertRecognizerTest_0() {

        String sql = "INSERT INTO t1 (name) VALUES ('name1')";

        SQLStatement statement = getSQLStatement(sql);

        MySQLInsertRecognizer mySQLInsertRecognizer = new MySQLInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLInsertRecognizer.getTableName());
        Assertions.assertEquals(Collections.singletonList("name"), mySQLInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, mySQLInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Collections.singletonList("name1"), mySQLInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
    }

    /**
     * Insert recognizer test 1.
     */
    @Test
    public void insertRecognizerTest_1() {

        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 'name2')";

        SQLStatement statement = getSQLStatement(sql);

        MySQLInsertRecognizer mySQLInsertRecognizer = new MySQLInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLInsertRecognizer.getTableName());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), mySQLInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, mySQLInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), mySQLInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
    }

    /**
     * Insert recognizer test 3.
     */
    @Test
    public void insertRecognizerTest_3() {

        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 'name2'), ('name3', 'name4'), ('name5', 'name6')";

        SQLStatement statement = getSQLStatement(sql);

        MySQLInsertRecognizer mySQLInsertRecognizer = new MySQLInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLInsertRecognizer.getTableName());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), mySQLInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(3, mySQLInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), mySQLInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
        Assertions.assertEquals(Arrays.asList("name3", "name4"), mySQLInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(1));
        Assertions.assertEquals(Arrays.asList("name5", "name6"), mySQLInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(2));
    }

    @Test
    public void testGetSqlType() {
        String sql = "insert into t(id) values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLInsertRecognizer recognizer = new MySQLInsertRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.INSERT);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "insert into t(id) values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLInsertRecognizer recognizer = new MySQLInsertRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetInsertColumns() {

        //test for no column
        String sql = "insert into t values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLInsertRecognizer recognizer = new MySQLInsertRecognizer(sql, asts.get(0));
        List<String> insertColumns = recognizer.getInsertColumns();
        Assertions.assertNull(insertColumns);

        //test for normal
        sql = "insert into t(a) values (?)";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        recognizer = new MySQLInsertRecognizer(sql, asts.get(0));
        insertColumns = recognizer.getInsertColumns();
        Assertions.assertEquals(1, insertColumns.size());

        //test for exception
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "insert into t(a) values (?)";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MYSQL);
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement)sqlStatements.get(0);
            sqlInsertStatement.getColumns().add(new MySqlOrderingExpr());

            MySQLInsertRecognizer oracleInsertRecognizer = new MySQLInsertRecognizer(s, sqlInsertStatement);
            oracleInsertRecognizer.getInsertColumns();
        });
    }

    @Test
    public void testGetInsertRows() {
        //test for null value
        String sql = "insert into t(id, no, name, age, time) values (1, null, 'a', ?, now())";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLInsertRecognizer recognizer = new MySQLInsertRecognizer(sql, asts.get(0));
        List<List<Object>> insertRows = recognizer.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(1, insertRows.size());

        //test for exception
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "insert into t(a) values (?)";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MYSQL);
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement)sqlStatements.get(0);
            sqlInsertStatement.getValuesList().get(0).getValues().set(pkIndex, new MySqlOrderingExpr());

            MySQLInsertRecognizer mysqlInsertRecognizer = new MySQLInsertRecognizer(s, sqlInsertStatement);
            mysqlInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex));
        });
    }

    @Override
    public String getDbType() {
        return JdbcConstants.MYSQL;
    }

    @Test
    public void testGetInsertColumns_2() {
        String sql = "insert into t(`id`, `no`, `name`, `age`) values (1, 'no001', 'aaa', '20')";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLInsertRecognizer recognizer = new MySQLInsertRecognizer(sql, asts.get(0));
        List<String> insertColumns = recognizer.getInsertColumns();
        for (String insertColumn : insertColumns) {
            Assertions.assertTrue(insertColumn.contains("`"));
        }
    }
}
