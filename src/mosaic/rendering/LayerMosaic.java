package mosaic.rendering;

import java.awt.*;
import java.awt.image.BufferedImage;
import mosaic.layers.Layer;
import io.Log;
import transforms.ToBricksTransform;

/**
 * Mosaico independiente para una capa específica - Versión simplificada inicial.
 * 
 * CONCEPTO ARQUITECTÓNICO:
 * Cada LayerMosaic encapsula:
 * - Su propio sistema de mosaico independiente
 * - Su sistema de pintado independiente  
 * - Preservación de transparencia original
 * - Capacidad de renderizado con modos de fusión
 * 
 * Esta es una versión inicial para demostrar el concepto.
 * La versión completa requerirá integración con ToBricksTransform.
 */
public class LayerMosaic {
    private Layer layer;
    private BufferedImage originalImage;
    private Dimension mosaicSize;
    private boolean hasTransparency;
    
    // Sistema de pintado independiente simplificado
    private PaintOverlay paintOverlay;
    
    // Transform para convertir a LEGO (compartido desde BrickedView)
    private ToBricksTransform sharedTransform;
    
    /**
     * Constructor para crear un mosaico independiente para una capa.
     * @param layer La capa asociada
     * @param mosaicSize Tamaño del mosaico en píxeles
     */
    public LayerMosaic(Layer layer, Dimension mosaicSize) {
        this.layer = layer;
        this.mosaicSize = mosaicSize;
        this.originalImage = layer.getImage();
        this.sharedTransform = null; // Por ahora null, se establecerá después
        
        // Detectar si la capa tiene transparencia
        this.hasTransparency = hasImageTransparency(originalImage);
        
        // Inicializar sistema de pintado independiente
        this.paintOverlay = new PaintOverlay(mosaicSize);
        
        Log.log("LayerMosaic creado para capa '" + layer.getName() + "' - " +
                "Transparencia: " + hasTransparency + " - Tamaño: " + mosaicSize.width + "x" + mosaicSize.height);
    }
    
    /**
     * Establece el ToBricksTransform a usar para renderizado LEGO.
     * @param transform El transform para procesar imágenes a LEGO
     */
    public void setSharedTransform(ToBricksTransform transform) {
        this.sharedTransform = transform;
    }
    
    /**
     * Detecta si una imagen tiene transparencia.
     */
    private boolean hasImageTransparency(BufferedImage img) {
        if (img == null) return false;
        
        // Verificar si el tipo de imagen soporta transparencia
        if (img.getType() == BufferedImage.TYPE_INT_ARGB ||
            img.getType() == BufferedImage.TYPE_4BYTE_ABGR ||
            img.getColorModel().hasAlpha()) {
            
            // Verificar si hay píxeles con alpha < 255 (muestreo para performance)
            int width = img.getWidth();
            int height = img.getHeight();
            
            for (int y = 0; y < height; y += 5) { // Muestreo cada 5 píxeles
                for (int x = 0; x < width; x += 5) {
                    int pixel = img.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xFF;
                    if (alpha < 255) {
                        return true; // Encontramos transparencia
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Renderiza este mosaico de capa en el contexto gráfico dado.
     * Procesa la imagen de la capa usando ToBricksTransform para generar mosaico LEGO real.
     * 
     * @param g2 Contexto gráfico donde renderizar
     * @param renderSize Tamaño de renderizado en pantalla
     * @param opacity Opacidad para la capa (0.0 - 1.0)
     */
    public void render(Graphics2D g2, Dimension renderSize, float opacity) {
        if (originalImage == null) {
            return;
        }
        
        try {
            // Crear contexto temporal para renderizado
            Graphics2D tempG2 = (Graphics2D) g2.create();
            
            // Configurar composición con opacidad
            if (opacity < 1.0f) {
                tempG2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            }
            
            // NUEVO: Usar ToBricksTransform para renderizar mosaico LEGO real
            if (sharedTransform != null) {
                try {
                    // Procesar la imagen para convertirla a LEGO usando el transform compartido
                    sharedTransform.transform(originalImage);
                    
                    // Renderizar directamente usando el algoritmo LEGO
                    sharedTransform.drawAll(tempG2, renderSize);
                    
                    Log.log("✅ LayerMosaic.render - Mosaico LEGO generado para '" + layer.getName() + "' - Tamaño: " + renderSize.width + "x" + renderSize.height);
                    
                } catch (Exception ex) {
                    Log.log("⚠️ LayerMosaic.render - Error generando mosaico LEGO, usando imagen original: " + ex.getMessage());
                    
                    // Fallback: Renderizar imagen original escalada
                    tempG2.drawImage(originalImage, 0, 0, renderSize.width, renderSize.height, null);
                }
            } else {
                // Fallback: Renderizar imagen original escalada
                tempG2.drawImage(originalImage, 0, 0, renderSize.width, renderSize.height, null);
            }
            
            // Renderizar overlay de pintado de esta capa si está habilitado
            if (paintOverlay != null && paintOverlay.isEnabled()) {
                Graphics2D overlayG2 = (Graphics2D) tempG2.create();
                
                // Escalar overlay si es necesario
                if (!renderSize.equals(mosaicSize)) {
                    double scaleX = (double) renderSize.width / mosaicSize.width;
                    double scaleY = (double) renderSize.height / mosaicSize.height;
                    overlayG2.scale(scaleX, scaleY);
                }
                
                paintOverlay.render(overlayG2, 0.8f);
                overlayG2.dispose();
            }
            
            tempG2.dispose();
            
        } catch (Exception ex) {
            Log.log("ERROR: LayerMosaic.render falló para " + layer.getName() + ": " + ex.getMessage());
        }
    }
    
    /**
     * Aplica pintura en este mosaico de capa.
     * @param x coordenada X en píxeles del mosaico
     * @param y coordenada Y en píxeles del mosaico  
     * @param radius radio del pincel
     * @param color color a aplicar
     * @return true si se aplicó correctamente
     */
    public boolean applyPaint(int x, int y, int radius, Color color) {
        if (paintOverlay != null) {
            try {
                boolean result = paintOverlay.applyBrushStroke(x, y, radius, color);
                if (result) {
                    Log.log("LayerMosaic.applyPaint en '" + layer.getName() + 
                           "' - pos=(" + x + "," + y + ") radius=" + radius);
                }
                return result;
            } catch (Exception ex) {
                Log.log("ERROR: LayerMosaic.applyPaint falló: " + ex.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * Limpia todo el pintado de esta capa.
     */
    public void clearPaint() {
        if (paintOverlay != null) {
            paintOverlay.clearOverlay();
            Log.log("LayerMosaic.clearPaint - pintado limpiado en '" + layer.getName() + "'");
        }
    }
    
    /**
     * Habilita/deshabilita el sistema de pintado para esta capa.
     */
    public void setPaintEnabled(boolean enabled) {
        if (paintOverlay != null) {
            paintOverlay.setEnabled(enabled);
        }
    }
    
    /**
     * Verifica si el pintado está habilitado para esta capa.
     */
    public boolean isPaintEnabled() {
        return paintOverlay != null && paintOverlay.isEnabled();
    }
    
    // Getters
    public Layer getLayer() { return layer; }
    public boolean hasTransparency() { return hasTransparency; }
    public Dimension getMosaicSize() { return mosaicSize; }
    public PaintOverlay getPaintOverlay() { return paintOverlay; }
}