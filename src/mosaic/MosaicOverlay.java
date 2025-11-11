package mosaic;

import java.awt.*;
import java.awt.image.BufferedImage;
import colors.LEGOColorGrid;
import bricks.ToBricksType;
import transforms.ToBricksTransform;
import mosaic.controllers.ColorController;

/**
 * Representa un mosaico individual que puede ser superpuesto sobre otros
 * con configuración independiente de colores, tipo de ladrillos, posición y opacidad.
 * 
 * @author BrickGraphics
 */
public class MosaicOverlay {
    
    // Configuración del overlay
    private String name;
    private boolean visible;
    private float opacity;
    private Point position;
    private Dimension size;
    
    // Configuración del mosaico
    private LEGOColorGrid colorGrid;
    private String colorSetName;
    private ToBricksType brickType;
    private BufferedImage sourceImage;
    private BufferedImage processedMosaic;
    
    // Estado del procesamiento
    private boolean needsReprocessing;
    private long lastProcessTime;
    
    /**
     * Constructor para un nuevo overlay de mosaico
     */
    public MosaicOverlay(String name) {
        this.name = name;
        this.visible = true;
        this.opacity = 1.0f;
        this.position = new Point(0, 0);
        this.size = new Dimension(100, 100);
        this.brickType = ToBricksType.STUD_FROM_TOP;
        this.needsReprocessing = true;
        this.lastProcessTime = 0;
    }
    
    /**
     * Constructor completo
     */
    public MosaicOverlay(String name, BufferedImage sourceImage, Point position) {
        this(name);
        this.sourceImage = sourceImage;
        this.position = new Point(position.x, position.y);
        if (sourceImage != null) {
            this.size = new Dimension(sourceImage.getWidth(), sourceImage.getHeight());
        }
    }
    
    // Getters y Setters básicos
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            // Si se oculta, liberamos memoria del mosaico procesado si no se usa
            // (implementación opcional para optimizar memoria)
        }
    }
    
    public float getOpacity() {
        return opacity;
    }
    
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }
    
    public Point getPosition() {
        return new Point(position.x, position.y);
    }
    
    public void setPosition(Point position) {
        this.position = new Point(position.x, position.y);
    }
    
    public void setPosition(int x, int y) {
        this.position = new Point(x, y);
    }
    
    public Dimension getSize() {
        return new Dimension(size.width, size.height);
    }
    
    public void setSize(Dimension size) {
        this.size = new Dimension(size.width, size.height);
        this.needsReprocessing = true;
    }
    
    public BufferedImage getSourceImage() {
        return sourceImage;
    }
    
    public void setSourceImage(BufferedImage sourceImage) {
        this.sourceImage = sourceImage;
        this.needsReprocessing = true;
        if (sourceImage != null) {
            this.size = new Dimension(sourceImage.getWidth(), sourceImage.getHeight());
        }
    }
    
    // Configuración del mosaico
    public LEGOColorGrid getColorGrid() {
        return colorGrid;
    }
    
    public void setColorConfiguration(LEGOColorGrid colorGrid, String setName) {
        this.colorGrid = colorGrid;
        this.colorSetName = setName;
        this.needsReprocessing = true;
    }
    
    public String getColorSetName() {
        return colorSetName;
    }
    
    public ToBricksType getBrickType() {
        return brickType;
    }
    
    public void setBrickType(ToBricksType brickType) {
        this.brickType = brickType;
        this.needsReprocessing = true;
    }
    
    /**
     * Procesa la imagen fuente para convertirla en mosaico LEGO
     */
    public void processMosaic() {
        if (sourceImage == null || colorGrid == null) {
            return;
        }
        
        if (!needsReprocessing && processedMosaic != null) {
            return; // Ya está procesado
        }
        
        try {
            // Por ahora, simplemente escalamos la imagen sin aplicar transformación LEGO
            // La implementación completa requerirá integración con el sistema ToBricksTransform
            processedMosaic = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = processedMosaic.createGraphics();
            
            // Configurar calidad de renderizado
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            // Escalar y dibujar la imagen fuente
            g2d.drawImage(sourceImage, 0, 0, size.width, size.height, null);
            
            g2d.dispose();
            
            needsReprocessing = false;
            lastProcessTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            System.err.println("Error processing mosaic for overlay " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Renderiza este overlay en el contexto gráfico dado
     */
    public void render(Graphics2D g2d, Dimension canvasSize) {
        if (!visible || opacity <= 0.0f) {
            return;
        }
        
        // Procesar si es necesario
        if (needsReprocessing || processedMosaic == null) {
            processMosaic();
        }
        
        if (processedMosaic == null) {
            return;
        }
        
        // Guardar el estado original del contexto gráfico
        Composite originalComposite = g2d.getComposite();
        
        try {
            // Configurar la opacidad
            if (opacity < 1.0f) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            }
            
            // Calcular posición de renderizado (puede estar fuera del canvas)
            int renderX = position.x;
            int renderY = position.y;
            
            // Renderizar el mosaico procesado
            g2d.drawImage(processedMosaic, renderX, renderY, null);
            
        } finally {
            // Restaurar el estado del contexto gráfico
            g2d.setComposite(originalComposite);
        }
    }
    
    /**
     * Verifica si este overlay intersecta con el área visible del canvas
     */
    public boolean intersectsCanvas(Dimension canvasSize) {
        Rectangle overlayBounds = new Rectangle(position.x, position.y, size.width, size.height);
        Rectangle canvasBounds = new Rectangle(0, 0, canvasSize.width, canvasSize.height);
        return overlayBounds.intersects(canvasBounds);
    }
    
    /**
     * Calcula el área de intersección con el canvas
     */
    public Rectangle getVisibleArea(Dimension canvasSize) {
        Rectangle overlayBounds = new Rectangle(position.x, position.y, size.width, size.height);
        Rectangle canvasBounds = new Rectangle(0, 0, canvasSize.width, canvasSize.height);
        return overlayBounds.intersection(canvasBounds);
    }
    
    /**
     * Limpia recursos para liberar memoria
     */
    public void dispose() {
        if (processedMosaic != null) {
            processedMosaic.flush();
            processedMosaic = null;
        }
        if (sourceImage != null) {
            sourceImage.flush();
            sourceImage = null;
        }
    }
    
    /**
     * Información de estado para debugging
     */
    @Override
    public String toString() {
        return String.format("MosaicOverlay[name=%s, visible=%b, opacity=%.2f, pos=(%d,%d), size=%dx%d, processed=%b]",
                name, visible, opacity, position.x, position.y, size.width, size.height, 
                (processedMosaic != null));
    }
    
    /**
     * Obtiene estadísticas de memoria utilizada por este overlay
     */
    public long getMemoryUsage() {
        long usage = 0;
        if (sourceImage != null) {
            usage += sourceImage.getWidth() * sourceImage.getHeight() * 4; // ARGB
        }
        if (processedMosaic != null) {
            usage += processedMosaic.getWidth() * processedMosaic.getHeight() * 4; // ARGB
        }
        return usage;
    }
    
    /**
     * Marca que este overlay necesita ser reprocesado
     */
    public void markForReprocessing() {
        this.needsReprocessing = true;
    }
    
    /**
     * Verifica si necesita reprocesamiento
     */
    public boolean needsReprocessing() {
        return needsReprocessing;
    }
}