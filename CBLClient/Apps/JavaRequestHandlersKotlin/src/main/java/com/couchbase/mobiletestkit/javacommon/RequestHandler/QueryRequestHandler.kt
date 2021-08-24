package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.*
import com.couchbase.lite.Function
import com.couchbase.mobiletestkit.javacommon.Args
import com.couchbase.mobiletestkit.javacommon.util.ConcurrentExecutor
import com.couchbase.mobiletestkit.javacommon.util.Log
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class QueryRequestHandler {
    fun select(args: Args): Query {
        val select_result = args.get<SelectResult>("select_result")
        return QueryBuilder.select(select_result)
    }

    fun distinct(args: Args): Query {
        val select_result = args.get<SelectResult>("select_prop")
        val from_prop = args.get<DataSource>("from_prop")
        val whr_key_prop = Expression.value(args.get("whr_key_prop"))
        return QueryBuilder.select(select_result).from(from_prop).where(whr_key_prop)
    }

    fun create(args: Args): Query {
        val select_result = args.get<SelectResult>("select_result")
        return QueryBuilder.select(select_result)
    }

    @Throws(CouchbaseLiteException::class)
    fun run(args: Args): ResultSet {
        val query = args.get<Query>("query")
        return query.execute()
    }

    fun nextResult(args: Args): Result? {
        val query_result_set = args.get<ResultSet>("query_result_set")
        return query_result_set.next()
    }

    @Throws(CouchbaseLiteException::class)
    fun getDoc(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val out = database.count
        val doc_id = Expression.value(args.get("doc_id"))
        val query: Query = QueryBuilder
            .select(SelectResult.all())
            .from(DataSource.database(database))
            .where(Meta.id.equalTo(doc_id))
        val resultArray: MutableList<Any> = ArrayList()
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun anyOperator(args: Args): List<Any?> {
        /*select meta().id from `travel-sample` where type="route"
        AND ANY departure IN schedule SATISFIES departure.utc > "03:41:00" END;

        "$1": 24024

        */
        val database = args.get<Database>("database")
        val whr_prop = args.get<String>("whr_prop")
        val whr_val = args.get<String>("whr_val")
        val schedule = args.get<String>("schedule")
        val departure = args.get<String>("departure")
        val departure_prop = args.get<String>("departure_prop")
        val departure_val = args.get<String>("departure_val")
        val dep_schedule = ArrayExpression.variable(departure)
        val departure_utc = ArrayExpression.variable(departure_prop)
        val search_query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(database))
            .where(
                Expression.property(whr_prop).equalTo(Expression.value(whr_val))
                    .and(
                        ArrayExpression.any(dep_schedule).`in`(Expression.property(schedule))
                            .satisfies(departure_utc.greaterThan(Expression.value(departure_val)))
                    )
            )
        val rows = search_query.execute()
        val resultArray: MutableList<Any?> = ArrayList()
        for (row in rows) {
            resultArray.add(row.getString("id"))
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun docsLimitOffset(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val limit = Expression.value(args.get("limit"))
        val offset = Expression.value(args.get("offset"))
        val search_query: Query = QueryBuilder
            .select(SelectResult.all())
            .from(DataSource.database(database))
            .limit(limit, offset)
        val resultArray: MutableList<Any> = ArrayList()
        val rows = search_query.execute()
        for (row in rows) {
            resultArray.add(row)
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun multipleSelects(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val select_property1 = args.get<String>("select_property1")
        val select_property2 = args.get<String>("select_property2")
        val whr_key = args.get<String>("whr_key")
        val whr_val = Expression.value(args.get("whr_val"))
        val search_query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1)),
                SelectResult.expression(Expression.property(select_property2))
            )
            .from(DataSource.database(database))
            .where(Expression.property(whr_key).equalTo(whr_val))
        val resultArray: MutableList<Any> = ArrayList()
        val rows = search_query.execute()
        for (row in rows) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun multipleSelectsDoubleValue(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val select_property1 = args.get<String>("select_property1")
        val select_property2 = args.get<String>("select_property2")
        val whr_key = args.get<String>("whr_key")
        val valDouble = args.get<Double>("whr_val")
        val whr_val = valDouble.toFloat()
        val exp_val = Expression.floatValue(whr_val)
        val search_query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1)),
                SelectResult.expression(Expression.property(select_property2))
            )
            .from(DataSource.database(database))
            .where(Expression.property(whr_key).equalTo(exp_val))
        val resultArray: MutableList<Any> = ArrayList()
        val rows = search_query.execute()
        for (row in rows) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun multipleSelectsOrderByLocaleValue(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val select_property1 = args.get<String>("select_property1")
        val select_property2 = args.get<String>("select_property2")
        val whr_key = args.get<String>("whr_key")
        val locale = args.get<String>("locale")
        val with_locale: Collation = Collation.unicode().setLocale(locale)
        val search_query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1)),
                SelectResult.expression(Expression.property(select_property2))
            )
            .from(DataSource.database(database))
            .orderBy(Ordering.expression(Expression.property(whr_key).collate(with_locale)))
        val resultArray: MutableList<Any> = ArrayList()
        val rows = search_query.execute()
        for (row in rows) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun whereAndOr(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val whr_key1 = args.get<String>("whr_key1")
        val whr_key2 = args.get<String>("whr_key2")
        val whr_key3 = args.get<String>("whr_key3")
        val whr_key4 = args.get<String>("whr_key4")
        val whr_val1 = Expression.value(args.get("whr_val1"))
        val whr_val2 = Expression.value(args.get("whr_val2"))
        val whr_val3 = Expression.value(args.get("whr_val3"))
        val whr_val4 = Expression.value(args.get("whr_val4"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(database))
            .where(
                Expression.property(whr_key1).equalTo(whr_val1)
                    .and(
                        Expression.property(whr_key2).equalTo(whr_val2)
                            .or(Expression.property(whr_key3).equalTo(whr_val3))
                    )
                    .and(Expression.property(whr_key4).equalTo(whr_val4))
            )
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun like(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val out = database.count
        val whr_key = args.get<String>("whr_key")
        val select_property1 = args.get<String>("select_property1")
        val select_property2 = args.get<String>("select_property2")
        val like_key = args.get<String>("like_key")
        val whr_val = Expression.value(args.get("whr_val"))
        val like_val = Expression.value(args.get("like_val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1)),
                SelectResult.expression(Expression.property(select_property2))
            )
            .from(DataSource.database(database))
            .where(
                Expression.property(whr_key).equalTo(whr_val)
                    .and(Expression.property(like_key).like(like_val))
            )
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun regex(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val whr_key = args.get<String>("whr_key")
        val select_property1 = args.get<String>("select_property1")
        val select_property2 = args.get<String>("select_property2")
        val regex_key = args.get<String>("regex_key")
        val whr_val = Expression.value(args.get("whr_val"))
        val regex_val = Expression.value(args.get("regex_val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1)),
                SelectResult.expression(Expression.property(select_property2))
            )
            .from(DataSource.database(database))
            .where(
                Expression.property(whr_key).equalTo(whr_val)
                    .and(Expression.property(regex_key).regex(regex_val))
            )
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun ordering(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val whr_key = args.get<String>("whr_key")
        val select_property1 = args.get<String>("select_property1")
        val whr_val = Expression.value(args.get("whr_val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1))
            )
            .from(DataSource.database(database))
            .where(Expression.property(whr_key).equalTo(whr_val))
            .orderBy(Ordering.property(select_property1).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun substring(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val select_property1 = args.get<String>("select_property1")
        val select_property2 = args.get<String>("select_property2")
        val substring = Expression.value(args.get("substring"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1)),
                SelectResult.expression(Function.upper(Expression.property(select_property2)))
            )
            .from(DataSource.database(database))
            .where(Function.contains(Expression.property(select_property1), substring))
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun isNullOrMissing(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val select_property1 = args.get<String>("select_property1")
        val limit = Expression.value(args.get("limit"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1))
            )
            .from(DataSource.database(database))
            .where(Expression.property(select_property1).isNullOrMissing)
            .orderBy(Ordering.expression(Meta.id).ascending())
            .limit(limit)
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun collation(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val select_property1 = args.get<String>("select_property1")
        val whr_key1 = args.get<String>("whr_key1")
        val whr_key2 = args.get<String>("whr_key2")
        val whr_val1 = Expression.value(args.get("whr_val1"))
        val whr_val2 = Expression.value(args.get("whr_val2"))
        val equal_to = Expression.value(args.get("equal_to"))
        val resultArray: MutableList<Any> = ArrayList()
        val collation: Collation = Collation.unicode()
            .setIgnoreAccents(true)
            .setIgnoreCase(true)
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(select_property1))
            )
            .from(DataSource.database(database))
            .where(
                Expression.property(whr_key1).equalTo(whr_val1)
                    .and(
                        Expression.property(whr_key2).equalTo(whr_val2)
                            .and(
                                Expression.property(select_property1).collate(collation)
                                    .equalTo(equal_to)
                            )
                    )
            )
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun join(args: Args): List<Any> {
        val db = args.get<Database>("database")
        val prop1 = args.get<String>("select_property1")
        val prop2 = args.get<String>("select_property2")
        val prop3 = args.get<String>("select_property3")
        val prop4 = args.get<String>("select_property4")
        val prop5 = args.get<String>("select_property5")
        val joinKey = args.get<String>("join_key")
        val whrKey1 = args.get<String>("whr_key1")
        val whrKey2 = args.get<String>("whr_key2")
        val whrKey3 = args.get<String>("whr_key3")
        val limit = Expression.value(args.get("limit"))
        val whrVal1 = Expression.value(args.get("whr_val1"))
        val whrVal2 = Expression.value(args.get("whr_val2"))
        val whrVal3 = Expression.value(args.get("whr_val3"))
        val main = "route"
        val secondary = "airline"
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .selectDistinct(
                SelectResult.expression(Expression.property(prop1).from(secondary)),
                SelectResult.expression(Expression.property(prop2).from(secondary)),
                SelectResult.expression(Expression.property(prop3).from(main)),
                SelectResult.expression(Expression.property(prop4).from(main)),
                SelectResult.expression(Expression.property(prop5).from(main))
            )
            .from(DataSource.database(db).`as`(main))
            .join(
                Join.join(DataSource.database(db).`as`(secondary))
                    .on(Meta.id.from(secondary).equalTo(Expression.property(joinKey).from(main)))
            )
            .where(
                Expression.property(whrKey1).from(main).equalTo(whrVal1)
                    .and(Expression.property(whrKey2).from(secondary).equalTo(whrVal2))
                    .and(Expression.property(whrKey3).from(main).equalTo(whrVal3))
            )
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun leftJoin(args: Args): List<Any> {
        val db = args.get<Database>("database")
        val prop = args.get<String>("select_property")
        val limit = args.get<Int>("limit")
        val main = "airline"
        val secondary = "route"
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.all().from(main),
                SelectResult.all().from(secondary)
            )
            .from(DataSource.database(db).`as`(main))
            .join(
                Join.leftJoin(DataSource.database(db).`as`(secondary))
                    .on(Meta.id.from(main).equalTo(Expression.property(prop).from(secondary)))
            ) //.orderBy(Ordering.expression(Expression.property(prop).from(secondary)).ascending())
            .limit(Expression.intValue(limit))
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun leftOuterJoin(args: Args): List<Any> {
        val db = args.get<Database>("database")
        val prop = args.get<String>("select_property")
        val limit = args.get<Int>("limit")
        val main = "airline"
        val secondary = "route"
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.all().from(main),
                SelectResult.all().from(secondary)
            )
            .from(DataSource.database(db).`as`(main))
            .join(
                Join.leftOuterJoin(DataSource.database(db).`as`(secondary))
                    .on(Meta.id.from(main).equalTo(Expression.property(prop).from(secondary)))
            ) //.orderBy(Ordering.expression(Expression.property(prop).from(secondary)).ascending())
            .limit(Expression.intValue(limit))
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun innerJoin(args: Args): List<Any> {
        /*
        SELECT
          employeeDS.firstname,
          employeeDS.lastname,
          departmentDS.name
        FROM
          `travel-sample` employeeDS
          INNER JOIN `travel-sample` departmentDS ON employeeDS.department = departmentDS.code
        WHERE
          employeeDS.type = "employee"
          AND departmentDS.type = "department"
         */
        val db = args.get<Database>("database")
        val prop1 = args.get<String>("select_property1")
        val prop2 = args.get<String>("select_property2")
        val prop3 = args.get<String>("select_property3")
        val joinKey1 = args.get<String>("join_key1")
        val joinKey2 = args.get<String>("join_key2")
        val whrKey1 = args.get<String>("whr_key1")
        val whrKey2 = args.get<String>("whr_key2")
        val whrVal1 = args.get<String>("whr_val1")
        val whrVal2 = args.get<Int>("whr_val2")
        val limit = args.get<Int>("limit")
        val main = "route"
        val secondary = "airline"
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Expression.property(prop1).from(main)),
                SelectResult.expression(Expression.property(prop2).from(main)),
                SelectResult.expression(Expression.property(prop3).from(secondary))
            )
            .from(DataSource.database(db).`as`(main))
            .join(
                Join.innerJoin(DataSource.database(db).`as`(secondary))
                    .on(
                        Expression.property(joinKey1).from(secondary)
                            .equalTo(Expression.property(joinKey2).from(main))
                            .and(
                                Expression.property(whrKey1).from(secondary)
                                    .equalTo(Expression.string(whrVal1))
                            )
                            .and(
                                Expression.property(whrKey2).from(main)
                                    .equalTo(Expression.intValue(whrVal2))
                            )
                    )
            ) //.orderBy(Ordering.expression(Expression.property(prop1).from(main)).ascending())
            .limit(Expression.intValue(limit))
        val queryResults = query.execute()
        for (row in queryResults) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun crossJoin(args: Args): List<Any> {
        /*
        SELECT
          departmentDS.name AS DeptName,
          locationDS.name AS LocationName,
          locationDS.address
        FROM
          `travel-sample` departmentDS
          CROSS JOIN `travel-sample` locationDS
        WHERE
          departmentDS.type = "department"
         */
        val db = args.get<Database>("database")
        val prop1 = args.get<String>("select_property1")
        val prop2 = args.get<String>("select_property2")
        val whrKey1 = args.get<String>("whr_key1")
        val whrKey2 = args.get<String>("whr_key2")
        val whrVal1 = args.get<String>("whr_val1")
        val whrVal2 = args.get<String>("whr_val2")
        val limit = args.get<Int>("limit")
        val main = "airport"
        val secondary = "airline"
        val firstName = "firstName"
        val secondName = "secondName"
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Expression.property(prop1).from(main)).`as`(firstName),
                SelectResult.expression(Expression.property(prop1).from(secondary))
                    .`as`(secondName),
                SelectResult.expression(Expression.property(prop2).from(secondary))
            )
            .from(DataSource.database(db).`as`(main))
            .join(Join.crossJoin(DataSource.database(db).`as`(secondary)))
            .where(
                Expression.property(whrKey1).from(main).equalTo(Expression.string(whrVal1))
                    .and(
                        Expression.property(whrKey2).from(secondary)
                            .equalTo(Expression.string(whrVal2))
                    )
            ) //.orderBy(Ordering.expression(Expression.property(prop1).from(main)).ascending())
            .limit(Expression.intValue(limit))
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun equalTo(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where id = 24
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val `val` = Expression.value(args.get("val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).equalTo(`val`))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun notEqualTo(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where id != 24
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val `val` = Expression.value(args.get("val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).notEqualTo(`val`))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun greaterThan(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where id > 1000
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val `val` = Expression.value(args.get("val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).greaterThan(`val`))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun greaterThanOrEqualTo(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where id >= 31000 limit 5
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val `val` = Expression.value(args.get("val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).greaterThanOrEqualTo(`val`))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun lessThan(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where id < 100 limit 5
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val `val` = Expression.value(args.get("val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).lessThan(`val`))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun lessThanOrEqualTo(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where id <= 100 limit 5
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val `val` = Expression.value(args.get("val"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).lessThanOrEqualTo(`val`))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun between(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where id between 100 and 200
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val val1 = Expression.value(args.get("val1"))
        val val2 = Expression.value(args.get("val2"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).between(val1, val2))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun `in`(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where country in ["france", "United States"]
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val val1 = args.get<String>("val1")
        val val2 = args.get<String>("val2")
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).`in`(Expression.value(val1), Expression.value(val2)))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun `is`(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where callsign is null
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.property(prop).`is`(Expression.value(null)))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun not(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where id not  between 100 and 200 limit 5
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val val1 = Expression.value(args.get("val1"))
        val val2 = Expression.value(args.get("val2"))
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(db))
            .where(Expression.not(Expression.property(prop).between(val1, val2)))
            .orderBy(Ordering.expression(Expression.property(prop)).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun isNot(args: Args): List<Any> {
        //SELECT * FROM `travel-sample` where callsign is not null limit 5
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(prop))
            )
            .from(DataSource.database(db))
            .where(Expression.property(prop).isNot(Expression.value(null)))
            .orderBy(Ordering.expression(Meta.id).ascending())
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun singlePropertyFTS(args: Args): List<Any> {
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val `val` = args.get<String>("val")
        val stemming = args.get<Boolean>("stemming")
        val docType = Expression.value(args.get("doc_type"))
        val limit = Expression.value(args.get("limit"))
        val index = "singlePropertyIndex"
        val ftsIndex: FullTextIndex
        ftsIndex = if (stemming) {
            IndexBuilder.fullTextIndex(FullTextIndexItem.property(prop))
        } else {
            IndexBuilder.fullTextIndex(FullTextIndexItem.property(prop)).setLanguage("en")
        }
        db.createIndex(index, ftsIndex)
        val ftsExpression = FullTextExpression.index(index)
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(prop))
            )
            .from(DataSource.database(db))
            .where(Expression.property("type").equalTo(docType).and(ftsExpression.match(`val`)))
            .limit(limit)
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun multiplePropertyFTS(args: Args): List<Any> {
        val db = args.get<Database>("database")
        val prop1 = args.get<String>("prop1")
        val prop2 = args.get<String>("prop2")
        val `val` = args.get<String>("val")
        val stemming = args.get<Boolean>("stemming")
        val docType = Expression.value(args.get("doc_type"))
        val limit = Expression.value(args.get("limit"))
        val index = "multiplePropertyIndex"
        val ftsIndex: FullTextIndex
        ftsIndex = if (stemming) {
            IndexBuilder.fullTextIndex(
                FullTextIndexItem.property(prop1),
                FullTextIndexItem.property(prop2)
            )
        } else {
            IndexBuilder.fullTextIndex(
                FullTextIndexItem.property(prop1),
                FullTextIndexItem.property(prop2)
            )
                .setLanguage("en")
        }
        db.createIndex(index, ftsIndex)
        val ftsExpression = FullTextExpression.index(index)
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(prop1)),
                SelectResult.expression(Expression.property(prop2))
            )
            .from(DataSource.database(db))
            .where(Expression.property("type").equalTo(docType).and(ftsExpression.match(`val`)))
            .limit(limit)
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun ftsWithRanking(args: Args): List<Any> {
        val db = args.get<Database>("database")
        val prop = args.get<String>("prop")
        val `val` = args.get<String>("val")
        val docType = Expression.value(args.get("doc_type"))
        val limit = Expression.value(args.get("limit"))
        val index = "singlePropertyIndex"
        val ftsIndex = IndexBuilder.fullTextIndex(FullTextIndexItem.property(prop))
        db.createIndex(index, ftsIndex)
        val ftsExpression = FullTextExpression.index(index)
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.expression(Expression.property(prop))
            )
            .from(DataSource.database(db))
            .where(Expression.property("type").equalTo(docType).and(ftsExpression.match(`val`)))
            .orderBy(Ordering.expression(FullTextFunction.rank(index)).descending())
            .limit(limit)
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun arthimetic(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val resultArray: MutableList<Any> = ArrayList()
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(database))
            .where(
                Expression.property("number1").modulo(Expression.intValue(2))
                    .equalTo(Expression.intValue(0))
            )
        for (row in query.execute()) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    fun addChangeListener(args: Args): QueryChangeListener {
        val query = args.get<Query>("query")
        val changeListener = MyQueryListener()
        val token = query.addChangeListener(ConcurrentExecutor.EXECUTOR, changeListener)
        changeListener.token = token
        return changeListener
    }

    fun removeChangeListener(args: Args) {
        val query = args.get<Query>("query")
        val changeListener: MyQueryListener = args["changeListener"]
        query.removeChangeListener(changeListener.token!!)
    }

    fun selectAll(args: Args): Query {
        val database = args.get<Database>("database")
        return QueryBuilder
            .select(SelectResult.all())
            .from(DataSource.database(database))
    }

    @Throws(CouchbaseLiteException::class, InterruptedException::class)
    fun getLiveQueryResponseTime(args: Args): Long {
        /**
         * This function contains logic to pull live query response time on query changes
         * validating CBL-172 which reported there are 200 millionseconds delay
         */
        var returnValue: Long = 0
        val TAG = "LIVEQUERY"
        val KEY = "sequence_number"
        val db = args.get<Database>("database")
        val liveQueryActivities: MutableList<Long> = ArrayList()

        // define a query with Parameters object
        val query: Query = QueryBuilder
            .select(SelectResult.all())
            .from(DataSource.database(db))
            .where(Expression.property(KEY).lessThanOrEqualTo(Expression.parameter("VALUE")))
        var params = Parameters()
        params.setInt("VALUE", 50)
        query.parameters = params

        // register a query change listener to capture live resultset changes
        val exec: Executor = Executors.newSingleThreadExecutor()
        val token = query.addChangeListener(exec, { change: QueryChange ->
            val curMillis = System.currentTimeMillis()
            liveQueryActivities.add(curMillis)
            var count = 0
            for (result in change.results!!) {
                Log.d(
                    TAG,
                    "results: " + result.keys
                )
                count++
            }
            Log.d(
                TAG,
                "results count: $count"
            )
            Log.d(
                TAG,
                "live query captured timestamp in milliseconds: $curMillis"
            )
        })

        // record timestamp before submitting the change
        val queryChangeTimestamp = System.currentTimeMillis()

        // make the query change,
        // the listener should be able to capture the changes,
        // record timestamp of the change event
        params = Parameters()
        params.setInt("VALUE", 75)
        query.parameters = params
        TimeUnit.MILLISECONDS.sleep(500)
        query.removeChangeListener(token)
        if (liveQueryActivities.isEmpty()) {
            Log.d(TAG, "liveQueryActivities is empty")
        } else {
            Log.d(TAG, "liveQueryActivities.size is " + liveQueryActivities.size.toString())
            val liveQueryCapturedTimestamp = liveQueryActivities[liveQueryActivities.size - 1]
            returnValue = liveQueryCapturedTimestamp - queryChangeTimestamp
            Log.d(
                TAG,
                "query change timestamp: $queryChangeTimestamp"
            )
            Log.d(
                TAG,
                "live query captured timestamp: $liveQueryCapturedTimestamp"
            )
        }
        return returnValue
    }
}

internal class MyQueryListener : QueryChangeListener {
    private val changes: MutableList<QueryChange> = ArrayList()
    var token: ListenerToken? = null

    fun getChanges(): List<QueryChange> {
        return changes
    }

    override fun changed(change: QueryChange) {
        changes.add(change)
    }
}