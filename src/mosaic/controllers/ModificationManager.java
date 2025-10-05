package mosaic.controllers;

import java.util.HashMap;
import java.util.Map;

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
    
    /**
     * Registra una modificación manual en una coordenada específica.
     */
    public void recordModification(int x, int y, LEGOColor newColor, LEGOColor originalColor) {
        String key = x + "," + y;
        
        if (originalColor != null && originalColor.equals(newColor)) {
            // Si volvemos al color original, eliminamos la modificación
            modifications.remove(key);
            System.out.println("DEBUG: ModificationManager - Removida modificación en (" + x + "," + y + ")");
        } else {
            // Registramos la nueva modificación
            modifications.put(key, newColor);
            System.out.println("DEBUG: ModificationManager - Registrada modificación en (" + x + "," + y + ") -> " + newColor.toString());
        }
    }
    
    /**
     * Hace backup del grid actual antes de que se regenere.
     */
    public void backupGrid(LEGOColorGrid grid) {
        if (grid != null && !modifications.isEmpty()) {
            originalGrid = grid;
            System.out.println("DEBUG: ModificationManager - Backup realizado. Modificaciones: " + modifications.size());
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
        
        System.out.println("DEBUG: ModificationManager - Restauradas " + restored + " de " + modifications.size() + " modificaciones");
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
        System.out.println("DEBUG: ModificationManager - Todas las modificaciones limpiadas");
    }
    
    /**
     * Obtiene el color modificado en una coordenada específica, o null si no hay modificación.
     */
    public LEGOColor getModificationAt(int x, int y) {
        return modifications.get(x + "," + y);
    }
}