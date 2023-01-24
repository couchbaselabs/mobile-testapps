using System;
using System.IO;
using System.Reflection;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Net;
using System.Linq;
using System.Security.Cryptography.X509Certificates;
using System.Runtime.Serialization.Formatters.Binary;

using Couchbase.Lite.Sync;
using Couchbase.Lite.Util;

using JetBrains.Annotations;

using Newtonsoft.Json.Linq;

using static Couchbase.Lite.Testing.DatabaseMethods;
using System.Threading;

namespace Couchbase.Lite.Testing
{
    public class CollectionMethods
    {
        public static void defaultCollection([NotNull] NameValueCollection args,
                                             [NotNull] IReadOnlyDictionary<string, object> postBody,
                                             [NotNull] HttpListenerResponse response)
        {
            With<Database>(postBody, "database", database =>
            {
                response.WriteBody(MemoryMap.Store(database.GetDefaultCollection()));
            });
        }

        public static void createColelction([NotNull] NameValueCollection args,
                                            [NotNull] IReadOnlyDictionary<string, object> postBody,
                                            [NotNull] HttpListenerResponse response)
        {
            With<Database>(postBody, "database", database =>
            {
                String collectionName = postBody["collectionName"].ToString();
                String scopeName;
                if (postBody.ContainsKey("scopeName"))
                    scopeName = postBody["scopeName"].ToString();
                else
                    scopeName = "_default";
                response.WriteBody(MemoryMap.Store(database.CreateCollection(collectionName, scopeName)));
            });
        }

        public static void deleteCollection([NotNull] NameValueCollection args,
                                            [NotNull] IReadOnlyDictionary<string, object> postBody,
                                            [NotNull] HttpListenerResponse response)
        {
            With<Database>(postBody, "database", database =>
            {
                string collectionName = postBody["collectionName"].ToString();
                string scopeName;
                if (postBody.ContainsKey("scopeName"))
                    scopeName = postBody["scopeName"].ToString();
                else
                    scopeName = "_default";
                database.DeleteCollection(collectionName, scopeName);
                response.WriteBody(true);
            });
        }

        public static void collectionNames([NotNull] NameValueCollection args,
                                            [NotNull] IReadOnlyDictionary<string, object> postBody,
                                            [NotNull] HttpListenerResponse response)
        {
            With<Database>(postBody, "database", database =>
            {
                string scopeName = postBody["scopeName"].ToString();
                List<string> collectionNames = new List<string>();
                IReadOnlyList<Collection> collectionObjects = database.GetCollections(scopeName);
                foreach (Collection col in collectionObjects)
                {
                    string name = col.Name;
                    collectionNames.Add(name);
                }
                response.WriteBody(collectionNames);
            });
        }

        public static void collectionName([NotNull] NameValueCollection args,
                                            [NotNull] IReadOnlyDictionary<string, object> postBody,
                                            [NotNull] HttpListenerResponse response)
        {
            With<Collection>(postBody, "collection", collection =>
            {
                response.WriteBody(collection.Name);
            });
        }
    }
}