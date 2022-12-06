package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import java.util.Date;

import com.couchbase.mobiletestkit.javacommon.Args;


public class DataTypesInitiatorHandler {
    /* ---------------------------------- */
    /* - Initiates Complex Java Objects - */
    /* ---------------------------------- */


    public Date setDate(Args args) {
        return new Date();
    }

    public Double setDouble(Args args) {
        return Double.parseDouble(args.getString("value"));
    }

    public Float setFloat(Args args) {
        return Float.parseFloat(args.getString("value"));
    }

    public Long setLong(Args args) {
        return Long.parseLong(args.getString("value"));
    }

    public Boolean compare(Args args) {
        String first = args.getString("first");
        String second = args.getString("second");
        return first.equals(second);
    }

    public Boolean compareDate(Args args) {
        Date first = args.get("date1", Date.class);
        Date second = args.get("date2", Date.class);
        return first.equals(second);
    }

    public Boolean compareDouble(Args args) {
        Double first = Double.valueOf(args.getString("double1"));
        Double second = Double.valueOf(args.getString("double2"));
        return first.equals(second);
    }

    public Boolean compareLong(Args args) {
        Long first = args.getLong("long1");
        Long second = args.getLong("long2");
        return first.equals(second);
    }

}

