using System;
using System.IO;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using Couchbase.Lite.Query;
using JetBrains.Annotations;
using Newtonsoft.Json.Linq;

using static Couchbase.Lite.Testing.DatabaseMethods;

namespace Couchbase.Lite.Testing
{
    public static class VectorSearchMethods
    {
        public static void createIndex([NotNull] NameValueCollection args,
                                       [NotNull] IReadOnlyDictionary<string, object> postBody,
                                       [NotNull] HttpListenerResponse response)
        {
            With<Database>(postBody, "database", database =>
            {
                response.WriteBody(MemoryMap.Store(database.GetDefaultCollection()));
            });
        }

    }
}   