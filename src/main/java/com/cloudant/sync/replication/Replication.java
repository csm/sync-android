package com.cloudant.sync.replication;

import com.cloudant.mazha.CouchConfig;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages information about a replication. It could be "pull" or "push",
 * and concrete classes "PullReplication" and "PushReplication" are for
 * them respectively.
 *
 * The concrete object is used to create a {@link com.cloudant.sync.replication.Replicator}
 * that can be used to managed a pull or push replication:
 * {@code ReplicatorFactory.oneway(PullReplication)} or
 * {@code ReplicatorFactory.oneway(PushReplication)}
 *
 * @see com.cloudant.sync.replication.PullReplication
 * @see com.cloudant.sync.replication.PushReplication
 * @see com.cloudant.sync.replication.ReplicatorFactory
 */
abstract class Replication {

    public String username;
    public String password;

    public abstract void validate();

    public abstract String getReplicatorName();

    public abstract ReplicationStrategy createReplicationStrategy();

    /**
     * Describe "Filter Function" used in {@code PullReplication}.
     * It includes the name of the function, and the query
     * parameters for the function. For example:
     *
     * {@code
     *     Filter filter = new Filter( "filerDoc/filterName", ImmutableMap.of("key", "value"));
     * }
     *
     * @see com.cloudant.sync.replication.PullReplication
     * @see http://docs.couchdb.org/en/1.4.x/replication.html#controlling-which-documents-to-replicate
     * @see http://docs.couchdb.org/en/1.4.x/ddocs.html#filterfun
     */
    public static class Filter {

        /**
         * Name of the "Filter Function", indicates which function will
         * be called for this filter.
         */
        public String name;

        /**
         * Query parameters, which will be put as part of the "request"
         * when filter function is called.
         *
         * @see @see http://docs.couchdb.org/en/1.4.x/ddocs.html#filterfun
         */
        public Map<String, String> parameters;

        /**
         * Construct a filter without any parameters
         *
         * @param name of the filter function
         */
        public Filter(String name) {
            this.name = name;
        }

        /**
         * Construct a filter with filter function name and query parameters.
         * The query parameters should not be escaped.
         *
         * @param name filter function name
         * @param parameters filter function parameters
         */
        public Filter(String name, Map<String, String> parameters) {
            this.name = name;
            this.parameters = parameters;
        }

        /**
         * Generated {@code String} should not be used in URL as request
         * query as they are not properly escaped.
         *
         * Filter parameters are sorted by key so that the  generated
         * String are the same for different calls. This is imporant
         * because the String is part of the replication id.
         *
         * @return String can use to display the filter name and parameters.
         *
         * @see BasicPullStrategy#getReplicationId() 
         */
        public String toQueryString() {
            if(this.parameters == null) {
                return String.format("filter=%s", this.name);
            } else {
                List<String> queries = new ArrayList<String>();
                for(Map.Entry<String, String> parameter : this.parameters.entrySet()) {
                    queries.add(String.format("%s=%s", parameter.getKey(), parameter.getValue()));
                }
                Collections.sort(queries);
                return String.format("filter=%s&%s", this.name,
                        Joiner.on('&').skipNulls().join(queries));
            }
        }
    }

    CouchConfig createCouchConfig(URI uri, String username, String password) {
        int port = uri.getPort() < 0 ? getDefaultPort(uri.getScheme()) : uri.getPort();
        return new CouchConfig(uri.getScheme(), uri.getHost(),  port, username, password);
    }

    int getDefaultPort(String protocol) {
        if(protocol.equalsIgnoreCase("http")) {
            return 80;
        } else if(protocol.equalsIgnoreCase("https")) {
            return 443;
        } else {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
    }

    String extractDatabaseName(URI uri) {
        String db =  uri.getPath().substring(1);
        if(db.contains("/"))
            throw new IllegalArgumentException("DB name can not contain slash: '/'");
        return db;
    }

    void checkURI(URI uri) {
        Preconditions.checkArgument(uri.getUserInfo() == null,
                "There must be no user info (credentials) in replication URI " +
                        "(use Replication instance attributes)");
        Preconditions.checkArgument(uri.getScheme() != null, "Protocol must be provided in replication URI");
        Preconditions.checkArgument(uri.getHost() != null, "Host must be provided in replication URI");
        Preconditions.checkArgument(uri.getScheme().equalsIgnoreCase("http")
                || uri.getScheme().equalsIgnoreCase("https"), "Only http/https are supported in replication URI");
    }
}
