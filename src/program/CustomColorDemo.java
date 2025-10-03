package program;

import mosaic.controllers.*;
import colors.LEGOColor;

/**
 * Programa de ejemplo para demostrar el uso de números personalizados de colores.
 * Ejecuta este programa después de abrir BrickGraphics para personalizar los números.
 */
public class CustomColorDemo {
    
    public static void main(String[] args) {
        System.out.println("=== DEMO DE NÚMEROS PERSONALIZADOS DE COLORES ===");
        System.out.println("Este programa requiere que BrickGraphics esté ejecutándose");
        System.out.println("con una imagen cargada y colores seleccionados.");
        System.out.println("");
        
        // Nota: En una implementación real, necesitarías obtener la instancia 
        // del ColorController desde la aplicación principal.
        // Este es un ejemplo de cómo usarías la funcionalidad.
        
        System.out.println("COMANDOS DISPONIBLES:");
        System.out.println("1. Ver colores actuales:");
        System.out.println("   customManager.printCurrentColorAssignments();");
        System.out.println("");
        System.out.println("2. Asignar número personalizado:");
        System.out.println("   customManager.setCustomNumberByColorName(\"Turrón\", 15);");
        System.out.println("");
        System.out.println("3. Eliminar número personalizado:");
        System.out.println("   customManager.removeCustomNumberByColorName(\"Turrón\");");
        System.out.println("");
        System.out.println("4. Limpiar todos los números:");
        System.out.println("   customManager.clearAllCustomNumbers();");
        System.out.println("");
        System.out.println("Para usar esta funcionalidad, necesitas:");
        System.out.println("1. Ejecutar BrickGraphics normalmente");
        System.out.println("2. Cargar una imagen y configurar colores");
        System.out.println("3. Acceder al ColorController desde la aplicación principal");
        System.out.println("4. Crear un CustomColorIDManager y usar sus métodos");
        System.out.println("");
        System.out.println("Los cambios se reflejarán inmediatamente en la leyenda!");
        System.out.println("================================================");
    }
}