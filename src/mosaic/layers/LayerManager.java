package mosaic.layers;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import io.Model;
import io.ModelHandler;
import mosaic.io.BrickGraphicsState;
import mosaic.layers.Layer.BlendMode;

/**
 * Gestor principal del sistema de capas.
 * Maneja la carga, posicionamiento y renderizado de capas sobre el mosaico base.
 */
public class LayerManager implements ModelHandler<BrickGraphicsState> {
    private List<Layer> layers;
    private Layer selectedLayer;
    private boolean layersEnabled;
    private float globalOpacity;
    private Runnable onAllLayersRemovedCallback;
    private File mosaicFile; // archivo .kvm actual para resolver rutas relativas
    private mosaic.controllers.ModificationManager modificationManager; // Para preservar modificaciones del sistema base
    private colors.LEGOColorGrid colorGrid; // Para aplicar modificaciones visuales
    private mosaic.ui.BrickedView brickedView; // Para actualizar la vista después de cambios
    
    /**
     * Constructor del gestor de capas
     */
    public LayerManager() {
        this.layers = new ArrayList<>();
        this.layersEnabled = true;
        this.globalOpacity = 1.0f;
        this.lastOverlayPixelCounts = new HashMap<>();
    }
    
    /**
     * Establece el callback para cuando se eliminen todas las capas
     */
    public void setOnAllLayersRemovedCallback(Runnable callback) {
        this.onAllLayersRemovedCallback = callback;
    }

    /**
     * Establece el ModificationManager para preservar modificaciones del sistema base
     */
    public void setModificationManager(mosaic.controllers.ModificationManager modificationManager) {
        this.modificationManager = modificationManager;
        io.Log.log("DEBUG: LayerManager - ModificationManager establecido: " + (modificationManager != null));
    }
    
    /**
     * Obtiene el ModificationManager asociado
     */
    public mosaic.controllers.ModificationManager getModificationManager() {
        return this.modificationManager;
    }
    
    /**
     * Establece el ColorGrid para aplicar modificaciones visuales
     */
    public void setColorGrid(colors.LEGOColorGrid colorGrid) {
        this.colorGrid = colorGrid;
        io.Log.log("DEBUG: LayerManager - ColorGrid establecido: " + (colorGrid != null));
    }
    
    /**
     * Obtiene el ColorGrid asociado
     */
    public colors.LEGOColorGrid getColorGrid() {
        return this.colorGrid;
    }
    
    /**
     * Establece el BrickedView para actualizar la vista después de cambios
     */
    public void setBrickedView(mosaic.ui.BrickedView brickedView) {
        this.brickedView = brickedView;
        io.Log.log("DEBUG: LayerManager - BrickedView establecido: " + (brickedView != null));
    }

    /**
     * Informa al gestor de capas del archivo de mosaico (.kvm) actual.
     * Esto permite guardar/leer imágenes de capas en una carpeta compañera junto al archivo.
     */
    public void setMosaicFile(File mosaicFile) {
        this.mosaicFile = mosaicFile;
        io.Log.log("DEBUG: LayerManager - mosaicFile actualizado: " + (mosaicFile != null ? mosaicFile.getAbsolutePath() : "null"));
    }

