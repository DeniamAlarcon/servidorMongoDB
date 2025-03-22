/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import utils.JWTUtils;
import utils.MongoDBUtil;

/**
 *
 * @author deni
 */
@Path("/auth")
public class LoginService {

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(UserCredentials credentials) {
        try {
            // Obtener la base de datos centralizada
            MongoDatabase database = MongoDBUtil.getDatabase("usuarios");
            MongoCollection<Document> userCollection = database.getCollection("users");

            // Buscar el usuario en la base de datos
            Document user = userCollection.find(new Document("correo", credentials.getCorreo())).first();

            if (user != null) {
                // Verificar si la cuenta está bloqueada
                Date bloqueadoHasta = user.getDate("bloqueadoHasta");
                if (bloqueadoHasta != null && bloqueadoHasta.after(new Date())) {
                    Respuestas respuesta = new Respuestas("error", "ACCOUNT_BLOCKING", "Cuenta bloqueada. Intenta de nuevo después de 30 minutos.", 403);
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(respuesta)
                            .build();
                }

                // Verificar la contraseña
                if (validarContrasenia(credentials.getPassword(), user.getString("password"))) {
                    // Reseteamos los intentos fallidos si la contraseña es correcta
                    userCollection.updateOne(new Document("correo", credentials.getCorreo()),
                            new Document("$set", new Document("intentosFallidos", 0).append("bloqueadoHasta", null)));

                    // Obtener la base de datos asignada al usuario
                    String userDatabase = user.getString("baseDatos");

                    // Obtener los roles del usuario
                    List<Document> roles = (List<Document>) user.get("roles");

                    // Generar el token con la base de datos del usuario
                    String token = JWTUtils.generarToken(credentials.getCorreo(), userDatabase);

                    // Crear la respuesta con el token, roles y correo
                    Document response = new Document();
                    response.append("token", token);
                    response.append("roles", roles);
                    response.append("correo", credentials.getCorreo());

                    // Respuesta con el token, roles y correo
                    return Response.ok(response.toJson()).build();
                } else {
                    // Si la contraseña es incorrecta, incrementamos el contador de intentos fallidos
                    int intentosFallidos = user.getInteger("intentosFallidos", 0);
                    intentosFallidos++;

                    if (intentosFallidos >= 5) {
                        // Bloquear la cuenta durante 30 minutos
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, 30);
                        Date bloqueadoHastaFecha = calendar.getTime();

                        userCollection.updateOne(new Document("correo", credentials.getCorreo()),
                                new Document("$set", new Document("intentosFallidos", intentosFallidos)
                                        .append("bloqueadoHasta", bloqueadoHastaFecha)));

                        Respuestas respuesta = new Respuestas("error", "ACCOUNT_BLOCKING", "Cuenta bloqueada. Intenta de nuevo después de 30 minutos.", 403);
                        return Response.status(Response.Status.FORBIDDEN)
                                .entity(respuesta)
                                .build();
                    } else {
                        // Actualizar el contador de intentos fallidos
                        userCollection.updateOne(new Document("correo", credentials.getCorreo()),
                                new Document("$set", new Document("intentosFallidos", intentosFallidos)));

                        Respuestas respuesta = new Respuestas("error", "ACCOUNT_PRECAUTION", "Credenciales inválidas. Intentos restantes: " + (5 - intentosFallidos), 401);
                        return Response.status(Response.Status.UNAUTHORIZED)
                                .entity(respuesta)
                                .build();
                    }
                }
            } else {
                Respuestas respuesta = new Respuestas("error", "ACCOUNT_PRECAUTION", "Credenciales inválidas", 401);
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(respuesta)
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Respuestas respuesta = new Respuestas("error", "INTERNAL_SERVER_ERROR", "Error en el servidor", 500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(respuesta)
                    .build();
        }
    }

    // Método para validar la contraseña (suponiendo que usas algún método de comparación)
    private boolean validarContrasenia(String contrasenia, String hashedPassword) {
        // Implementar la validación de la contraseña, por ejemplo, comparando el hash de las contraseñas
        return contrasenia.equals(hashedPassword); // Esto es solo un ejemplo
    }

    public static class UserCredentials {

        private String correo;
        private String password;

        public String getCorreo() {
            return correo;
        }

        public void setCorreo(String correo) {
            this.correo = correo;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
