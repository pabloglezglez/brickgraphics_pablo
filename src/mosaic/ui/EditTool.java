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
     * Herramienta borrador - restaura studs individuales al color original
     */
    ERASER("Eraser", "Eraser - Restore individual studs to original"),
    
    /**
     * Herramienta restaurar - restaura una zona al estado original
     */
    RESTORE("Restore", "Restore - Restore area to original state"),
    
    /**
     * Herramienta reset - borra un stud individual (lo hace transparente)
     */
    RESET("Reset Single", "Reset Single - Clear individual stud");
    
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