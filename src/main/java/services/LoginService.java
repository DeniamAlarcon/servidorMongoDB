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
            //obtener la base de datos cenralizada
            MongoDatabase database = MongoDBUtil.getDatabase("usuarios");
            MongoCollection<Document> userCollection = database.getCollection("users");

            //Buscar el usuario en la base de datos
            Document user = userCollection.find(new Document("correo", credentials.getCorreo())
                    .append("password", credentials.getPassword())).first();

            if (user != null) {
                //Obtener la base de datos asignada a este usuario
                String userDatabase = user.getString("baseDatos");

                //Genear el token con la base de datos del usuario
                String token = JWTUtils.generarToken(credentials.getCorreo(), userDatabase);

                //Respuesta con el token
                return Response.ok(new Document("token", token).toJson()).build();

            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new Document("error", "Credenciales invalidas"))
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Document("error","error en el servidor").toJson())
                    .build();
        }
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
