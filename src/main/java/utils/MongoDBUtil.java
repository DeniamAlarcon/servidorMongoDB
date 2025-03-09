/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 *
 * @author deni
 */
public class MongoDBUtil {

    private static final String URI = "mongodb+srv://deniamalarcon:7tCmAKDICg4AC0yg@tecnm.jneks.mongodb.net/?retryWrites=true&w=majority&appname=Tecnm";
    private static MongoClient mongoClient;
    
    //conectar a Mongo Atlas
    public static void conectar() {
        if(mongoClient == null) {
            mongoClient = MongoClients.create(URI);
            System.out.println("Conectado a base de datos");
        }
    }
    
    //obtener la base de datos
    public static MongoDatabase getDatabase(String nombreDB) {
        conectar();
        return mongoClient.getDatabase(nombreDB);
    }
    
    public static void cerrarConexion() {
        if(mongoClient != null ){
            mongoClient.close();
            mongoClient = null;
        }
    }
}
