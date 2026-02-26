package fr.aplose.erp.dolibarr.client;

import java.util.List;
import java.util.Map;

/**
 * Client for Dolibarr REST API (GET with DOLAPIKEY header).
 */
public interface DolibarrApiClient {

    /**
     * Configure client for a run (base URL and API key). Call before testConnection or getList/getOne.
     */
    void configure(String baseUrl, String apiKey);

    /**
     * Test connection: GET a resource with limit=1 to validate URL and API key.
     *
     * @return true if response is 2xx
     */
    boolean testConnection();

    /**
     * GET list from Dolibarr API. Handles pagination by requesting with limit/sortfield.
     *
     * @param resource   e.g. "thirdparties", "products", "contacts"
     * @param queryParams optional (limit, sortfield, sortorder, sqlfilters...)
     * @return list of objects (parsed from JSON array); empty list on error or empty response
     */
    List<Map<String, Object>> getList(String resource, Map<String, String> queryParams);

    /**
     * GET single object by id: /resource/{id}
     *
     * @param resource e.g. "thirdparties"
     * @param id       Dolibarr id
     * @return map of attributes or null if not found/error
     */
    Map<String, Object> getOne(String resource, long id);

    /**
     * Base URL (normalized, no trailing slash).
     */
    String getBaseUrl();
}
