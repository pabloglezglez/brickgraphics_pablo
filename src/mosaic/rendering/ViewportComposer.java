package mosaic.rendering;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import mosaic.layers.Layer;
import io.Log;

/**
 * ViewportComposer - Sistema de composición que mezcla múltiples mosaicos independientes.
 * 
 * ARQUITECTURA:
 * - Mosaico principal (imagen base)
 * - Lista de LayerMosaics independientes
 * - Cada LayerMosaic tiene su modo de fusión configurado 
 * - Renderiza todos en un viewport final
 * - El resultado final se usa para generación de instrucciones
 * 
 * MODOS DE FUSIÓN SOPORTADOS:
 * - NORMAL: Superposición estándar con alpha blending
 * - MULTIPLY: Multiplica colores (oscurece)
 * - OVERLAY: Mezcla selectiva que realza contraste
 * - SCREEN: Clarifica y aumenta brillo
 * - SOFT_LIGHT: Iluminación suave
 */
public class ViewportComposer {
    
    public enum BlendMode {
        NORMAL,     // Alpha blending estándar
        MULTIPLY,   // Multiplica - oscurece
        OVERLAY,    // Contraste selectivo
        SCREEN,     // Clarifica
        SOFT_LIGHT  // Iluminación suave
    }
    
    // Mosaico principal (imagen base)
    private BufferedImage mainMosaic;
    private Dimension viewportSize;
    
    // Lista de mosaicos de capas con sus configuraciones
    private List<LayerMosaic> layerMosaics;
    private Map<LayerMosaic, BlendMode> blendModes;
    private Map<LayerMosaic, Float> opacities;
    
    // Cache para el resultado final compuesto
    private BufferedImage composedViewport;
    private boolean needsRecomposition;
    
    /**
     * Constructor para crear el compositor del viewport.
     * @param mainMosaic Imagen del mosaico principal
     * @param viewportSize Tamaño del viewport final
     */
    public ViewportComposer(BufferedImage mainMosaic, Dimension viewportSize) {
        this.mainMosaic = mainMosaic;
        this.viewportSize = new Dimension(viewportSize);
        this.layerMosaics = new ArrayList<>();
        this.blendModes = new HashMap<>();
        this.opacities = new HashMap<>();
        this.needsRecomposition = true;
        
        Log.log("ViewportComposer inicializado - Viewport: " + viewportSize.width + "x" + viewportSize.height);
    }
    
    /**
     * Actualiza el mosaico principal.
     * @param newMainMosaic Nueva imagen del mosaico principal
     */
    public void updateMainMosaic(BufferedImage newMainMosaic) {
        this.mainMosaic = newMainMosaic;
        this.needsRecomposition = true;
        Log.log("ViewportComposer.updateMainMosaic - Mosaico principal actualizado, recomposición marcada");
    }
    
    /**
     * Limpia todas las capas existentes.
     * Útil para re-integración completa.
     */
    public void clearAllLayers() {
        layerMosaics.clear();
        blendModes.clear();
        opacities.clear();
        needsRecomposition = true;
        Log.log("ViewportComposer.clearAllLayers - Todas las capas limpiadas");
    }
    
    /**
     * Añade un LayerMosaic al viewport con configuración de fusión.
     * @param layerMosaic El mosaico de capa a añadir
     * @param blendMode Modo de fusión a usar
     * @param opacity Opacidad de la capa (0.0 - 1.0)
     */
    public void addLayerMosaic(LayerMosaic layerMosaic, BlendMode blendMode, float opacity) {
        if (layerMosaic != null) {
            layerMosaics.add(layerMosaic);
            blendModes.put(layerMosaic, blendMode);
            opacities.put(layerMosaic, Math.max(0.0f, Math.min(1.0f, opacity)));
            needsRecomposition = true;
            
            Log.log("ViewportComposer.addLayerMosaic - Capa '" + layerMosaic.getLayer().getName() + 
                   "' añadida con modo=" + blendMode + " opacidad=" + opacity);
        }
    }
    
    /**
     * Actualiza el modo de fusión de una capa.
     */
    public void setBlendMode(LayerMosaic layerMosaic, BlendMode blendMode) {
        if (layerMosaics.contains(layerMosaic)) {
            blendModes.put(layerMosaic, blendMode);
            needsRecomposition = true;
            Log.log("ViewportComposer.setBlendMode - Capa '" + layerMosaic.getLayer().getName() + 
                   "' modo actualizado a " + blendMode);
        }
    }
    
    /**
     * Actualiza la opacidad de una capa.
     */
    public void setOpacity(LayerMosaic layerMosaic, float opacity) {
        if (layerMosaics.contains(layerMosaic)) {
            opacities.put(layerMosaic, Math.max(0.0f, Math.min(1.0f, opacity)));
            needsRecomposition = true;
            Log.log("ViewportComposer.setOpacity - Capa '" + layerMosaic.getLayer().getName() + 
                   "' opacidad actualizada a " + opacity);
        }
    }
    
    /**
     * Remueve una capa del viewport.
     */
    public void removeLayerMosaic(LayerMosaic layerMosaic) {
        if (layerMosaics.remove(layerMosaic)) {
            blendModes.remove(layerMosaic);
            opacities.remove(layerMosaic);
            needsRecomposition = true;
            Log.log("ViewportComposer.removeLayerMosaic - Capa '" + layerMosaic.getLayer().getName() + "' removida");
        }
    }
    
