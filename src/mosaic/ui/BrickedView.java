package mosaic.ui;

import transforms.*;
import transforms.ScaleTransform.ScaleQuality;
import icon.Icons;
import io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.event.*;
import colors.*;
import mosaic.controllers.*;
import mosaic.controllers.PrintController.ShowPosition;
import mosaic.io.*;
import mosaic.layers.LayerManager;
import mosaic.rendering.Pipeline;
import mosaic.rendering.PipelineMosaicListener;
import bricks.ToBricksType;

public class BrickedView extends JPanel implements ChangeListener, PipelineMosaicListener {
	private Dimension mosaicImageSize;
	private ToBricksTransform toBricksTransform; // Used by CAD accessing functions.
	private Pipeline pipeline;
	private ToBricksController toBricksController;
	private MagnifierController magnifierController;
	private ColorController colorController;
	private UIController uiController;
	private ScaleTransform scaler; // Used for size calculations
	private ColorLegend legend;
	private PrintController printController;
	private StudEditController studEditController;
	private MosaicZoomController mosaicZoomController;
	private Dimension shownImageSize;
	
	// Sistema de preservación de modificaciones
	private ModificationManager modificationManager;
	private LEGOColorGrid lastValidGrid;
	
	// UI:
	public static final String MAGNIFIER = "MAGNIFIER", MOSAIC = "MOSAIC";	
	private MagnifierCanvas magnifierCanvas;
	private boolean showMagnifier;
	private CardLayout cardLayout;
	
