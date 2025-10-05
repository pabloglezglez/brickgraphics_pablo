package mosaic.controllers;

import mosaic.ui.EditTool;
import mosaic.ui.BrushSize;
import colors.LEGOColor;
import colors.LEGOColorGrid;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Controlador para las herramientas de edición del mosaico.
 * Maneja el cambio de herramientas activas y las operaciones de edición.
 */
public class StudEditController {
    private EditTool activeTool;
    private LEGOColor selectedColor;
    private List<ChangeListener> listeners;
    private ColorController colorController;
    private boolean hasChanges; // Bandera para saber si hay cambios no guardados
    private ModificationManager modificationManager; // Gestor de modificaciones manuales
    private BrushSize brushSize = BrushSize.SMALL; // Tamaño del pincel
    
    public StudEditController(ColorController colorController) {
        this.activeTool = EditTool.DEFAULT;
        this.colorController = colorController;
        this.listeners = new ArrayList<>();
        this.hasChanges = false;
        this.modificationManager = new ModificationManager();
    }
    
    /**
     * Establece la herramienta activa.
     * @param tool la nueva herramienta activa
     */
    public void setActiveTool(EditTool tool) {
        if (this.activeTool != tool) {
            this.activeTool = tool;
            fireStateChanged();
        }
    }
    
    /**
     * Obtiene la herramienta activa.
     * @return la herramienta actualmente activa
     */
    public EditTool getActiveTool() {
        return activeTool;
    }
    
    /**
     * Establece el tamaño del pincel.
     * @param size el nuevo tamaño del pincel
     */
    public void setBrushSize(BrushSize size) {
        if (size != null && this.brushSize != size) {
            this.brushSize = size;
            fireStateChanged();
        }
    }
    
    /**
     * Obtiene el tamaño actual del pincel.
     * @return el tamaño del pincel
     */
    public BrushSize getBrushSize() {
        return brushSize;
    }
    
    /**
     * Establece el color seleccionado para la herramienta pincel.
     * @param color el color a usar
     */
    public void setSelectedColor(LEGOColor color) {
        this.selectedColor = color;
        fireStateChanged();
    }
    
    /**
     * Obtiene el color seleccionado.
     * @return el color actualmente seleccionado
     */
    public LEGOColor getSelectedColor() {
        return selectedColor;
    }
    
    /**
     * Aplica la herramienta activa en las coordenadas especificadas del grid.
     * @param grid el grid de colores del mosaico
     * @param x coordenada x (columna)
     * @param y coordenada y (fila)
     * @return true si se realizó algún cambio
     */
    public boolean applyToolAt(LEGOColorGrid grid, int x, int y) {
        switch (activeTool) {
            case BRUSH:
                return applyBrushWithSize(grid, x, y);
            case EYEDROPPER:
                return applyEyedropper(grid, x, y);
            case RESET:
                return applyResetWithSize(grid, x, y);
            default:
                return false;
        }
    }
    
    /**
     * Aplica la herramienta pincel.
     */
    private boolean applyBrush(LEGOColorGrid grid, int x, int y) {
        if (selectedColor == null) {
            return false;
        }
        
        LEGOColor currentColor = grid.getColorAt(x, y);
        if (currentColor != selectedColor) {
            boolean success = grid.setColorAt(x, y, selectedColor);
            if (success) {
                // Registrar la modificación en el ModificationManager
                modificationManager.recordModification(x, y, selectedColor, currentColor);
                hasChanges = true;
                fireStateChanged();
            }
            return success;
        }
        return false;
    }
    
    /**
     * Aplica la herramienta eyedropper.
     */
    private boolean applyEyedropper(LEGOColorGrid grid, int x, int y) {
        LEGOColor colorAtPosition = grid.getColorAt(x, y);
        if (colorAtPosition != null) {
            setSelectedColor(colorAtPosition);
            // Cambiar automáticamente a herramienta pincel después de seleccionar color
            setActiveTool(EditTool.BRUSH);
            return true;
        }
        return false;
    }
    
    /**
     * Aplica la herramienta pincel con el tamaño especificado.
     */
    private boolean applyBrushWithSize(LEGOColorGrid grid, int x, int y) {
        if (selectedColor == null) {
            return false;
        }
        
        boolean anyChange = false;
        int radius = brushSize.getRadius();
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int targetX = x + dx;
                int targetY = y + dy;
                
                LEGOColor currentColor = grid.getColorAt(targetX, targetY);
                if (currentColor != null && currentColor != selectedColor) {
                    boolean success = grid.setColorAt(targetX, targetY, selectedColor);
                    if (success) {
                        modificationManager.recordModification(targetX, targetY, selectedColor, currentColor);
                        anyChange = true;
                    }
                }
            }
        }
        
        if (anyChange) {
            hasChanges = true;
            fireStateChanged();
        }
        return anyChange;
    }
    

    
    /**
     * Aplica la herramienta reset con el tamaño especificado.
     */
    private boolean applyResetWithSize(LEGOColorGrid grid, int x, int y) {
        boolean anyChange = false;
        int radius = brushSize.getRadius();
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int targetX = x + dx;
                int targetY = y + dy;
                
                LEGOColor currentColor = grid.getColorAt(targetX, targetY);
                if (currentColor != null) {
                    boolean success = grid.resetAt(targetX, targetY);
                    if (success) {
                        LEGOColor originalColor = grid.getColorAt(targetX, targetY); // Color después del reset
                        // Registrar que se volvió al color original (elimina la modificación)
                        modificationManager.recordModification(targetX, targetY, originalColor, originalColor);
                        anyChange = true;
                    }
                }
            }
        }
        
        if (anyChange) {
            hasChanges = true; // Sigue habiendo cambios, solo restauramos studs
            fireStateChanged();
        }
        return anyChange;
    }
    
    /**
     * Reset global: restaura todo el mosaico a su estado original.
     */
    public void globalReset(LEGOColorGrid grid) {
        if (grid != null) {
            for (int y = 0; y < grid.getHeight(); y++) {
                for (int x = 0; x < grid.getWidth(); x++) {
                    grid.resetAt(x, y);
                }
            }
            modificationManager.clearModifications();
            hasChanges = false;
            fireStateChanged();
        }
    }
    
    /**
     * Indica si hay cambios no guardados en el mosaico.
     * @return true si hay cambios
     */
    public boolean hasUnsavedChanges() {
        return hasChanges;
    }
    
    /**
     * Marca los cambios como guardados.
     */
    public void markChangesSaved() {
        hasChanges = false;
    }
    
    /**
     * Obtiene el gestor de modificaciones.
     */
    public ModificationManager getModificationManager() {
        return modificationManager;
    }
    
    /**
     * Agrega un listener para cambios de estado.
     * @param listener el listener a agregar
     */
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remueve un listener de cambios de estado.
     * @param listener el listener a remover
     */
    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifica a todos los listeners de un cambio de estado.
     */
    private void fireStateChanged() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener listener : listeners) {
            listener.stateChanged(e);
        }
    }
}