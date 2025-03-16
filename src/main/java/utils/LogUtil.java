/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

/**
 *
 * @author deni
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class LogUtil {
    
   private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);

    // Método para registrar errores en la base de datos
    public static void logError(String mensajeError, Exception exception, String baseDatos) {
        logger.error("Error en la base de datos: {}, Mensaje: {}, Detalles: {}", baseDatos, mensajeError, exception.getMessage());
    }
    
}
