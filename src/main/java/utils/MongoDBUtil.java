/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author deni
 */
public class MongoDBUtil {

    //private static final String URI = "mongodb+srv://deniamalarcon:7tCmAKDICg4AC0yg@tecnm.jneks.mongodb.net/?retryWrites=true&w=majority&appname=Tecnm";
    //private static final String URI = "mongodb://10.250.1.245:27017,10.250.1.252:27017,10.250.1.200:27017/?replicaSet=rs0&retryWrites=true&w=1&readPreference=primaryPreferred&connectTimeoutMS=3000&socketTimeoutMS=3000";
    private static final String URI = "mongodb://192.168.1.85:27017,10.250.1.252:27017,192.168.1.80:27017/?replicaSet=rs0&retryWrites=true&w=1&readPreference=primaryPreferred&connectTimeoutMS=3000&socketTimeoutMS=3000";

    private static MongoClient mongoClient;

    //conectar a Mongo Atlas
    public static void conectar() {
        if (mongoClient == null) {
            try {
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(URI))
                        .applyToClusterSettings(builder
                                -> builder.serverSelectionTimeout(15000, TimeUnit.MILLISECONDS)) // ⏳ Más tiempo para seleccionar Primary
                        .applyToSocketSettings(builder
                                -> builder.connectTimeout(10000, TimeUnit.MILLISECONDS) // 🔄 Espera más tiempo para conectar
                                .readTimeout(20000, TimeUnit.MILLISECONDS)) // 📥 Mayor tiempo de espera en lecturas
                        .applyToServerSettings(builder
                                -> builder.heartbeatFrequency(3000, TimeUnit.MILLISECONDS)) // 💓 Monitoreo más frecuente de nodos
                        .build();

                mongoClient = MongoClients.create(settings);
                System.out.println("✅ Conectado a la base de datos");
            } catch (Exception e) {
                System.err.println("❌ Error al conectar a MongoDB: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    //obtener la base de datos
    public static MongoDatabase getDatabase(String nombreDB) {
        conectar();
        if (mongoClient != null) {
            return mongoClient.getDatabase(nombreDB);
        } else {
            throw new IllegalStateException("❌ No se pudo conectar a MongoDB");
        }
    }

    public static void cerrarConexion() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}
