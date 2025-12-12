package mosaic.controllers;

import mosaic.ui.EditTool;
import mosaic.ui.BrushSize;
import mosaic.io.BrickGraphicsState;
import mosaic.ui.BrickedView;
import io.Model;
import io.ModelHandler;
import io.Log;
import colors.LEGOColor;
import colors.LEGOColorGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    private boolean forcePaintMode = false; // NUEVO: Modo de pintado forzado para proteger píxeles
    private boolean manualPaintingVisible = true; // Estado mostrado/oculto del pintado manual
    
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
            setManualPaintingVisible(manualPaintingVisible);
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
     * NUEVO: Establece el modo de pintado forzado
     * Cuando está activo, el pintado se registra incluso si el color es igual al existente
     * Esto protege los píxeles de las transformaciones de imagen (brillo, contraste, etc.)
     */
    public void setForcePaintMode(boolean forceMode) {
        this.forcePaintMode = forceMode;
        io.Log.log("DEBUG: StudEditController - Modo pintado forzado: " + (forceMode ? "ACTIVADO" : "DESACTIVADO"));
    }
    
    /**
     * NUEVO: Obtiene el estado del modo de pintado forzado
     */
    public boolean isForcePaintMode() {
        return forcePaintMode;
    }

    /**
     * Permite mostrar u ocultar temporalmente el pintado manual aplicado sobre el grid.
     */
    public void setManualPaintingVisible(boolean visible) {
        boolean changed = this.manualPaintingVisible != visible;
        this.manualPaintingVisible = visible;

        LEGOColorGrid grid = (brickedView != null) ? brickedView.getColorGrid() : null;
        modificationManager.setModificationsVisible(visible, grid);
        if (brickedView != null) {
            brickedView.repaint();
        }

        if (changed) {
            fireStateChanged();
        }
    }

    /**
     * Retorna si el pintado manual está visible actualmente.
     */
    public boolean isManualPaintingVisible() {
        return manualPaintingVisible;
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
            io.Log.log("ERROR: Grid es null en applyToolAt");
            return false;
        }
        
        if ((activeTool == EditTool.BRUSH || activeTool == EditTool.RESET) && (!manualPaintingVisible || !modificationManager.areModificationsVisible())) {
            ensureManualPaintingVisible();
        }
        
            Log.log("DEBUG: Aplicando herramienta " + activeTool + " con pincel " + brushSize + " en (" + x + "," + y + ")");
            Log.log("DEBUG: Grid dimensiones: " + grid.getWidth() + "x" + grid.getHeight());
        
        // Verificar que las coordenadas estén dentro del rango válido
        if (x < 0 || x >= grid.getWidth() || y < 0 || y >= grid.getHeight()) {
            io.Log.log("ERROR: Coordenadas fuera de rango - Grid: " + grid.getWidth() + "x" + grid.getHeight() + ", Click: (" + x + "," + y + ")");
            return false;
        }
        
        switch (activeTool) {
            case BRUSH:
                if (brushSize == BrushSize.SMALL) {
                    return applyBrush(grid, x, y, forcePaintMode); // Usar estado del checkbox
                } else {
                    return applyBrushWithSize(grid, x, y, forcePaintMode); // Usar estado del checkbox
                }
            case EYEDROPPER:
                return applyEyedropper(grid, x, y);
            case RESET:
                return applyResetWithSize(grid, x, y);
            default:
                return false;
        }
    }
    
    /**
     * Aplica la herramienta pincel con opción de modo forzado.
     * @param forceMode Si es true, fuerza el pintado incluso si el color es igual (protege de transformaciones)
     */
    private boolean applyBrush(LEGOColorGrid grid, int x, int y, boolean forceMode) {
        if (selectedColor == null) {
            return false;
        }
        
        LEGOColor currentColor = grid.getColorAt(x, y);
        
        if (forceMode) {
            // MODO FORZADO: Siempre pintar y registrar modificación (proteger de transformaciones)
            boolean success = grid.setColorAt(x, y, selectedColor);
            if (success) {
                modificationManager.forceRecordModification(x, y, selectedColor, currentColor);
                hasChanges = true;
                fireStateChanged();
                io.Log.log("DEBUG: FORZADO pintado pincel en (" + x + "," + y + ") color=" + selectedColor.getName() + " (protegido)");
            }
            return success;
        } else {
            // MODO NORMAL: Solo pintar si el color es diferente
            if (currentColor != selectedColor) {
                boolean success = grid.setColorAt(x, y, selectedColor);
                if (success) {
                    modificationManager.recordModification(x, y, selectedColor, currentColor);
                    hasChanges = true;
                    fireStateChanged();
                    io.Log.log("DEBUG: Pintado normal en (" + x + "," + y + ") color=" + selectedColor.getName());
                }
                return success;
            }
        }
        return false;
    }
    
    /**
     * COMPATIBILIDAD: Mantener método original para código existente
     */
    private boolean applyBrush(LEGOColorGrid grid, int x, int y) {
        return applyBrush(grid, x, y, forcePaintMode); // Usar estado del checkbox
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
     * Aplica la herramienta pincel con tamaño específico.
     * @param forceMode Si es true, fuerza el pintado incluso si el color es igual
     */
    private boolean applyBrushWithSize(LEGOColorGrid grid, int x, int y, boolean forceMode) {
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
                
                if (currentColor != null) {
                    if (forceMode) {
                        // MODO FORZADO: Siempre pintar
                        boolean success = grid.setColorAt(targetX, targetY, selectedColor);
                        if (success) {
                            modificationManager.forceRecordModification(targetX, targetY, selectedColor, currentColor);
                            io.Log.log("DEBUG: FORZADO pintado brush en (" + targetX + "," + targetY + ") con color " + selectedColor.getName() + " (protegido)");
                            anyChange = true;
                        }
                    } else if (currentColor != selectedColor) {
                        // MODO NORMAL: Solo pintar si el color es diferente
                        boolean success = grid.setColorAt(targetX, targetY, selectedColor);
                        if (success) {
                            modificationManager.recordModification(targetX, targetY, selectedColor, currentColor);
                            io.Log.log("DEBUG: Pintado normal brush en (" + targetX + "," + targetY + ") con color " + selectedColor.getName());
                            anyChange = true;
                        }
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
    io.Log.log("DEBUG: StudEditController.save() - MÉTODO SAVE INVOCADO");
        
        // Guardar las modificaciones manuales en el modelo
        Map<String, Integer> modificationsForSave = modificationManager.getModificationsForSave();
            Log.log("DEBUG: StudEditController.save() - ModificationManager devuelve " + modificationsForSave.size() + " modificaciones");
        
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
            Log.log("DEBUG: StudEditController - Guardando " + modificationsForSave.size() + " modificaciones manuales como: " + serializedModifications);
        
        model.set(BrickGraphicsState.ManualModifications, serializedModifications);
            Log.log("DEBUG: StudEditController - Guardadas " + modificationsForSave.size() + " modificaciones manuales");

        model.set(BrickGraphicsState.ManualPaintingVisible, manualPaintingVisible);
    }

    @Override
    public void handleModelChange(Model<BrickGraphicsState> model) {
        boolean savedVisibility = true;
        Object visibilityValue = model.get(BrickGraphicsState.ManualPaintingVisible);
        if (visibilityValue instanceof Boolean) {
            savedVisibility = (Boolean) visibilityValue;
        }
        setManualPaintingVisible(savedVisibility);

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
                                    io.Log.log("WARNING: No se pudo convertir el valor '" + parts[1] + "' a Integer para la clave '" + parts[0] + "'");
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
                                io.Log.log("WARNING: No se pudo convertir el valor '" + value + "' a Integer para la clave '" + entry.getKey() + "'");
                            }
                        }
                    }
                }
                
                if (!savedModifications.isEmpty()) {
                    modificationManager.loadModificationsFromSave(savedModifications);
                        Log.log("DEBUG: StudEditController - Cargadas " + savedModifications.size() + " modificaciones manuales desde KMV");
                    
                    // Aplicar las modificaciones al grid actual inmediatamente después de cargar
                    if (brickedView != null) {
                        LEGOColorGrid currentGrid = brickedView.getColorGrid();
                        if (currentGrid != null) {
                                Log.log("DEBUG: StudEditController - Aplicando " + savedModifications.size() + " modificaciones al grid");
                            if (modificationManager.areModificationsVisible()) {
                                modificationManager.restoreModifications(currentGrid);
                                Log.log("DEBUG: StudEditController - COMPLETADO: Aplicadas modificaciones al grid después de cargar desde KMV");
                            } else {
                                Log.log("DEBUG: StudEditController - Pintado oculto, se omite aplicación inmediata de modificaciones");
                            }
                        } else {
                                Log.log("DEBUG: StudEditController - Grid no disponible, programando aplicación retrasada");
                            // Usar un timer para intentar aplicar cuando el grid esté listo
                            javax.swing.Timer retryTimer = new javax.swing.Timer(200, new ActionListener() {
                                private int attempts = 0;
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    attempts++;
                                    LEGOColorGrid grid = brickedView.getColorGrid();
                                    if (grid != null) {
                                        Log.log("DEBUG: StudEditController - Grid disponible en intento " + attempts + ", aplicando modificaciones");
                                        if (modificationManager.areModificationsVisible()) {
                                            modificationManager.applyAllModificationsToGrid(grid);
                                        }
                                        ((javax.swing.Timer) e.getSource()).stop();
                                        // Forzar repaint para mostrar los cambios
                                        SwingUtilities.invokeLater(() -> brickedView.repaint());
                                        Log.log("DEBUG: StudEditController - COMPLETADO: Modificaciones aplicadas con delay y repaint forzado");
                                    } else if (attempts >= 25) { // 5 segundos máximo
                                        Log.log("WARNING: StudEditController - Timeout esperando grid, abandoning");
                                        ((javax.swing.Timer) e.getSource()).stop();
                                    }
                                }
                            });
                            retryTimer.start();
                        }
                    } else {
                            Log.log("DEBUG: StudEditController - BrickedView no disponible, las modificaciones se aplicarán cuando esté listo");
                    }
                }
            } catch (Exception e) {
                Log.log("ERROR: StudEditController - Error al cargar modificaciones: " + e.getMessage());
                Log.log(e);
            }
        }
    }

    private void ensureManualPaintingVisible() {
        if (!manualPaintingVisible || !modificationManager.areModificationsVisible()) {
            setManualPaintingVisible(true);
        }
    }
}