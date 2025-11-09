package mosaic.rendering;

import java.awt.*;
import java.awt.image.BufferedImage;
import io.*;

/**
 * Sistema de overlay de pintura global que se superpone al mosaico final.
 * El pintado es independiente de las capas y permanece estable cuando estas se mueven.
 */
public class PaintOverlay {
    private BufferedImage overlayImage;
    private boolean enabled = false;
    private Dimension mosaicSize;
    
    /**
     * Constructor que establece el tamaño del mosaico sobre el que se pintará
     */
    public PaintOverlay(Dimension mosaicSize) {
        this.mosaicSize = new Dimension(mosaicSize);
        initializeOverlay();
    }
    
    /**
     * Inicializa el overlay con el tamaño del mosaico
     */
    private void initializeOverlay() {
        if (mosaicSize.width > 0 && mosaicSize.height > 0) {
            overlayImage = new BufferedImage(mosaicSize.width, mosaicSize.height, BufferedImage.TYPE_INT_ARGB);
            clearOverlay();
        }
    }
    
    /**
     * Actualiza el tamaño del overlay cuando cambia el tamaño del mosaico
     */
    public void updateMosaicSize(Dimension newSize) {
        if (!newSize.equals(this.mosaicSize)) {
            BufferedImage oldOverlay = overlayImage;
            this.mosaicSize = new Dimension(newSize);
            initializeOverlay();
            
            // Preservar pintura existente si había
            if (oldOverlay != null && enabled) {
                Graphics2D g2d = overlayImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.drawImage(oldOverlay, 0, 0, newSize.width, newSize.height, null);
                g2d.dispose();
            }
            
            io.Log.log("DEBUG: PaintOverlay - Actualizado tamaño de " + oldOverlay.getWidth() + "x" + oldOverlay.getHeight() + 
                      " a " + newSize.width + "x" + newSize.height);
        }
    }
    
    /**
     * Aplica un trazo de pincel en las coordenadas del mosaico final
     */
    public boolean applyBrushStroke(int mosaicX, int mosaicY, int radius, Color color) {
        if (!enabled || overlayImage == null) {
            return false;
        }
        
        int argb = color.getRGB();
        boolean modified = false;
        int r2 = radius * radius;
        
        // Bounding box para optimización
        int minX = Math.max(0, mosaicX - radius);
        int maxX = Math.min(overlayImage.getWidth() - 1, mosaicX + radius);
        int minY = Math.max(0, mosaicY - radius);
        int maxY = Math.min(overlayImage.getHeight() - 1, mosaicY + radius);
        
        for (int y = minY; y <= maxY; y++) {
            int dy = y - mosaicY;
            for (int x = minX; x <= maxX; x++) {
                int dx = x - mosaicX;
                if (dx * dx + dy * dy <= r2) {
                    overlayImage.setRGB(x, y, argb);
                    modified = true;
                }
            }
        }
        
        if (modified) {
            io.Log.log("DEBUG: PaintOverlay - Aplicado trazo en (" + mosaicX + "," + mosaicY + 
                      ") radio=" + radius + " color=" + Integer.toHexString(argb));
        }
        
        return modified;
    }
    
    /**
     * Renderiza el overlay sobre el mosaico
     */
    public void render(Graphics2D g2d, float opacity) {
        if (!enabled || overlayImage == null) {
            return;
        }
        
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2d.drawImage(overlayImage, 0, 0, null);
        g2d.setComposite(originalComposite);
    }
    
    /**
     * Limpia todo el overlay de pintura
     */
    public void clearOverlay() {
        if (overlayImage != null) {
            Graphics2D g2d = overlayImage.createGraphics();
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, overlayImage.getWidth(), overlayImage.getHeight());
            g2d.dispose();
            io.Log.log("DEBUG: PaintOverlay - Overlay limpiado");
        }
    }
    
    /**
     * Habilita o deshabilita el overlay de pintura
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        io.Log.log("DEBUG: PaintOverlay - " + (enabled ? "Habilitado" : "Deshabilitado"));
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public BufferedImage getOverlayImage() {
        return overlayImage;
    }
    
    public Dimension getMosaicSize() {
        return new Dimension(mosaicSize);
    }
    
    /**
     * Verifica si hay contenido pintado en el overlay
     */
    public boolean hasContent() {
        if (!enabled || overlayImage == null) {
            return false;
        }
        
        // Verificación rápida: buscar píxeles no transparentes
        for (int y = 0; y < overlayImage.getHeight(); y += 10) { // Sampling para performance
            for (int x = 0; x < overlayImage.getWidth(); x += 10) {
                int argb = overlayImage.getRGB(x, y);
                if ((argb >>> 24) > 0) { // Tiene alpha > 0
                    return true;
                }
            }
        }
        return false;
    }
}