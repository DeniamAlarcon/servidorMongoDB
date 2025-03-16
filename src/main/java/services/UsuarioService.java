/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import com.auth0.jwt.exceptions.JWTVerificationException;
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
import utils.LogUtil;
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
        String baseDatos = null;
        try {
            // Obtener token y la base de datos
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                Respuestas respuesta = new Respuestas("error", "TOKEN_MISSING", "Falta el token", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            }

            baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
            if (baseDatos == null) {
                Respuestas respuesta = new Respuestas("error", "BASE_DE_DATOS_NO_ENCONTRADA", "No se pudo obtener la base de datos", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            } else {
                if (baseDatos.contains("expirado")) {
                    Respuestas respuesta = new Respuestas("error", "TOKEN_EXPIRADO", "El token ha expirado", 401);
                    return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                } else {
                    if (baseDatos.contains("inválido")) {
                        Respuestas respuesta = new Respuestas("error", "TOKEN_INVALIDO", "El token es inválido", 401);
                        return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                    }
                }
            }
            // Conectar a la base de datos correcta
            MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);

            // Verificar si la colección existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                Respuestas respuesta = new Respuestas("error", "COLECCION_NO_ENCONTRADA", "La colección no existe", 404);
                return Response.status(Response.Status.NOT_FOUND).entity(respuesta).build();
            }

            // Obtener datos de la colección
            MongoCollection<Document> collection = database.getCollection(coleccion);

            // Crear filtro dinámico
            Document filtro = new Document();
            MultivaluedMap<String, String> parametros = uriInfo.getQueryParameters();

            for (String key : parametros.keySet()) {
                filtro.append(key, parametros.getFirst(key));  // Añadir filtro para cada parámetro
            }

            // Obtener los datos con el filtro
            FindIterable<Document> datos = collection.find(filtro);

            // Procesar los documentos obtenidos
            List<Document> listaDatos = new ArrayList<>();
            for (Document doc : datos) {
                doc.put("_id", doc.getObjectId("_id").toHexString());  // Convertir el _id a string
                listaDatos.add(doc);
            }

            return Response.ok(listaDatos).build();
        } catch (Exception e) {
            LogUtil.logError("Error al obtener datos en la colección: " + coleccion, e, baseDatos != null ? baseDatos : "Desconocida");
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error al obtener los datos", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(respuesta).build();
        }
    }

    @POST
    @Path("/{coleccion}/filtrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerDatosconFiltro(
            @PathParam("coleccion") String coleccion,
            Document filtros,
            @Context HttpHeaders headers) {

        String baseDatos = null;  // Declaramos baseDatos fuera del try para que se pueda usar en el catch

        try {
            // Obtener token y la base de datos
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                Respuestas respuesta = new Respuestas("error", "TOKEN_MISSING", "Falta el token", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            }

            baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
            if (baseDatos == null) {
                Respuestas respuesta = new Respuestas("error", "BASE_DE_DATOS_NO_ENCONTRADA", "No se pudo obtener la base de datos", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            } else {
                if (baseDatos.contains("expirado")) {
                    Respuestas respuesta = new Respuestas("error", "TOKEN_EXPIRADO", "El token ha expirado", 401);
                    return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                } else {
                    if (baseDatos.contains("inválido")) {
                        Respuestas respuesta = new Respuestas("error", "TOKEN_INVALIDO", "El token es inválido", 401);
                        return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                    }
                }
            }

            // Conectar a la base de datos correcta
            MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);

            // Verificar si la colección existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                Respuestas respuesta = new Respuestas("error", "COLECCION_NO_ENCONTRADA", "La colección no existe", 404);
                return Response.status(Response.Status.NOT_FOUND).entity(respuesta).build();
            }

            MongoCollection<Document> collection = database.getCollection(coleccion);

            // Obtener datos con los filtros proporcionados
            FindIterable<Document> datos = collection.find(filtros);

            List<Document> listaDatos = new ArrayList<>();
            for (Document doc : datos) {
                doc.put("_id", doc.getObjectId("_id").toHexString());
                listaDatos.add(doc);
            }

            return Response.ok(listaDatos).build();

        } catch (Exception e) {
            // Registrar el error
            if (baseDatos != null) {
                LogUtil.logError("Error al obtener datos con filtro en la colección: " + coleccion, e, baseDatos);
            } else {
                LogUtil.logError("Error al obtener datos con filtro en la colección: " + coleccion, e, "Desconocida");
            }
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error al obtener los datos", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(respuesta).build();
        }
    }

    @POST
    @Path("/{coleccion}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response agregarDato(
            @PathParam("coleccion") String coleccion,
            Document nuevoDato,
            @Context HttpHeaders headers) {

        String baseDatos = null;  // Declaramos fuera del bloque try para que sea accesible en el bloque catch

        try {
            // Obtener token y la base de datos
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                Respuestas respuesta = new Respuestas("error", "TOKEN_MISSING", "Falta el token", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            }

            baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
            if (baseDatos == null) {
                Respuestas respuesta = new Respuestas("error", "BASE_DE_DATOS_NO_ENCONTRADA", "No se pudo obtener la base de datos", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            } else {
                if (baseDatos.contains("expirado")) {
                    Respuestas respuesta = new Respuestas("error", "TOKEN_EXPIRADO", "El token ha expirado", 401);
                    return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                } else {
                    if (baseDatos.contains("inválido")) {
                        Respuestas respuesta = new Respuestas("error", "TOKEN_INVALIDO", "El token es inválido", 401);
                        return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                    }
                }
            }

            // Conectar a la base de datos correcta
            MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);

            // Verificar si la colección existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                Respuestas respuesta = new Respuestas("error", "COLECCION_NO_ENCONTRADA", "La colección no existe", 404);
                return Response.status(Response.Status.NOT_FOUND).entity(respuesta).build();
            }

            // Obtener datos de la colección
            MongoCollection<Document> collection = database.getCollection(coleccion);

            // Insertar el documento
            collection.insertOne(nuevoDato);

            Respuestas respuesta = new Respuestas("succes", "Documento agregado correctamente a la colección: " + coleccion, 200);
            return Response.ok(respuesta).build();
        } catch (Exception e) {
            // Registrar el error
            if (baseDatos != null) {
                LogUtil.logError("Error al agregar documento en la colección: " + coleccion, e, baseDatos);
            } else {
                LogUtil.logError("Error al agregar documento en la colección: " + coleccion, e, "Desconocida");
            }
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error al agregar el documento", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(respuesta).build();
        }
    }

    @PUT
    @Path("/{coleccion}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarDocumento(
            @PathParam("coleccion") String coleccion,
            @PathParam("id") String id,
            Document nuevoDato,
            @Context HttpHeaders headers) {

        String baseDatos = null;  // Declarar fuera del bloque try para que sea accesible en el bloque catch.

        // Validar si el ID tiene un formato correcto
        if (!ObjectId.isValid(id)) {
            Respuestas respuesta = new Respuestas("error", "ID_INVALIDO", "El ID proporcionado no es válido", 400);
            return Response.status(Response.Status.BAD_REQUEST).entity(respuesta).build();
        }

        try {
            // Obtener token y la base de datos
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                Respuestas respuesta = new Respuestas("error", "TOKEN_MISSING", "Falta el token", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            }

            baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
            if (baseDatos == null) {
                Respuestas respuesta = new Respuestas("error", "BASE_DE_DATOS_NO_ENCONTRADA", "No se pudo obtener la base de datos", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            } else {
                if (baseDatos.contains("expirado")) {
                    Respuestas respuesta = new Respuestas("error", "TOKEN_EXPIRADO", "El token ha expirado", 401);
                    return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                } else {
                    if (baseDatos.contains("inválido")) {
                        Respuestas respuesta = new Respuestas("error", "TOKEN_INVALIDO", "El token es inválido", 401);
                        return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                    }
                }
            }

            // Conectar a la base de datos correcta
            MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);

            // Verificar si la colección existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                Respuestas respuesta = new Respuestas("error", "COLECCION_NO_ENCONTRADA", "La colección no existe", 404);
                return Response.status(Response.Status.NOT_FOUND).entity(respuesta).build();
            }

            // Obtener datos de la colección
            MongoCollection<Document> collection = database.getCollection(coleccion);

            // Crear la actualización
            Document updateQuery = new Document("$set", nuevoDato);
            Document filtro = new Document("_id", new ObjectId(id));

            // Aplicar la actualización
            UpdateResult resultado = collection.updateOne(filtro, updateQuery);
            if (resultado.getMatchedCount() <= 0) {
                Respuestas respuesta = new Respuestas("error", "DOCUMENTO_NO_ENCONTRADA", "El documento no existe", 404);
                return Response.status(Response.Status.NOT_FOUND).entity(respuesta).build();
            }

            Respuestas respuesta = new Respuestas("succes", "Documento actualizado correctament", 200);
            return Response.ok(respuesta).build();
        } catch (Exception e) {
            // Registrar el error
            if (baseDatos != null) {
                LogUtil.logError("Error al actualizar documento en la colección: " + coleccion, e, baseDatos);
            } else {
                LogUtil.logError("Error al actualizar documento en la colección: " + coleccion, e, "Desconocida");
            }
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error al actualizar el documento", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(respuesta).build();
        }
    }

    @DELETE
    @Path("/{coleccion}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminarDocumento(
            @PathParam("coleccion") String coleccion,
            @PathParam("id") String id,
            @Context HttpHeaders headers) {

        String baseDatos = null;  // Declarar fuera del bloque try para que sea accesible en el bloque catch.
        // Validar si el ID tiene un formato correcto
        if (!ObjectId.isValid(id)) {
            Respuestas respuesta = new Respuestas("error", "ID_INVALIDO", "El ID proporcionado no es válido", 400);
            return Response.status(Response.Status.BAD_REQUEST).entity(respuesta).build();
        }

        try {
            // Obtener token y la base de datos
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                Respuestas respuesta = new Respuestas("error", "TOKEN_MISSING", "Falta el token", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            }

            baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
            if (baseDatos == null) {
                Respuestas respuesta = new Respuestas("error", "BASE_DE_DATOS_NO_ENCONTRADA", "No se pudo obtener la base de datos", 401);
                return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
            } else {
                if (baseDatos.contains("expirado")) {
                    Respuestas respuesta = new Respuestas("error", "TOKEN_EXPIRADO", "El token ha expirado", 401);
                    return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                } else {
                    if (baseDatos.contains("inválido")) {
                        Respuestas respuesta = new Respuestas("error", "TOKEN_INVALIDO", "El token es inválido", 401);
                        return Response.status(Response.Status.UNAUTHORIZED).entity(respuesta).build();
                    }
                }
            }

            // Conectar a la base de datos correcta
            MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);

            // Verificar si la colección existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                Respuestas respuesta = new Respuestas("error", "COLECCION_NO_ENCONTRADA", "La colección no existe", 404);
                return Response.status(Response.Status.NOT_FOUND).entity(respuesta).build();
            }

            // Obtener datos de la colección
            MongoCollection<Document> collection = database.getCollection(coleccion);

            // Eliminar el documento
            Document filtro = new Document("_id", new ObjectId(id));
            DeleteResult resultado = collection.deleteOne(filtro);

            if (resultado.getDeletedCount() == 0) {
                Respuestas respuesta = new Respuestas("error", "DOCUMENTO_NO_ENCONTRADA", "El documento no existe", 404);
                return Response.status(Response.Status.NOT_FOUND).entity(respuesta).build();
            }

            Respuestas respuesta = new Respuestas("succes", "Documento eliminado correctamente", 200);
            return Response.ok(respuesta).build();
        } catch (Exception e) {
            // Registrar el error
            if (baseDatos != null) {
                LogUtil.logError("Error al eliminar documento en la colección: " + coleccion, e, baseDatos);
            } else {
                LogUtil.logError("Error al eliminar documento en la colección: " + coleccion, e, "Desconocida");
            }
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error al eliminar el documento", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(respuesta).build();
        }
    }

}
