/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoWriteException;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        String baseDatos = null;
        try {
            // Obtener token y validar
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "TOKEN_MISSING", "Falta el token", 401))
                        .build();
            }

            baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
            if (baseDatos == null || baseDatos.contains("expirado") || baseDatos.contains("inválido")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "TOKEN_INVALIDO", "El token es inválido o ha expirado", 401))
                        .build();
            }

            // Conectar a la base de datos correcta
            MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);

            // Verificar si la colección existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new Respuestas("error", "COLECCION_NO_ENCONTRADA", "La colección no existe", 404))
                        .build();
            }

            // Obtener la colección
            MongoCollection<Document> collection = database.getCollection(coleccion);

            // Crear filtro dinámico con operadores de comparación
            Document filtro = construirFiltro(uriInfo.getQueryParameters());

            // Obtener documentos
            FindIterable<Document> datos = collection.find(filtro);

            // Convertir documentos a lista
            List<Document> listaDatos = new ArrayList<>();
            for (Document doc : datos) {
                if (doc.containsKey("_id") && doc.get("_id") instanceof ObjectId) {
                    doc.put("_id", doc.getObjectId("_id").toHexString());  // Convertir _id a string
                }
                listaDatos.add(doc);
            }

            if (listaDatos.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            return Response.ok(listaDatos).build();
        } catch (MongoException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Respuestas("error", "DATABASE_ERROR", "Error de conexión con la base de datos", 500))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error inesperado", 500))
                    .build();
        }
    }

    /**
     * Construye el filtro MongoDB a partir de los parámetros de la URL.
     */
    private Document construirFiltro(MultivaluedMap<String, String> parametros) {
        Document filtro = new Document();

        for (String key : parametros.keySet()) {
            String valor = parametros.getFirst(key);
            Object valorConvertido = convertirValor(valor);

            if (key.endsWith("_gt")) {
                filtro.append(key.replace("_gt", ""), new Document("$gt", valorConvertido));
            } else if (key.endsWith("_gte")) {
                filtro.append(key.replace("_gte", ""), new Document("$gte", valorConvertido));
            } else if (key.endsWith("_lt")) {
                filtro.append(key.replace("_lt", ""), new Document("$lt", valorConvertido));
            } else if (key.endsWith("_lte")) {
                filtro.append(key.replace("_lte", ""), new Document("$lte", valorConvertido));
            } else if (key.equals("_id")) {
                try {
                    filtro.append("_id", new ObjectId(valor));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("ID inválido: " + valor);
                }
            } else {
                filtro.append(key, valorConvertido);
            }
        }

        return filtro;
    }

    /**
     * Convierte valores de String a Integer, Double o mantiene como String.
     */
    private Object convertirValor(String valor) {
        if (valor.matches("-?\\d+")) {
            return Integer.parseInt(valor); // Entero
        } else if (valor.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(valor); // Decimal
        } else {
            return valor; // String
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

        String baseDatos = null;

        try {
            // Validación del token
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "TOKEN_MISSING", "Falta el token", 401)).build();
            }

            baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
            if (baseDatos == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "BASE_DE_DATOS_NO_ENCONTRADA", "No se pudo obtener la base de datos", 401)).build();
            }

            if (baseDatos.contains("expirado")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "TOKEN_EXPIRADO", "El token ha expirado", 401)).build();
            }
            if (baseDatos.contains("inválido")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "TOKEN_INVALIDO", "El token es inválido", 401)).build();
            }

            // Validación del cuerpo de la solicitud
            if (filtros == null || filtros.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new Respuestas("error", "FILTRO_INVALIDO", "El cuerpo de la solicitud no puede estar vacío", 400)).build();
            }

            // Validar operadores permitidos en los filtros
            if (!isValidFilter(filtros)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new Respuestas("error", "FILTRO_INVALIDO", "Operadores de filtro no permitidos", 400)).build();
            }

            // Conectar a la base de datos
            MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);

            // Verificar si la colección existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new Respuestas("error", "COLECCION_NO_ENCONTRADA", "La colección no existe", 404)).build();
            }

            MongoCollection<Document> collection = database.getCollection(coleccion);

            // Ejecutar la consulta
            FindIterable<Document> datos = collection.find(filtros);
            List<Document> listaDatos = new ArrayList<>();

            for (Document doc : datos) {
                doc.put("_id", doc.getObjectId("_id").toHexString());
                listaDatos.add(doc);
            }

            // Si no hay resultados, devolver un 204
            if (listaDatos.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            return Response.ok(listaDatos).build();

        } catch (MongoTimeoutException e) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT)
                    .entity(new Respuestas("error", "DB_TIMEOUT", "Tiempo de espera agotado al conectar con la base de datos", 504)).build();
        } catch (MongoException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Respuestas("error", "DB_ERROR", "Error en la base de datos", 500)).build();
        } catch (Exception e) {
            //LogUtil.logError("Error al obtener datos con filtro en la colección: " + coleccion, e, baseDatos != null ? baseDatos : "Desconocida");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error al obtener los datos", 500)).build();
        }
    }

