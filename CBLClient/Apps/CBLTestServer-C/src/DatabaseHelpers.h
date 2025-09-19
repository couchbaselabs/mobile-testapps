#pragma once

#include <functional>

class CBLDatabase;
class CBLCollection;

void withDefaultCollection(const CBLDatabase* db, const std::function<void(CBLCollection*)>& fn);