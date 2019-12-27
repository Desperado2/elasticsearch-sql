package org.nlpcn.es4sql.domain;

import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.nlpcn.es4sql.parse.SubQueryExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 将sql语句转换为select 对象
 * 
 * @author ansj
 */
public class Select extends Query {

    public static int DEFAULT_ROWCOUNT = 1000;

	// Using this functions, will cause query to execute as aggregation.
	private final List<String> aggsFunctions = Arrays.asList("SUM", "MAX", "MIN", "AVG", "TOPHITS", "COUNT", "STATS","EXTENDED_STATS","PERCENTILES","SCRIPTED_METRIC");
	private List<Field> fields = new ArrayList<>();
	private List<List<Field>> groupBys = new ArrayList<>();
	private List<Order> orderBys = new ArrayList<>();
    private boolean containsSubQueries;
    private List<SubQueryExpression> subQueries;
	public boolean isQuery = false;
    private boolean selectAll = false;

	public boolean isAgg = false;
    private Having having;

    public Select() {
        setRowCount(DEFAULT_ROWCOUNT);
    }

	public List<Field> getFields() {
		return fields;
	}

	public void addGroupBy(Field field) {
		List<Field> wrapper = new ArrayList<>();
		wrapper.add(field);
		addGroupBy(wrapper);
	}

	public void addGroupBy(List<Field> fields) {
		isAgg = true;
		this.groupBys.add(fields);
	}

	public List<List<Field>> getGroupBys() {
		return groupBys;
	}

	public List<Order> getOrderBys() {
		return orderBys;
	}

	public void addOrderBy(String nestedPath, String name, String type, ScriptSortBuilder.ScriptSortType scriptSortType,Field sortField) {
		if ("_score".equals(name)) { //zhongshu-comment 可以直接在order by子句中写_score，根据该字段排序 select * from tbl order by _score asc
			isQuery = true;
		}
		Order order = new Order(nestedPath, name, type,sortField);

		order.setScriptSortType(scriptSortType);
		this.orderBys.add(order);
	}


	public void addField(Field field) {
		if (field == null ) {
			return;
		}
        if(field.getName().equals("*")){
            this.selectAll = true;
        }

		if(field instanceof  MethodField && aggsFunctions.contains(field.getName().toUpperCase())) {
			isAgg = true;
		}

		fields.add(field);
	}

    public void fillSubQueries() {
        subQueries = new ArrayList<>();
        Where where = this.getWhere();
        fillSubQueriesFromWhereRecursive(where);
    }

    private void fillSubQueriesFromWhereRecursive(Where where) {
        if(where == null) return;
        if(where instanceof Condition){
            Condition condition = (Condition) where;
            if ( condition.getValue() instanceof SubQueryExpression){
                this.subQueries.add((SubQueryExpression) condition.getValue());
                this.containsSubQueries = true;
            }
            if(condition.getValue() instanceof Object[]){

                for(Object o : (Object[]) condition.getValue()){
                    if ( o instanceof SubQueryExpression){
                        this.subQueries.add((SubQueryExpression) o);
                        this.containsSubQueries = true;
                    }
                }
            }
        }
        else {
            for(Where innerWhere : where.getWheres())
                fillSubQueriesFromWhereRecursive(innerWhere);
        }
    }

    public boolean containsSubQueries() {
        return containsSubQueries;
    }

    public List<SubQueryExpression> getSubQueries() {
        return subQueries;
    }

    public boolean isOrderdSelect(){
        return this.getOrderBys()!=null && this.getOrderBys().size() >0 ;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public Having getHaving() {
        return having;
    }

    public void setHaving(Having having) {
        this.having = having;
    }
}

