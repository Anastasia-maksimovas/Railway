package util;

import com.github.javafaker.Faker;
import com.github.javafaker.Number;
import com.healthmarketscience.sqlbuilder.InsertQuery;
import com.healthmarketscience.sqlbuilder.JdbcEscape;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import dao.mysql.TypePlace;
import model.entity.Price;
import model.entity.Request;
import model.entity.Route;
import model.entity.Station;
import model.entity.Train;
import model.entity.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.Locale.ENGLISH;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

public class SqlDataGenerator {

    private static final int STATIONS_SIZE = 128;
    private static final int UNFILTERED_ROUTES_SIZE = STATIONS_SIZE * STATIONS_SIZE;
    private static final int PRICES_SIZE = IntStream.range(1, STATIONS_SIZE).sum();
    private static final int TRAINS_SIZE = STATIONS_SIZE / 4;

    private static final int BERTH_FREE_MIN_SIZE = 30;
    private static final int COMPARTMENT_FREE_MIN_SIZE = 30;
    private static final int DELUXE_FREE_MIN_SIZE = 30;

    private static final int BERTH_FACTOR_MAX = 1;
    private static final int COMPARTMENT_FACTOR_MAX = 2;
    private static final int DELUXE_FACTOR_MAX = 3;

    private static final int NUMBER_OF_DECIMALS = 2;

    private static final int DISTANCE_MIN = 100;
    private static final int DISTANCE_MAX = 1_000;

    private static final int DAYS_TO_BUY_TICKET = 30;
    private static final int ROUT_MAX_DURATION_HOURS = 24;

    private static final String TYPICAL_PASSWORD_HASH = md5Hex("root");

    private static final String TYPICAL_PHONE_NUMBER = "(012) 345-67-89";

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private static final String STATIONS_FILE_NAME_FORMAT = "C:\\Users\\Public\\Documents\\sql\\stations_%d.sql";

    private static final String PRICES_FILE_NAME_FORMAT = "C:\\Users\\Public\\Documents\\sql\\prices_%d.sql";

    private static final String ROUTES_FILE_NAME_FORMAT = "C:\\Users\\Public\\Documents\\sql\\routes_%d.sql";

    private static final String TRAINS_FILE_NAME_FORMAT = "C:\\Users\\Public\\Documents\\sql\\trains_%d.sql";

    private static final String USERS_FILE_NAME_FORMAT = "C:\\Users\\Public\\Documents\\sql\\users_%d.sql";

    private static final String REQUESTS_FILE_NAME_FORMAT = "C:\\Users\\Public\\Documents\\sql\\requests_%d.sql";

