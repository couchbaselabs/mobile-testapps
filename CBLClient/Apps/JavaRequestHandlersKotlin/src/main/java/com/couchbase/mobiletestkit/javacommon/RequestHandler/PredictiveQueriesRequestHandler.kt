package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.*
import com.couchbase.lite.Dictionary
import com.couchbase.lite.Function
import com.couchbase.mobiletestkit.javacommon.Args
import com.couchbase.mobiletestkit.javacommon.util.Log
import java.util.*

/*
 Created by sridevi.saragadam on 2/26/19.
*/   class PredictiveQueriesRequestHandler {
    fun registerModel(args: Args): EchoModel {
        val modelName = args.get<String>("model_name")
        val echoModel = EchoModel(modelName)
        Database.prediction.registerModel(modelName, echoModel)
        return echoModel
    }

    fun unRegisterModel(args: Args) {
        val modelName = args.get<String>("model_name")
        Database.prediction.unregisterModel(modelName)
    }

    @Throws(CouchbaseLiteException::class)
    fun getPredictionQueryResult(args: Args): List<Any> {
        val echoModel: EchoModel = args["model"]
        val database = args.get<Database>("database")
        val dict = args.get<Map<String, Any>>("dictionary")
        val input = Expression.value(dict)
        val prediction = Function.prediction(echoModel.name, input)
        val query: Query = QueryBuilder
            .select(SelectResult.expression(prediction))
            .from(DataSource.database(database))
        val resultArray: MutableList<Any> = ArrayList()
        val rows = query.execute()
        for (row in rows) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    fun nonDictionary(args: Args): String {
        val echoModel: EchoModel = args["model"]
        val database = args.get<Database>("database")
        val dict = args.get<String>("nonDictionary")
        val input = Expression.value(dict)
        val prediction = Function.prediction(echoModel.name, input)
        val query: Query = QueryBuilder
            .select(SelectResult.expression(prediction))
            .from(DataSource.database(database))
        val resultArray: List<Any> = ArrayList()
        try {
            query.execute()
        } catch (e: Exception) {
            return e.localizedMessage
        }
        return "success"
    }

    @Throws(CouchbaseLiteException::class)
    fun getEuclideanDistance(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val key1 = args.get<String>("key1")
        val key2 = args.get<String>("key2")
        val distance =
            Function.euclideanDistance(Expression.property(key1), Expression.property(key2))
        val query: Query = QueryBuilder
            .select(SelectResult.expression(distance))
            .from(DataSource.database(database))
        val resultArray: MutableList<Any> = ArrayList()
        val rows = query.execute()
        for (row in rows) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    fun getNumberOfCalls(args: Args): Int {
        val echoModel: EchoModel = args["model"]
        return echoModel.numberOfCalls
    }

    @Throws(CouchbaseLiteException::class)
    fun getSquaredEuclideanDistance(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val key1 = args.get<String>("key1")
        val key2 = args.get<String>("key2")
        val distance =
            Function.squaredEuclideanDistance(Expression.property(key1), Expression.property(key2))
        val query: Query = QueryBuilder
            .select(SelectResult.expression(distance))
            .from(DataSource.database(database))
        val resultArray: MutableList<Any> = ArrayList()
        val rows = query.execute()
        for (row in rows) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    @Throws(CouchbaseLiteException::class)
    fun getCosineDistance(args: Args): List<Any> {
        val database = args.get<Database>("database")
        val key1 = args.get<String>("key1")
        val key2 = args.get<String>("key2")
        val distance = Function.cosineDistance(Expression.property(key1), Expression.property(key2))
        val query: Query = QueryBuilder
            .select(SelectResult.expression(distance))
            .from(DataSource.database(database))
        val resultArray: MutableList<Any> = ArrayList()
        val rows = query.execute()
        for (row in rows) {
            resultArray.add(row.toMap())
        }
        return resultArray
    }

    class EchoModel internal constructor(name: String) : PredictiveModel {
        val name: String
        var numberOfCalls = 0
        override fun predict(input: Dictionary): Dictionary? {
            numberOfCalls++
            return input
        }

        companion object {
            private const val TAG = "ECHO"
        }

        init {
            Log.i(TAG, "Entered into echo model")
            this.name = name
        }
    }
}