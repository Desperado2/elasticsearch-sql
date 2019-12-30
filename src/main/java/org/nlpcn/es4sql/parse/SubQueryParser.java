package org.nlpcn.es4sql.parse;

/**
 * @author mujingjing
 * @date 2019/12/27 16:27
 */

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import org.nlpcn.es4sql.domain.Condition ;
import org.nlpcn.es4sql.domain.Field;
import org.nlpcn.es4sql.domain.Order;
import org.nlpcn.es4sql.domain.Select;
import org.nlpcn.es4sql.domain.Where;
import org.nlpcn.es4sql.exception.SqlParseException;
import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Definition of SubQuery Parser
 */
public class SubQueryParser {
    private final SqlParser sqlParser;

    public SubQueryParser(SqlParser sqlParser) {
        this.sqlParser = sqlParser;
    }

    public boolean containSubqueryInFrom(MySqlSelectQueryBlock query) {
        return query.getFrom() instanceof SQLSubqueryTableSource;
    }

    public Select parseSubQueryInFrom(MySqlSelectQueryBlock query) throws SqlParseException {
        assert query.getFrom() instanceof SQLSubqueryTableSource;
        MySqlSelectQueryBlock query1 = (MySqlSelectQueryBlock) ((SQLSubqueryTableSource) query.getFrom()).getSelect()
                .getQuery();
        Select select = sqlParser.parseSelect(query1);
        String subQueryAlias = query.getFrom().getAlias();
        return pushSelect(query.getSelectList(), select, subQueryAlias);
    }

    private Select pushSelect(List<SQLSelectItem> selectItems, Select subquerySelect, String subQueryAlias) {
        Map<String, Function<String, String>> fieldAliasRewriter = prepareFieldAliasRewriter(
                selectItems,
                subQueryAlias);

        //1. rewrite field in select list
        Iterator<Field> fieldIterator = subquerySelect.getFields().iterator();
        while (fieldIterator.hasNext()) {
            Field field = fieldIterator.next();
            /*
             * return true if the subquerySelectItem in the final select list.
             * for example, subquerySelectItem is "SUM(emp.empno) as TEMP",
             * and final select list is TEMP. then return true.
             */
            if (fieldAliasRewriter.containsKey(field.getAlias())) {
                field.setAlias(fieldAliasRewriter.get(field.getAlias()).apply(field.getAlias()));
            } else {
                fieldIterator.remove();
            }
        }

        //2. rewrite field in order by
        for (Order orderBy : subquerySelect.getOrderBys()) {
            if (fieldAliasRewriter.containsKey(orderBy.getName())) {
                String replaceOrderName = fieldAliasRewriter.get(orderBy.getName()).apply(orderBy.getName());
                orderBy.setName(replaceOrderName);
                orderBy.getSortField().setName(replaceOrderName);
            }
        }

        // 3. rewrite field in having
        if (subquerySelect.getHaving() != null) {
            for (Where condition : subquerySelect.getHaving().getConditions()) {
                Condition cond = (Condition) condition;
                if (fieldAliasRewriter.containsKey(cond.getName())) {
                    String replaceOrderName = fieldAliasRewriter.get(cond.getName()).apply(cond.getName());
                    cond.setName(replaceOrderName);
                }
            }
        }
        return subquerySelect;
    }

    private Map<String, Function<String, String>> prepareFieldAliasRewriter(List<SQLSelectItem> selectItems,
                                                                            String owner) {
        HashMap<String, Function<String, String>> selectMap = new HashMap<>();
        for (SQLSelectItem item : selectItems) {
            if (Strings.isNullOrEmpty(item.getAlias())) {
                selectMap.put(getFieldName(item.getExpr(), owner), Function.identity());
            } else {
                selectMap.put(getFieldName(item.getExpr(), owner), s -> item.getAlias());
            }
        }
        return selectMap;
    }

    private String getFieldName(SQLExpr expr, String owner) {
        return expr.toString().replace(String.format("%s.", owner), "");
    }
}

