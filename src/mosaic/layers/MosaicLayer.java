package mosaic.layers;

import java.awt.image.BufferedImage;
import java.awt.Point;
import colors.LEGOColorGrid;
import bricks.ToBricksType;
import io.Log;

/**
 * Extensión de Layer que soporta configuración de mosaico independiente.
 * Permite usar paletas de colores y tipos de ladrillos diferentes por capa.
 */
public class MosaicLayer extends Layer {
    
    // Configuración de mosaico específica para esta capa
    private LEGOColorGrid independentColorGrid;
    private ToBricksType independentBrickType;
    private boolean useMosaicMode;
    private String colorSetName;
    
    /**
     * Constructor para crear una capa con configuración de mosaico independiente
     */
    public MosaicLayer(String name, BufferedImage image, Point position) {
        super(name, image, position);
        this.useMosaicMode = false;
        this.colorSetName = "Default";
        this.independentBrickType = ToBricksType.Bricks1x1; // Por defecto 1x1
        
        Log.log("MosaicLayer creado: " + name + " - Modo mosaico: " + useMosaicMode);
    }
    
    /**
     * Habilita el modo mosaico para esta capa
     */
    public void enableMosaicMode(boolean enable) {
        this.useMosaicMode = enable;
        Log.log("MosaicLayer '" + getName() + "' - Modo mosaico " + 
               (enable ? "habilitado" : "deshabilitado"));
    }
    
    /**
     * Establece la paleta de colores independiente para esta capa
     */
    public void setIndependentColorGrid(LEGOColorGrid colorGrid, String setName) {
        this.independentColorGrid = colorGrid;
        this.colorSetName = setName;
        Log.log("MosaicLayer '" + getName() + "' - Paleta establecida: " + setName);
    }
    
    /**
     * Establece el tipo de ladrillos para esta capa
     */
    public void setIndependentBrickType(ToBricksType brickType) {
        this.independentBrickType = brickType;
        Log.log("MosaicLayer '" + getName() + "' - Tipo de ladrillos: " + brickType);
    }
    
    /**
     * Retorna true si esta capa debe procesarse como mosaico independiente
     */
    public boolean isInMosaicMode() {
        return useMosaicMode;
    }
    
    /**
     * Obtiene la paleta de colores independiente
     */
    public LEGOColorGrid getIndependentColorGrid() {
        return independentColorGrid;
    }
    
    /**
     * Obtiene el tipo de ladrillos independiente
     */
    public ToBricksType getIndependentBrickType() {
        return independentBrickType;
    }
    
    /**
     * Obtiene el nombre del conjunto de colores
     */
    public String getColorSetName() {
        return colorSetName != null ? colorSetName : "Default";
    }
    
    /**
     * Verifica si tiene configuración de mosaico personalizada
     */
    public boolean hasIndependentMosaicConfig() {
        return useMosaicMode && independentColorGrid != null;
    }
    
    /**
     * Información de debug sobre la configuración del mosaico
     */
    public String getMosaicConfigInfo() {
        if (!useMosaicMode) {
            return "Normal Layer";
        }
        
        return String.format("Mosaic Layer - Set: %s, Bricks: %s", 
                           getColorSetName(), 
                           independentBrickType != null ? independentBrickType.toString() : "Default");
    }
}