    /**
     * Devuelve la carpeta compañera (<base>_layers) junto al archivo .kvm actual
     */
    private File getCompanionDir() {
        File kmvFile;
        if (this.mosaicFile != null) {
            kmvFile = this.mosaicFile.getAbsoluteFile();
        } else {
            String kmvFileName = mosaic.controllers.MainController.STATE_FILE_NAME; // p.ej. lddmc.kvm
            kmvFile = new File(kmvFileName).getAbsoluteFile();
        }
        String baseName = kmvFile.getName();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);
        File companionDir = new File(kmvFile.getParentFile(), baseName + "_layers");
        return companionDir;
    }
    
    /**
     * Añade una nueva capa desde un archivo de imagen
     */
    public Layer addLayerFromFile(String filePath, Point position) throws IOException {
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            throw new IOException("El archivo no existe: " + filePath);
        }
        
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("No se pudo cargar la imagen: " + filePath);
        }
        
        // Convertir a ARGB si no tiene canal alpha
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            BufferedImage argbImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = argbImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            image = argbImage;
        }
        
        String layerName = generateLayerName(imageFile.getName());
        Layer layer = new Layer(layerName, image, position, filePath);
        
        addLayer(layer);
        setSelectedLayer(layer);
        
        return layer;
    }
    
    /**
     * NUEVO: Crea una capa de pintado vacía especialmente diseñada para pintar píxeles directamente
     */
    public Layer createPaintLayer(String name, Point position) {
        io.Log.log("DEBUG: LayerManager - Creando capa de pintado: " + name);
        
        // Crear una imagen transparente pequeña que será expandida dinámicamente al pintar
        BufferedImage emptyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        // La imagen permanece completamente transparente
        
        // Crear la capa con un nombre específico para identificarla como capa de pintado
        String paintLayerName = name + " (Pintado)";
        Layer paintLayer = new Layer(paintLayerName, emptyImage, position, null); // Sin archivo fuente
        
        // Configurar la capa como una capa de pintado especial
        // paintLayer.setPaintLayer(true); // REMOVIDO: Sistema de paint layer no utilizado
        
        // CORREGIDO: Insertar la capa de pintado inmediatamente después de la capa seleccionada
        // en lugar de añadirla siempre al final
        if (selectedLayer != null) {
            int selectedIndex = layers.indexOf(selectedLayer);
            if (selectedIndex >= 0) {
                // Insertar después de la capa seleccionada (índice mayor = más arriba)
                layers.add(selectedIndex + 1, paintLayer);
                io.Log.log("DEBUG: LayerManager - Capa de pintado insertada en posición " + (selectedIndex + 1) + " (después de '" + selectedLayer.getName() + "')");
            } else {
                // Fallback: añadir al final si no se encuentra la capa seleccionada
                layers.add(paintLayer);
                io.Log.log("DEBUG: LayerManager - Capa de pintado añadida al final (fallback)");
            }
        } else {
            // Sin capa seleccionada, añadir al final
            layers.add(paintLayer);
            io.Log.log("DEBUG: LayerManager - Capa de pintado añadida al final (sin selección)");
        }
        
        setSelectedLayer(paintLayer);
        
        io.Log.log("DEBUG: LayerManager - Capa de pintado creada exitosamente: " + paintLayerName);
        
        return paintLayer;
    }
    
    /**
     * Añade una capa existente
     */
    public void addLayer(Layer layer) {
        if (layer != null) {
            layers.add(layer);
            io.Log.log("DEBUG: LayerManager - Añadida capa: " + layer.getName());
        }
    }
    
    /**
     * Elimina una capa
     */
    public boolean removeLayer(Layer layer) {
        if (layer != null && layers.remove(layer)) {
            if (selectedLayer == layer) {
                selectedLayer = layers.isEmpty() ? null : layers.get(layers.size() - 1);
            }
            io.Log.log("DEBUG: LayerManager - Eliminada capa: " + layer.getName());
            
            // Si no quedan capas, notificar para restaurar imagen original
            if (layers.isEmpty() && onAllLayersRemovedCallback != null) {
                io.Log.log("DEBUG: LayerManager - No quedan capas, restaurando imagen original");
                onAllLayersRemovedCallback.run();
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Elimina una capa por índice
     */
    public boolean removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            Layer removedLayer = layers.remove(index);
            if (selectedLayer == removedLayer) {
                selectedLayer = layers.isEmpty() ? null : layers.get(Math.min(index, layers.size() - 1));
            }
            io.Log.log("DEBUG: LayerManager - Eliminada capa: " + removedLayer.getName());
            
            // Si no quedan capas, notificar para restaurar imagen original
            if (layers.isEmpty() && onAllLayersRemovedCallback != null) {
                io.Log.log("DEBUG: LayerManager - No quedan capas, restaurando imagen original");
                onAllLayersRemovedCallback.run();
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Mueve una capa hacia arriba en la pila (visualmente encima)
     */
    public boolean moveLayerUp(Layer layer) {
        io.Log.log("DEBUG: moveLayerUp - INICIADO para capa: " + layer.getName());
        int index = layers.indexOf(layer);
        // CORREGIDO: Mover hacia arriba significa índice MAYOR (layers.size()-1 está arriba)
        if (index >= 0 && index < layers.size() - 1) {
            // Preserve both old BufferedImage overlay system and new PixelModification system
            BufferedImage overlay = layer.getPaintingOverlay();
            java.util.List<mosaic.layers.PixelModification> modifications = null;
            
            // Preserve base grid modifications (sistema base)
            java.util.Map<String, colors.LEGOColor> baseModifications = null;
            if (modificationManager != null) {
                baseModifications = new java.util.HashMap<>(modificationManager.getModifications());
                io.Log.log("DEBUG: moveLayerUp - preserving " + baseModifications.size() + " base grid modifications");
            }
            
            io.Log.log("DEBUG: moveLayerUp - moviendo capa '" + layer.getName() + "' de índice " + index + " a " + (index + 1));
            
            if (layer.hasPixelModifications()) {
                modifications = new java.util.ArrayList<mosaic.layers.PixelModification>(layer.getPixelModifications().values());
                io.Log.log("DEBUG: moveLayerUp - preserving " + modifications.size() + " PixelModifications for: " + layer.getName());
                
                // DEBUG: Verificar el contenido de las modificaciones
                if (!modifications.isEmpty()) {
                    io.Log.log("DEBUG: moveLayerUp - primera modificación: " + modifications.get(0).toString());
                }
            }
            
            // CORREGIDO: Intercambiar con la capa que está arriba (índice mayor)
            Collections.swap(layers, index, index + 1);
            
            // Mantener selectedLayer referencia exacta
            if (selectedLayer == layer) {
                selectedLayer = layer; // explícito para claridad
            }
            
            // Restore old BufferedImage overlay system
            if (overlay != null && layer.getPaintingOverlay() != overlay) {
                layer.setPaintingOverlay(overlay);
            }
            
            // Restore new PixelModification system
            if (modifications != null && !modifications.isEmpty()) {
                // CRÍTICO: Limpiar TODAS las modificaciones antes de restaurar
                layer.clearPixelModifications();
                
                // Restaurar cada modificación individualmente
                for (mosaic.layers.PixelModification mod : modifications) {
                    layer.addPixelModification(mod);
                }
                
                io.Log.log("DEBUG: moveLayerUp - restored " + modifications.size() + " PixelModifications for: " + layer.getName());
                
                // DEBUG: Verificar que se restauraron correctamente
                io.Log.log("DEBUG: moveLayerUp - layer now has " + layer.getPixelModifications().size() + " PixelModifications");
                
                // NUEVO: Verificar contenido después de la restauración
                if (!layer.getPixelModifications().isEmpty()) {
                    io.Log.log("DEBUG: moveLayerUp - verificación: primera modificación preservada correctamente");
                }
            }
            
            // Restore base grid modifications with visual application
            if (baseModifications != null && modificationManager != null && colorGrid != null) {
                modificationManager.restoreModificationsFromCopyAndApply(baseModifications, colorGrid);
                io.Log.log("DEBUG: moveLayerUp - restored and applied " + baseModifications.size() + " base grid modifications");
                
                // CRÍTICO: Forzar repaint para actualizar viewport tras aplicar modificaciones directamente
                if (brickedView != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        brickedView.repaint();
                        io.Log.log("DEBUG: moveLayerUp - Forzado repaint() del BrickedView (camino directo)");
                    });
                }
            } else if (baseModifications != null && modificationManager != null) {
                // Fallback: solo restaurar sin aplicación visual
                modificationManager.restoreModificationsFromCopy(baseModifications);
                io.Log.log("DEBUG: moveLayerUp - restored " + baseModifications.size() + " base grid modifications (sin aplicación visual)");
                
                // NUEVA SOLUCIÓN: Guardar y recargar desde JSON para aplicar visualmente
                try {
                    saveAndReloadMosaicModifications();
                    io.Log.log("DEBUG: moveLayerUp - aplicadas modificaciones vía JSON reload");
                } catch (Exception ex) {
                    io.Log.log("WARN: moveLayerUp - fallo JSON reload: " + ex.getMessage());
                }
            }
            
            if (layer.getPaintingOverlay() != null) {
                BufferedImage after = layer.getPaintingOverlay();
                io.Log.log("DEBUG: moveLayerUp - overlay after move:  " + System.identityHashCode(after) +
                           " size=" + after.getWidth() + "x" + after.getHeight());
            } else {
                io.Log.log("DEBUG: moveLayerUp - overlay missing after move for: " + layer.getName());
            }
            io.Log.log("DEBUG: LayerManager - Movida capa hacia arriba: " + layer.getName());
            // Persist reordering and any current overlays so layers.json stays in sync
            try { autosaveArtifacts(); } catch (Exception ex) { io.Log.log("WARN: moveLayerUp - autosaveArtifacts fallo: " + ex.getMessage()); }
            return true;
        }
        return false;
    }
    
    /**
     * Mueve una capa hacia abajo en la pila
     */
    public boolean moveLayerDown(Layer layer) {
        io.Log.log("DEBUG: moveLayerDown - INICIADO para capa: " + layer.getName());
        int index = layers.indexOf(layer);
        if (index > 0) {
            // Preserve both old BufferedImage overlay system and new PixelModification system
            BufferedImage overlay = layer.getPaintingOverlay();
            java.util.List<mosaic.layers.PixelModification> modifications = null;
            
            // Preserve base grid modifications (sistema base)
            java.util.Map<String, colors.LEGOColor> baseModifications = null;
            if (modificationManager != null) {
                baseModifications = new java.util.HashMap<>(modificationManager.getModifications());
                io.Log.log("DEBUG: moveLayerDown - preserving " + baseModifications.size() + " base grid modifications");
            }
            
            io.Log.log("DEBUG: moveLayerDown - moviendo capa '" + layer.getName() + "' de índice " + index + " a " + (index - 1));
            
            if (layer.hasPixelModifications()) {
                modifications = new java.util.ArrayList<mosaic.layers.PixelModification>(layer.getPixelModifications().values());
                io.Log.log("DEBUG: moveLayerDown - preserving " + modifications.size() + " PixelModifications for: " + layer.getName());
                
                // DEBUG: Verificar el contenido de las modificaciones
                if (!modifications.isEmpty()) {
                    io.Log.log("DEBUG: moveLayerDown - primera modificación: " + modifications.get(0).toString());
                }
            }
            
            if (overlay != null) {
                io.Log.log("DEBUG: moveLayerDown - overlay before move: " + System.identityHashCode(overlay) +
                           " size=" + overlay.getWidth() + "x" + overlay.getHeight());
            } else {
                io.Log.log("DEBUG: moveLayerDown - no overlay before move for: " + layer.getName());
            }
            
            Collections.swap(layers, index, index - 1);
            
            if (selectedLayer == layer) {
                selectedLayer = layer;
            }
            
            // Restore old BufferedImage overlay system
            if (overlay != null && layer.getPaintingOverlay() != overlay) {
                layer.setPaintingOverlay(overlay);
            }
            
            // Restore new PixelModification system
            if (modifications != null && !modifications.isEmpty()) {
                // CRÍTICO: Limpiar TODAS las modificaciones antes de restaurar
                layer.clearPixelModifications();
                
                // Restaurar cada modificación individualmente
                for (mosaic.layers.PixelModification mod : modifications) {
                    layer.addPixelModification(mod);
                }
                
                io.Log.log("DEBUG: moveLayerDown - restored " + modifications.size() + " PixelModifications for: " + layer.getName());
                
                // DEBUG: Verificar que se restauraron correctamente
                io.Log.log("DEBUG: moveLayerDown - layer now has " + layer.getPixelModifications().size() + " PixelModifications");
                
                // NUEVO: Verificar contenido después de la restauración
                if (!layer.getPixelModifications().isEmpty()) {
                    io.Log.log("DEBUG: moveLayerDown - verificación: primera modificación preservada correctamente");
                }
            }
            
            // Restore base grid modifications with visual application
            if (baseModifications != null && modificationManager != null && colorGrid != null) {
                modificationManager.restoreModificationsFromCopyAndApply(baseModifications, colorGrid);
                io.Log.log("DEBUG: moveLayerDown - restored and applied " + baseModifications.size() + " base grid modifications");
                
                // CRÍTICO: Forzar repaint para actualizar viewport tras aplicar modificaciones directamente
                if (brickedView != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        brickedView.repaint();
                        io.Log.log("DEBUG: moveLayerDown - Forzado repaint() del BrickedView (camino directo)");
                    });
                }
            } else if (baseModifications != null && modificationManager != null) {
                // Fallback: solo restaurar sin aplicación visual
                modificationManager.restoreModificationsFromCopy(baseModifications);
                io.Log.log("DEBUG: moveLayerDown - restored " + baseModifications.size() + " base grid modifications (sin aplicación visual)");
                
                // NUEVA SOLUCIÓN: Guardar y recargar desde JSON para aplicar visualmente
                try {
                    saveAndReloadMosaicModifications();
                    io.Log.log("DEBUG: moveLayerDown - aplicadas modificaciones vía JSON reload");
                } catch (Exception ex) {
                    io.Log.log("WARN: moveLayerDown - fallo JSON reload: " + ex.getMessage());
                }
            }
            
            if (layer.getPaintingOverlay() != null) {
                BufferedImage after = layer.getPaintingOverlay();
                io.Log.log("DEBUG: moveLayerDown - overlay after move:  " + System.identityHashCode(after) +
                           " size=" + after.getWidth() + "x" + after.getHeight());
            } else {
                io.Log.log("DEBUG: moveLayerDown - overlay missing after move for: " + layer.getName());
            }
            io.Log.log("DEBUG: LayerManager - Movida capa hacia abajo: " + layer.getName());
            // Persist reordering and any current overlays so layers.json stays in sync
            try { autosaveArtifacts(); } catch (Exception ex) { io.Log.log("WARN: moveLayerDown - autosaveArtifacts fallo: " + ex.getMessage()); }
            return true;
        }
        return false;
    }
    
    /**
     * Duplica una capa
     */
    public Layer duplicateLayer(Layer layer) {
        if (layer != null) {
            Layer duplicate = layer.copy();
            int index = layers.indexOf(layer);
            layers.add(index + 1, duplicate);
            setSelectedLayer(duplicate);
            io.Log.log("DEBUG: LayerManager - Duplicada capa: " + layer.getName());
            return duplicate;
        }
        return null;
    }
    
    /**
     * Renderiza todas las capas visibles sobre el gráfico dado
     */
    public void renderLayers(Graphics2D g2d) {
        renderLayers(g2d, 1); // Tamaño por defecto de 1 píxel por stud
    }
    
    /**
     * NUEVO: Renderiza capas con tamaño de stud específico para alineación correcta
     */
    public void renderLayers(Graphics2D g2d, int studPixelSize) {
        if (!layersEnabled) return;
        
        Graphics2D g = (Graphics2D) g2d.create();
        
        System.out.println("DEBUG: LayerManager.renderLayers - Renderizando " + layers.size() + " capas, studPixelSize=" + studPixelSize);
        
        // DEBUG: Mostrar orden de capas antes de renderizar
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            System.out.println("DEBUG: Orden capas[" + i + "]: '" + layer.getName() + "' visible=" + layer.isVisible());
        }
        
        // Renderizar capas de abajo hacia arriba (índice mayor = encima)
        // Lista: [capa_fondo=0, capa_media=1, capa_superior=2]
        // Orden de renderizado: 0 -> 1 -> 2 (cada una encima de la anterior)
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            System.out.println("DEBUG: Renderizando capa[" + i + "]: '" + layer.getName() + "' visible=" + layer.isVisible());
            
            if (layer.isVisible()) {
                System.out.println("DEBUG: -> Usando render() para capa de imagen");
                // Renderizar capa normalmente
                layer.render(g, globalOpacity);
            } else {
                System.out.println("DEBUG: -> Capa no visible, saltando");
            }
        }
        
        g.dispose();
    }
    
    /**
     * Encuentra la capa superior en una posición dada
     */
    public Layer getLayerAt(int x, int y) {
        // Buscar de arriba hacia abajo
        for (Layer layer : layers) {
            if (layer.isVisible() && layer.contains(x, y)) {
                return layer;
            }
        }
        return null;
    }
    
    /**
     * Genera un nombre único para la capa
     */
    private String generateLayerName(String originalName) {
        String baseName = originalName;
        
        // Remover extensión
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        
        // Buscar nombre único
        String layerName = baseName;
        int counter = 1;
        while (hasLayerWithName(layerName)) {
            layerName = baseName + " (" + counter + ")";
            counter++;
        }
        
        return layerName;
    }
    
    /**
     * Verifica si existe una capa con el nombre dado
     */
    private boolean hasLayerWithName(String name) {
        return layers.stream().anyMatch(layer -> layer.getName().equals(name));
    }
    
    /**
     * Combina todas las capas en una sola imagen
     */
    public BufferedImage flattenLayers(int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        
        renderLayers(g2d);
        
        g2d.dispose();
        return result;
    }
    
    // Getters y Setters
    public List<Layer> getLayers() {
        return new ArrayList<>(layers);
    }
    
    public Layer getSelectedLayer() {
        return selectedLayer;
    }
    
    public void setSelectedLayer(Layer layer) {
        if (layers.contains(layer)) {
            this.selectedLayer = layer;
            io.Log.log("DEBUG: LayerManager - Seleccionada capa: " + 
                             (layer != null ? layer.getName() : "null"));
        }
    }
    
    public void setSelectedLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            setSelectedLayer(layers.get(index));
        }
    }
    
    public boolean isLayersEnabled() {
        return layersEnabled;
    }
    
    public void setLayersEnabled(boolean enabled) {
    this.layersEnabled = enabled;
    io.Log.log("DEBUG: LayerManager - Capas " + (enabled ? "habilitadas" : "deshabilitadas"));
    }
    
    public float getGlobalOpacity() {
        return globalOpacity;
    }
    
    public void setGlobalOpacity(float opacity) {
        this.globalOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }
    
    public int getLayerCount() {
        return layers.size();
    }
    
    public boolean isEmpty() {
        return layers.isEmpty();
    }
    
    /**
     * Limpia todas las capas
     */
    public void clear() {
    layers.clear();
    selectedLayer = null;
    io.Log.log("DEBUG: LayerManager - Todas las capas eliminadas");
    }
    
    /**
     * Obtiene información de todas las capas
     */
    public String getLayersInfo() {
        if (layers.isEmpty()) {
            return "No hay capas";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Capas (").append(layers.size()).append("):\n");
        
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            info.append("  [").append(i).append("] ")
                .append(layer.toString())
                .append(layer == selectedLayer ? " (seleccionada)" : "")
                .append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Aplica transformaciones específicas solo a la imagen de fondo usando operaciones optimizadas
     */
    private BufferedImage applyBackgroundTransformations(BufferedImage baseImage, 
                                                         float brightness, float contrast, 
                                                         float saturation, float gamma, 
                                                         float sharpness) {
        if (baseImage == null) return null;
        
        // Crear una copia de la imagen base
        BufferedImage transformedImage = new BufferedImage(
            baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = transformedImage.createGraphics();
        g2d.drawImage(baseImage, 0, 0, null);
        g2d.dispose();
        
        // Aplicar transformaciones pixel por pixel (optimizado)
        int width = transformedImage.getWidth();
        int height = transformedImage.getHeight();
        
        // Obtener array de pixels para procesamiento más rápido
        int[] pixels = new int[width * height];
        transformedImage.getRGB(0, 0, width, height, pixels, 0, width);
        
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            
            // Extraer componentes RGB
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            
            // Aplicar brillo
            red = (int) Math.min(255, Math.max(0, red * brightness));
            green = (int) Math.min(255, Math.max(0, green * brightness));
            blue = (int) Math.min(255, Math.max(0, blue * brightness));
            
            // Aplicar contraste
            red = (int) Math.min(255, Math.max(0, (red - 128) * contrast + 128));
            green = (int) Math.min(255, Math.max(0, (green - 128) * contrast + 128));
            blue = (int) Math.min(255, Math.max(0, (blue - 128) * contrast + 128));
            
            // Aplicar gamma
            if (gamma != 1.0f) {
                red = (int) Math.min(255, Math.max(0, 255 * Math.pow(red / 255.0, 1.0 / gamma)));
                green = (int) Math.min(255, Math.max(0, 255 * Math.pow(green / 255.0, 1.0 / gamma)));
                blue = (int) Math.min(255, Math.max(0, 255 * Math.pow(blue / 255.0, 1.0 / gamma)));
            }
            
            // Aplicar saturación (conversión a HSB y vuelta)
            if (saturation != 1.0f) {
                float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                hsb[1] = Math.min(1.0f, Math.max(0.0f, hsb[1] * saturation));
                int newRgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                red = (newRgb >> 16) & 0xFF;
                green = (newRgb >> 8) & 0xFF;
                blue = newRgb & 0xFF;
            }
            
            // Reconstruir color y almacenar
            pixels[i] = (red << 16) | (green << 8) | blue | (rgb & 0xFF000000); // Mantener alpha
        }
        
        // Aplicar los pixels transformados de vuelta a la imagen
        transformedImage.setRGB(0, 0, width, height, pixels, 0, width);
        
    io.Log.log("DEBUG: Transformaciones aplicadas a " + pixels.length + " píxeles");
        
        // TODO: Aplicar sharpness si es necesario (requiere convolución)
        if (sharpness != 1.0f) {
            io.Log.log("DEBUG: Nitidez no implementada completamente, valor: " + sharpness);
        }
        
        return transformedImage;
    }

    /**
     * Combina la imagen base (con transformaciones de fondo aplicadas) con todas las capas activas.
     * 
     * @param baseImage La imagen original sobre la que aplicar transformaciones y capas
     * @param brightness Brillo para la imagen de fondo (1.0 = normal)
     * @param contrast Contraste para la imagen de fondo (1.0 = normal)  
     * @param saturation Saturación para la imagen de fondo (1.0 = normal)
     * @param gamma Gamma para la imagen de fondo (1.0 = normal)
     * @param sharpness Nitidez para la imagen de fondo (1.0 = normal)
     * @return Una nueva imagen con transformaciones de fondo y capas aplicadas
     */
    public BufferedImage applyBackgroundTransformationsAndLayers(BufferedImage baseImage, 
                                                                 float brightness, float contrast,
                                                                 float saturation, float gamma, 
                                                                 float sharpness) {
        if (baseImage == null) return null;
        
        // DEBUG log comentado para rendimiento:
        // io.Log.log("DEBUG: Aplicando transformaciones de fondo - Brillo: " + brightness + 
        //           ", Contraste: " + contrast + ", Saturación: " + saturation + 
        //           ", Gamma: " + gamma + ", Nitidez: " + sharpness);
        
        // Verificar si hay transformaciones que aplicar
        boolean hasTransformations = brightness != 1.0f || contrast != 1.0f || 
                                   saturation != 1.0f || gamma != 1.0f || sharpness != 1.0f;
        
        BufferedImage transformedBase;
        if (hasTransformations) {
            // Aplicar transformaciones SOLO a la imagen de fondo
            transformedBase = applyBackgroundTransformations(baseImage, 
                                                          brightness, contrast, 
                                                          saturation, gamma, 
                                                          sharpness);
            // io.Log.log("DEBUG: Transformaciones aplicadas a imagen de fondo"); // COMENTADO PARA RENDIMIENTO
        } else {
            // Si no hay transformaciones, usar imagen original
            transformedBase = baseImage;
            // io.Log.log("DEBUG: No hay transformaciones, usando imagen original"); // COMENTADO PARA RENDIMIENTO
        }
        
        // Luego aplicar las capas sobre la imagen de fondo transformada
        BufferedImage result = applyLayersToImage(transformedBase);
        // io.Log.log("DEBUG: Capas aplicadas sobre imagen transformada"); // COMENTADO PARA RENDIMIENTO
        return result;
    }

    /**
     * Combina la imagen base con todas las capas activas.
     * Este método es clave para aplicar las capas sobre la imagen original
     * antes de que sea procesada por el pipeline del mosaico.
     * 
     * @param baseImage La imagen original sobre la que aplicar las capas
     * @return Una nueva imagen con las capas aplicadas, o la imagen original si no hay capas
     */
    public BufferedImage applyLayersToImage(BufferedImage baseImage) {
        if (!layersEnabled || layers.isEmpty() || baseImage == null) {
            return baseImage;
        }

        // Antes de renderizar, recentrar automáticamente capas totalmente fuera del canvas
        try {
            ensureLayersWithinCanvas(baseImage.getWidth(), baseImage.getHeight(), true);
        } catch (Exception ignore) { /* no bloquear render por esto */ }

        BufferedImage result = new BufferedImage(
            baseImage.getWidth(), 
            baseImage.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g_ = result.createGraphics();
        g_.drawImage(baseImage, 0, 0, null);

        for (Layer layer : layers) {
            if (!layer.isVisible()) {
                continue;
            }

			// Apply transformations to get the image to be rendered
			BufferedImage layerImage = layer.applyImageTransforms(layer.getImage());
			if (layerImage == null)
				continue;

            // Prepare overlay image if present
            BufferedImage overlayImage = layer.getPaintingOverlay();

			int originalWidth = layerImage.getWidth();
			int originalHeight = layerImage.getHeight();
			
			// Handle scaling
			float scale = layer.getScale();
			int newWidth = (int) (originalWidth * scale);
			int newHeight = (int) (originalHeight * scale);
			
			int layerX = layer.getX();
			int layerY = layer.getY();

			if (Math.abs(scale - 1.0f) > 1e-6) {
				if (newWidth > 0 && newHeight > 0) {
					BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = scaledImage.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g.drawImage(layerImage, 0, 0, newWidth, newHeight, null);
					g.dispose();
					layerImage = scaledImage;

                    // Scale overlay if present
                    if (overlayImage != null) {
                        BufferedImage scaledOverlay = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D gOv = scaledOverlay.createGraphics();
                        gOv.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        gOv.drawImage(overlayImage, 0, 0, newWidth, newHeight, null);
                        gOv.dispose();
                        overlayImage = scaledOverlay;
                    }
					
					// Adjust position to keep center
					layerX += (originalWidth - newWidth) / 2;
					layerY += (originalHeight - newHeight) / 2;
				}
			}

			// Blend modes
			BlendMode mode = layer.getBlendMode();
            float opacity = layer.getOpacity() * globalOpacity;

			if (mode == BlendMode.NORMAL) {
				// Use standard alpha compositing for NORMAL mode
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
				g_.setComposite(ac);
				g_.drawImage(layerImage, layerX, layerY, null);

                // Draw painting overlay on top (same composite), if any
                if (overlayImage != null) {
                    g_.drawImage(overlayImage, layerX, layerY, null);
                }
			}
			else {
				// Manual blending for other modes
				int imageW = result.getWidth();
				int imageH = result.getHeight();
				int layerW = layerImage.getWidth();
				int layerH = layerImage.getHeight();

				for (int y = 0; y < layerH; y++) {
					int canvasY = y + layerY;
					if (canvasY < 0 || canvasY >= imageH)
						continue;
					for (int x = 0; x < layerW; x++) {
						int canvasX = x + layerX;
						if (canvasX < 0 || canvasX >= imageW)
							continue;

						int frontARGB = layerImage.getRGB(x, y);
						int backARGB = result.getRGB(canvasX, canvasY);

						int blendedARGB = Layer.blend(backARGB, frontARGB, mode);

						// Manual alpha compositing
						float pixelAlpha = ((frontARGB >> 24) & 0xff) / 255.0f;
						float finalAlpha = opacity * pixelAlpha;

						int r_back = (backARGB >> 16) & 0xff;
						int g_back = (backARGB >> 8) & 0xff;
						int b_back = (backARGB) & 0xff;

						int r_blend = (blendedARGB >> 16) & 0xff;
						int g_blend = (blendedARGB >> 8) & 0xff;
						int b_blend = (blendedARGB) & 0xff;

						int r = (int) (r_back * (1 - finalAlpha) + r_blend * finalAlpha);
						int g = (int) (g_back * (1 - finalAlpha) + g_blend * finalAlpha);
						int b = (int) (b_back * (1 - finalAlpha) + b_blend * finalAlpha);

						int finalARGB = (backARGB & 0xFF000000) | (r << 16) | (g << 8) | b;
						result.setRGB(canvasX, canvasY, finalARGB);
					}
				}

                // After manual blend of the base layer image, alpha-composite the overlay on top (NORMAL), if present
                if (overlayImage != null) {
                    AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
                    g_.setComposite(ac);
                    g_.drawImage(overlayImage, layerX, layerY, null);
                }
			}
		}
		g_.dispose();
		return result;
	}

    /**
     * Reposiciona al centro cualquier capa que esté completamente fuera del canvas.
     * @param baseW ancho del lienzo base
     * @param baseH alto del lienzo base
     * @param onlyFullyOut si es true, solo actúa cuando no hay intersección alguna con el canvas
     */
    public void ensureLayersWithinCanvas(int baseW, int baseH, boolean onlyFullyOut) {
        if (baseW <= 0 || baseH <= 0 || layers.isEmpty()) return;
        for (Layer layer : layers) {
            if (!layer.isVisible()) continue;
            BufferedImage img = layer.getImage();
            if (img == null) continue;
            int origW = img.getWidth();
            int origH = img.getHeight();
            float s = layer.getScale();
            int scaledW = Math.max(1, (int) Math.round(origW * s));
            int scaledH = Math.max(1, (int) Math.round(origH * s));
            // posición mostrada (tras centrar por escala)
            int dispX = layer.getX();
            int dispY = layer.getY();
            if (Math.abs(s - 1.0f) > 1e-6) {
                dispX += (origW - scaledW) / 2;
                dispY += (origH - scaledH) / 2;
            }
            boolean intersects = dispX < baseW && dispY < baseH && (dispX + scaledW) > 0 && (dispY + scaledH) > 0;
            if (!intersects || !onlyFullyOut) {
                if (!intersects) {
                    // Recentrar: elegir coordenada de capa sin escalar que produzca centrado visual
                    int newX = (baseW - origW) / 2;
                    int newY = (baseH - origH) / 2;
                    layer.setPosition(newX, newY);
                    io.Log.log("DEBUG: LayerManager.ensureLayersWithinCanvas - Recentrada capa '" + layer.getName() + "' a (" + newX + "," + newY + ") sobre " + baseW + "x" + baseH);
                }
            }
        }
    }
    
    /**
     * Limpia todas las capas de la lista
     */
    public void clearLayers() {
        boolean hadLayers = !layers.isEmpty();
        layers.clear();
        selectedLayer = null;
        
        // Si había capas y ahora no quedan, notificar para restaurar imagen original
        if (hadLayers && onAllLayersRemovedCallback != null) {
            io.Log.log("DEBUG: LayerManager - Todas las capas eliminadas, restaurando imagen original");
            onAllLayersRemovedCallback.run();
        }
    }
    
    /**
     * Notifica cambios en las capas a los listeners
     */
    private void notifyLayersChanged() {
        // Este método puede ser expandido en el futuro para notificar a observers
        // Por ahora, simplemente marca que las capas han cambiado
    }
    
    /**
     * Implementación de ModelHandler para guardar las capas en el archivo KMV
     */
    @Override
    public void save(Model<BrickGraphicsState> model) {
        try {
            // Verificar si el sistema de capas está habilitado
            model.set(BrickGraphicsState.LayersEnabled, !layers.isEmpty());
            
            if (!layers.isEmpty()) {
                // 1. Determinar carpeta companion para las imágenes de capa
                File companionDir = getCompanionDir();
                if (!companionDir.exists()) {
                    companionDir.mkdirs();
                    io.Log.log("DEBUG: LayerManager.save() - Creada carpeta de capas: " + companionDir.getAbsolutePath());
                }

                // 2. Exportar cada imagen original de la capa (sin aplanar) a PNG
                // Guardamos el archivo como <index>_<nombre_normalizado>.png
                List<String> relativeFiles = new ArrayList<>();
                List<String> relativePaintFiles = new ArrayList<>();
                java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();
                for (int i = 0; i < layers.size(); i++) {
                    Layer layer = layers.get(i);
                    BufferedImage img = layer.getImage(); // Imagen base de la capa
                    if (img == null) continue;

                    // Si la capa ya apunta a un archivo dentro de la carpeta companion, reutilizarlo
                    String existingPath = layer.getImageFilePath();
                    String fileNameToUse = null;
                    String paintFileNameToUse = null;
                    if (existingPath != null) {
                        try {
                            File existing = new File(existingPath).getAbsoluteFile();
                            if (existing.getParentFile() != null && existing.getParentFile().equals(companionDir)) {
                                fileNameToUse = existing.getName();
                                // Si el archivo no existe (fue movido/limpiado), lo volvemos a escribir
                                if (!existing.exists()) {
                                    try {
                                        ImageIO.write(img, "PNG", existing);
                                        io.Log.log("DEBUG: LayerManager.save() - (re)creado archivo de capa faltante: " + existing.getAbsolutePath());
                                    } catch (IOException ex) {
                                        io.Log.log("ERROR: LayerManager.save() - Falló re-crear archivo: " + existing.getAbsolutePath());
                                    }
                                }
                                // Derivar nombre del overlay a partir del nombre base
                                paintFileNameToUse = fileNameToUse.replaceFirst("(?i)\\.png$", "_paint.png");
                            }
                        } catch (Exception ignore) {}
                    }

                    if (fileNameToUse == null) {
                        // Generar nombre estable sin índice; resolver colisiones con contador
                        String base = normalizeName(layer.getName());
                        int count = nameCounts.getOrDefault(base, 0);
                        nameCounts.put(base, count + 1);
                        if (count > 0) {
                            fileNameToUse = base + "(" + count + ")" + ".png";
                        } else {
                            fileNameToUse = base + ".png";
                        }
                        paintFileNameToUse = base + ((count > 0) ? "(" + count + ")" : "") + "_paint.png";

                        File outFile = new File(companionDir, fileNameToUse);
                        if (!outFile.exists()) {
                            try {
                                ImageIO.write(img, "PNG", outFile);
                                io.Log.log("DEBUG: LayerManager.save() - Guardada imagen de capa: " + outFile.getAbsolutePath());
                            } catch (IOException ex) {
                                io.Log.log("ERROR: LayerManager.save() - Falló guardado de capa: " + outFile.getAbsolutePath());
                                // Fallback: mantener ruta original si existe
                                relativeFiles.add(layer.getImageFilePath());
                                relativePaintFiles.add(null);
                                continue;
                            }
                        } else {
                            // Archivo ya existe: no reescribir para acelerar guardado
                            io.Log.log("DEBUG: LayerManager.save() - Reutilizando imagen de capa existente: " + outFile.getAbsolutePath());
                        }
                    }

                    // Usamos marcador relativo REL: para reconstruir luego
                    relativeFiles.add("REL:" + fileNameToUse);

                    // 3. Guardar modificaciones de píxeles como JSON si existen
                    Map<Point, PixelModification> pixelModifications = layer.getPixelModifications();
                    String relPaintRef = null;
                    if (pixelModifications != null && !pixelModifications.isEmpty()) {
                        try {
                            String paintJsonFileName = paintFileNameToUse.replaceFirst("\\.png$", ".json");
                            File paintJsonFile = new File(companionDir, paintJsonFileName);
                            
                            // Serializar las modificaciones a JSON
                            StringBuilder jsonBuilder = new StringBuilder();
                            jsonBuilder.append("{\n");
                            jsonBuilder.append("  \"pixelModifications\": {\n");
                            boolean first = true;
                            for (Map.Entry<Point, PixelModification> entry : pixelModifications.entrySet()) {
                                if (!first) jsonBuilder.append(",\n");
                                first = false;
                                Point p = entry.getKey();
                                PixelModification mod = entry.getValue();
                                jsonBuilder.append("    \"").append(p.x).append(",").append(p.y).append("\": ")
                                          .append(mod.toJson());
                            }
                            jsonBuilder.append("\n  }\n");
                            jsonBuilder.append("}\n");
                            
                            java.nio.file.Files.writeString(paintJsonFile.toPath(), jsonBuilder.toString());
                            io.Log.log("DEBUG: LayerManager.save() - Guardadas modificaciones de píxeles: " + paintJsonFile.getAbsolutePath());
                            relPaintRef = "REL:" + paintJsonFileName;
                        } catch (IOException ex) {
                            io.Log.log("ERROR: LayerManager.save() - Falló guardado de modificaciones: " + paintFileNameToUse);
                        }
                    }
                    
                    // Mantener compatibilidad con sistema anterior (BufferedImage overlay)
                    BufferedImage overlay = layer.getPaintingOverlay();
                    if (relPaintRef == null && overlay != null && hasNonTransparentPixels(overlay)) {
                        try {
                            File paintFile = new File(companionDir, paintFileNameToUse);
                            ImageIO.write(overlay, "PNG", paintFile);
                            io.Log.log("DEBUG: LayerManager.save() - Guardado overlay de pintura (compatibilidad): " + paintFile.getAbsolutePath());
                            relPaintRef = "REL:" + paintFileNameToUse;
                        } catch (IOException ex) {
                            io.Log.log("ERROR: LayerManager.save() - Falló guardado de overlay: " + paintFileNameToUse);
                        }
                    }
                    relativePaintFiles.add(relPaintRef);
                }

                StringBuilder layerData = new StringBuilder();
                
                for (int i = 0; i < layers.size(); i++) {
                    Layer layer = layers.get(i);
                    
                    // Serializar datos de la capa en formato JSON simple
                    layerData.append("{");
                    layerData.append("\"name\":\"").append(escapeJson(layer.getName())).append("\",");
                    // Sustituimos el campo file por referencia relativa si la exportamos
                    String relRef = (i < relativeFiles.size()) ? relativeFiles.get(i) : layer.getImageFilePath();
                    layerData.append("\"file\":\"").append(escapeJson(relRef)).append("\",");
                    layerData.append("\"visible\":").append(layer.isVisible()).append(",");
                    layerData.append("\"x\":").append(layer.getX()).append(",");
                    layerData.append("\"y\":").append(layer.getY()).append(",");
                    layerData.append("\"opacity\":").append(layer.getOpacity()).append(",");
                    layerData.append("\"brightness\":").append(layer.getBrightness()).append(",");
                    layerData.append("\"contrast\":").append(layer.getContrast()).append(",");
                    layerData.append("\"saturation\":").append(layer.getSaturation()).append(",");
                    layerData.append("\"gamma\":").append(layer.getGamma()).append(",");
                    layerData.append("\"sharpness\":").append(layer.getSharpness()).append(",");
                    layerData.append("\"scale\":").append(layer.getScale());
                    layerData.append("}");
                    
                    if (i < layers.size() - 1) {
                        layerData.append(",");
                    }
                }
                
                model.set(BrickGraphicsState.LayerData, "[" + layerData.toString() + "]");

                // 4. Escribir layers.json (paralelo, con metadatos extendidos)
                writeLayersJson(companionDir, relativeFiles, relativePaintFiles);
            } else {
                model.set(BrickGraphicsState.LayerData, "");
            }
        } catch (Exception e) {
            io.Log.log(e);
            // En caso de error, limpiar los datos
            model.set(BrickGraphicsState.LayersEnabled, false);
            model.set(BrickGraphicsState.LayerData, "");
        }
    }
    
    /**
     * Implementación de ModelHandler para cargar las capas desde el archivo KMV
     */
    @Override
    public void handleModelChange(Model<BrickGraphicsState> model) {
        try {
            // Snapshot current overlays to preserve them across model reloads (e.g., reorder or other updates)
            java.util.Map<String, BufferedImage> previousOverlays = snapshotOverlaysByKey();

            Object layersEnabledObj = model.get(BrickGraphicsState.LayersEnabled);
            Object layerDataObj = model.get(BrickGraphicsState.LayerData);
            
            Boolean layersEnabled = (layersEnabledObj instanceof Boolean) ? (Boolean) layersEnabledObj : false;
            String layerData = (layerDataObj instanceof String) ? (String) layerDataObj : "";

            // Sincronizar el flag interno (si no lo hacemos, aunque carguemos capas no se renderizan)
            this.layersEnabled = layersEnabled != null ? layersEnabled.booleanValue() : false;
            io.Log.log("DEBUG: LayerManager.handleModelChange - Flag layersEnabled cargado: " + this.layersEnabled);
            
            // IMPORTANTE: Guardar modificaciones actuales antes de limpiar las capas
            // Esto evita que se pierdan las modificaciones del pincel cuando se recarga el modelo
            try {
                autosaveArtifacts();
                io.Log.log("DEBUG: LayerManager.handleModelChange - Guardado automático antes de recargar capas");
            } catch (Exception ex) {
                io.Log.log("WARN: LayerManager.handleModelChange - Error en guardado automático: " + ex.getMessage());
            }
            
            // Limpiar capas existentes
            clearLayers();
            
            if (layersEnabled != null && layersEnabled) {
                // Intentar cargar desde layers.json si existe
                File companionDir = getCompanionDir();
                File jsonFile = new File(companionDir, "layers.json");
                boolean loadedFromJson = false;
                if (jsonFile.exists()) {
                    try {
                        loadedFromJson = loadFromLayersJson(jsonFile);
                        io.Log.log("DEBUG: LayerManager.handleModelChange - Cargado desde layers.json: " + loadedFromJson);
                    } catch (Exception ex) {
                        io.Log.log("WARN: LayerManager.handleModelChange - Error al leer layers.json, usando LayerData: " + ex.getMessage());
                    }
                }

                // Fallback a LayerData en el KMV si no se pudo cargar desde JSON
                if (!loadedFromJson && layerData != null && !layerData.trim().isEmpty()) {
                // Parsear datos de capas (JSON simple)
                layerData = layerData.trim();
                if (layerData.startsWith("[") && layerData.endsWith("]")) {
                    layerData = layerData.substring(1, layerData.length() - 1);
                    
                    if (!layerData.isEmpty()) {
                        String[] layerEntries = splitJsonArray(layerData);
                        
                        for (String entry : layerEntries) {
                            try {
                                loadLayerFromJson(entry.trim());
                            } catch (Exception e) {
                                io.Log.log(e);
                                // Continuar con la siguiente capa si hay error
                            }
                        }
                    }
                }
                }

                // Re-asignar overlays previos a capas equivalentes si no se cargaron desde JSON
                if (!previousOverlays.isEmpty() && !layers.isEmpty()) {
                    int restored = 0;
                    for (Layer l : layers) {
                        if (l.getPaintingOverlay() != null) continue; // ya cargado desde paintFile
                        String key = layerKey(l);
                        BufferedImage ov = previousOverlays.get(key);
                        if (ov == null) {
                            // Intentar por nombre como alternativa
                            ov = previousOverlays.get("NAME:" + l.getName());
                        }
                        if (ov != null) {
                            // Validar tamaño compatible
                            if (l.getImage() != null && l.getImage().getWidth() == ov.getWidth() && l.getImage().getHeight() == ov.getHeight()) {
                                l.setPaintingOverlay(ov);
                                restored++;
                            }
                        }
                    }
                    if (restored > 0) {
                        io.Log.log("DEBUG: LayerManager.handleModelChange - Overlays re-asignados tras recarga: " + restored);
                    }
                }
            }
        } catch (Exception e) {
            io.Log.log(e);
            // En caso de error, mantener lista vacía
            clearLayers();
        }
        
        // Notificar cambios
        notifyLayersChanged();

        // Asegurar una capa seleccionada coherente tras la carga
        if (!layers.isEmpty() && selectedLayer == null) {
            setSelectedLayer(layers.get(layers.size() - 1)); // seleccionar la superior
        }
        io.Log.log("DEBUG: LayerManager.handleModelChange - Capas cargadas: " + layers.size());
    }

    /**
     * Crea un snapshot de los overlays actuales, indexados por una clave estable (ruta absoluta del archivo si existe, si no, nombre).
     */
    private java.util.Map<String, BufferedImage> snapshotOverlaysByKey() {
        java.util.Map<String, BufferedImage> map = new java.util.HashMap<>();
        if (layers == null || layers.isEmpty()) return map;
        for (Layer l : layers) {
            BufferedImage ov = l.getPaintingOverlay();
            if (ov == null) continue;
            String key = layerKey(l);
            map.put(key, ov);
            // También indexar por nombre como fallback
            map.put("NAME:" + l.getName(), ov);
        }
        return map;
    }

    /**
     * Obtiene una clave estable para la capa: ruta absoluta del archivo si está disponible, si no, el nombre.
     */
    private String layerKey(Layer l) {
        try {
            String path = l.getImageFilePath();
            if (path != null && !path.isEmpty()) {
                return new File(path).getAbsoluteFile().getPath();
            }
        } catch (Exception ignore) {}
        return "NAME:" + l.getName();
    }

    /**
     * Aplica un trazo de pincel sobre la capa seleccionada, en coordenadas de imagen (pre-escala).
     * No realiza conversión desde coordenadas de pantalla.
     */
    public boolean applyBrushToSelectedLayer(int imageX, int imageY, int radius, Color color) {
        if (selectedLayer == null) return false;
        int argb = color.getRGB();
        io.Log.log("DEBUG: LayerManager.applyBrushToSelectedLayer - layer='" + selectedLayer.getName() + "' point=(" + imageX + "," + imageY + ") r=" + radius + " argb=0x" + Integer.toHexString(argb));
        boolean modified = selectedLayer.applyBrushStroke(imageX, imageY, radius, argb);
        if (modified) {
            // Persist overlay and layers.json without touching the KMV file
            try {
                autosaveArtifacts();
                io.Log.log("DEBUG: LayerManager.applyBrushToSelectedLayer - Autosave artifacts after brush stroke");
            } catch (Exception ex) {
                io.Log.log("WARN: LayerManager.applyBrushToSelectedLayer - Autosave artifacts failed: " + ex.getMessage());
            }
        }
        return modified;
    }

    /**
     * Guarda solo los artefactos externos (carpeta companion):
     * - PNGs base de las capas (si faltan o aún no están en la carpeta companion)
     * - PNGs del overlay de pintura (si existen y tienen píxeles no transparentes)
     * - layers.json con metadatos extendidos y hashes
     * No modifica el archivo KMV.
     */
    public void autosaveArtifacts() {
        try {
            if (layers.isEmpty()) return;
            File companionDir = getCompanionDir();
            io.Log.log("DEBUG: LayerManager.autosaveArtifacts - companionDir=" + companionDir.getAbsolutePath());
            if (!companionDir.exists()) companionDir.mkdirs();

            List<String> relativeFiles = new ArrayList<>();
            List<String> relativePaintFiles = new ArrayList<>();
            java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();

            for (int i = 0; i < layers.size(); i++) {
                Layer layer = layers.get(i);
                BufferedImage img = layer.getImage();
                if (img == null) { relativeFiles.add(null); relativePaintFiles.add(null); continue; }

                String existingPath = layer.getImageFilePath();
                String fileNameToUse = null;
                String paintFileNameToUse = null;
                if (existingPath != null) {
                    try {
                        File existing = new File(existingPath).getAbsoluteFile();
                        if (existing.getParentFile() != null && existing.getParentFile().equals(companionDir)) {
                            fileNameToUse = existing.getName();
                            if (!existing.exists()) {
                                try { ImageIO.write(img, "PNG", existing); } catch (IOException ignore) {}
                            }
                            paintFileNameToUse = fileNameToUse.replaceFirst("(?i)\\.png$", "_paint.png");
                        }
                    } catch (Exception ignore) {}
                }

                if (fileNameToUse == null) {
                    String base = normalizeName(layer.getName());
                    int count = nameCounts.getOrDefault(base, 0);
                    nameCounts.put(base, count + 1);
                    if (count > 0) {
                        fileNameToUse = base + "(" + count + ")" + ".png";
                    } else {
                        fileNameToUse = base + ".png";
                    }
                    paintFileNameToUse = base + ((count > 0) ? "(" + count + ")" : "") + "_paint.png";

                    File outFile = new File(companionDir, fileNameToUse);
                    if (!outFile.exists()) {
                        try { ImageIO.write(img, "PNG", outFile); } catch (IOException ignore) {}
                    }
                }

                relativeFiles.add("REL:" + fileNameToUse);

                // Guardar modificaciones de píxeles como JSON primero (nuevo sistema)
                Map<Point, PixelModification> pixelModifications = layer.getPixelModifications();
                String relPaintRef = null;
                
                // DEBUG: Agregar información detallada sobre modificaciones
                io.Log.log("DEBUG: LayerManager.autosaveArtifacts - capa '" + layer.getName() + "'");
                io.Log.log("DEBUG: LayerManager.autosaveArtifacts - pixelModifications null=" + (pixelModifications == null) + 
                          " size=" + (pixelModifications != null ? pixelModifications.size() : "N/A"));
                
                if (pixelModifications != null && !pixelModifications.isEmpty()) {
                    String key = layerKey(layer);
                    Integer prev = lastOverlayPixelCounts.get(key);
                    int modCount = pixelModifications.size();
                    if (prev != null && prev == modCount) {
                        io.Log.log("DEBUG: LayerManager.autosaveArtifacts - modificaciones sin cambios para '" + layer.getName() + "' (" + modCount + " px), se omite escritura");
                        relPaintRef = "REL:" + paintFileNameToUse.replaceFirst("\\.png$", ".json");
                    } else {
                        try {
                            String paintJsonFileName = paintFileNameToUse.replaceFirst("\\.png$", ".json");
                            File paintJsonFile = new File(companionDir, paintJsonFileName);
                            
                            // Serializar las modificaciones a JSON
                            StringBuilder jsonBuilder = new StringBuilder();
                            jsonBuilder.append("{\n");
                            jsonBuilder.append("  \"pixelModifications\": {\n");
                            boolean first = true;
                            for (Map.Entry<Point, PixelModification> entry : pixelModifications.entrySet()) {
                                if (!first) jsonBuilder.append(",\n");
                                first = false;
                                Point p = entry.getKey();
                                PixelModification mod = entry.getValue();
                                jsonBuilder.append("    \"").append(p.x).append(",").append(p.y).append("\": ")
                                          .append(mod.toJson());
                            }
                            jsonBuilder.append("\n  }\n");
                            jsonBuilder.append("}\n");
                            
                            java.nio.file.Files.writeString(paintJsonFile.toPath(), jsonBuilder.toString());
                            io.Log.log("DEBUG: LayerManager.autosaveArtifacts - escrito modificaciones JSON '" + paintJsonFile.getName() + "' mods=" + modCount);
                            relPaintRef = "REL:" + paintJsonFileName;
                            lastOverlayPixelCounts.put(key, modCount);
                        } catch (IOException ioe) {
                            io.Log.log("WARN: LayerManager.autosaveArtifacts - fallo escribiendo modificaciones JSON '" + paintFileNameToUse + "': " + ioe.getMessage());
                        }
                    }
                } else {
                    // Fallback a sistema anterior (BufferedImage overlay) para compatibilidad
                    BufferedImage overlay = layer.getPaintingOverlay();
                    if (overlay != null) {
                        int nonZero = hasNonTransparentPixels(overlay) ? countNonTransparentPixels(overlay) : 0;
                        if (nonZero > 0) {
                            String key = layerKey(layer);
                            Integer prev = lastOverlayPixelCounts.get(key);
                            if (prev != null && prev == nonZero) {
                                io.Log.log("DEBUG: LayerManager.autosaveArtifacts - overlay sin cambios para '" + layer.getName() + "' (" + nonZero + " px), se omite escritura");
                                relPaintRef = "REL:" + paintFileNameToUse;
                            } else {
                                try {
                                    File paintFile = new File(companionDir, paintFileNameToUse);
                                    ImageIO.write(overlay, "PNG", paintFile);
                                    io.Log.log("DEBUG: LayerManager.autosaveArtifacts - escrito overlay '" + paintFile.getName() + "' nonTransparent=" + nonZero);
                                    relPaintRef = "REL:" + paintFileNameToUse;
                                    lastOverlayPixelCounts.put(key, nonZero);
                                } catch (IOException ioe) {
                                    io.Log.log("WARN: LayerManager.autosaveArtifacts - fallo escribiendo overlay '" + paintFileNameToUse + "': " + ioe.getMessage());
                                }
                            }
                        } else {
                            io.Log.log("DEBUG: LayerManager.autosaveArtifacts - overlay vacío para capa '" + layer.getName() + "', no se guarda archivo");
                        }
                    } else {
                        io.Log.log("DEBUG: LayerManager.autosaveArtifacts - capa '" + layer.getName() + "' sin overlay (null), no se guarda archivo");
                    }
                }
                relativePaintFiles.add(relPaintRef);
            }

            writeLayersJson(companionDir, relativeFiles, relativePaintFiles);
        } catch (Exception ex) {
            io.Log.log("WARN: LayerManager.autosaveArtifacts - fallo al persistir artefactos: " + ex.getMessage());
        }
    }

    /**
     * Devuelve un conteo simple de píxeles con alpha > 0 en una imagen (para diagnóstico).
     */
    private int countNonTransparentPixels(BufferedImage img) {
        if (img == null) return 0;
        int w = img.getWidth();
        int h = img.getHeight();
        int count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xff;
                if (a != 0) count++;
            }
        }
        return count;
    }

    // Cache de conteos de píxeles para evitar escritura redundante de overlays sin cambios
    private Map<String,Integer> lastOverlayPixelCounts;

    /**
     * Escribe un archivo layers.json en la carpeta companion con metadatos extendidos.
     */
    private void writeLayersJson(File companionDir, List<String> relFiles, List<String> relPaintFiles) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"version\": 1,\n");
            sb.append("  \"layersEnabled\": ").append(this.layersEnabled).append(",\n");
            sb.append("  \"layers\": [\n");
            for (int i = 0; i < layers.size(); i++) {
                Layer layer = layers.get(i);
                String rel = (i < relFiles.size()) ? relFiles.get(i) : layer.getImageFilePath();
                String relPaint = (i < relPaintFiles.size()) ? relPaintFiles.get(i) : null;
                // Calcular hash del archivo base para invalidar overlays obsoletos
                String fileHash = computeFileHash(resolveRelativeFile(rel));
                String paintHash = relPaint != null ? computeFileHash(resolveRelativeFile(relPaint)) : null;
                sb.append("    {");
                sb.append("\"name\": \"").append(escapeJson(layer.getName())).append("\",");
                sb.append(" \"file\": \"").append(escapeJson(rel)).append("\",");
                if (relPaint != null) {
                    sb.append(" \"paintFile\": \"").append(escapeJson(relPaint)).append("\",");
                    if (paintHash != null) {
                        sb.append(" \"paintHash\": \"").append(paintHash).append("\",");
                    }
                }
                if (fileHash != null) {
                    sb.append(" \"fileHash\": \"").append(fileHash).append("\",");
                }
                sb.append(" \"visible\": ").append(layer.isVisible()).append(",");
                sb.append(" \"x\": ").append(layer.getX()).append(",");
                sb.append(" \"y\": ").append(layer.getY()).append(",");
                sb.append(" \"opacity\": ").append(layer.getOpacity()).append(",");
                sb.append(" \"brightness\": ").append(layer.getBrightness()).append(",");
                sb.append(" \"contrast\": ").append(layer.getContrast()).append(",");
                sb.append(" \"saturation\": ").append(layer.getSaturation()).append(",");
                sb.append(" \"gamma\": ").append(layer.getGamma()).append(",");
                sb.append(" \"sharpness\": ").append(layer.getSharpness()).append(",");
                sb.append(" \"scale\": ").append(layer.getScale());
                sb.append(" }");
                if (i < layers.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}\n");

            File jsonFile = new File(companionDir, "layers.json");
            java.nio.file.Files.writeString(jsonFile.toPath(), sb.toString());
            io.Log.log("DEBUG: LayerManager.save() - Escrito layers.json en: " + jsonFile.getAbsolutePath());
        } catch (Exception ex) {
            io.Log.log("WARN: LayerManager.save() - No se pudo escribir layers.json: " + ex.getMessage());
        }
    }

    /**
     * Carga capas desde layers.json. Devuelve true si al menos una capa fue cargada.
     */
    private boolean loadFromLayersJson(File jsonFile) throws Exception {
        String content = java.nio.file.Files.readString(jsonFile.toPath());
        if (content == null || content.isEmpty()) return false;
        // Buscar array de capas
        int idx = content.indexOf("\"layers\"");
        if (idx < 0) return false;
        int startArr = content.indexOf('[', idx);
        int endArr = content.indexOf(']', startArr);
        if (startArr < 0 || endArr < 0) return false;
        String array = content.substring(startArr + 1, endArr);
        String[] entries = splitJsonArray(array);
        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) continue;
            // Reusar extractor simple
            String name = extractJsonValue(entry, "name");
            String file = extractJsonValue(entry, "file");
            String paintFile = extractJsonValue(entry, "paintFile");
            String fileHashStored = extractJsonValue(entry, "fileHash");
            String paintHashStored = extractJsonValue(entry, "paintHash");
            boolean visible = Boolean.parseBoolean(extractJsonValue(entry, "visible"));
            int x = Integer.parseInt(extractJsonValue(entry, "x"));
            int y = Integer.parseInt(extractJsonValue(entry, "y"));
            float opacity = Float.parseFloat(extractJsonValue(entry, "opacity"));
            float brightness = Float.parseFloat(extractJsonValue(entry, "brightness"));
            float contrast = Float.parseFloat(extractJsonValue(entry, "contrast"));
            float saturation = Float.parseFloat(extractJsonValue(entry, "saturation"));
            float gamma = Float.parseFloat(extractJsonValue(entry, "gamma"));
            float sharpness = Float.parseFloat(extractJsonValue(entry, "sharpness"));
            float scale = Float.parseFloat(extractJsonValue(entry, "scale"));

            // Resolver ruta de imagen
            File baseImageFile = resolveRelativeFile(file);
            if (baseImageFile == null || !baseImageFile.exists()) {
                io.Log.log("WARN: LayerManager.loadFromLayersJson - Archivo base no encontrado: " + file);
                continue;
            }
            // Validar hash del archivo base: si difiere, descartar overlay de pintura
            String currentFileHash = computeFileHash(baseImageFile);
            boolean overlayValid = fileHashStored == null || fileHashStored.equals(currentFileHash);
            Layer layer = addLayerFromFile(baseImageFile.getAbsolutePath(), new Point(x, y));
            layer.setName(name);
            layer.setVisible(visible);
            layer.setOpacity(opacity);
            layer.setBrightness(brightness);
            layer.setContrast(contrast);
            layer.setSaturation(saturation);
            layer.setGamma(gamma);
            layer.setSharpness(sharpness);
            layer.setScale(scale);

            if (overlayValid && paintFile != null && !paintFile.isEmpty()) {
                File paint = resolveRelativeFile(paintFile);
                if (paint != null && paint.exists()) {
                    // Intentar cargar como JSON primero (nuevo sistema)
                    if (paintFile.endsWith(".json")) {
                        try {
                            String jsonContent = java.nio.file.Files.readString(paint.toPath());
                            Map<Point, PixelModification> pixelMods = loadPixelModificationsFromJson(jsonContent);
                            if (pixelMods != null && !pixelMods.isEmpty()) {
                                layer.setPixelModifications(pixelMods);
                                io.Log.log("DEBUG: LayerManager.loadFromLayersJson - Cargadas " + pixelMods.size() + " modificaciones de píxeles: " + paint.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            io.Log.log("ERROR: LayerManager.loadFromLayersJson - Error cargando modificaciones JSON: " + e.getMessage());
                        }
                    } else {
                        // Cargar como PNG (sistema anterior para compatibilidad)
                        BufferedImage overlay = ImageIO.read(paint);
                        if (overlay != null && overlay.getType() != BufferedImage.TYPE_INT_ARGB) {
                            BufferedImage argb = new BufferedImage(overlay.getWidth(), overlay.getHeight(), BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g2d = argb.createGraphics();
                            g2d.drawImage(overlay, 0, 0, null);
                            g2d.dispose();
                            overlay = argb;
                        }
                        // Validar hash de overlay si está presente
                        if (paintHashStored != null) {
                            String currentPaintHash = computeFileHash(paint);
                            if (!paintHashStored.equals(currentPaintHash)) {
                                io.Log.log("WARN: LayerManager.loadFromLayersJson - paintHash mismatch, descartando overlay: " + paint.getName());
                                overlay = null;
                            }
                        }
                        if (overlay != null) {
                            layer.setPaintingOverlay(overlay);
                            io.Log.log("DEBUG: LayerManager.loadFromLayersJson - Cargado overlay de pintura: " + paint.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return !layers.isEmpty();
    }

    private File resolveRelativeFile(String ref) {
        if (ref == null || ref.isEmpty()) return null;
        if (ref.startsWith("REL:")) {
            File companionDir = getCompanionDir();
            String name = ref.substring("REL:".length());
            return new File(companionDir, name);
        }
        return new File(ref);
    }
    
    /**
     * Carga modificaciones de píxeles desde contenido JSON
     */
    private Map<Point, PixelModification> loadPixelModificationsFromJson(String jsonContent) {
        Map<Point, PixelModification> modifications = new HashMap<>();
        try {
            // Buscar el objeto pixelModifications
            int pixelModsIndex = jsonContent.indexOf("\"pixelModifications\"");
            if (pixelModsIndex < 0) return modifications;
            
            int startObj = jsonContent.indexOf('{', pixelModsIndex);
            int braceCount = 0;
            int endObj = startObj;
            boolean inString = false;
            char prevChar = ' ';
            
            // Encontrar el cierre del objeto pixelModifications
            for (int i = startObj; i < jsonContent.length(); i++) {
                char c = jsonContent.charAt(i);
                if (c == '"' && prevChar != '\\') {
                    inString = !inString;
                } else if (!inString) {
                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;
                    if (braceCount == 0) {
                        endObj = i;
                        break;
                    }
                }
                prevChar = c;
            }
            
            if (endObj > startObj) {
                String pixelModsContent = jsonContent.substring(startObj + 1, endObj);
                
                // Parsear cada entrada de modificación de píxel
                String[] entries = pixelModsContent.split(",\\s*\"");
                for (String entry : entries) {
                    entry = entry.trim();
                    if (entry.isEmpty()) continue;
                    
                    // Extraer coordenadas "x,y"
                    int colonIndex = entry.indexOf("\":");
                    if (colonIndex < 0) continue;
                    
                    String coordStr = entry.substring(entry.startsWith("\"") ? 1 : 0, colonIndex);
                    String[] coords = coordStr.split(",");
                    if (coords.length != 2) continue;
                    
                    try {
                        int x = Integer.parseInt(coords[0].trim());
                        int y = Integer.parseInt(coords[1].trim());
                        
                        // Extraer el JSON de la modificación
                        String modJson = entry.substring(colonIndex + 2).trim();
                        PixelModification mod = PixelModification.fromJson(modJson);
                        if (mod != null) {
                            modifications.put(new Point(x, y), mod);
                        }
                    } catch (NumberFormatException e) {
                        io.Log.log("WARN: LayerManager.loadPixelModificationsFromJson - Coordenadas inválidas: " + coordStr);
                    }
                }
            }
        } catch (Exception e) {
            io.Log.log("ERROR: LayerManager.loadPixelModificationsFromJson - " + e.getMessage());
        }
        return modifications;
    }

    /**
     * Verifica si un overlay tiene al menos un píxel no transparente
     */
    private boolean hasNonTransparentPixels(BufferedImage img) {
        if (img == null) return false;
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xff;
                if (a != 0) return true;
            }
        }
        return false;
    }

    /**
     * Calcula hash MD5 de un archivo para validar si ha cambiado.
     */
    private String computeFileHash(File f) {
        if (f == null || !f.exists()) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
    
    /**
     * Parsea y carga una capa desde datos JSON
     */
    private void loadLayerFromJson(String jsonData) throws Exception {
        if (!jsonData.startsWith("{") || !jsonData.endsWith("}")) {
            return;
        }
        
        jsonData = jsonData.substring(1, jsonData.length() - 1);
        
        // Extraer valores
        String name = extractJsonValue(jsonData, "name");
        String file = extractJsonValue(jsonData, "file");
        boolean visible = Boolean.parseBoolean(extractJsonValue(jsonData, "visible"));
        int x = Integer.parseInt(extractJsonValue(jsonData, "x"));
        int y = Integer.parseInt(extractJsonValue(jsonData, "y"));
        float opacity = Float.parseFloat(extractJsonValue(jsonData, "opacity"));
        float brightness = Float.parseFloat(extractJsonValue(jsonData, "brightness"));
        float contrast = Float.parseFloat(extractJsonValue(jsonData, "contrast"));
        float saturation = Float.parseFloat(extractJsonValue(jsonData, "saturation"));
        float gamma = Float.parseFloat(extractJsonValue(jsonData, "gamma"));
        float sharpness = Float.parseFloat(extractJsonValue(jsonData, "sharpness"));
        float scale = Float.parseFloat(extractJsonValue(jsonData, "scale"));
        
        // Verificar que el archivo existe
        File imageFile;

        if (file != null && file.startsWith("REL:")) {
            // Reconstruir ruta relativa dentro de la carpeta companion
            File kmvFile;
            if (this.mosaicFile != null) {
                kmvFile = this.mosaicFile.getAbsoluteFile();
            } else {
                String kmvFileName = mosaic.controllers.MainController.STATE_FILE_NAME;
                kmvFile = new File(kmvFileName).getAbsoluteFile();
            }
            String baseName = kmvFile.getName();
            int dot = baseName.lastIndexOf('.');
            if (dot > 0) baseName = baseName.substring(0, dot);
            File companionDir = new File(kmvFile.getParentFile(), baseName + "_layers");
            String relativeFileName = file.substring("REL:".length());
            imageFile = new File(companionDir, relativeFileName);
        } else {
            imageFile = new File(file);
        }

        if (!imageFile.exists()) {
            io.Log.log("WARN: LayerManager.loadLayerFromJson - Archivo de capa no encontrado: " + imageFile.getAbsolutePath());
            return; // Saltar esta capa si no existe
        }
        
    // Crear y configurar la capa usando la ruta absoluta resuelta
    Layer layer = addLayerFromFile(imageFile.getAbsolutePath(), new Point(x, y));
        io.Log.log("DEBUG: LayerManager.loadLayerFromJson - Capa cargada desde: " + imageFile.getAbsolutePath());
        layer.setName(name);
        layer.setVisible(visible);
        layer.setOpacity(opacity);
        layer.setBrightness(brightness);
        layer.setContrast(contrast);
        layer.setSaturation(saturation);
        layer.setGamma(gamma);
        layer.setSharpness(sharpness);
        layer.setScale(scale);
    }
    
    /**
     * Extrae un valor de una cadena JSON simple
     */
    private String extractJsonValue(String jsonData, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = jsonData.indexOf(searchKey);
        if (startIndex == -1) {
            return "";
        }
        
        startIndex += searchKey.length();
        
        // Saltar espacios
        while (startIndex < jsonData.length() && jsonData.charAt(startIndex) == ' ') {
            startIndex++;
        }
        
        int endIndex;
        if (jsonData.charAt(startIndex) == '"') {
            // Valor string
            startIndex++; // Saltar comilla inicial
            endIndex = jsonData.indexOf('"', startIndex);
            if (endIndex == -1) {
                return "";
            }
        } else {
            // Valor numérico o boolean: detener en coma, llave de cierre o espacio
            int i = startIndex;
            while (i < jsonData.length()) {
                char c = jsonData.charAt(i);
                if (c == ',' || c == '}' || c == '\n' || c == '\r' || c == '\t' || c == ' ') {
                    break;
                }
                i++;
            }
            endIndex = i;
        }
        
        return jsonData.substring(startIndex, endIndex).trim();
    }
    
    /**
     * Divide un array JSON en entradas individuales
     */
    private String[] splitJsonArray(String arrayContent) {
        List<String> entries = new ArrayList<>();
        int depth = 0;
        int start = 0;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    entries.add(arrayContent.substring(start, i + 1));
                    start = i + 1;
                    // Buscar la siguiente entrada
                    while (start < arrayContent.length() && 
                           (arrayContent.charAt(start) == ',' || arrayContent.charAt(start) == ' ')) {
                        start++;
                    }
                    i = start - 1; // -1 porque el bucle incrementará
                }
            }
        }
        
        return entries.toArray(new String[0]);
    }
    
    /**
     * Escapa caracteres especiales para JSON
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Normaliza un nombre de capa para usarlo como parte de un nombre de archivo.
     * Sustituye espacios y caracteres no alfanuméricos por '_'.
     */
    private String normalizeName(String name) {
        if (name == null || name.isEmpty()) return "layer";
        // Eliminar acentos simples: podemos dejar tarea futura; por ahora solo reemplazar caracteres no permitidos
        return name.trim().replaceAll("[^a-zA-Z0-9_-]+", "_");
    }
    
    /**
     * NUEVA SOLUCIÓN: Guarda las modificaciones del mosaico y fuerza su reaplicación
     * para preservar pintura cuando colorGrid no está disponible.
     */
    private void saveAndReloadMosaicModifications() throws Exception {
        io.Log.log("DEBUG: saveAndReloadMosaicModifications - INICIADO");
        if (modificationManager == null) {
            io.Log.log("DEBUG: saveAndReloadMosaicModifications - ModificationManager es null, no hay nada que hacer");
            return;
        }
        
        // 1. Obtener las modificaciones actuales del mosaico base
        java.util.Map<String, colors.LEGOColor> baseModifications = new java.util.HashMap<>(modificationManager.getModifications());
        if (baseModifications.isEmpty()) {
            io.Log.log("DEBUG: saveAndReloadMosaicModifications - No hay modificaciones para guardar");
            return;
        }
        
        io.Log.log("DEBUG: saveAndReloadMosaicModifications - Obtenidas " + baseModifications.size() + " modificaciones");
        
        // 2. Obtener ColorGrid del BrickedView directamente si está disponible
        colors.LEGOColorGrid currentColorGrid = null;
        if (brickedView != null) {
            currentColorGrid = brickedView.getColorGrid();
            io.Log.log("DEBUG: saveAndReloadMosaicModifications - ColorGrid obtenido del BrickedView: " + (currentColorGrid != null ? "disponible" : "NULL"));
        }
        
        // 3. Aplicar las modificaciones al ColorGrid disponible usando el método correcto
        if (currentColorGrid != null) {
            modificationManager.restoreModificationsFromCopyAndApply(baseModifications, currentColorGrid);
            io.Log.log("DEBUG: saveAndReloadMosaicModifications - Aplicadas " + baseModifications.size() + " modificaciones al ColorGrid via restoreFromCopy");
        } else {
            io.Log.log("DEBUG: saveAndReloadMosaicModifications - ColorGrid no disponible, no se pueden aplicar modificaciones");
        }
        
        // 4. Forzar actualización visual del viewport con delay para asegurar que terminen todos los procesos
        if (brickedView != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                // Primer repaint inmediato
                brickedView.repaint();
                // Segundo repaint con delay para asegurar consistency
                javax.swing.Timer timer = new javax.swing.Timer(50, e -> {
                    brickedView.repaint();
                    io.Log.log("DEBUG: saveAndReloadMosaicModifications - Ejecutado repaint() con delay");
                });
                timer.setRepeats(false);
                timer.start();
                io.Log.log("DEBUG: saveAndReloadMosaicModifications - Forzado repaint() inmediato y programado repaint con delay");
            });
        } else {
            io.Log.log("DEBUG: saveAndReloadMosaicModifications - BrickedView null, no se puede actualizar vista");
        }
    }
}