    /**
     * Actualiza el mosaico principal.
     */
    public void setMainMosaic(BufferedImage newMainMosaic) {
        this.mainMosaic = newMainMosaic;
        needsRecomposition = true;
        Log.log("ViewportComposer.setMainMosaic - Mosaico principal actualizado");
    }
    
    /**
     * Marca que el viewport necesita recomposición.
     */
    public void markForRecomposition() {
        needsRecomposition = true;
    }
    
    /**
     * Compone todos los mosaicos en el viewport final.
     * Este es el núcleo del sistema - mezcla el mosaico principal con todas las capas.
     * @return BufferedImage con el resultado final compuesto
     */
    public BufferedImage composeViewport() {
        if (!needsRecomposition && composedViewport != null) {
            return composedViewport; // Usar cache si no hay cambios
        }
        
        try {
            // Crear nueva imagen para el viewport compuesto
            composedViewport = new BufferedImage(viewportSize.width, viewportSize.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = composedViewport.createGraphics();
            
            try {
                // Configurar calidad de renderizado
                g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // 1. DIBUJAR MOSAICO PRINCIPAL COMO BASE
                if (mainMosaic != null) {
                    g2.drawImage(mainMosaic, 0, 0, viewportSize.width, viewportSize.height, null);
                    Log.log("ViewportComposer.composeViewport - Mosaico principal dibujado");
                }
                
                // 2. COMPONER CADA LAYERMOSAIC EN ORDEN
                for (LayerMosaic layerMosaic : layerMosaics) {
                    BlendMode blendMode = blendModes.get(layerMosaic);
                    float opacity = opacities.get(layerMosaic);
                    
                    if (opacity > 0.0f) {
                        renderLayerWithBlending(g2, layerMosaic, blendMode, opacity);
                    }
                }
                
                Log.log("ViewportComposer.composeViewport - Composición completada con " + layerMosaics.size() + " capas");
                
            } finally {
                g2.dispose();
            }
            
            needsRecomposition = false;
            return composedViewport;
            
        } catch (Exception ex) {
            Log.log("ERROR: ViewportComposer.composeViewport falló: " + ex.getMessage());
            ex.printStackTrace();
            return mainMosaic; // Fallback al mosaico principal
        }
    }
    
    /**
     * Renderiza una LayerMosaic con el modo de fusión especificado.
     */
    private void renderLayerWithBlending(Graphics2D g2, LayerMosaic layerMosaic, BlendMode blendMode, float opacity) {
        try {
            // Crear contexto temporal para la capa
            Graphics2D layerG2 = (Graphics2D) g2.create();
            
            // Aplicar modo de fusión
            Composite composite = getCompositeForBlendMode(blendMode, opacity);
            if (composite != null) {
                layerG2.setComposite(composite);
            }
            
            // Renderizar la LayerMosaic
            layerMosaic.render(layerG2, viewportSize, opacity);
            
            layerG2.dispose();
            
        } catch (Exception ex) {
            Log.log("ERROR: ViewportComposer.renderLayerWithBlending falló para " + 
                   layerMosaic.getLayer().getName() + ": " + ex.getMessage());
        }
    }
    
    /**
     * Obtiene el Composite apropiado para el modo de fusión.
     * NOTA: Java's AlphaComposite tiene limitaciones. Para modos avanzados como MULTIPLY, OVERLAY, 
     * se necesitaría implementación custom con CompositeContext.
     */
    private Composite getCompositeForBlendMode(BlendMode blendMode, float opacity) {
        switch (blendMode) {
            case NORMAL:
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            
            case MULTIPLY:
                // Java no tiene MULTIPLY built-in. Por ahora usar SRC_OVER con menor opacidad
                // TODO: Implementar custom Composite para MULTIPLY real
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity * 0.7f);
            
            case OVERLAY:
                // Similar limitación - usar SRC_OVER modificado
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            
            case SCREEN:
                // Simulación básica con SRC_OVER
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity * 0.9f);
            
            case SOFT_LIGHT:
                // Simulación básica
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity * 0.8f);
            
            default:
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
        }
    }
    
    /**
     * Renderiza el viewport compuesto en un contexto gráfico.
     * @param g2 Contexto donde renderizar
     * @param destBounds Bounds de destino para el renderizado
     */
    public void render(Graphics2D g2, Rectangle destBounds) {
        BufferedImage viewport = composeViewport();
        
        if (viewport != null) {
            g2.drawImage(viewport, destBounds.x, destBounds.y, 
                        destBounds.width, destBounds.height, null);
        }
    }
    
    /**
     * Obtiene el viewport compuesto final.
     * Este es el resultado que debe usar el generador de instrucciones.
     */
    public BufferedImage getFinalViewport() {
        return composeViewport();
    }
    
    /**
     * Obtiene los píxeles finales para generación de instrucciones.
     * @return Array de píxeles RGB del viewport compuesto
     */
    public int[] getFinalPixels() {
        BufferedImage viewport = composeViewport();
        if (viewport != null) {
            int width = viewport.getWidth();
            int height = viewport.getHeight();
            int[] pixels = new int[width * height];
            viewport.getRGB(0, 0, width, height, pixels, 0, width);
            return pixels;
        }
        return new int[0];
    }
    
    // Getters
    public Dimension getViewportSize() { return new Dimension(viewportSize); }
    public List<LayerMosaic> getLayerMosaics() { return new ArrayList<>(layerMosaics); }
    public BlendMode getBlendMode(LayerMosaic layerMosaic) { return blendModes.get(layerMosaic); }
    public Float getOpacity(LayerMosaic layerMosaic) { return opacities.get(layerMosaic); }
    public boolean needsRecomposition() { return needsRecomposition; }
}