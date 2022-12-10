package com.couchbase.lite.mobiletestkit.legacy;


import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Function;


public class FunctionRequestHandler {

    public Expression avg(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.avg(expression);
    }

    public Expression count(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.count(expression);
    }

    public Expression min(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.min(expression);
    }

    public Expression max(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.max(expression);
    }

    public Expression sum(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.sum(expression);
    }

    public Expression abs(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.abs(expression);
    }

    public Expression acos(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.acos(expression);
    }

    public Expression asin(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.asin(expression);
    }

    public Expression atan(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.atan(expression);
    }

    public Expression atan2(Args args) {
        Expression x = args.get("x", Expression.class);;
        Expression y = args.get("y", Expression.class);;
        return Function.atan2(x, y);
    }

    public Expression ceil(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.ceil(expression);
    }

    public Expression cos(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.cos(expression);
    }

    public Expression degrees(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.degrees(expression);
    }

    public Expression e() {
        return Function.e();
    }

    public Expression exp(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.exp(expression);
    }

    public Expression floor(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.floor(expression);
    }

    public Expression ln(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.ln(expression);
    }

    public Expression log(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.log(expression);
    }

    public Expression pi(Args args) {
        return Function.pi();
    }

    public Expression power(Args args) {
        Expression base = args.get("base", Expression.class);
        Expression exponent = args.get("exponent", Expression.class);
        return Function.power(base, exponent);
    }

    public Expression radians(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.radians(expression);
    }

    public Expression round(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.round(expression);
    }

    public Expression roundDigits(Args args) {
        Expression expression = args.get("expression", Expression.class);
        Expression digits = args.get("digits", Expression.class);
        return Function.round(expression, digits);
    }

    public Expression sign(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.sign(expression);
    }

    public Expression sin(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.sin(expression);
    }

    public Expression sqrt(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.sqrt(expression);
    }

    public Expression tan(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.tan(expression);
    }

    public Expression trunc(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.trunc(expression);
    }

    public Expression truncDigits(Args args) {
        Expression expression = args.get("expression", Expression.class);
        Expression digits = args.get("digits", Expression.class);
        return Function.trunc(expression, digits);
    }

    public Expression contains(Args args) {
        Expression expression = args.get("expression", Expression.class);
        Expression substring = args.get("substring", Expression.class);
        return Function.contains(expression, substring);
    }

    public Expression length(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.length(expression);
    }

    public Expression lower(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.lower(expression);
    }

    public Expression ltrim(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.ltrim(expression);
    }

    public Expression rtrim(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.rtrim(expression);
    }

    public Expression trim(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.trim(expression);
    }

    public Expression upper(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return Function.upper(expression);
    }

}
