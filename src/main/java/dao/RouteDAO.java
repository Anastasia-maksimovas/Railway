package dao;

import model.entity.Route;

import java.util.List;

public interface RouteDAO {

    /**
     * Find all ROUTES in DB
     */
    List<Route> findAll();

    /**
     * Find ROUTE by ID
     */
    Route findById(String id);

    /**
     * Find ROUTES by given ID of STATION
     */
    List<Route> findByFromId(String id);

    /**
     * Insert new ROUTE
     */
    Route create(Route route);

    /**
     * Update ROUTE
     */
    Route update(Route route);

    /**
     * Delete ROUTE
     */
    void delete(Route route);
}
