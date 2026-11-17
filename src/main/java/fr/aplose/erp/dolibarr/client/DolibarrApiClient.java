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

    /** Forget the settings given to {@link #configure} for the current thread. */
    void clear();

    /**
     * GET every record of a resource, page by page (limit/page parameters), until a page comes back incomplete.
     * Any "limit" or "page" in queryParams is overridden.
     *
     * @return all records; empty list if there is none
     */
    List<Map<String, Object>> getAll(String resource, Map<String, String> queryParams);

    /**
     * GET one page from Dolibarr API (no pagination: use {@link #getAll} to read a whole resource).
     * A 404 answer (empty list on older Dolibarr versions) gives an empty list.
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