// Método para validar si los filtros contienen operadores permitidos
    private boolean isValidFilter(Document filtros) {
        for (String key : filtros.keySet()) {
            Object value = filtros.get(key);

            // Comprobar si el valor es un Document, lo que indica un operador de MongoDB
            if (value instanceof Document) {
                Document doc = (Document) value;
                // Comprobar si contiene operadores válidos de MongoDB
                for (String operator : doc.keySet()) {
                    if (!isValidMongoOperator(operator)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

// Método para verificar si un operador MongoDB es válido
    private boolean isValidMongoOperator(String operator) {
        // Lista de operadores MongoDB permitidos
        List<String> validOperators = Arrays.asList("$gt", "$gte", "$lt", "$lte", "$ne", "$in", "$nin", "$regex", "$or", "$and", "$nor");
        return validOperators.contains(operator);
    }

    @POST
    @Path("/{coleccion}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response agregarDato(
            @PathParam("coleccion") String coleccion,
            List<Document> documentos, // Aceptamos una lista de documentos
            @Context HttpHeaders headers) {

        String baseDatos = null;

        try {
            // Obtener token y la base de datos
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "TOKEN_MISSING", "Falta el token", 401))
                        .build();
            }

            baseDatos = JWTUtils.obtenerBaseDeDatosDesdeToken(token.substring(7));
            if (baseDatos == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "BASE_DE_DATOS_NO_ENCONTRADA", "No se pudo obtener la base de datos", 401))
                        .build();
            }
            if (baseDatos.contains("expirado")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "TOKEN_EXPIRADO", "El token ha expirado", 401))
                        .build();
            }
            if (baseDatos.contains("inválido")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Respuestas("error", "TOKEN_INVALIDO", "El token es inválido", 401))
                        .build();
            }

            // Conectar a la base de datos correcta
            MongoDatabase database = MongoDBUtil.getDatabase(baseDatos);
            if (database == null) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(new Respuestas("error", "CONEXION_FALLIDA", "No se pudo conectar a la base de datos", 503))
                        .build();
            }

            // Verificar si la colección existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (!StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new Respuestas("error", "COLECCION_NO_ENCONTRADA", "La colección no existe", 404))
                        .build();
            }

            // Obtener la colección
            MongoCollection<Document> collection = database.getCollection(coleccion);
            if (collection == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new Respuestas("error", "COLECCION_ERROR", "Error al obtener la colección", 500))
                        .build();
            }

            // Validar que los documentos no estén vacíos
            if (documentos == null || documentos.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new Respuestas("error", "DOCUMENTO_VACIO", "No se recibieron documentos", 400))
                        .build();
            }

            // Insertar documentos en la colección
            if (documentos.size() == 1) {
                collection.insertOne(documentos.get(0));
            } else {
                collection.insertMany(documentos);
            }

            return Response.ok(new Respuestas("success", "Documentos agregados correctamente", 200)).build();
        } catch (MongoException e) {
            //LogUtil.logError("Error en MongoDB al agregar documentos en la colección: " + coleccion, e, baseDatos);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Respuestas("error", "MONGO_ERROR", "Error en la base de datos", 500))
                    .build();
        } catch (Exception e) {
            //LogUtil.logError("Error inesperado al agregar documentos en la colección: " + coleccion, e, baseDatos);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error al agregar los documentos", 500))
                    .build();
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

            if (nuevoDato == null || nuevoDato.isEmpty()) {
                Respuestas respuesta = new Respuestas("error", "DATOS_INVALIDOS", "No se proporcionaron datos para actualizar", 400);
                return Response.status(Response.Status.BAD_REQUEST).entity(respuesta).build();
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
                //LogUtil.logError("Error al actualizar documento en la colección: " + coleccion, e, baseDatos);
            } else {
                //LogUtil.logError("Error al actualizar documento en la colección: " + coleccion, e, "Desconocida");
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
                // LogUtil.logError("Error al eliminar documento en la colección: " + coleccion);
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
                //LogUtil.logError("Error al eliminar documento en la colección: " + coleccion, e, baseDatos);
            } else {
                //LogUtil.logError("Error al eliminar documento en la colección: " + coleccion, e, "Desconocida");
            }
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Ocurrió un error al eliminar el documento", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(respuesta).build();
        }
    }

    @POST
    @Path("/crearColeccion/{coleccion}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)  // Aceptar cuerpo en JSON
    public Response crearColeccion(
            @PathParam("coleccion") String coleccion,
            @Context HttpHeaders headers,
            List<Map<String, Object>> documentos) {  // Parámetro para recibir los documentos en el cuerpo
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

            // Verificar si la colección ya existe
            MongoIterable<String> colecciones = database.listCollectionNames();
            if (StreamSupport.stream(colecciones.spliterator(), false).anyMatch(c -> c.equals(coleccion))) {
                Respuestas respuesta = new Respuestas("error", "COLECCION_EXISTENTE", "La colección ya existe", 400);
                return Response.status(Response.Status.BAD_REQUEST).entity(respuesta).build();
            }

            // Crear la nueva colección
            database.createCollection(coleccion);

            // Verificar si se enviaron documentos en el cuerpo
            if (documentos != null && !documentos.isEmpty()) {
                // Insertar documentos en la nueva colección
                MongoCollection<Document> collection = database.getCollection(coleccion);
                List<Document> documentosMongo = documentos.stream()
                        .map(doc -> new Document(doc)) // Convertir cada mapa en un documento de Mongo
                        .collect(Collectors.toList());
                collection.insertMany(documentosMongo);
            }

            // Respuesta exitosa
            Respuestas respuesta = new Respuestas("success", "Colección creada correctamente", 201);
            return Response.status(Response.Status.CREATED).entity(respuesta).build();
        } catch (Exception e) {
            //LogUtil.logError("Error al crear la colección: " + coleccion, e, baseDatos != null ? baseDatos : "Desconocida");
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Error al crear la colección", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(respuesta).build();
        }
    }

    @DELETE
    @Path("/eliminarColeccion/{coleccion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminarColeccion(
            @PathParam("coleccion") String coleccion,
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
                Respuestas respuesta = new Respuestas("error", "COLECCION_NO_EXISTE", "La colección no existe", 404);
                return Response.status(Response.Status.NOT_FOUND).entity(respuesta).build();
            }

            // Eliminar la colección
            database.getCollection(coleccion).drop();

            // Respuesta exitosa
            Respuestas respuesta = new Respuestas("success", "Colección eliminada correctamente", 200);
            return Response.status(Response.Status.OK).entity(respuesta).build();
        } catch (Exception e) {
            //LogUtil.logError("Error al eliminar la colección: " + coleccion, e, baseDatos != null ? baseDatos : "Desconocida");
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Error al eliminar la colección", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(respuesta).build();
        }
    }
}
