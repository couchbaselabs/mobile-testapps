#pragma once

#include <nlohmann/json.hpp>
#include <civetweb.h>

namespace vectorSearch_methods
{
    void vectorSearch_createIndex(nlohmann::json &body, mg_connection *conn);

}