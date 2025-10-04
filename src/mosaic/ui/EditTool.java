package mosaic.ui;

/**
 * Enumeración para las herramientas de edición del mosaico.
 */
public enum EditTool {
    /**
     * Herramienta por defecto - navegación normal
     */
    DEFAULT("Default", "Navegación normal"),
    
    /**
     * Herramienta pincel - permite cambiar el color de studs individuales
     */
    BRUSH("Brush", "Pincel - Cambiar color de stud"),
    
    /**
     * Herramienta eyedropper - permite seleccionar un color del mosaico
     */
    EYEDROPPER("Eyedropper", "Cuentagotas - Seleccionar color del mosaico"),
    
    /**
     * Herramienta reset - restaura el mosaico al estado original
     */
    RESET("Reset", "Restaurar - Volver al mosaico original");
    
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