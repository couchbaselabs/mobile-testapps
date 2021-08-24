package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.mobiletestkit.javacommon.Args
import java.util.*

class DataTypesInitiatorHandler {
    /* ---------------------------------- */ /* - Initiates Complex Java Objects - */ /* ---------------------------------- */
    fun setDate(args: Args?): Date {
        return Date()
    }

    fun setDouble(args: Args): Double {
        return args.get<Any>("value").toString().toDouble()
    }

    fun setFloat(args: Args): Float {
        return args.get<Any>("value").toString().toFloat()
    }

    fun setLong(args: Args): Long {
        return args.get<Any>("value").toString().toLong()
    }

    fun compare(args: Args): Boolean {
        val first = args.get<Any>("first").toString()
        val second = args.get<Any>("second").toString()
        return first == second
    }

    fun compareDate(args: Args): Boolean {
        val first = args.get<Date>("date1")
        val second = args.get<Date>("date2")
        return first == second
    }

    fun compareDouble(args: Args): Boolean {
        val first = java.lang.Double.valueOf(args.get<Any>("double1").toString())
        val second = java.lang.Double.valueOf(args.get<Any>("double2").toString())
        return first == second
    }

    fun compareLong(args: Args): Boolean {
        val first = args.get<Long>("long1")
        val second = args.get<Long>("long2")
        return first == second
    }
}