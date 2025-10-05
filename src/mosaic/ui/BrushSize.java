package mosaic.ui;

/**
 * Enumeración que define los diferentes tamaños de pincel disponibles.
 * Cada tamaño especifica las dimensiones del área de efecto del pincel.
 */
public enum BrushSize {
    SMALL(1, "1x1", "Small brush - single stud"),
    SMALL_2X2(2, "2x2", "Small 2x2 brush - 4 studs"),
    MEDIUM(3, "3x3", "Medium brush - 3x3 studs"),
    LARGE(5, "5x5", "Large brush - 5x5 studs"),
    EXTRA_LARGE(7, "7x7", "Extra large brush - 7x7 studs"),
    HUGE(8, "8x8", "Huge brush - 8x8 studs");
    
    private final int size;
    private final String displayName;
    private final String description;
    
    /**
     * Constructor de BrushSize.
     * @param size el tamaño del pincel (1, 3, 5, etc.)
     * @param displayName nombre para mostrar en la UI
     * @param description descripción detallada
     */
    BrushSize(int size, String displayName, String description) {
        this.size = size;
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * @return el tamaño numérico del pincel
     */
    public int getSize() {
        return size;
    }
    
    /**
     * @return el nombre para mostrar en la UI
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @return la descripción detallada
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * @return el radio del pincel (para cálculos de área)
     */
    public int getRadius() {
        return (size - 1) / 2;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}