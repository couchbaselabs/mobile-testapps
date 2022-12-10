package com.couchbase.lite.mobiletestkit.legacy;


import com.couchbase.lite.ArrayExpression;
import com.couchbase.lite.ArrayExpressionIn;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MetaExpression;
import com.couchbase.lite.PropertyExpression;
import com.couchbase.lite.VariableExpression;
import com.couchbase.lite.mobiletestkit.Args;


public class ExpressionRequestHandler {

    public PropertyExpression property(Args args) {
        String property = args.getString("property");
        return Expression.property(property);
    }

    public MetaExpression metaId() {
        return Meta.id;
    }

    public MetaExpression metaSequence() {
        return Meta.sequence;
    }

    public Expression parameter(Args args) {
        String parameter = args.getString("parameter");
        return Expression.parameter(parameter);
    }

    public Expression negated(Args args) {
        Expression expression = getExpression(args, "expression");
        return Expression.negated(expression);
    }


    public Expression not(Args args) {
        Expression expression = getExpression(args, "expression");
        return Expression.not(expression);
    }

    public VariableExpression variable(Args args) {
        String name = args.getString("name");
        return ArrayExpression.variable(name);
    }


    public ArrayExpressionIn any(Args args) {
        VariableExpression variable = args.get("variable", VariableExpression.class);
        return ArrayExpression.any(variable);
    }


    public ArrayExpressionIn anyAndEvery(Args args) {
        VariableExpression variable = args.get("variable", VariableExpression.class);
        return ArrayExpression.anyAndEvery(variable);
    }


    public ArrayExpressionIn every(Args args) {
        VariableExpression variable = args.get("variable", VariableExpression.class);
        return ArrayExpression.every(variable);
    }


    public Expression createEqualTo(Args args) {
        Expression expression1 = getExpression(args, "expression1");
        Expression expression2 = getExpression(args, "expression2");
        return expression1.equalTo(expression2);
    }


    public Expression createAnd(Args args) {
        Expression expression1 = getExpression(args, "expression1");
        Expression expression2 = getExpression(args, "expression2");
        return expression1.and(expression2);
    }

    public Expression createOr(Args args) {
        Expression expression1 = getExpression(args, "expression1");
        Expression expression2 = getExpression(args, "expression2");
        return expression1.or(expression2);
    }

    private Expression getExpression(Args args, String name) {
        return args.get(name, Expression.class);
    }
}
