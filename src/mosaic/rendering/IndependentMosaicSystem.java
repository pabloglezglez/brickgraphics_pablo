package mosaic.rendering;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import mosaic.layers.LayerManager;
import mosaic.layers.Layer;
import io.Log;

/**
 * Demostración del nuevo sistema de mosaicos independientes.
 * Integración inicial de tu propuesta arquitectónica:
 * 
 * ARQUITECTURA PROPUESTA IMPLEMENTADA:
 * 1. ✅ Mosaico principal para imagen base
 * 2. ✅ LayerMosaic independiente por capa con:
 *    - Sistema de pintado independiente
 *    - Preservación de transparencia
 *    - Sistema de mosaico independiente (simplificado)
 * 3. ✅ ViewportComposer para mezclar todo con modos de fusión
 * 4. ✅ Viewport final para generación de instrucciones
 * 
 * Esta es una demostración funcional del concepto.
 */
public class IndependentMosaicSystem {
    
    private BufferedImage mainMosaicImage;
    private ViewportComposer viewportComposer;
    private Dimension mosaicSize;
    
    /**
     * Inicializa el sistema de mosaicos independientes.
     * @param mainImage Imagen principal base
     * @param mosaicSize Tamaño del mosaico
     */
    public IndependentMosaicSystem(BufferedImage mainImage, Dimension mosaicSize) {
        this.mosaicSize = mosaicSize;
        this.mainMosaicImage = mainImage;
        
        // Crear el compositor de viewport
        this.viewportComposer = new ViewportComposer(mainImage, mosaicSize);
        
        Log.log("IndependentMosaicSystem inicializado - Tamaño: " + 
               mosaicSize.width + "x" + mosaicSize.height);
    }
    
