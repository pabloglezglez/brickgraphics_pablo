package mosaic.ui;

import transforms.*;
import transforms.ScaleTransform.ScaleQuality;
import icon.Icons;
import io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.event.*;
import colors.*;
import mosaic.controllers.*;
import mosaic.controllers.PrintController.ShowPosition;
import mosaic.io.*;
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
	private Dimension shownImageSize;
	
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
		if(toBricksTransform.getToBricksType() == ToBricksType.SNOT_IN_2_BY_2)
			colors = toBricksTransform.lastUsedColorCounts();				
		else
			colors = toBricksTransform.getMainTransform().lastUsedColorCounts();
			
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
	 * @param clickX coordenada X del click
	 * @param clickY coordenada Y del click
	 */
	private void handleMosaicClick(int clickX, int clickY) {
		if (studEditController.getActiveTool() == EditTool.DEFAULT) {
			return; // No hacer nada si está en modo navegación
		}
		
		// Convertir coordenadas del click a coordenadas del grid del mosaico
		LEGOColorGrid colorGrid = getColorGrid();
		if (colorGrid == null) {
			return;
		}
		
		// Calcular las coordenadas del stud basándose en la escala y posición
		if (shownImageSize == null || mosaicImageSize == null) {
			return;
		}
		
		// Calcular la escala de transformación
		double scaleX = (double) shownImageSize.width / mosaicImageSize.width;
		double scaleY = (double) shownImageSize.height / mosaicImageSize.height;
		
		// Convertir coordenadas del click a coordenadas del mosaico original
		int mosaicX = (int) (clickX / scaleX);
		int mosaicY = (int) (clickY / scaleY);
		
		// Convertir a coordenadas del grid (cada stud puede representar múltiples píxeles)
		// Cada stud cubre varios píxeles del mosaico
		int studPixelWidth = mosaicImageSize.width / colorGrid.getWidth();
		int studPixelHeight = mosaicImageSize.height / colorGrid.getHeight();
		
		int gridX = mosaicX / studPixelWidth;
		int gridY = mosaicY / studPixelHeight;
		
		// Verificar límites
		if (gridX < 0 || gridX >= colorGrid.getWidth() || gridY < 0 || gridY >= colorGrid.getHeight()) {
			return;
		}
		
		// Aplicar la herramienta
		boolean changed = studEditController.applyToolAt(colorGrid, gridX, gridY);
		
		if (changed) {
			// Forzar actualización de la vista y leyenda
			pipeline.invalidate();
			repaint();
		}
	}
	
	/**
	 * Obtiene el grid de colores del mosaico actual.
	 * @return el LEGOColorGrid o null si no está disponible
	 */
	private LEGOColorGrid getColorGrid() {
		if (toBricksTransform == null) {
			return null;
		}
		
		// Obtener el grid de colores del transform principal
		BufferedLEGOColorTransform mainTransform = toBricksTransform.getMainTransform();
		if (mainTransform != null) {
			return mainTransform.getCurrentColorGrid();
		}
		
		return null;
	}
	
	// Variables para el cursor visual
	private int hoveredX = -1; // Coordenadas del stud bajo el cursor
	private int hoveredY = -1;
	private boolean showHoverCursor = false;
	
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
		
		// Convertir coordenadas del mouse a coordenadas del grid
		double scaleX = (double) shownImageSize.width / mosaicImageSize.width;
		double scaleY = (double) shownImageSize.height / mosaicImageSize.height;
		
		int mosaicX = (int) (mouseX / scaleX);
		int mosaicY = (int) (mouseY / scaleY);
		
		// Calcular coordenadas del grid basándose en el tamaño de los studs
		int studPixelWidth = mosaicImageSize.width / colorGrid.getWidth();
		int studPixelHeight = mosaicImageSize.height / colorGrid.getHeight();
		
		int newHoveredX = mosaicX / studPixelWidth;
		int newHoveredY = mosaicY / studPixelHeight;
		
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

	// Used by CAD exports
	public Dimension getBrickedSize() {
		return mosaicImageSize;
	}

	@Override
	public void mosaicChanged(Dimension mosaicImageSize) {
		this.mosaicImageSize = mosaicImageSize;
		repaint();
	}
	
	private class MosaicCanvas extends JPanel {
		public MosaicCanvas() {
			// Agregar mouse listener para herramientas de edición
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					handleMosaicClick(e.getX(), e.getY());
				}
			});
			
			// Agregar mouse motion listener para cursor visual
			addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					updateHoverCursor(e.getX(), e.getY());
				}
			});
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
			magnifierController.setShownImageSize(shownImageSize);
				
			Graphics2D g2 = (Graphics2D)g;

			// Perform actual drawing:
			toBricksTransform.drawAll(g2, shownImageSize);
			
			// Dibujar cursor de edición si está activo
			drawEditCursor(g2);
		}
		
		/**
		 * Dibuja el cursor de edición sobre el stud seleccionado.
		 */
		private void drawEditCursor(Graphics2D g2) {
			if (!showHoverCursor || studEditController.getActiveTool() == EditTool.DEFAULT) {
				return;
			}
			
			LEGOColorGrid colorGrid = getColorGrid();
			if (colorGrid == null || hoveredX < 0 || hoveredY < 0) {
				return;
			}
			
			// Calcular posición en píxeles del stud
			int studPixelWidth = mosaicImageSize.width / colorGrid.getWidth();
			int studPixelHeight = mosaicImageSize.height / colorGrid.getHeight();
			
			// Aplicar escala de visualización
			double scaleX = (double) shownImageSize.width / mosaicImageSize.width;
			double scaleY = (double) shownImageSize.height / mosaicImageSize.height;
			
			int pixelX = (int) (hoveredX * studPixelWidth * scaleX);
			int pixelY = (int) (hoveredY * studPixelHeight * scaleY);
			int pixelW = (int) (studPixelWidth * scaleX);
			int pixelH = (int) (studPixelHeight * scaleY);
			
			// Configurar el cursor según la herramienta activa
			g2.setStroke(new BasicStroke(2));
			
			switch (studEditController.getActiveTool()) {
				case BRUSH:
					// Cuadrado verde para el pincel
					g2.setColor(Color.GREEN);
					g2.drawRect(pixelX, pixelY, pixelW, pixelH);
					break;
				case EYEDROPPER:
					// Círculo azul para el eyedropper
					g2.setColor(Color.BLUE);
					g2.drawOval(pixelX, pixelY, pixelW, pixelH);
					break;
				case RESET:
					// Cruz roja para reset
					g2.setColor(Color.RED);
					g2.drawLine(pixelX, pixelY, pixelX + pixelW, pixelY + pixelH);
					g2.drawLine(pixelX + pixelW, pixelY, pixelX, pixelY + pixelH);
					break;
			}
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
