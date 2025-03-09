/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;

/**
 *
 * @author deni
 */
public class JWTUtils {
    private static final String SECRET_KEY ="DsAA2810";
    private static final long EXPIRATION_TIME = 86400000; //1 dia de expiracion
    
    //Generar token JWT
    public static String generarToken(String usuario, String baseDatos) {
        return JWT.create()
                .withSubject(usuario)
                .withClaim("baseDatos", baseDatos)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }
    
    //Verificar token JWT y obtner la base de datos asociada
    public static String obtenerBaseDeDatosDesdeToken(String token){
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET_KEY)).build();
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaim("baseDatos").asString();
        } catch (Exception e) {
            System.out.println("No verifico bien");
            return null;
        }
    }
}