    /**
     * Integra el LayerManager existente creando LayerMosaics independientes.
     * @param layerManager El LayerManager actual del sistema
     */
    public void integrateWithLayerManager(LayerManager layerManager) {
        if (layerManager == null) {
            Log.log("WARN: IndependentMosaicSystem.integrateWithLayerManager - layerManager es null");
            return;
        }
        
        try {
            // IMPORTANTE: Limpiar capas existentes para re-integración limpia
            viewportComposer.clearAllLayers();
            
            List<Layer> layers = layerManager.getLayers();
            Log.log("IndependentMosaicSystem.integrateWithLayerManager - Integrando " + layers.size() + " capas");
            
            for (Layer layer : layers) {
                // Crear LayerMosaic independiente para cada capa
                LayerMosaic layerMosaic = new LayerMosaic(layer, mosaicSize);
                
                // Determinar modo de fusión basado en propiedades de la capa
                ViewportComposer.BlendMode blendMode = determineBlendMode(layer);
                
                // Determinar opacidad
                float opacity = determineOpacity(layer);
                
                // Añadir al compositor
                viewportComposer.addLayerMosaic(layerMosaic, blendMode, opacity);
                
                Log.log("IndependentMosaicSystem - LayerMosaic creado para '" + layer.getName() + 
                       "' con modo=" + blendMode + " opacidad=" + opacity);
            }
            
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.integrateWithLayerManager falló: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Determina el modo de fusión apropiado para una capa.
     * En el futuro esto puede basarse en metadatos de la capa.
     */
    private ViewportComposer.BlendMode determineBlendMode(Layer layer) {
        // Por ahora usar NORMAL para todas las capas
        // En el futuro: leer de layer.getBlendMode() o similares
        return ViewportComposer.BlendMode.NORMAL;
    }
    
    /**
     * Determina la opacidad apropiada para una capa.
     */
    private float determineOpacity(Layer layer) {
        // Por ahora usar opacidad completa
        // En el futuro: leer de layer.getOpacity() o similares
        return 1.0f;
    }
    
    /**
     * Aplica pintura a una capa específica.
     * @param layerName Nombre de la capa
     * @param x coordenada X
     * @param y coordenada Y 
     * @param radius radio del pincel
     * @param color color a aplicar
     * @return true si se aplicó correctamente
     */
    public boolean paintToLayer(String layerName, int x, int y, int radius, Color color) {
        try {
            // Buscar LayerMosaic por nombre de capa
            for (LayerMosaic layerMosaic : viewportComposer.getLayerMosaics()) {
                if (layerMosaic.getLayer().getName().equals(layerName)) {
                    boolean result = layerMosaic.applyPaint(x, y, radius, color);
                    if (result) {
                        // Marcar viewport para recomposición
                        viewportComposer.markForRecomposition();
                        Log.log("IndependentMosaicSystem.paintToLayer - Pintado aplicado a '" + layerName + 
                               "' en (" + x + "," + y + ") r=" + radius);
                    }
                    return result;
                }
            }
            
            Log.log("WARN: IndependentMosaicSystem.paintToLayer - Capa '" + layerName + "' no encontrada");
            return false;
            
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.paintToLayer falló: " + ex.getMessage());
            return false;
        }
    }
    
    /**
     * Habilita/deshabilita el pintado para una capa específica.
     */
    public void setLayerPaintEnabled(String layerName, boolean enabled) {
        try {
            for (LayerMosaic layerMosaic : viewportComposer.getLayerMosaics()) {
                if (layerMosaic.getLayer().getName().equals(layerName)) {
                    layerMosaic.setPaintEnabled(enabled);
                    Log.log("IndependentMosaicSystem.setLayerPaintEnabled - Capa '" + layerName + 
                           "' pintado " + (enabled ? "habilitado" : "deshabilitado"));
                    return;
                }
            }
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.setLayerPaintEnabled falló: " + ex.getMessage());
        }
    }
    
    /**
     * Limpia el pintado de una capa específica.
     */
    public void clearLayerPaint(String layerName) {
        try {
            for (LayerMosaic layerMosaic : viewportComposer.getLayerMosaics()) {
                if (layerMosaic.getLayer().getName().equals(layerName)) {
                    layerMosaic.clearPaint();
                    viewportComposer.markForRecomposition();
                    Log.log("IndependentMosaicSystem.clearLayerPaint - Pintado limpiado en '" + layerName + "'");
                    return;
                }
            }
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.clearLayerPaint falló: " + ex.getMessage());
        }
    }
    
    /**
     * Aplica pintura directamente al mosaico principal.
     * @param x coordenada X
     * @param y coordenada Y
     * @param radius radio del pincel
     * @param color color a aplicar
     * @return true si se aplicó correctamente
     */
    public boolean paintToMainMosaic(int x, int y, int radius, Color color) {
        try {
            if (mainMosaicImage == null) {
                Log.log("WARN: paintToMainMosaic - mainMosaicImage es null");
                return false;
            }
            
            // Crear Graphics2D para pintar directamente en el mosaico principal
            Graphics2D g2 = mainMosaicImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            
            // Aplicar pintura circular
            g2.fillOval(x - radius/2, y - radius/2, radius, radius);
            g2.dispose();
            
            // Marcar para recomposición
            viewportComposer.markForRecomposition();
            
            Log.log("IndependentMosaicSystem.paintToMainMosaic - Pintado aplicado al mosaico principal en (" + 
                   x + "," + y + ") r=" + radius);
            return true;
            
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.paintToMainMosaic falló: " + ex.getMessage());
            return false;
        }
    }
    
    /**
     * Actualiza el mosaico principal con una nueva imagen.
     * @param newMainMosaic Nueva imagen del mosaico principal
     */
    public void updateMainMosaic(BufferedImage newMainMosaic) {
        try {
            this.mainMosaicImage = newMainMosaic;
            if (viewportComposer != null) {
                viewportComposer.updateMainMosaic(newMainMosaic);
                Log.log("IndependentMosaicSystem.updateMainMosaic - Mosaico principal actualizado");
            }
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.updateMainMosaic falló: " + ex.getMessage());
        }
    }
    
    /**
     * Configura el modo de fusión de una capa.
     */
    public void setLayerBlendMode(String layerName, ViewportComposer.BlendMode blendMode) {
        try {
            for (LayerMosaic layerMosaic : viewportComposer.getLayerMosaics()) {
                if (layerMosaic.getLayer().getName().equals(layerName)) {
                    viewportComposer.setBlendMode(layerMosaic, blendMode);
                    Log.log("IndependentMosaicSystem.setLayerBlendMode - Capa '" + layerName + 
                           "' modo establecido a " + blendMode);
                    return;
                }
            }
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.setLayerBlendMode falló: " + ex.getMessage());
        }
    }
    
    /**
     * Renderiza el viewport final compuesto.
     * @param g2 Contexto gráfico
     * @param bounds Área de renderizado
     */
    public void render(Graphics2D g2, Rectangle bounds) {
        try {
            viewportComposer.render(g2, bounds);
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.render falló: " + ex.getMessage());
        }
    }
    
    /**
     * Obtiene el viewport final compuesto para generación de instrucciones.
     * ESTE ES EL PUNTO CLAVE: El generador de instrucciones debe leer 
     * los píxeles finales de este viewport en lugar de capas individuales.
     * @return BufferedImage con el resultado final compuesto
     */
    public BufferedImage getFinalInstructionsViewport() {
        try {
            BufferedImage finalViewport = viewportComposer.getFinalViewport();
            Log.log("IndependentMosaicSystem.getFinalInstructionsViewport - Viewport final generado para instrucciones");
            return finalViewport;
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.getFinalInstructionsViewport falló: " + ex.getMessage());
            return mainMosaicImage; // Fallback
        }
    }
    
    /**
     * Obtiene los píxeles finales del viewport compuesto.
     * @return Array de píxeles RGB del viewport final
     */
    public int[] getFinalPixelsForInstructions() {
        try {
            return viewportComposer.getFinalPixels();
        } catch (Exception ex) {
            Log.log("ERROR: IndependentMosaicSystem.getFinalPixelsForInstructions falló: " + ex.getMessage());
            return new int[0];
        }
    }
    
    // Getters
    public ViewportComposer getViewportComposer() { return viewportComposer; }
    public Dimension getMosaicSize() { return new Dimension(mosaicSize); }
    public BufferedImage getMainMosaicImage() { return mainMosaicImage; }
}