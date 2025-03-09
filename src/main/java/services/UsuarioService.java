/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.bson.types.ObjectId;
import utils.JWTUtils;
import utils.MongoDBUtil;

/**
 *
 * @author deni
 */
@Path("/service")
public class UsuarioService {
    
    @GET
    @Path("/{coleccion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerDatosDinamicos(
            @PathParam("coleccion") String coleccion,
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers) {
        
        
        //obtener token y la base de datos
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta el token").build();
        }
        
        String baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
        if (baseDatos == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("No se pudo obtener la base de datos").build();
        }
        
        //Conectar a la base de datos correcta
        MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);
        
        
        //verificar si la collecion existe
        MongoIterable<String> colecciones = database.listCollectionNames();
        if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
            return Response.status(Response.Status.NOT_FOUND).entity("La coleccion no existe").build();
        }
        
        //Obtener datos de la coleccion
        MongoCollection<Document> collection = database.getCollection(coleccion);
        
        //Crear filtro dinamico
        Document filtro = new Document();
        MultivaluedMap<String, String> parametros = uriInfo.getQueryParameters();
        
        for (String key : parametros.keySet()) {
            filtro.append(key, parametros.getFirst(key));
        }
        
        FindIterable<Document> datos = collection.find(filtro);
        
        List<Document> listaDatos = new ArrayList<>();
        for(Document doc : datos) {
            doc.put("_id", doc.getObjectId("_id").toHexString());
            listaDatos.add(doc);
            
        }
        
        return Response.ok(listaDatos).build();
    }
    
    @POST
    @Path("/{coleccion}/filtrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerDatosconFiltro(
            @PathParam("coleccion") String coleccion,
            Document filtros,
            @Context HttpHeaders headers) {
        
        //obtener token y la base de datos
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta el token").build();
        }
        
        String baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
        if (baseDatos == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("No se pudo obtener la base de datos").build();
        }
        
        //Conectar a la base de datos correcta
        MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);
        
        
        //verificar si la collecion existe
        MongoIterable<String> colecciones = database.listCollectionNames();
        if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
            return Response.status(Response.Status.NOT_FOUND).entity("La coleccion no existe").build();
        }
        
        MongoCollection<Document> collection = database.getCollection(coleccion);
        
        //obtener datos con los filtros proporcionados
        FindIterable<Document> datos = collection.find(filtros);
        
        List<Document> listaDatos = new ArrayList<>();
        for (Document doc : datos) {
            doc.put("_id", doc.getObjectId("_id").toHexString());
            listaDatos.add(doc);
        }
        
        return Response.ok(listaDatos).build();
    }
    
    @POST
    @Path("/{coleccion}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response agregarDato(
            @PathParam("coleccion") String coleccion, 
            Document nuevoDato, 
            @Context HttpHeaders headers) {
        //obtener token y la base de datos
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta el token").build();
        }
        
        String baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
        if (baseDatos == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("No se pudo obtener la base de datos").build();
        }
        
        //Conectar a la base de datos correcta
        MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);
        
        
        //verificar si la collecion existe
        MongoIterable<String> colecciones = database.listCollectionNames();
        if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
            return Response.status(Response.Status.NOT_FOUND).entity("La coleccion no existe").build();
        }
        
        //Obtener datos de la coleccion
        MongoCollection<Document> collection = database.getCollection(coleccion);
        
        //insertar el documento
        collection.insertOne(nuevoDato);
        
        return Response.ok("Documento agregado correctamente a la coleccion: " + coleccion).build();
    }
    
    @PUT
    @Path("/{coleccion}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarDocumento(
            @PathParam("coleccion") String coleccion, 
            @PathParam("id") String id,
            Document nuevoDato, 
            @Context HttpHeaders headers) {
        
        //obtener token y la base de datos
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta el token").build();
        }
        
        String baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
        if (baseDatos == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("No se pudo obtener la base de datos").build();
        }
        
        //Conectar a la base de datos correcta
        MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);
        
        
        //verificar si la collecion existe
        MongoIterable<String> colecciones = database.listCollectionNames();
        if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
            return Response.status(Response.Status.NOT_FOUND).entity("La coleccion no existe").build();
        }
        
        //Obtener datos de la coleccion
        MongoCollection<Document> collection = database.getCollection(coleccion);
        
        //crear la actualizacion
        Document updateQuery = new Document("$set", nuevoDato);
        Document filtro = new Document("_id",new ObjectId(id));
        
        //Aplicar la actualizacion
        UpdateResult resultado = collection.updateOne(filtro, updateQuery);
        
        if (resultado.getMatchedCount() == 0) {
            return Response.status(Response.Status.NOT_FOUND).entity("Documento no encontrado").build();
        }
        
        return Response.ok("Documento actualizado correctamente").build();
    }
    
    
    @DELETE
    @Path("/{coleccion}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminarDocumento(
            @PathParam("coleccion") String coleccion, 
            @PathParam("id") String id,
            @Context HttpHeaders headers) {
        
        //obtener token y la base de datos
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Falta el token").build();
        }
        
        String baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
        if (baseDatos == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("No se pudo obtener la base de datos").build();
        }
        
        //Conectar a la base de datos correcta
        MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);
        
        
        //verificar si la collecion existe
        MongoIterable<String> colecciones = database.listCollectionNames();
        if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
            return Response.status(Response.Status.NOT_FOUND).entity("La coleccion no existe").build();
        }
        
        //Obtener datos de la coleccion
        MongoCollection<Document> collection = database.getCollection(coleccion);
        
        //eliminar el documento
        Document filtro = new Document("_id", new ObjectId(id));
        DeleteResult resultado = collection.deleteOne(filtro);
        
        if (resultado.getDeletedCount() == 0) {
            return Response.status(Response.Status.NOT_FOUND).entity("Documento no encontrado").build();
        }
        
        return Response.ok("Documento elimado correctamente").build();
    }
}
