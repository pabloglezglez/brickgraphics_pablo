package mosaic.ui;

/**
 * Enumeración para las herramientas de edición del mosaico.
 */
public enum EditTool {
    /**
     * Herramienta por defecto - navegación normal
     */
    DEFAULT("Default", "Default - Normal navigation"),
    
    /**
     * Herramienta pincel - permite cambiar el color de studs individuales
     */
    BRUSH("Brush", "Brush - Paint individual studs"),
    
    /**
     * Herramienta cuentagotas - permite seleccionar un color del mosaico
     */
    EYEDROPPER("Eyedropper", "Eyedropper - Pick color from mosaic"),
    
    /**
     * Herramienta reset - borra studs con tamaño seleccionable
     */
    RESET("Reset Single", "Reset Single - Clear studs with selectable size");
    
    private final String id;
    private final String description;
    
    EditTool(String id, String description) {
        this.id = id;
        this.description = description;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDescription() {
        return description;
    }
}