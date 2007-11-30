/* Script to initialize the usage log database.  Will do nothing
   if the database already exists */

CREATE TABLE IF NOT EXISTS usage_log
(
    request_time TIMESTAMP NOT NULL, /* The time at which the request was made */
    client_ip VARCHAR(15) NOT NULL, /* The client's IP address */
    client_hostname VARCHAR(100), /* The host name of the client (if available) */
    client_referrer VARCHAR(100), /* The site from which the client came (if available) */
    client_user_agent VARCHAR(150), /* The application/browser that the client is using (if available) */
    http_method VARCHAR(6) NOT NULL, /* The HTTP method that the client is using (will almost always be GET) */
    wms_version VARCHAR(10), /* The version of WMS that the client requests */
    wms_operation VARCHAR(20) NOT NULL /* The operation that the client is performing
                                           (GetMap, etc).  Note that this includes non-standard
                                           requests for metadata. */
    
);