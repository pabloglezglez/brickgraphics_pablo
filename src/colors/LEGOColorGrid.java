package colors;

public class LEGOColorGrid {
	private LEGOColor[][] grid;
	private LEGOColor[][] originalGrid; // Copia para función reset
	
	public LEGOColorGrid(LEGOColor[][] grid) {
		this.grid = grid;
		// Guardar copia del grid original para reset
		this.originalGrid = new LEGOColor[grid.length][];
		for (int i = 0; i < grid.length; i++) {
			this.originalGrid[i] = new LEGOColor[grid[i].length];
			System.arraycopy(grid[i], 0, this.originalGrid[i], 0, grid[i].length);
		}
	}
	
	public LEGOColor[] getRow(int y) {
		return grid[y];
	}
	
	public int getHeight() {
		return grid.length;
	}
	
	public int getWidth() {
		return grid[0].length;
	}
	
	/**
	 * Obtiene el color en las coordenadas especificadas.
	 * @param x coordenada x (columna)
	 * @param y coordenada y (fila)
	 * @return el color en esas coordenadas, o null si están fuera de rango
	 */
	public LEGOColor getColorAt(int x, int y) {
		if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
			return null;
		}
		return grid[y][x];
	}
	
	/**
	 * Establece el color en las coordenadas especificadas.
	 * @param x coordenada x (columna)
	 * @param y coordenada y (fila)
	 * @param color el nuevo color a establecer
	 * @return true si se estableció correctamente, false si las coordenadas están fuera de rango
	 */
	public boolean setColorAt(int x, int y, LEGOColor color) {
		if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
			return false;
		}
		grid[y][x] = color;
		return true;
	}
	
	/**
	 * Restaura el grid a su estado original.
	 */
	public void reset() {
		for (int i = 0; i < originalGrid.length; i++) {
			System.arraycopy(originalGrid[i], 0, grid[i], 0, originalGrid[i].length);
		}
	}
	
	/**
	 * Restaura un stud individual a su color original.
	 * @param x coordenada x (columna)
	 * @param y coordenada y (fila)
	 * @return true si se restauró correctamente, false si las coordenadas están fuera de rango
	 */
	public boolean resetAt(int x, int y) {
		if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
			return false;
		}
		grid[y][x] = originalGrid[y][x];
		return true;
	}
	
	/**
	 * Obtiene el grid interno (para compatibilidad con código existente).
	 * @return el array 2D de colores
	 */
	public LEGOColor[][] getGrid() {
		return grid;
	}
}
