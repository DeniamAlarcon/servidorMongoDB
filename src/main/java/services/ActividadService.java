/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import utils.MongoDBUtil;

/**
 *
 * @author deni
 */
@Path("/actividades")
public class ActividadService {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerActividades(@Context HttpHeaders headers) {
        //obtener token desde los headers
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta el token").build();
            
        }
        
        //Extraer la base de datos del token
        String databaseName = utils.JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
        if (databaseName == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token invalido").build();
            
        }
        
        //conectar a la base de datos correspondiente
        MongoDatabase database = MongoDBUtil.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection("Productos");
        
        
        //obtener todos los datos de prubas
        List<Document> productos = collection.find().into(new ArrayList<>());
        
        return Response.ok(productos).build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response agregarProducto(Document producto, @Context HttpHeaders headers) {
        //Obtener el token
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta el token").build();
        }
        
        //EXtraer la  base de datos desde el token
        String databaseName = utils.JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
        if (databaseName == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token invalido").build();
            
        }
        
        //conectar a la base de datos del usuario
        MongoDatabase database = MongoDBUtil.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection("Productos");
        
        //Insertar el prducto en la base de datos
        collection.insertOne(producto);
        
        return Response.status(Response.Status.CREATED).entity(producto).build();
    }
}
