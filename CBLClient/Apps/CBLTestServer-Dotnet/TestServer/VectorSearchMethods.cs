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


namespace Couchbase.Lite.Testing
{
    public static class VectorSearchMethods
    {
        public static void createIndex([NotNull] NameValueCollection args,
                                       [NotNull] IReadOnlyDictionary<string, object> postBody,
                                       [NotNull] HttpListenerResponse response)
        // temp method body, check API spec and update correctly
        {
            With<Database>(postBody, "database", database =>
            {
                response.WriteBody(MemoryMap.Store(database.GetDefaultCollection()));
            });
        }

    }

}