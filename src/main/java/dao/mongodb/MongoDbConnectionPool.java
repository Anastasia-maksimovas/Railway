package dao.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import util.MongoDbConfiguration;

import static java.util.Collections.singletonList;
import static util.MongoDbConfiguration.AUTH_DATABASE;
import static util.MongoDbConfiguration.AUTH_PASSWORD;
import static util.MongoDbConfiguration.AUTH_USERNAME;
import static util.MongoDbConfiguration.DATABASE;
import static util.MongoDbConfiguration.HOST;
import static util.MongoDbConfiguration.PORT;

public class MongoDbConnectionPool {
    private static MongoDbConnectionPool instance;

    private MongoClient client;

    private MongoDbConnectionPool() {
        ServerAddress address = new ServerAddress(
                MongoDbConfiguration.getInstance().getConfig(HOST),
                Integer.parseInt(MongoDbConfiguration.getInstance().getConfig(PORT))
        );

        MongoCredential credential = MongoCredential.createCredential(
                MongoDbConfiguration.getInstance().getConfig(AUTH_USERNAME),
                MongoDbConfiguration.getInstance().getConfig(AUTH_DATABASE),
                MongoDbConfiguration.getInstance().getConfig(AUTH_PASSWORD).toCharArray()
        );

        MongoClientOptions options = MongoClientOptions.builder()
                .sslEnabled(false)
                .build();

        client = new MongoClient(address, singletonList(credential), options);
    }

    public static MongoDbConnectionPool getInstance() {
        if (instance == null) {
            synchronized (MongoDbConnectionPool.class) {
                if (instance == null) {
                    instance = new MongoDbConnectionPool();
                }
            }
        }
        return instance;
    }

    public MongoDatabase getConnection() {
        return client.getDatabase(MongoDbConfiguration.getInstance().getConfig(DATABASE));
    }

    public void close() {
        client.close();
    }
}
