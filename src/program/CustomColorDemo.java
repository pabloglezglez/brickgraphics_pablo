package program;

import mosaic.controllers.*;
import colors.LEGOColor;
import io.Log;

/**
 * Programa de ejemplo para demostrar el uso de números personalizados de colores.
 * Ejecuta este programa después de abrir BrickGraphics para personalizar los números.
 */
public class CustomColorDemo {
    
    public static void main(String[] args) {
    Log.log("=== DEMO DE NÚMEROS PERSONALIZADOS DE COLORES ===");
    Log.log("Este programa requiere que BrickGraphics esté ejecutándose");
    Log.log("con una imagen cargada y colores seleccionados.");
    Log.log("");
        
        // Nota: En una implementación real, necesitarías obtener la instancia 
        // del ColorController desde la aplicación principal.
        // Este es un ejemplo de cómo usarías la funcionalidad.
        
        Log.log("COMANDOS DISPONIBLES:");
        Log.log("1. Ver colores actuales:");
        Log.log("   customManager.printCurrentColorAssignments();");
        Log.log("");
        Log.log("2. Asignar número personalizado:");
        Log.log("   customManager.setCustomNumberByColorName(\"Turrón\", 15);");
        Log.log("");
        Log.log("3. Eliminar número personalizado:");
        Log.log("   customManager.removeCustomNumberByColorName(\"Turrón\");");
        Log.log("");
        Log.log("4. Limpiar todos los números:");
        Log.log("   customManager.clearAllCustomNumbers();");
        Log.log("");
        Log.log("Para usar esta funcionalidad, necesitas:");
        Log.log("1. Ejecutar BrickGraphics normalmente");
        Log.log("2. Cargar una imagen y configurar colores");
        Log.log("3. Acceder al ColorController desde la aplicación principal");
        Log.log("4. Crear un CustomColorIDManager y usar sus métodos");
        Log.log("");
        Log.log("Los cambios se reflejarán inmediatamente en la leyenda!");
        Log.log("================================================");
    }
}