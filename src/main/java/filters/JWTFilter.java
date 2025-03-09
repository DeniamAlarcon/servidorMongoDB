/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filters;

import com.mongodb.RequestContext;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import utils.JWTUtils;


/**
 *
 * @author deni
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTFilter implements ContainerRequestFilter{

    @Override
    public void filter(ContainerRequestContext crc) throws IOException {
        //obtener el encabezado "Authorization"
        String authorizationHeader = crc.getHeaderString(HttpHeaders.AUTHORIZATION);
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            crc.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .entity("Falta el token de autorizacion").build());
            return;
        }
        
        //Extraer el token
        String token = authorizationHeader.substring("Bearer ".length());
        
        //obtener la base de datos desde el token
        String databaseName = JWTUtils.obtenerBaseDeDatosDesdeToken(token);
        
        if (databaseName == null) {
            crc.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .entity("Token invalido o expirado").build());
        } else {
            //Guardar la  base de dato en el contexto de la peticion para que los servicios la usen
            crc.setProperty("baseDatos", databaseName);
        }
    }
    
}
