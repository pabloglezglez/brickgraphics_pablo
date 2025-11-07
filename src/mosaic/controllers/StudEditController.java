package mosaic.controllers;

import mosaic.ui.EditTool;
import mosaic.ui.BrushSize;
import mosaic.io.BrickGraphicsState;
import mosaic.ui.BrickedView;
import io.Model;
import io.ModelHandler;
import colors.LEGOColor;
import colors.LEGOColorGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Controlador para las herramientas de edición del mosaico.
 * Maneja el cambio de herramientas activas y las operaciones de edición.
 */
public class StudEditController implements ModelHandler<BrickGraphicsState> {
    private EditTool activeTool;
    private LEGOColor selectedColor;
    private List<ChangeListener> listeners;
    private ColorController colorController;
    private BrickedView brickedView; // Referencia al view para acceder al grid
    private boolean hasChanges; // Bandera para saber si hay cambios no guardados
    private ModificationManager modificationManager; // Gestor de modificaciones manuales
    private BrushSize brushSize = BrushSize.SMALL; // Tamaño del pincel
    
    public StudEditController(ColorController colorController, BrickedView brickedView) {
        this.activeTool = EditTool.DEFAULT;
        this.colorController = colorController;
        this.brickedView = brickedView;
        this.listeners = new ArrayList<>();
        this.hasChanges = false;
        this.modificationManager = new ModificationManager(colorController);
    }
    
    /**
     * Constructor alternativo sin BrickedView para evitar dependencias circulares.
     */
    public StudEditController(ColorController colorController) {
        this.activeTool = EditTool.DEFAULT;
        this.colorController = colorController;
        this.brickedView = null; // Se establecerá después
        this.listeners = new ArrayList<>();
        this.hasChanges = false;
        this.modificationManager = new ModificationManager(colorController);
    }
    
