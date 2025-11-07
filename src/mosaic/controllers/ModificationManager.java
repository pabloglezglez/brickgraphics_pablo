package mosaic.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import colors.LEGOColor;
import colors.LEGOColorGrid;

/**
 * Gestiona las modificaciones manuales hechas con las herramientas de edición.
 * Preserva automáticamente las modificaciones cuando el grid se regenera.
 */
public class ModificationManager {
    // Mapa que guarda las modificaciones: coordenada -> color modificado
    private Map<String, LEGOColor> modifications = new HashMap<>();
    private LEGOColorGrid originalGrid = null;
    private ColorController colorController; // Referencia para convertir IDs a colores
    
    public ModificationManager() {
        // Constructor por defecto
    }
    
    public ModificationManager(ColorController colorController) {
        this.colorController = colorController;
    }
    
    public void setColorController(ColorController colorController) {
        this.colorController = colorController;
    }
    
    /**
     * Registra una modificación manual en una coordenada específica.
     */
    public void recordModification(int x, int y, LEGOColor newColor, LEGOColor originalColor) {
        String key = x + "," + y;
        
        if (originalColor != null && originalColor.equals(newColor)) {
            // Si volvemos al color original, eliminamos la modificación
            modifications.remove(key);
            io.Log.log("DEBUG: ModificationManager - Removida modificación en (" + x + "," + y + ")");
        } else {
            // Registramos la nueva modificación
            modifications.put(key, newColor);
            io.Log.log("DEBUG: ModificationManager - Registrada modificación en (" + x + "," + y + ") -> " + newColor.toString());
        }
    }
    
    /**
     * Hace backup del grid actual antes de que se regenere.
     */
    public void backupGrid(LEGOColorGrid grid) {
        if (grid != null && !modifications.isEmpty()) {
            originalGrid = grid;
            io.Log.log("DEBUG: ModificationManager - Backup realizado. Modificaciones: " + modifications.size());
        }
    }
    
    /**
     * Restaura todas las modificaciones en el nuevo grid regenerado.
     */
    public void restoreModifications(LEGOColorGrid newGrid) {
        if (newGrid == null || modifications.isEmpty()) {
            return;
        }
        
        int restored = 0;
        for (Map.Entry<String, LEGOColor> entry : modifications.entrySet()) {
            String[] coords = entry.getKey().split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            LEGOColor color = entry.getValue();
            
            if (x >= 0 && x < newGrid.getWidth() && y >= 0 && y < newGrid.getHeight()) {
                if (newGrid.setColorAt(x, y, color)) {
                    restored++;
                }
            }
        }
        
    io.Log.log("DEBUG: ModificationManager - Restauradas " + restored + " de " + modifications.size() + " modificaciones");
    }
    
    /**
     * Verifica si hay modificaciones pendientes.
     */
    public boolean hasModifications() {
        return !modifications.isEmpty();
    }
    
    /**
     * Obtiene el número de modificaciones.
     */
    public int getModificationCount() {
        return modifications.size();
    }
    
    /**
     * Limpia todas las modificaciones registradas.
     */
    public void clearModifications() {
    modifications.clear();
    originalGrid = null;
    io.Log.log("DEBUG: ModificationManager - Todas las modificaciones limpiadas");
    }
    
    /**
     * Obtiene el color modificado en una coordenada específica, o null si no hay modificación.
     */
    public LEGOColor getModificationAt(int x, int y) {
        return modifications.get(x + "," + y);
    }
    
    /**
     * Convierte las modificaciones a un formato serializable (coordenada → colorID).
     */
    public Map<String, Integer> getModificationsForSave() {
        Map<String, Integer> saveMap = new TreeMap<>();
        for (Map.Entry<String, LEGOColor> entry : modifications.entrySet()) {
            String coordinates = entry.getKey();
            LEGOColor color = entry.getValue();
            if (coordinates != null && color != null) {
                // Obtener el ID del color como String y luego convertir a Integer
                String colorIDStr = String.valueOf(color.getIDRebrickable());
                try {
                    Integer colorID = Integer.valueOf(colorIDStr);
                    saveMap.put(coordinates, colorID);
                    io.Log.log("DEBUG: ModificationManager - Agregando al mapa: " + coordinates + " -> " + colorID);
                } catch (NumberFormatException e) {
                    io.Log.log("ERROR: ModificationManager - No se puede convertir '" + colorIDStr + "' a Integer para coordenada " + coordinates);
                    continue;
                }
            } else {
                io.Log.log("WARNING: ModificationManager - Entrada inválida: coordinates=" + coordinates + ", color=" + color);
            }
        }
        
    io.Log.log("DEBUG: ModificationManager - Retornando " + saveMap.size() + " modificaciones válidas para guardado");
        return saveMap;
    }
    
    /**
     * Carga las modificaciones desde un formato serializable.
     */
    public void loadModificationsFromSave(Map<String, Integer> saveMap) {
        modifications.clear();
        if (colorController == null || saveMap == null || saveMap.isEmpty()) {
            return;
        }
        
        // Obtener todos los colores disponibles del controlador
        List<LEGOColor> availableColors = colorController.getColorsFromDisk();
        
        for (Map.Entry<String, Integer> entry : saveMap.entrySet()) {
            String coordinates = entry.getKey();
            Object colorIDValue = entry.getValue();
            
            // Conversión robusta de String/Integer a int
            int colorID;
            try {
                if (colorIDValue instanceof String) {
                    colorID = Integer.parseInt((String) colorIDValue);
                    io.Log.log("DEBUG: ModificationManager - Convertido String a Integer: " + colorIDValue + " -> " + colorID);
                } else if (colorIDValue instanceof Integer) {
                    colorID = ((Integer) colorIDValue).intValue();
                } else {
                    io.Log.log("ERROR: ModificationManager - Tipo de dato inesperado para colorID: " + colorIDValue.getClass() + " valor: " + colorIDValue);
                    continue;
                }
            } catch (Exception e) {
                io.Log.log("ERROR: ModificationManager - No se puede convertir a Integer: " + colorIDValue + " error: " + e.getMessage());
                continue;
            }
            
            // Buscar el color por su ID Rebrickable
            LEGOColor foundColor = null;
            for (LEGOColor color : availableColors) {
                if (color.getIDRebrickable() == colorID) {
                    foundColor = color;
                    break;
                }
            }
            
            if (foundColor != null) {
                modifications.put(coordinates, foundColor);
                io.Log.log("DEBUG: ModificationManager - Cargada modificación en " + coordinates + " -> " + foundColor.toString());
            } else {
                io.Log.log("WARNING: ModificationManager - Color con ID " + colorID + " no encontrado");
            }
        }
    }
}