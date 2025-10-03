package mosaic.controllers;

import colors.LEGOColor;
import java.util.*;

/**
 * Utilidad para manejar números personalizados de colores.
 * Permite ver y modificar los números asignados a cada color.
 */
public class CustomColorIDManager {
    
    private ColorController colorController;
    
    public CustomColorIDManager(ColorController colorController) {
        this.colorController = colorController;
    }
    
    /**
     * Imprime la lista actual de colores con sus números asignados
     */
    public void printCurrentColorAssignments() {
        System.out.println("=== ASIGNACIÓN ACTUAL DE NÚMEROS DE COLORES ===");
        
        // Obtener colores usados (necesitamos acceso a los colores seleccionados)
        List<LEGOColor> filteredColors = colorController.getFilteredColors();
        
        if (filteredColors != null && !filteredColors.isEmpty()) {
            for (LEGOColor color : filteredColors) {
                String currentID = colorController.getShownID(color);
                String colorName = colorController.getShownName(color);
                Integer customID = colorController.getCustomColorID(color);
                
                System.out.println(String.format("Color: %s | Número actual: %s | Número personalizado: %s", 
                    colorName != null ? colorName : "Sin nombre", 
                    currentID != null ? currentID : "Sin ID",
                    customID != null ? customID.toString() : "No asignado"));
            }
        } else {
            System.out.println("No hay colores filtrados disponibles.");
        }
        System.out.println("=============================================");
    }
    
    /**
     * Establece un número personalizado para un color específico por su nombre
     */
    public boolean setCustomNumberByColorName(String colorName, int customNumber) {
        List<LEGOColor> filteredColors = colorController.getFilteredColors();
        
        if (filteredColors != null) {
            for (LEGOColor color : filteredColors) {
                String currentColorName = colorController.getShownName(color);
                if (currentColorName != null && currentColorName.equalsIgnoreCase(colorName)) {
                    colorController.setCustomColorID(color, customNumber);
                    System.out.println(String.format("✅ Número %d asignado al color '%s'", customNumber, colorName));
                    return true;
                }
            }
        }
        
        System.out.println(String.format("❌ No se encontró el color '%s'", colorName));
        return false;
    }
    
    /**
     * Elimina el número personalizado de un color por su nombre
     */
    public boolean removeCustomNumberByColorName(String colorName) {
        List<LEGOColor> filteredColors = colorController.getFilteredColors();
        
        if (filteredColors != null) {
            for (LEGOColor color : filteredColors) {
                String currentColorName = colorController.getShownName(color);
                if (currentColorName != null && currentColorName.equalsIgnoreCase(colorName)) {
                    colorController.removeCustomColorID(color);
                    System.out.println(String.format("✅ Número personalizado eliminado del color '%s'", colorName));
                    return true;
                }
            }
        }
        
        System.out.println(String.format("❌ No se encontró el color '%s'", colorName));
        return false;
    }
    
    /**
     * Elimina todos los números personalizados
     */
    public void clearAllCustomNumbers() {
        colorController.clearAllCustomColorIDs();
        System.out.println("✅ Todos los números personalizados han sido eliminados");
    }
    
    /**
     * Método de ejemplo para establecer algunos números personalizados comunes
     */
    public void setExampleCustomNumbers() {
        // Ejemplos de números personalizados que podrías querer usar
        setCustomNumberByColorName("Blanco", 1);
        setCustomNumberByColorName("Negro", 2);
        setCustomNumberByColorName("Rojo", 3);
        setCustomNumberByColorName("Azul", 4);
        setCustomNumberByColorName("Amarillo", 5);
        setCustomNumberByColorName("Verde", 6);
        setCustomNumberByColorName("Turrón", 15); // Tu color personalizado
        
        System.out.println("✅ Números de ejemplo establecidos");
    }
}