    private static final int FAKERS_SIZE = UNFILTERED_ROUTES_SIZE;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("You should pass iteration number starting from 0");
            return;
        }

        int iteration = Integer.parseInt(args[0]);
        System.out.println("Starting generator iteration " + iteration);
        System.out.println();

        List<Faker> fakers = generateFakers(iteration);
        System.out.println("Initialized fakers with size of " + FAKERS_SIZE);
        System.out.println();

        DbSpec dbSpec = new DbSpec();
        DbSchema dbSchema = dbSpec.addSchema("railway_system");

        DbTable stationTable = dbSchema.addTable("station");
        DbColumn stationIdColumn = stationTable.addColumn("id", "number", null);
        DbColumn stationNameColumn = stationTable.addColumn("name", "varchar", 100);

        DbTable priceTable = dbSchema.addTable("price");
        DbColumn priceIdColumn = priceTable.addColumn("id", "number", null);
        DbColumn priceCompartmentFactorColumn = priceTable.addColumn("compartmentFactor", "number", null);
        DbColumn priceDeluxeFactorColumn = priceTable.addColumn("deluxeFactor", "number", null);
        DbColumn priceBerthFactorColumn = priceTable.addColumn("berthFactor", "number", null);

        DbTable routeTable = dbSchema.addTable("route");
        DbColumn routeIdColumn = routeTable.addColumn("id", "number", null);
        DbColumn routeFromTimeColumn = routeTable.addColumn("fromTime", "varchar", null);
        DbColumn routeToTimeColumn = routeTable.addColumn("toTime", "varchar", null);
        DbColumn routePriceIdColumn = routeTable.addColumn("priceId", "number", null);
        DbColumn routeFromIdColumn = routeTable.addColumn("fromId", "number", null);
        DbColumn routeToIdColumn = routeTable.addColumn("toId", "number", null);
        DbColumn routeDistanceColumn = routeTable.addColumn("distance", "number", null);

        DbTable trainTable = dbSchema.addTable("train");
        DbColumn trainIdColumn = trainTable.addColumn("id", "number", null);
        DbColumn trainRouteIdColumn = trainTable.addColumn("routeId", "number", null);
        DbColumn trainCompartmentFreeColumn = trainTable.addColumn("compartmentFree", "number", null);
        DbColumn trainDeluxeFreeColumn = trainTable.addColumn("deluxeFree", "number", null);
        DbColumn trainBerthFreeColumn = trainTable.addColumn("berthFree", "number", null);

        DbTable userTable = dbSchema.addTable("user");
        DbColumn userIdColumn = userTable.addColumn("id", "number", null);
        DbColumn userEmailColumn = userTable.addColumn("email", "varchar", 100);
        DbColumn userPasswordColumn = userTable.addColumn("password", "varchar", 36);
        DbColumn userNameColumn = userTable.addColumn("name", "varchar", 45);
        DbColumn userSurnameColumn = userTable.addColumn("surname", "varchar", 45);
        DbColumn userPhoneColumn = userTable.addColumn("phone", "varchar", 12);
        DbColumn userIsAdminColumn = userTable.addColumn("admin", "number", null);

        DbTable requestTable = dbSchema.addTable("request");
        DbColumn requestIdColumn = requestTable.addColumn("id", "number", null);
        DbColumn requestUserIdColumn = requestTable.addColumn("userId", "number", null);
        DbColumn requestTrainIdColumn = requestTable.addColumn("trainId", "number", null);
        DbColumn requestTypeColumn = requestTable.addColumn("type", "varchar", 1);
        DbColumn requestPriceColumn = requestTable.addColumn("price", "number", null);

        List<Station> stations = generateStations(iteration, fakers);
        System.out.println("Generated stations with size of " + stations.size());
        String stationsFileName = String.format(STATIONS_FILE_NAME_FORMAT, iteration);
        for (Station station : stations) {
            serializeToSql(new InsertQuery(stationTable)
                    .addColumn(stationIdColumn, station.getId())
                    .addColumn(stationNameColumn, station.getName().replace('\'', ' '))
                    .toString(), stationsFileName);
        }
        System.out.println("Successfully imported stations to " + stationsFileName);
        System.out.println();

        List<Price> prices = generatePrices(iteration, fakers);
        System.out.println("Generated prices with size of " + prices.size());
        String pricesFileName = String.format(PRICES_FILE_NAME_FORMAT, iteration);
        for (Price price : prices) {
            serializeToSql(new InsertQuery(priceTable)
                    .addColumn(priceIdColumn, price.getId())
                    .addColumn(priceCompartmentFactorColumn, price.getCompartmentFactor())
                    .addColumn(priceDeluxeFactorColumn, price.getDeluxeFactor())
                    .addColumn(priceBerthFactorColumn, price.getBerthFactor())
                    .toString(), pricesFileName);
        }
        System.out.println("Successfully imported prices to " + pricesFileName);
        System.out.println();

        List<Route> routes = generateRoutes(iteration, fakers);
        System.out.println("Generated filtered routes with size of " + routes.size());
        String routesFileName = String.format(ROUTES_FILE_NAME_FORMAT, iteration);
        for (Route route : routes) {
            serializeToSql(new InsertQuery(routeTable)
                    .addColumn(routeIdColumn, route.getId())
                    .addColumn(routeFromTimeColumn, route.getFromTime())
                    .addColumn(routeToTimeColumn, route.getToTime())
                    .addColumn(routePriceIdColumn, route.getPriceId())
                    .addColumn(routeFromIdColumn, route.getFromId())
                    .addColumn(routeToIdColumn, route.getToId())
                    .addColumn(routeDistanceColumn, route.getDistance())
                    .toString(), routesFileName);
        }
        System.out.println("Successfully imported filtered routes to " + routesFileName);
        System.out.println();

        Map<Integer, Train> trainByIdMap = generateTrains(iteration, routes.size(), fakers);
        System.out.println("Generated trains with size of " + trainByIdMap.size());
        String trainsFileName = String.format(TRAINS_FILE_NAME_FORMAT, iteration);
        for (Train train : trainByIdMap.values()) {
            serializeToSql(new InsertQuery(trainTable)
                    .addColumn(trainIdColumn, train.getId())
                    .addColumn(trainRouteIdColumn, train.getRouteId())
                    .addColumn(trainCompartmentFreeColumn, train.getCompartmentFree())
                    .addColumn(trainDeluxeFreeColumn, train.getDeluxeFree())
                    .addColumn(trainBerthFreeColumn, train.getBerthFree())
                    .toString(), trainsFileName);
        }
        System.out.println("Successfully imported trains to " + trainsFileName);
        System.out.println();

        List<User> users = generateUsers(iteration, routes.size(), fakers);
        System.out.println("Generated users with size of " + users.size());
        String usersFileName = String.format(USERS_FILE_NAME_FORMAT, iteration);
        for (User user : users) {
            serializeToSql(new InsertQuery(userTable)
                    .addColumn(userIdColumn, user.getId())
                    .addColumn(userEmailColumn, user.getEmail())
                    .addColumn(userPasswordColumn, user.getPassword())
                    .addColumn(userNameColumn, user.getName())
                    .addColumn(userSurnameColumn, user.getSurname())
                    .addColumn(userPhoneColumn, user.getPhone())
                    .addColumn(userIsAdminColumn, user.getAdmin())
                    .toString(), usersFileName);
        }
        System.out.println("Successfully imported users to " + usersFileName);
        System.out.println();

        List<Request> requests = generateRequests(iteration, prices, routes);
        System.out.println("Generated requests with size of " + requests.size());
        String requestsFileName = String.format(REQUESTS_FILE_NAME_FORMAT, iteration);
        for (Request request : requests) {
            serializeToSql(new InsertQuery(requestTable)
                    .addColumn(requestIdColumn, request.getId())
                    .addColumn(requestUserIdColumn, request.getUserId())
                    .addColumn(requestTrainIdColumn, request.getTrainId())
                    .addColumn(requestTypeColumn, request.getType())
                    .addColumn(requestPriceColumn, request.getPrice())
                    .toString(), requestsFileName);
        }
        System.out.println("Successfully imported requests to " + requestsFileName);
        System.out.println();
    }

    private static List<Faker> generateFakers(int iteration) {
        int maxId = toMaxId(iteration, FAKERS_SIZE);
        int minId = toMinId(maxId, FAKERS_SIZE);

        List<Faker> fakers = new ArrayList<>(FAKERS_SIZE);
        for (int id = minId; id <= maxId; id++) {
            fakers.add(new Faker(ENGLISH, new Random(id)));
        }

        return fakers;
    }

    private static List<Station> generateStations(int iteration, List<Faker> fakers) {
        int maxId = toMaxId(iteration, STATIONS_SIZE);
        int minId = toMinId(maxId, STATIONS_SIZE);

        List<Station> stations = new ArrayList<>(STATIONS_SIZE);
        for (int id = minId; id <= maxId; id++) {
            stations.add(createStation(id, fakers.get(indexOf(iteration, STATIONS_SIZE, id))));
        }

        return stations;
    }

    private static List<Price> generatePrices(int iteration, List<Faker> fakers) {
        int maxId = toMaxId(iteration, PRICES_SIZE);
        int minId = toMinId(maxId, PRICES_SIZE);

        List<Price> prices = new ArrayList<>(PRICES_SIZE);
        for (int id = minId; id <= maxId; id++) {
            prices.add(createPrice(id, fakers.get(indexOf(iteration, PRICES_SIZE, id))));
        }

        return prices;
    }

    private static List<Route> generateRoutes(int iteration, List<Faker> fakers) {
        int maxId = toMaxId(iteration, UNFILTERED_ROUTES_SIZE);
        int minId = toMinId(maxId, UNFILTERED_ROUTES_SIZE);
        int maxStationId = toMaxId(iteration, STATIONS_SIZE);
        int minStationId = toMinId(maxStationId, STATIONS_SIZE);
        int priceOffset = toOffset(iteration, PRICES_SIZE);
        int stationsOffset = toOffset(iteration, STATIONS_SIZE);

        List<Route> unfilteredRoutes = new ArrayList<>(UNFILTERED_ROUTES_SIZE);
        for (int id = minId; id <= maxId; id++) {
            Number fakeNumber = fakers.get(indexOf(iteration, UNFILTERED_ROUTES_SIZE, id)).number();
            int priceId = priceOffset + id % PRICES_SIZE + 1;
            int fromStationId = stationsOffset + id % STATIONS_SIZE + 1;
            int toStationId = fakeNumber.numberBetween(minStationId, maxStationId);
            Route route = createRoute(id, priceId, fromStationId, toStationId, fakers.get(indexOf(iteration, UNFILTERED_ROUTES_SIZE, id)));
            unfilteredRoutes.add(route);
        }

        List<Route> routes = new ArrayList<>(UNFILTERED_ROUTES_SIZE);
        for (Route route : unfilteredRoutes) {
            if (!route.getFromId().equals(route.getToId())) {
                routes.add(route);
            }
        }
        return routes;
    }

    private static Map<Integer, Train> generateTrains(int iteration, int routesSize, List<Faker> fakers) {
        int maxRouteId = toMaxId(iteration, routesSize);
        int minRouteId = toMinId(maxRouteId, routesSize);
        int trainsOffset = toOffset(iteration, TRAINS_SIZE);

        Map<Integer, Train> trainByIdMap = new HashMap<>(TRAINS_SIZE);
        for (int routeId = minRouteId; routeId <= maxRouteId; routeId++) {
            int trainId = trainsOffset + routeId % TRAINS_SIZE + 1;
            if (!trainByIdMap.containsKey(trainId)) {
                trainByIdMap.put(trainId, createTrain(trainId, routeId, fakers.get(indexOf(iteration, TRAINS_SIZE, trainId))));
            }
        }

        return trainByIdMap;
    }

    private static List<User> generateUsers(int iteration, int usersSize, List<Faker> fakers) {
        int maxId = toMaxId(iteration, usersSize);
        int minId = toMinId(maxId, usersSize);

        List<User> users = new ArrayList<>(usersSize);
        for (int id = minId; id <= maxId; id++) {
            users.add(createUser(id, fakers.get(indexOf(iteration, usersSize, id))));
        }

        return users;
    }

    private static List<Request> generateRequests(int iteration, List<Price> prices, List<Route> routes) {
        int maxId = toMaxId(iteration, routes.size());
        int minId = toMinId(maxId, routes.size());
        int trainsOffset = toOffset(iteration, TRAINS_SIZE);

        List<Request> requests = new ArrayList<>(routes.size());
        for (int id = minId; id <= maxId; id++) {
            int trainId = trainsOffset + id % TRAINS_SIZE + 1;
            TypePlace typePlace = TypePlace.get(id % TypePlace.SIZE);
            Price price = prices.get(id % PRICES_SIZE);
            Route route = routes.get(id % routes.size());
            requests.add(createRequest(id, id, trainId, typePlace.calculatePrice(price, route), typePlace));
        }

        return requests;
    }

    private static User createUser(long id, Faker faker) {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        String email = firstName.toLowerCase() + '.' + lastName.toLowerCase() + "@gmail.com";

        return new User(id, firstName, lastName, TYPICAL_PHONE_NUMBER, email, TYPICAL_PASSWORD_HASH, false);
    }

    private static int indexOf(int iteration, int size, int id) {
        return id - toOffset(iteration, size) - 1;
    }

    private static int toMinId(int maxId, int size) {
        return (maxId - size) + 1;
    }

    private static int toMaxId(int iteration, int size) {
        return toOffset(iteration + 1, size);
    }

    private static int toOffset(int iteration, int size) {
        return iteration * size;
    }

    private static Station createStation(long id, Faker faker) {
        String cityName = faker.address().cityName();

        return new Station(id, cityName);
    }

    private static Price createPrice(long id, Faker faker) {
        double berthFactor = faker.number().randomDouble(NUMBER_OF_DECIMALS, 0, BERTH_FACTOR_MAX);
        double compartmentFactor = faker.number().randomDouble(NUMBER_OF_DECIMALS, 1, COMPARTMENT_FACTOR_MAX);
        double deluxeFactor = faker.number().randomDouble(NUMBER_OF_DECIMALS, 2, DELUXE_FACTOR_MAX);

        return new Price(id, compartmentFactor, deluxeFactor, berthFactor);
    }

    private static Route createRoute(long id, long priceId, long fromId, long toId, Faker faker) {
        Date fromDate = faker.date().future(DAYS_TO_BUY_TICKET, 1, TimeUnit.DAYS);
        Date toDate = faker.date().future(ROUT_MAX_DURATION_HOURS, TimeUnit.HOURS, fromDate);
        double distance = faker.number().randomDouble(NUMBER_OF_DECIMALS, DISTANCE_MIN, DISTANCE_MAX);

        return new Route(id, priceId, fromId, toId, FORMATTER.format(fromDate), FORMATTER.format(toDate), distance);
    }

    private static Train createTrain(long id, long routeId, Faker faker) {
        int compartmentFree = faker.number().numberBetween(COMPARTMENT_FREE_MIN_SIZE, COMPARTMENT_FREE_MIN_SIZE + 20);
        int deluxeFree = faker.number().numberBetween(DELUXE_FREE_MIN_SIZE, DELUXE_FREE_MIN_SIZE + 10);
        int berthFree = faker.number().numberBetween(BERTH_FREE_MIN_SIZE, 30);

        return new Train(id, routeId, compartmentFree, deluxeFree, berthFree);
    }

    private static Request createRequest(long id, long userId, long trainId, double price, TypePlace typePlace) {

        return new Request(id, userId, trainId, typePlace, price);
    }

    private static void serializeToSql(String statement, String fileName) throws IOException {
        Path path = Paths.get(fileName);
        File file = path.toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        try (Writer writer = new FileWriter(file, true)) {
            writer.append(statement).append(';').append(System.getProperty("line.separator"));
        }
    }
}