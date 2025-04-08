#pragma once

#include <nlohmann/json.hpp>
#include <civetweb.h>

namespace peer_to_peer_methods {
    void peerToPeer_serverStart(nlohmann::json& body, mg_connection* conn);
    void peerToPeer_messageEndpointListenerStart(nlohmann::json& body, mg_connection* conn);
    void peerToPeer_getListenerPort(nlohmann::json& body, mg_connection* conn);
    void peerToPeer_serverStop(nlohmann::json& body, mg_connection* conn);
    void peerToPeer_clientStart(nlohmann::json& body, mg_connection* conn);
    void peerToPeer_configure(nlohmann::json& body, mg_connection* conn);
    void peerToPeer_configureCollection(nlohmann::json& body, mg_connection* conn);


}