    /**
     * Establece la referencia al BrickedView después de la creación.
     */
    public void setBrickedView(BrickedView brickedView) {
        this.brickedView = brickedView;
        if (brickedView != null) {
            brickedView.setModificationManager(this.modificationManager);
        }
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
        if (grid == null) {
            System.out.println("ERROR: Grid es null en applyToolAt");
            return false;
        }
        
        System.out.println("DEBUG: Aplicando herramienta " + activeTool + " con pincel " + brushSize + " en (" + x + "," + y + ")");
        System.out.println("DEBUG: Grid dimensiones: " + grid.getWidth() + "x" + grid.getHeight());
        
        // Verificar que las coordenadas estén dentro del rango válido
        if (x < 0 || x >= grid.getWidth() || y < 0 || y >= grid.getHeight()) {
            System.out.println("ERROR: Coordenadas fuera de rango - Grid: " + grid.getWidth() + "x" + grid.getHeight() + ", Click: (" + x + "," + y + ")");
            return false;
        }
        
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
        int brushSizeValue = brushSize.getSize();
        
        // Calculamos las coordenadas de inicio correctamente
        int startX, startY;
        
        if (brushSizeValue % 2 == 1) {
            // Tamaños impares: centrar alrededor del punto clickeado
            int offset = (brushSizeValue - 1) / 2;
            startX = x - offset;
            startY = y - offset;
        } else {
            // Tamaños pares: el punto clickeado está en la esquina superior izquierda del área
            int offset = brushSizeValue / 2;
            startX = x - offset + 1;
            startY = y - offset + 1;
        }
        
        int endX = startX + brushSizeValue - 1;
        int endY = startY + brushSizeValue - 1;
        
        for (int targetY = startY; targetY <= endY; targetY++) {
            for (int targetX = startX; targetX <= endX; targetX++) {
                LEGOColor currentColor = grid.getColorAt(targetX, targetY);
                if (currentColor != null && currentColor != selectedColor) {
                    boolean success = grid.setColorAt(targetX, targetY, selectedColor);
                    if (success) {
                        modificationManager.recordModification(targetX, targetY, selectedColor, currentColor);
                        anyChange = true;
                        System.out.println("DEBUG: Pintado stud en (" + targetX + "," + targetY + ") con color " + selectedColor.getName());
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
        int brushSizeValue = brushSize.getSize();
        
        // Calculamos las coordenadas de inicio correctamente
        int startX, startY;
        
        if (brushSizeValue % 2 == 1) {
            // Tamaños impares: centrar alrededor del punto clickeado
            int offset = (brushSizeValue - 1) / 2;
            startX = x - offset;
            startY = y - offset;
        } else {
            // Tamaños pares: el punto clickeado está en la esquina superior izquierda del área
            int offset = brushSizeValue / 2;
            startX = x - offset + 1;
            startY = y - offset + 1;
        }
        
        int endX = startX + brushSizeValue - 1;
        int endY = startY + brushSizeValue - 1;
        
        for (int targetY = startY; targetY <= endY; targetY++) {
            for (int targetX = startX; targetX <= endX; targetX++) {
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
    
    @Override
    public void save(Model<BrickGraphicsState> model) {
        System.out.println("DEBUG: StudEditController.save() - MÉTODO SAVE INVOCADO");
        
        // Guardar las modificaciones manuales en el modelo
        Map<String, Integer> modificationsForSave = modificationManager.getModificationsForSave();
        System.out.println("DEBUG: StudEditController.save() - ModificationManager devuelve " + modificationsForSave.size() + " modificaciones");
        
        // Serializar el mapa a String
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : modificationsForSave.entrySet()) {
            if (!first) {
                sb.append(";");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        
        String serializedModifications = sb.toString();
        System.out.println("DEBUG: StudEditController - Guardando " + modificationsForSave.size() + " modificaciones manuales como: " + serializedModifications);
        
        model.set(BrickGraphicsState.ManualModifications, serializedModifications);
        System.out.println("DEBUG: StudEditController - Guardadas " + modificationsForSave.size() + " modificaciones manuales");
    }

    @Override
    public void handleModelChange(Model<BrickGraphicsState> model) {
        // Cargar las modificaciones manuales desde el modelo
        Object savedData = model.get(BrickGraphicsState.ManualModifications);
        if (savedData != null) {
            try {
                Map<String, Integer> savedModifications = new TreeMap<>();
                
                if (savedData instanceof String) {
                    // Deserializar desde String
                    String serializedData = (String) savedData;
                    if (!serializedData.isEmpty()) {
                        String[] entries = serializedData.split(";");
                        for (String entry : entries) {
                            String[] parts = entry.split(":");
                            if (parts.length == 2) {
                                try {
                                    savedModifications.put(parts[0], Integer.parseInt(parts[1]));
                                } catch (NumberFormatException e) {
                                    System.out.println("WARNING: No se pudo convertir el valor '" + parts[1] + "' a Integer para la clave '" + parts[0] + "'");
                                }
                            }
                        }
                    }
                } else if (savedData instanceof Map) {
                    // Compatibilidad con formato anterior (Map)
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawMap = (Map<String, Object>) savedData;
                    
                    // Convertir todos los valores a Integer, ya sean String o Integer
                    for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Integer) {
                            savedModifications.put(entry.getKey(), (Integer) value);
                        } else if (value instanceof String) {
                            try {
                                savedModifications.put(entry.getKey(), Integer.parseInt((String) value));
                            } catch (NumberFormatException e) {
                                System.out.println("WARNING: No se pudo convertir el valor '" + value + "' a Integer para la clave '" + entry.getKey() + "'");
                            }
                        }
                    }
                }
                
                if (!savedModifications.isEmpty()) {
                    modificationManager.loadModificationsFromSave(savedModifications);
                    System.out.println("DEBUG: StudEditController - Cargadas " + savedModifications.size() + " modificaciones manuales desde KMV");
                    
                    // Aplicar las modificaciones al grid actual inmediatamente después de cargar
                    if (brickedView != null) {
                        LEGOColorGrid currentGrid = brickedView.getColorGrid();
                        if (currentGrid != null) {
                            System.out.println("DEBUG: StudEditController - Aplicando " + savedModifications.size() + " modificaciones al grid");
                            modificationManager.restoreModifications(currentGrid);
                            System.out.println("DEBUG: StudEditController - COMPLETADO: Aplicadas modificaciones al grid después de cargar desde KMV");
                        } else {
                            System.out.println("DEBUG: StudEditController - Grid no disponible, las modificaciones se aplicarán cuando esté listo");
                        }
                    } else {
                        System.out.println("DEBUG: StudEditController - BrickedView no disponible, las modificaciones se aplicarán cuando esté listo");
                    }
                }
            } catch (Exception e) {
                System.out.println("ERROR: StudEditController - Error al cargar modificaciones: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}