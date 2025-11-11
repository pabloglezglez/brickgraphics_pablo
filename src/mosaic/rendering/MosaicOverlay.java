package mosaic.rendering;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import transforms.ToBricksTransform;
import colors.LEGOColorGrid;
import bricks.ToBricksType;
import io.Log;

/**
 * Representa un mosaico overlay individual con su configuración independiente.
 * Cada overlay puede tener:
 * - Imagen fuente diferente
 * - Paleta de colores independiente
 * - Tipo de ladrillos específico
 * - Posición y opacidad personalizadas
 */
public class MosaicOverlay {
    
    private String name;
    private BufferedImage sourceImage;
    private BufferedImage processedMosaic;
    private Point position;
    private float opacity;
    private boolean visible;
    private boolean needsReprocessing;
    
    // Configuración de mosaico independiente
    private LEGOColorGrid colorGrid;
    private ToBricksType brickType;
    private String colorSetName;
    
    // Transform para procesamiento LEGO
    private ToBricksTransform transform;
    
    /**
     * Constructor para crear un nuevo overlay de mosaico
     */
    public MosaicOverlay(String name, BufferedImage sourceImage) {
        this.name = name;
        this.sourceImage = sourceImage;
        this.position = new Point(0, 0);
        this.opacity = 1.0f;
        this.visible = true;
        this.needsReprocessing = true;
        this.colorSetName = "Default";
        this.brickType = ToBricksType.Bricks1x1;
        
        Log.log("MosaicOverlay creado: " + name + " - Tamaño: " + 
                sourceImage.getWidth() + "x" + sourceImage.getHeight());
    }
    
    /**
     * Configura la paleta de colores para este overlay
     */
    public void setColorConfiguration(LEGOColorGrid colorGrid, String setName) {
        this.colorGrid = colorGrid;
        this.colorSetName = setName;
        this.needsReprocessing = true;
        
        Log.log("MosaicOverlay '" + name + "' - Nueva paleta: " + setName);
    }
    
    /**
     * Establece el tipo de ladrillos
     */
    public void setBrickType(ToBricksType brickType) {
        this.brickType = brickType;
        this.needsReprocessing = true;
        
        Log.log("MosaicOverlay '" + name + "' - Tipo ladrillos: " + brickType);
    }
    
    /**
     * Configura el transform para procesamiento LEGO
     */
    public void setTransform(ToBricksTransform transform) {
        this.transform = transform;
        this.needsReprocessing = true;
    }
    
    /**
     * Procesa la imagen fuente en mosaico LEGO
     */
    public void processMosaic() {
        if (!needsReprocessing || transform == null || colorGrid == null) {
            return;
        }
        
        try {
            // Configurar transform con parámetros específicos de este overlay
            transform.setColorGrid(colorGrid);
            transform.setToBricksType(brickType);
            
            // Procesar imagen a mosaico LEGO
            processedMosaic = transform.filter(sourceImage, null);
            needsReprocessing = false;
            
            Log.log("MosaicOverlay '" + name + "' - Mosaico procesado: " + 
                    processedMosaic.getWidth() + "x" + processedMosaic.getHeight());
            
        } catch (Exception e) {
            Log.log("ERROR: MosaicOverlay '" + name + "' - Falló procesamiento: " + e.getMessage());
        }
    }
    
    /**
     * Renderiza este overlay en el contexto gráfico
     */
    public void render(Graphics2D g2d, Dimension canvasSize) {
        if (!visible || processedMosaic == null) {
            return;
        }
        
        // Guardar composite original
        AlphaComposite originalComposite = (AlphaComposite) g2d.getComposite();
        
        // Aplicar opacidad
        if (opacity < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }
        
        // Renderizar en la posición especificada
        g2d.drawImage(processedMosaic, position.x, position.y, null);
        
        // Restaurar composite original
        g2d.setComposite(originalComposite);
    }
    
    /**
     * Setters y Getters
     */
    public void setPosition(Point position) {
        this.position = new Point(position);
    }
    
    public Point getPosition() {
        return new Point(position);
    }
    
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }
    
    public float getOpacity() {
        return opacity;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getColorSetName() {
        return colorSetName;
    }
    
    public ToBricksType getBrickType() {
        return brickType;
    }
    
    public BufferedImage getSourceImage() {
        return sourceImage;
    }
    
    public BufferedImage getProcessedMosaic() {
        return processedMosaic;
    }
    
    public boolean needsReprocessing() {
        return needsReprocessing;
    }
    
    /**
     * Información de debug
     */
    public String getDebugInfo() {
        return String.format("Overlay '%s' - Set: %s, Bricks: %s, Pos: (%d,%d), Op: %.2f, Vis: %s", 
                           name, colorSetName, brickType, position.x, position.y, opacity, visible);
    }
}