	public BrickedView(MainController mc, Model<BrickGraphicsState> model, Pipeline pipeline) {
		this.pipeline = pipeline;
		scaler = new ScaleTransform("Constructed view", true, ScaleQuality.RetainColors);
		magnifierController = mc.getMagnifierController();
		colorController = mc.getColorController();
		uiController = mc.getUIController();
		toBricksController = mc.getToBricksController();
		printController = mc.getPrintController();
		studEditController = mc.getStudEditController();
		mosaicZoomController = mc.getMosaicZoomController();
		System.out.println("BrickedView: MosaicZoomController obtenido: " + (mosaicZoomController != null));
		
		// Inicializar modificationManager como null, se establecerá después
		modificationManager = null;
		lastValidGrid = null;
		toBricksController.addChangeListener(magnifierController);
		magnifierController.addChangeListener(this);
		legend = mc.getLegend();		
		
		// UI:
		toBricksTransform = new ToBricksTransform(colorController.getColorChooserSelectedColors(), 
				toBricksController.getToBricksType(), 
				toBricksController.getPropagationPercentage(), 
				toBricksController.getConstructionWidthInBasicUnits(),
				toBricksController.getConstructionHeightInBasicUnits(),
				colorController);
		magnifierController.setTBTransform(toBricksTransform);

		// build UI components:
		setPreferredSize(new Dimension(32, 32)); // Ensure mosaic is shown when repositioning the slider.
		cardLayout = new CardLayout();
		setLayout(cardLayout);
		
		MosaicCanvas mosaicCanvas = new MosaicCanvas();
		add(mosaicCanvas, MOSAIC);
		showMagnifier = false;
		
		magnifierCanvas = new MagnifierCanvas();
		magnifierCanvas.setFocusable(true);
		magnifierCanvas.setRequestFocusEnabled(true);
		magnifierCanvas.addKeyListener(magnifierController);
		add(magnifierCanvas, MAGNIFIER);		
		
		magnifierCanvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				magnifierCanvas.grabFocus(); // Behave like a button.
			}
		});
		pipeline.setToBricksTransform(toBricksTransform);
		pipeline.addMosaicListener(magnifierController);
		pipeline.addMosaicListener(this);
		stateChanged(null);
	}
	
	public LEGOColor.CountingLEGOColor[] getLegendColors() {
		if(toBricksTransform == null)
			throw new IllegalStateException();
		
		LEGOColor.CountingLEGOColor[] colors;
		
		// Obtener colores del mosaico actual (incluyendo modificaciones de edición)
		LEGOColorGrid currentGrid = getColorGrid();
		if (currentGrid != null && studEditController != null && studEditController.hasUnsavedChanges()) {
			// Si hay cambios sin guardar, contar colores del grid modificado
			colors = countColorsFromGrid(currentGrid);
		} else {
			// Si no hay modificaciones, usar los colores originales
			if(toBricksTransform.getToBricksType() == ToBricksType.SNOT_IN_2_BY_2)
				colors = toBricksTransform.lastUsedColorCounts();				
			else
				colors = toBricksTransform.getMainTransform().lastUsedColorCounts();
		}
			
		// Sort colors by their effective number (custom IDs first, then automatic)
		Arrays.sort(colors, (a, b) -> {
			try {
				String idA = colorController.getShownID(a.c);
				String idB = colorController.getShownID(b.c);
				
				// Parse numbers, handling potential non-numeric IDs
				int numA = Integer.parseInt(idA);
				int numB = Integer.parseInt(idB);
				
				return Integer.compare(numA, numB);
			} catch (NumberFormatException e) {
				// Fallback to string comparison if parsing fails
				String idA = colorController.getShownID(a.c);
				String idB = colorController.getShownID(b.c);
				return idA.compareTo(idB);
			}
		});
		
		return colors;								
	}
	
	/**
	 * Cuenta los colores utilizados en el grid actual.
	 * @param grid el grid de colores modificado
	 * @return array de colores con conteo
	 */
	private LEGOColor.CountingLEGOColor[] countColorsFromGrid(LEGOColorGrid grid) {
		Map<LEGOColor, Integer> colorCounts = new HashMap<>();
		
		// Contar cada color en el grid
		for (int x = 0; x < grid.getWidth(); x++) {
			for (int y = 0; y < grid.getHeight(); y++) {
				LEGOColor color = grid.getColorAt(x, y);
				if (color != null) {
					colorCounts.put(color, colorCounts.getOrDefault(color, 0) + 1);
				}
			}
		}
		
		// Convertir a array de CountingLEGOColor
		LEGOColor.CountingLEGOColor[] result = new LEGOColor.CountingLEGOColor[colorCounts.size()];
		int i = 0;
		for (Map.Entry<LEGOColor, Integer> entry : colorCounts.entrySet()) {
			result[i] = new LEGOColor.CountingLEGOColor(entry.getKey(), entry.getValue());
			i++;
		}
		
		return result;
	}
	
	private void updateTransform(ToBricksController t) {
		toBricksTransform.setPropagationPercentage(t.getPropagationPercentage());
		toBricksTransform.setToBricksType(t.getToBricksType());
		toBricksTransform.setColors(colorController.getColorChooserSelectedColors());
		toBricksTransform.setBasicUnitSize(t.getConstructionWidthInBasicUnits(), t.getConstructionHeightInBasicUnits());
		pipeline.invalidate();
	}
	
	public Dimension getShownImageSize() {
		return shownImageSize;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if(e != null && e.getSource() instanceof ToBricksController) {
			updateTransform((ToBricksController)e.getSource());
		}
		if(showMagnifier != uiController.showMagnifier()) {
			showMagnifier = !showMagnifier;
			if(showMagnifier) {
				cardLayout.show(this, MAGNIFIER);
				magnifierCanvas.grabFocus();
			}
			else
				cardLayout.show(this, MOSAIC);
		}		

		repaint();
	}
	
	// Used by CAD exports
	public ToBricksTransform getToBricksTransform() {
		return toBricksTransform;
	}
	
	/**
	 * Maneja clicks del mouse en el mosaico para herramientas de edición.
	 * Usa las coordenadas del cursor visual para consistencia total.
	 * @param clickX coordenada X del click (usado para actualizar cursor)
	 * @param clickY coordenada Y del click (usado para actualizar cursor)
	 */
	private void handleMosaicClick(int clickX, int clickY) {
		if (studEditController.getActiveTool() == EditTool.DEFAULT) {
			return; // No hacer nada si está en modo navegación
		}
		
		LEGOColorGrid colorGrid = getColorGrid();
		if (colorGrid == null) {
			return;
		}
		
		// Primero actualizar la posición del cursor para asegurar que esté sincronizado
		updateHoverCursor(clickX, clickY);
		
		// DEBUG: Mostrar las coordenadas calculadas
		double zoomFactor = mosaicZoomController.getCurrentZoomFactor();
		System.out.println("Click en (" + clickX + "," + clickY + ") Zoom=" + zoomFactor + " -> Cursor en (" + hoveredX + "," + hoveredY + ")");
		
		// Usar directamente las coordenadas del cursor visual que ya están calculadas correctamente
		// Esto garantiza que TODAS las herramientas (BRUSH, EYEDROPPER, RESET) actúen 
		// exactamente donde está el cursor visual
		if (hoveredX >= 0 && hoveredY >= 0 && showHoverCursor) {
			// Aplicar la herramienta en las coordenadas del cursor visual
			boolean changed = studEditController.applyToolAt(colorGrid, hoveredX, hoveredY);
			
			if (changed) {
				// Para SNOT, necesitamos forzar actualización del renderizado
				if (toBricksTransform.getToBricksType() == ToBricksType.SNOT_IN_2_BY_2) {
					updateSnotVisualization();
				}
				
				// Actualizar la vista
				repaint();
			}
		}
	}
	
	// Grid editable para modo SNOT
	private LEGOColorGrid snotEditableGrid = null;
	
	/**
	 * Obtiene el grid de colores del mosaico actual.
	 * Sistema automático de preservación de modificaciones.
	 * @return el LEGOColorGrid o null si no está disponible
	 */
	/**
	 * Establece el gestor de modificaciones. Debe llamarse después de crear el StudEditController.
	 */
	public void setModificationManager(ModificationManager manager) {
		this.modificationManager = manager;
	}
	
	public LEGOColorGrid getColorGrid() {
		if (toBricksTransform == null) {
			return null;
		}
		
		// Verificar si estamos en modo SNOT - en este modo el grid manejo es diferente
		boolean isSnotMode = toBricksTransform.getToBricksType() == ToBricksType.SNOT_IN_2_BY_2;
		
		if (isSnotMode) {
			// Para el modo SNOT, crear un grid virtual que mapee a los grids reales
			return createSnotVirtualGrid();
		}
		
		// Obtener el grid de colores del transform principal para modos NO-SNOT
		BufferedLEGOColorTransform mainTransform = toBricksTransform.getMainTransform();
		if (mainTransform != null) {
			LEGOColorGrid grid = mainTransform.getCurrentColorGrid();
			
			// Sistema automático de preservación - SOLO para modos NO-SNOT
			if (grid == null && lastValidGrid != null) {
				// Grid se volvió null - preservar modificaciones del último grid válido
				if (modificationManager != null) {
					modificationManager.backupGrid(lastValidGrid);
				}
				lastValidGrid = null;
			} else if (grid != null && lastValidGrid == null) {
				// Grid se restauró - aplicar modificaciones preservadas
				if (modificationManager != null) {
					modificationManager.restoreModifications(grid);
				}
				lastValidGrid = grid;
			} else if (grid != null) {
				// Grid válido - actualizar referencia
				lastValidGrid = grid;
			}
			
			return grid;
		}
		
		return null;
	}
	
	/**
	 * Crea un grid virtual para SNOT que mapea las coordenadas del cursor 
	 * a los grids reales normalColors y sidewaysColors.
	 */
	private LEGOColorGrid createSnotVirtualGrid() {
		return new SnotVirtualGrid(toBricksTransform);
	}
	
	/**
	 * Grid virtual que permite acceso unificado a los grids SNOT reales.
	 * Mapea coordenadas de pantalla a las posiciones correctas en normalColors y sidewaysColors.
	 */
	private class SnotVirtualGrid extends LEGOColorGrid {
		private ToBricksTransform transform;
		
		public SnotVirtualGrid(ToBricksTransform transform) {
			super(new LEGOColor[1][1]); // Grid dummy, no se usa
			this.transform = transform;
		}
		
		@Override
		public int getWidth() {
			if (mosaicImageSize == null) return 0;
			// El ancho del grid virtual es el número de "studs" horizontales
			// Cada bloque SNOT de 10x10 píxeles contiene múltiples studs
			return mosaicImageSize.width / 2; // Cada stud = 2 píxeles de ancho
		}
		
		@Override
		public int getHeight() {
			if (mosaicImageSize == null) return 0;
			// El alto del grid virtual es el número de "studs" verticales
			return mosaicImageSize.height / 2; // Cada stud = 2 píxeles de alto
		}
		
		@Override
		public LEGOColor getColorAt(int x, int y) {
			if (transform == null) return LEGOColor.WHITE;
			
			// Determinar si estamos en una zona normal (2x5) o sideways (5x2)
			SnotPosition pos = calculateSnotPosition(x, y);
			
			// Acceder al grid apropiado según la orientación
			LEGOColorGrid targetGrid = pos.isNormal ? transform.getNormalColors() : transform.getSidewaysColors();
			
			if (targetGrid == null) return LEGOColor.WHITE;
			
			// Verificar límites del grid real
			if (pos.gridX < 0 || pos.gridX >= targetGrid.getWidth() || 
			    pos.gridY < 0 || pos.gridY >= targetGrid.getHeight()) {
				return LEGOColor.WHITE;
			}
			
			io.Log.log("DEBUG SNOT: getColorAt(" + x + "," + y + ") -> Grid " + 
							   (pos.isNormal ? "normal" : "sideways") + " en (" + pos.gridX + "," + pos.gridY + ")");
			
			return targetGrid.getColorAt(pos.gridX, pos.gridY);
		}
		
		@Override
		public boolean setColorAt(int x, int y, LEGOColor color) {
			if (transform == null) return false;
			
			// Determinar si estamos en una zona normal (2x5) o sideways (5x2)
			SnotPosition pos = calculateSnotPosition(x, y);
			
			// Acceder al grid apropiado según la orientación
			LEGOColorGrid targetGrid = pos.isNormal ? transform.getNormalColors() : transform.getSidewaysColors();
			
			if (targetGrid == null) return false;
			
			// Verificar límites del grid real
			if (pos.gridX < 0 || pos.gridX >= targetGrid.getWidth() || 
			    pos.gridY < 0 || pos.gridY >= targetGrid.getHeight()) {
				return false;
			}
			
			io.Log.log("DEBUG SNOT: setColorAt(" + x + "," + y + ", " + color.getName() + ") -> Grid " + 
							   (pos.isNormal ? "normal" : "sideways") + " en (" + pos.gridX + "," + pos.gridY + ")");
			
			boolean success = targetGrid.setColorAt(pos.gridX, pos.gridY, color);
			
			if (success) {
				// Forzar actualización visual
				updateSnotVisualization();
			}
			
			return success;
		}
		
		/**
		 * Calcula la posición en el grid SNOT correspondiente a las coordenadas virtuales.
		 */
		private SnotPosition calculateSnotPosition(int x, int y) {
			// Calcular en qué bloque SNOT estamos (cada bloque es 5x5 studs virtuales)
			int blockX = x / 5;
			int blockY = y / 5;
			
			// Coordenadas dentro del bloque (0-4 en ambas direcciones)
			int inBlockX = x % 5;
			int inBlockY = y % 5;
			
			// Determinar orientación basada en el patrón SNOT
			// Obtener el array normalColorsChoosen para saber la orientación
			boolean[][] orientations = transform.getNormalColorsChoosen();
			boolean isNormal = true; // Por defecto normal
			
			if (orientations != null && blockX < orientations.length && blockY < orientations[blockX].length) {
				isNormal = orientations[blockX][blockY];
			}
			
			int gridX, gridY;
			
			if (isNormal) {
				// Orientación normal: 2x5 (2 studs ancho, 5 studs alto)
				gridX = blockX * 2 + (inBlockX / 3); // Mapear 5 studs virtuales a 2 reales 
				gridY = blockY * 5 + inBlockY;
			} else {
				// Orientación sideways: 5x2 (5 studs ancho, 2 studs alto)  
				gridX = blockX * 5 + inBlockX;
				gridY = blockY * 2 + (inBlockY / 3); // Mapear 5 studs virtuales a 2 reales
			}
			
			return new SnotPosition(gridX, gridY, isNormal);
		}
	}
	
	/**
	 * Clase auxiliar para almacenar la posición calculada en SNOT.
	 */
	private static class SnotPosition {
		final int gridX, gridY;
		final boolean isNormal;
		
		SnotPosition(int gridX, int gridY, boolean isNormal) {
			this.gridX = gridX;
			this.gridY = gridY;
			this.isNormal = isNormal;
		}
	}
	
	/**
	 * Actualiza la visualización de SNOT después de cambios en el grid editable.
	 */
	private void updateSnotVisualization() {
		// Para SNOT, simplemente forzar una actualización visual
		// El grid editable ya contiene los cambios, solo necesitamos actualizar la vista
	Log.log("DEBUG: Actualizando visualización SNOT");
		
		// Notificar que hay cambios pendientes de visualizar
		MosaicCanvas canvas = getMosaicCanvas();
		if (canvas != null) {
			canvas.repaint();
		}
	}
	
	// Variables para el cursor visual
	private int hoveredX = -1; // Coordenadas del stud bajo el cursor
	private int hoveredY = -1;
	private boolean showHoverCursor = false;
	
	// Variables para el arrastre continuo
	private boolean isDragging = false;
	private int lastDraggedX = -1; // Último stud pintado durante arrastre
	private int lastDraggedY = -1;
	
	/**
	 * Convierte coordenadas de pantalla a coordenadas del mosaico considerando el zoom.
	 */
	private Point screenToMosaic(Point screenPoint) {
		if (mosaicImageSize == null || shownImageSize == null) {
			return new Point(0, 0);
		}
		
		// Si no hay zoom activo, usar conversión simple 
		double zoomFactor = mosaicZoomController.getCurrentZoomFactor();
		// CORREGIDO: Comparar directamente con 1.0 O usar precision más amplia para capturar zoom inicial
		if (zoomFactor == 1.0 || Math.abs(zoomFactor - 1.0) < 0.001) {
			// PRECISION FIX: Usar el tamaño exacto como en paintComponent sin zoom
			double scaleX = (double) mosaicImageSize.width / shownImageSize.width;
			double scaleY = (double) mosaicImageSize.height / shownImageSize.height;
			return new Point((int) Math.round(screenPoint.x * scaleX), (int) Math.round(screenPoint.y * scaleY));
		}
		
		// Con zoom: aplicar transformación inversa exacta a la del paintComponent
		Rectangle viewport = mosaicZoomController.getViewportBounds();
		Dimension size = getSize();
		
		// CORREGIDO: Usar size.width/size.height como en paintComponent
		// En paintComponent: scaleX = size.width / viewport.width
		// En paintComponent: scaleY = size.height / viewport.height
		double scaleX = (double) size.width / viewport.width;  
		double scaleY = (double) size.height / viewport.height;
		
		// Aplicar transformación inversa en el orden correcto:
		// paintComponent hace: scale() luego translate()  
		// Para la inversa: aplicar escala inversa directamente
		double invScaleX = (double) viewport.width / size.width;
		double invScaleY = (double) viewport.height / size.height;
		
		// Convertir coordenadas de pantalla a coordenadas del viewport
		double viewportX = screenPoint.x * invScaleX;
		double viewportY = screenPoint.y * invScaleY;
		
		// Agregar el offset del viewport para obtener coordenadas del mosaico
		int mosaicX = (int) (viewportX + viewport.x);
		int mosaicY = (int) (viewportY + viewport.y);
		
		return new Point(mosaicX, mosaicY);
	}

	/**
	 * Actualiza la posición del cursor visual sobre el mosaico.
	 */
	private void updateHoverCursor(int mouseX, int mouseY) {
		if (studEditController.getActiveTool() == EditTool.DEFAULT) {
			if (showHoverCursor) {
				showHoverCursor = false;
				repaint();
			}
			return;
		}
		
		// Calcular coordenadas del stud
		LEGOColorGrid colorGrid = getColorGrid();
		if (colorGrid == null || shownImageSize == null || mosaicImageSize == null) {
			return;
		}
		
		// Usar el zoom controller directamente desde BrickedView
		if (mosaicZoomController == null) {
			return;
		}
		
		// Convertir coordenadas del mouse a coordenadas del mosaico usando el método corregido
		Point mosaicPoint = screenToMosaic(new Point(mouseX, mouseY));
		
		// Convertir coordenadas del mosaico directamente a coordenadas del grid
		int newHoveredX = (mosaicPoint.x * colorGrid.getWidth()) / mosaicImageSize.width;
		int newHoveredY = (mosaicPoint.y * colorGrid.getHeight()) / mosaicImageSize.height;
		
		// Verificar que esté dentro del rango válido
		if (newHoveredX >= 0 && newHoveredX < colorGrid.getWidth() && 
			newHoveredY >= 0 && newHoveredY < colorGrid.getHeight()) {
			
			if (newHoveredX != hoveredX || newHoveredY != hoveredY) {
				hoveredX = newHoveredX;
				hoveredY = newHoveredY;
				showHoverCursor = true;
				repaint();
			}
		} else {
			if (showHoverCursor) {
				showHoverCursor = false;
				repaint();
			}
		}
	}
	
	/**
	 * Maneja el arrastre continuo para pintar múltiples studs.
	 * Usa las coordenadas del cursor visual para consistencia total.
	 * @param mouseX coordenada X del mouse
	 * @param mouseY coordenada Y del mouse
	 */
	private void handleMosaicDrag(int mouseX, int mouseY) {
		EditTool activeTool = studEditController.getActiveTool();
		if (!isDragging || (activeTool != EditTool.BRUSH && activeTool != EditTool.RESET)) {
			return; // Solo ciertas herramientas permiten arrastre continuo
		}
		
		LEGOColorGrid colorGrid = getColorGrid();
		if (colorGrid == null) {
			return;
		}
		
		// Actualizar cursor visual con la nueva posición
		updateHoverCursor(mouseX, mouseY);
		
		// Usar directamente las coordenadas del cursor visual
		// Esto asegura que el arrastre siga exactamente el cursor visual
		if (hoveredX >= 0 && hoveredY >= 0 && showHoverCursor) {
			// Solo pintar si nos movimos a un stud diferente
			if (hoveredX != lastDraggedX || hoveredY != lastDraggedY) {
				boolean changed = studEditController.applyToolAt(colorGrid, hoveredX, hoveredY);
				
				if (changed) {
					// NO invalidar el pipeline para preservar las modificaciones manuales
					repaint();
					lastDraggedX = hoveredX;
					lastDraggedY = hoveredY;
				}
			}
		}
	}

	// Used by CAD exports
	public Dimension getBrickedSize() {
		return mosaicImageSize;
	}
	
	// ===== MÉTODOS PÚBLICOS DE ZOOM =====
	
	/**
	 * Obtiene el controlador de zoom del mosaico.
	 */
	public MosaicZoomController getMosaicZoomController() {
		return mosaicZoomController;
	}
	
	/**
	 * Activa/desactiva el modo de zoom para selección.
	 */
	public void toggleZoomMode() {
		MosaicCanvas canvas = getMosaicCanvas();
		if (canvas != null) {
			canvas.toggleZoomMode();
		}
	}
	
	/**
	 * Establece el modo de zoom.
	 */
	public void setZoomMode(boolean enabled) {
		MosaicCanvas canvas = getMosaicCanvas();
		if (canvas != null) {
			canvas.setZoomMode(enabled);
			
			// Cuando se activa el modo zoom, desactivar herramientas de edición
			if (enabled) {
				// Guardar herramienta actual y establecer DEFAULT
				studEditController.setActiveTool(EditTool.DEFAULT);
			}
		}
	}
	
	/**
	 * Obtiene el canvas del mosaico.
	 */
	private MosaicCanvas getMosaicCanvas() {
		// Buscar el MosaicCanvas en los componentes
		for (Component comp : getComponents()) {
			if (comp instanceof MosaicCanvas) {
				return (MosaicCanvas) comp;
			}
		}
		return null;
	}
	
	/**
	 * Zoom in centrado en el centro actual.
	 */
	public void zoomIn() {
		mosaicZoomController.zoomIn();
	}
	
	/**
	 * Zoom out centrado en el centro actual.
	 */
	public void zoomOut() {
		mosaicZoomController.zoomOut();
	}
	
	/**
	 * Zoom para ajustar el mosaico completo en la ventana.
	 */
	public void zoomToFit() {
		mosaicZoomController.zoomToFit();
	}
	
	/**
	 * Zoom al tamaño actual (100%).
	 */
	public void zoomToActualSize() {
		mosaicZoomController.zoomToActualSize();
	}
	
	/**
	 * Obtiene el porcentaje de zoom actual como string.
	 */
	public String getCurrentZoomPercentage() {
		return mosaicZoomController.getCurrentZoomPercentage();
	}

	@Override
	public void mosaicChanged(Dimension mosaicImageSize) {
		this.mosaicImageSize = mosaicImageSize;
		repaint();
	}
	
	private class MosaicCanvas extends JPanel implements MosaicZoomController.MosaicZoomListener {
		private boolean isZoomMode = false;
		private Point zoomSelectionStart = null;
		
		// Variables para paneo
		private boolean isPanning = false;
		private Point panningStart = null;
		
		public MosaicCanvas() {
			// Configurar zoom controller listener
			mosaicZoomController.addZoomListener(this);
			
			// Agregar mouse listener para herramientas de edición y zoom
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e)) {
						// Click derecho para zoom in
						System.out.println("Click derecho para zoom in");
						if (mosaicZoomController != null) {
							Point mosaicPoint = screenToMosaic(new Point(e.getX(), e.getY()));
							System.out.println("Centering zoom en: " + mosaicPoint);
							mosaicZoomController.centerViewportOn(mosaicPoint);
							mosaicZoomController.zoomIn();
						}
					} else if (SwingUtilities.isMiddleMouseButton(e)) {
						// Click medio para zoom out
						System.out.println("Click medio para zoom out");
						if (mosaicZoomController != null) {
							mosaicZoomController.zoomOut();
						}
					} else {
						// Click izquierdo normal para herramientas de edición
						handleMosaicClick(e.getX(), e.getY());
					}
				}
				
				@Override
				public void mousePressed(MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						// Resetear estados anteriores
						isDragging = false;
						zoomSelectionStart = null;
						isPanning = false;
						
						// Decisión basada en herramienta activa y modificadores
						EditTool activeTool = studEditController.getActiveTool();
						if ((activeTool == EditTool.BRUSH || activeTool == EditTool.RESET) && !e.isControlDown()) {
							// Herramientas de edición (pincel/reset) SIN Ctrl - activar edición
							isDragging = true;
							lastDraggedX = -1;
							lastDraggedY = -1;
							handleMosaicClick(e.getX(), e.getY());
							System.out.println("Herramienta activada: " + activeTool);
						} else if (e.isControlDown() || isZoomMode) {
							// Ctrl presionado O modo zoom - activar selección de zoom
							zoomSelectionStart = new Point(e.getX(), e.getY());
							mosaicZoomController.startSelection(zoomSelectionStart);
							System.out.println("Selección de zoom iniciada");
						} else {
							// Click normal para otras herramientas
							handleMosaicClick(e.getX(), e.getY());
						}
					} else if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown())) {
						// Botón central o Shift+Click izquierdo para paneo
						if (mosaicZoomController != null && mosaicZoomController.isPanningAvailable()) {
							isPanning = true;
							panningStart = new Point(e.getX(), e.getY());
							setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
							System.out.println("Paneo iniciado en: " + panningStart);
						}
					}
				}
				
				@Override
				public void mouseReleased(MouseEvent e) {
					if (isPanning && panningStart != null) {
						// Terminar paneo
						isPanning = false;
						panningStart = null;
						setCursor(Cursor.getDefaultCursor());
						System.out.println("Paneo terminado");
					} else if (zoomSelectionStart != null) {
						// Terminar selección de zoom
						mosaicZoomController.endSelection();
						Rectangle selection = mosaicZoomController.getSelectionArea();
						if (!selection.isEmpty() && (selection.width > 10 || selection.height > 10)) {
							// Zoom a la selección si es suficientemente grande
							zoomToSelection(selection);
						}
						zoomSelectionStart = null;
					} else {
						// Terminar arrastre normal
						isDragging = false;
						lastDraggedX = -1;
						lastDraggedY = -1;
					}
				}
			});
			
			// Agregar mouse motion listener para cursor visual, arrastre y zoom
			addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					updateHoverCursor(e.getX(), e.getY());
					
					// Actualizar cursor para indicar disponibilidad de paneo
					if (!isPanning && !isDragging && zoomSelectionStart == null) {
						if (mosaicZoomController != null && mosaicZoomController.isPanningAvailable() && 
							(e.isShiftDown() || SwingUtilities.isMiddleMouseButton(e))) {
							setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
						} else {
							setCursor(Cursor.getDefaultCursor());
						}
					}
				}
				
				@Override
				public void mouseDragged(MouseEvent e) {
					if (isPanning && panningStart != null) {
						// Realizar paneo
						int deltaX = e.getX() - panningStart.x;
						int deltaY = e.getY() - panningStart.y;
						
						if (mosaicZoomController != null) {
							mosaicZoomController.panViewport(deltaX, deltaY);
							System.out.println("Paneo delta: (" + deltaX + ", " + deltaY + ")");
						}
						
						// Actualizar punto de inicio para el siguiente arrastre
						panningStart.setLocation(e.getX(), e.getY());
						repaint();
					} else if (zoomSelectionStart != null) {
						// Actualizar selección de zoom (solo si se inició zoom)
						mosaicZoomController.updateSelection(new Point(e.getX(), e.getY()));
						System.out.println("Actualizando selección de zoom");
					} else if (isDragging) {
						// Arrastre para herramientas de edición
						updateHoverCursor(e.getX(), e.getY());
						handleMosaicDrag(e.getX(), e.getY());
					} else {
						// Solo actualizar cursor si no hay arrastre activo
						updateHoverCursor(e.getX(), e.getY());
					}
				}
			});
			
			// Agregar mouse wheel listener directamente al canvas
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				System.out.println("MouseWheel en MosaicCanvas: Ctrl=" + e.isControlDown() + ", rotation=" + e.getWheelRotation());
				if (e.isControlDown() && mosaicZoomController != null) {
					// Consumir evento INMEDIATAMENTE para prevenir zoom del sistema
					e.consume();
					
					// Ctrl+Rueda: zoom centrado en cursor
					Point cursorPos = screenToMosaic(new Point(e.getX(), e.getY()));
					System.out.println("Zoom en cursor: " + cursorPos);
					
					double currentZoom = mosaicZoomController.getCurrentZoomFactor();
					System.out.println("Zoom actual: " + currentZoom);
					
					mosaicZoomController.centerViewportOn(cursorPos);
					
					if (e.getWheelRotation() < 0) {
						mosaicZoomController.zoomIn();
						System.out.println("Zoom IN ejecutado");
					} else {
						mosaicZoomController.zoomOut();
						System.out.println("Zoom OUT ejecutado");
					}
					
					double newZoom = mosaicZoomController.getCurrentZoomFactor();
					System.out.println("Nuevo zoom: " + newZoom);
					
					// Forzar repaint
					repaint();
				}
			}
		});
		
		// Agregar soporte de teclado para paneo
		setFocusable(true);
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (mosaicZoomController != null && mosaicZoomController.isPanningAvailable()) {
					int panStep = 20; // pixels de paneo por tecla
					
					switch (e.getKeyCode()) {
						case KeyEvent.VK_LEFT:
							mosaicZoomController.panViewport(-panStep, 0);
							repaint();
							break;
						case KeyEvent.VK_RIGHT:
							mosaicZoomController.panViewport(panStep, 0);
							repaint();
							break;
						case KeyEvent.VK_UP:
							mosaicZoomController.panViewport(0, -panStep);
							repaint();
							break;
						case KeyEvent.VK_DOWN:
							mosaicZoomController.panViewport(0, panStep);
							repaint();
							break;
					}
				}
			}
		});
		}
		
		/**
		 * Maneja eventos de rueda del mouse para zoom.
		 * Debe ser llamado desde la ventana principal cuando se detecta Ctrl+rueda.
		 */
		public void handleMouseWheelZoom(MouseWheelEvent e, Point relativeToCanvas) {
			System.out.println("HandleMouseWheelZoom: punto=" + relativeToCanvas + ", rotation=" + e.getWheelRotation());
			
			if (mosaicZoomController == null) {
				System.out.println("MosaicZoomController es null!");
				return;
			}
			
			// Verificar que el punto esté dentro del canvas del mosaico
			Rectangle canvasBounds = getBounds();
			System.out.println("Canvas bounds: " + canvasBounds);
			
			if (relativeToCanvas.x >= 0 && relativeToCanvas.y >= 0 && 
				relativeToCanvas.x < canvasBounds.width && relativeToCanvas.y < canvasBounds.height) {
				
				System.out.println("Punto dentro del canvas, procesando zoom...");
				
				// Ctrl+Rueda: zoom centrado en cursor
				Point cursorPos = screenToMosaic(relativeToCanvas);
				System.out.println("Coordenada en mosaico: " + cursorPos);
				
				// Obtener nivel de zoom actual
				double currentZoom = mosaicZoomController.getCurrentZoomFactor();
				System.out.println("Zoom actual: " + currentZoom);
				
				mosaicZoomController.centerViewportOn(cursorPos);
				
				if (e.getWheelRotation() < 0) {
					mosaicZoomController.zoomIn();
					System.out.println("Zoom IN ejecutado");
				} else {
					mosaicZoomController.zoomOut();
					System.out.println("Zoom OUT ejecutado");
				}
				
				// Verificar nuevo zoom
				double newZoom = mosaicZoomController.getCurrentZoomFactor();
				System.out.println("Nuevo zoom: " + newZoom);
				
				// Forzar repaint
				repaint();
			} else {
				System.out.println("Punto fuera del canvas: " + relativeToCanvas + " no está en " + canvasBounds);
			}
		}
		
		@Override 
		public void paintComponent(Graphics g) {				
			super.paintComponent(g);
			if(mosaicImageSize == null)
				return;

			Dimension size = getSize();
			scaler.setWidth(size.width);
			scaler.setHeight(size.height);

			shownImageSize = scaler.getTransformedSize(mosaicImageSize);
			
			// Log zoom info
			if (mosaicZoomController != null) {
				double zoom = mosaicZoomController.getCurrentZoomFactor();
				if (zoom != 1.0) {
					System.out.println("PaintComponent: Zoom=" + zoom + ", viewport=" + mosaicZoomController.getViewportBounds());
				}
			}
			magnifierController.setShownImageSize(shownImageSize);
				
			Graphics2D g2 = (Graphics2D)g;

			// Aplicar transformación de zoom si está activo
			if (mosaicZoomController != null && mosaicZoomController.getCurrentZoomFactor() != 1.0) {
				Rectangle viewport = mosaicZoomController.getViewportBounds();
				double zoomFactor = mosaicZoomController.getCurrentZoomFactor();
				
				// Crear nueva imagen escalada para el viewport
				double scaleX = (double) size.width / viewport.width;
				double scaleY = (double) size.height / viewport.height;
				
				// Aplicar transformación: escalar y trasladar
				g2.scale(scaleX, scaleY);
				g2.translate(-viewport.x, -viewport.y);
				
				// Dibujar con el tamaño del mosaico original
				toBricksTransform.drawAll(g2, mosaicImageSize);
				
				// Restaurar transformación para elementos de UI
				g2.translate(viewport.x, viewport.y);
				g2.scale(1.0/scaleX, 1.0/scaleY);
			} else {
				// Sin zoom, dibujar normalmente
				toBricksTransform.drawAll(g2, shownImageSize);
			}
			
			// Dibujar cursor de edición si está activo (siempre en coordenadas de pantalla)
			drawEditCursor(g2);
			
			// Dibujar selección de zoom si está activa
			drawZoomSelection(g2);
		}
		
	/**
	 * Dibuja el cursor de edición sobre el stud seleccionado.
	 * Muestra el área de efecto basada en el tamaño del pincel.
	 */
	private void drawEditCursor(Graphics2D g2) {
		if (!showHoverCursor || studEditController.getActiveTool() == EditTool.DEFAULT) {
			return;
		}
		
		LEGOColorGrid colorGrid = getColorGrid();
		if (colorGrid == null || hoveredX < 0 || hoveredY < 0) {
			return;
		}
		
		// Calcular dimensiones de un stud en píxeles del mosaico original
		int studPixelWidth = mosaicImageSize.width / colorGrid.getWidth();
		int studPixelHeight = mosaicImageSize.height / colorGrid.getHeight();
		
		// Obtener el tamaño del pincel
		BrushSize brushSize = studEditController.getBrushSize();
		int brushSizeValue = brushSize.getSize();
		
		// Configurar el cursor según la herramienta activa y estado de arrastre
		if (isDragging) {
			g2.setStroke(new BasicStroke(3)); // Línea más gruesa durante arrastre
		} else {
			g2.setStroke(new BasicStroke(2));
		}
		
		// Calcular el área correcta del cursor basado en el algoritmo real del brush
		int startX, startY, endX, endY;
		
		if (brushSizeValue % 2 == 1) {
			// Tamaños impares: centrar alrededor del punto hovereado
			int offset = (brushSizeValue - 1) / 2;
			startX = hoveredX - offset;
			startY = hoveredY - offset;
		} else {
			// Tamaños pares: el punto hovereado está en la esquina superior izquierda del área
			int offset = brushSizeValue / 2;
			startX = hoveredX - offset + 1;
			startY = hoveredY - offset + 1;
		}
		
		endX = startX + brushSizeValue - 1;
		endY = startY + brushSizeValue - 1;
		
		// Dibujar cada celda del área de efecto del pincel
		for (int targetY = startY; targetY <= endY; targetY++) {
			for (int targetX = startX; targetX <= endX; targetX++) {
				
				// Verificar que el stud objetivo esté dentro del grid
				if (targetX < 0 || targetX >= colorGrid.getWidth() || 
					targetY < 0 || targetY >= colorGrid.getHeight()) {
					continue;
				}
				
				// Coordenadas del stud en el mosaico original
				int mosaicX = targetX * studPixelWidth;
				int mosaicY = targetY * studPixelHeight;
				
				// Convertir a coordenadas de pantalla
				Point screenStart = mosaicToScreen(new Point(mosaicX, mosaicY));
				Point screenEnd = mosaicToScreen(new Point(mosaicX + studPixelWidth, mosaicY + studPixelHeight));
				
				// Si está fuera de la pantalla, no dibujar
				if (screenStart.x < 0 || screenStart.y < 0 || screenEnd.x < 0 || screenEnd.y < 0) {
					continue;
				}
				
				int pixelX = screenStart.x;
				int pixelY = screenStart.y;
				int pixelW = screenEnd.x - screenStart.x;
				int pixelH = screenEnd.y - screenStart.y;
				
				// Determinar si está en el centro basado en el tamaño del brush
				boolean isCenterCell = false;
				
				if (brushSizeValue % 2 == 1) {
					// Para tamaños impares, el centro es una sola celda
					isCenterCell = (targetX == hoveredX && targetY == hoveredY);
				} else {
					// Para tamaños pares, el centro es una cuadrícula de 2x2
					// Calculamos el centro del área
					int centerStartX = startX + brushSizeValue / 2 - 1;
					int centerEndX = centerStartX + 1;
					int centerStartY = startY + brushSizeValue / 2 - 1;
					int centerEndY = centerStartY + 1;
					
					isCenterCell = (targetX >= centerStartX && targetX <= centerEndX && 
								   targetY >= centerStartY && targetY <= centerEndY);
				}
				
				switch (studEditController.getActiveTool()) {
					case BRUSH:
						// Cuadrado verde para el pincel
						if (isCenterCell) {
							// Centro más intenso
							if (isDragging) {
								g2.setColor(new Color(0, 200, 0)); // Verde más intenso
								g2.fillRect(pixelX + 2, pixelY + 2, pixelW - 4, pixelH - 4);
							} else {
								g2.setColor(Color.GREEN);
							}
						} else {
							// Bordes menos intensos
							g2.setColor(new Color(0, 150, 0, 120)); // Verde semi-transparente
						}
						g2.drawRect(pixelX, pixelY, pixelW, pixelH);
						break;
					case EYEDROPPER:
						// Solo mostrar cursor en el centro para eyedropper
						if (isCenterCell) {
							g2.setColor(Color.BLUE);
							g2.drawOval(pixelX, pixelY, pixelW, pixelH);
						}
						break;
					case RESET:
						// Cuadrado rojo para reset con tamaño
						if (isCenterCell) {
							if (isDragging) {
								g2.setColor(new Color(255, 0, 0)); // Rojo intenso
								g2.fillRect(pixelX + 2, pixelY + 2, pixelW - 4, pixelH - 4);
							} else {
								g2.setColor(Color.RED);
							}
							// Dibujar cruz en el centro
							g2.drawLine(pixelX, pixelY, pixelX + pixelW, pixelY + pixelH);
							g2.drawLine(pixelX + pixelW, pixelY, pixelX, pixelY + pixelH);
						} else {
							g2.setColor(new Color(255, 0, 0, 120)); // Rojo semi-transparente
						}
						g2.drawRect(pixelX, pixelY, pixelW, pixelH);
						break;
				}
			}
		}
	}		/**
		 * Dibuja la selección de zoom cuando está activa.
		 */
		private void drawZoomSelection(Graphics2D g2) {
			if (!mosaicZoomController.isSelecting()) {
				return;
			}
			
			Rectangle selection = mosaicZoomController.getSelectionArea();
			if (selection.isEmpty()) {
				return;
			}
			
			// Convertir coordenadas del mosaico a pantalla
			Point screenStart = mosaicToScreen(new Point(selection.x, selection.y));
			Point screenEnd = mosaicToScreen(new Point(selection.x + selection.width, 
													   selection.y + selection.height));
			
			// Dibujar rectángulo de selección
			g2.setColor(new Color(100, 150, 255, 100)); // Azul semi-transparente
			g2.fillRect(screenStart.x, screenStart.y, 
					   screenEnd.x - screenStart.x, screenEnd.y - screenStart.y);
					   
			g2.setStroke(new BasicStroke(2));
			g2.setColor(new Color(50, 100, 200)); // Borde azul sólido
			g2.drawRect(screenStart.x, screenStart.y, 
					   screenEnd.x - screenStart.x, screenEnd.y - screenStart.y);
		}
		
		/**
		 * Convierte coordenadas de pantalla a coordenadas del mosaico considerando el zoom.
		 */
		private Point screenToMosaic(Point screenPoint) {
			if (mosaicImageSize == null || shownImageSize == null) {
				return new Point(0, 0);
			}
			
			// Si no hay zoom activo, usar conversión simple
			double zoomFactor = mosaicZoomController.getCurrentZoomFactor();
			// CORREGIDO: Comparar directamente con 1.0 O usar precision más amplia para capturar zoom inicial
			if (zoomFactor == 1.0 || Math.abs(zoomFactor - 1.0) < 0.001) {
				// PRECISION FIX: Usar el tamaño exacto como en paintComponent sin zoom
				double scaleX = (double) mosaicImageSize.width / shownImageSize.width;
				double scaleY = (double) mosaicImageSize.height / shownImageSize.height;
				return new Point((int) Math.round(screenPoint.x * scaleX), (int) Math.round(screenPoint.y * scaleY));
			}
			
			// Con zoom: aplicar transformación inversa exacta a la del paintComponent
			Rectangle viewport = mosaicZoomController.getViewportBounds();
			Dimension size = getSize();
			
			// En paintComponent se aplica: scale(size.width/viewport.width, size.height/viewport.height)
			// Para la inversa necesitamos: scale(viewport.width/size.width, viewport.height/size.height)
			double invScaleX = (double) viewport.width / size.width;
			double invScaleY = (double) viewport.height / size.height;
			
			// 1. Aplicar escala inversa
			double unscaledX = screenPoint.x * invScaleX;
			double unscaledY = screenPoint.y * invScaleY;
			
			// 2. Aplicar traslación inversa (en paintComponent se hace translate(-viewport.x, -viewport.y))
			int mosaicX = (int) (unscaledX + viewport.x);
			int mosaicY = (int) (unscaledY + viewport.y);
			
			return new Point(mosaicX, mosaicY);
		}
		
		/**
		 * Convierte coordenadas del mosaico a coordenadas de pantalla considerando el zoom.
		 */
		private Point mosaicToScreen(Point mosaicPoint) {
			if (mosaicImageSize == null || shownImageSize == null) {
				return new Point(0, 0);
			}
			
			// Si no hay zoom activo, usar conversión simple
			double zoomFactor = mosaicZoomController.getCurrentZoomFactor();
			// CORREGIDO: Comparar directamente con 1.0 O usar precision más amplia para capturar zoom inicial
			if (zoomFactor == 1.0 || Math.abs(zoomFactor - 1.0) < 0.001) {
				// PRECISION FIX: Mayor precisión para zoom 100%
				double scaleX = (double) shownImageSize.width / mosaicImageSize.width;
				double scaleY = (double) shownImageSize.height / mosaicImageSize.height;
				return new Point((int) Math.round(mosaicPoint.x * scaleX), (int) Math.round(mosaicPoint.y * scaleY));
			}
			
			// Con zoom: considerar el viewport actual
			Rectangle viewport = mosaicZoomController.getViewportBounds();
			Dimension size = getSize();
			
			// Convertir punto del mosaico a coordenadas relativas del viewport
			int relativeX = mosaicPoint.x - viewport.x;
			int relativeY = mosaicPoint.y - viewport.y;
			
			// Si está fuera del viewport, retornar punto fuera de pantalla
			if (relativeX < 0 || relativeY < 0 || 
				relativeX >= viewport.width || relativeY >= viewport.height) {
				return new Point(-1, -1);
			}
			
			// CORREGIDO: Usar getSize() como en screenToMosaic para consistencia
			double scaleX = (double) size.width / viewport.width;
			double scaleY = (double) size.height / viewport.height;
			
			// PRECISION FIX: Usar Math.round para mayor precisión
			return new Point((int) Math.round(relativeX * scaleX), (int) Math.round(relativeY * scaleY));
		}
		
		/**
		 * Realiza zoom a la selección especificada.
		 */
		private void zoomToSelection(Rectangle selection) {
			// Por ahora, simplemente centramos en la selección y hacemos zoom in
			Point center = new Point(selection.x + selection.width/2, selection.y + selection.height/2);
			mosaicZoomController.centerViewportOn(center);
			mosaicZoomController.zoomIn();
		}
		
		// Implementación de MosaicZoomListener
		@Override
		public void zoomChanged(double zoomFactor, int zoomIndex) {
			// Actualizar tamaños cuando cambia el zoom
			updateZoomDisplay();
			repaint();
		}
		
		@Override
		public void viewportChanged(Rectangle viewport) {
			// El viewport cambió, repintar
			repaint();
		}
		
		@Override
		public void selectionChanged(Rectangle selection, boolean isSelecting) {
			// La selección cambió, repintar para mostrar/ocultar selección
			repaint();
		}
		
		/**
		 * Actualiza la visualización según el nivel de zoom actual.
		 */
		private void updateZoomDisplay() {
			if (mosaicImageSize != null) {
				mosaicZoomController.setMosaicSize(mosaicImageSize);
				mosaicZoomController.setViewportSize(getSize());
			}
		}
		
		/**
		 * Alterna el modo de zoom.
		 */
		public void toggleZoomMode() {
			isZoomMode = !isZoomMode;
			// Cambiar cursor o indicador visual del modo
			setCursor(isZoomMode ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) 
								 : Cursor.getDefaultCursor());
		}
		
		/**
		 * Establece el modo de zoom.
		 */
		public void setZoomMode(boolean enabled) {
			isZoomMode = enabled;
			setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) 
							  : Cursor.getDefaultCursor());
		}
	}
	
	private class MagnifierCanvas extends JPanel {
		public MagnifierCanvas() {
			super(new BorderLayout()); // Orientation in top. ButtonPanel in center.
			
			// Top panel with position guide:
			final JPanel topPanel = new JPanel() {
				@Override
				public void paintComponent(Graphics g) {
					super.paintComponent(g);
					Graphics2D g2 = (Graphics2D)g;
					Dimension size = getSize();
					int page = magnifierController.getMagnifierPage();
					printController.drawShownPositionForMagnifier(g2, page, size.width, size.height);
				}
			};
			topPanel.setPreferredSize(new Dimension(16, 112));
			printController.addChangeListener(new ChangeListener() {				
				@Override
				public void stateChanged(ChangeEvent e) {
					if(printController.getShowPosition() == ShowPosition.None) {
						if(topPanel.isVisible())
							topPanel.setVisible(false);
					}
					else {
						if(!topPanel.isVisible())
							topPanel.setVisible(true);						
					}
					topPanel.repaint();
				}
			});
			add(topPanel, BorderLayout.NORTH);
			
			// Button + magnifier panel:
			JPanel buttonPanel = new JPanel(new BorderLayout());
			JButton bLeft = new JButton(Icons.moveLeft(16));
			bLeft.addKeyListener(magnifierController);
			bLeft.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					magnifierController.moveMagnifierLeft();
				}
			});
			buttonPanel.add(bLeft, BorderLayout.WEST);
			JButton bUp = new JButton(Icons.moveUp(16));
			bUp.addKeyListener(magnifierController);
			bUp.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					magnifierController.moveMagnifierUp();
				}
			});
			buttonPanel.add(bUp, BorderLayout.NORTH);
			JButton bRight = new JButton(Icons.moveRight(16));
			bRight.addKeyListener(magnifierController);
			bRight.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					magnifierController.moveMagnifierRight();
				}
			});
			buttonPanel.add(bRight, BorderLayout.EAST);
			JButton bDown = new JButton(Icons.moveDown(16));
			bDown.addKeyListener(magnifierController);
			bDown.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					magnifierController.moveMagnifierDown();
				}
			});
			buttonPanel.add(bDown, BorderLayout.SOUTH);
			
			JPanel displayPanel = new JPanel() {
				@Override
				public void paintComponent(Graphics g) {
					super.paintComponent(g);
					Graphics2D g2 = (Graphics2D)g;
					Rectangle viewRect = computeShownRect(getSize());
					Dimension shownMagnifierSize = new Dimension(viewRect.width, viewRect.height);
					setPreferredSize(shownMagnifierSize);
					
					g2.translate(viewRect.x, viewRect.y);

					// draw magnified:
					g2.setColor(Color.BLACK);
					ToBricksTransform tbTransform = magnifierController.getTBTransform();
					Rectangle basicUnitRect = magnifierController.getCoreRect();
					LEGOColor.CountingLEGOColor[] used = tbTransform.draw(g2, basicUnitRect, shownMagnifierSize, uiController.showColors(), true, false); // false = for viewport display
					legend.setHighlightedColors(used);
				}
			};
			buttonPanel.add(displayPanel);
			
			add(buttonPanel, BorderLayout.CENTER);
		}
		
		public Rectangle computeShownRect(final Dimension componentSize) {
			double componentW2H = componentSize.width / (double)componentSize.height;		
			Dimension magnifierSizeInUnits = magnifierController.getSizeInUnits();
			double imageW2H = magnifierSizeInUnits.width / (double)magnifierSizeInUnits.height;
			
			Dimension outSize;
			if(componentW2H < imageW2H) {
				outSize = new Dimension(componentSize.width, componentSize.width * magnifierSizeInUnits.height / magnifierSizeInUnits.width);
			}
			else {
				outSize = new Dimension(componentSize.height * magnifierSizeInUnits.width / magnifierSizeInUnits.height, componentSize.height);				
			}
			
			return new Rectangle((componentSize.width-outSize.width)/2, (componentSize.height-outSize.height)/2, outSize.width, outSize.height);
		}		
	}
}
