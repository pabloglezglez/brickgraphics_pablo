package mosaic.ui;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.*;
import java.io.File;
import java.io.IOException;
import icon.Icons;
import io.*;
import javax.swing.*;
import javax.swing.event.*;
import mosaic.controllers.*;
import mosaic.io.BrickGraphicsState;
import mosaic.io.MosaicIO;
import mosaic.rendering.Pipeline;
import mosaic.rendering.RenderingProgressBar;
import mosaic.ui.dialogs.ColorChooserDialog;
import mosaic.ui.panels.IntegratedImageLayerPanel;
import mosaic.ui.dialogs.ColorSettingsDialog;
import mosaic.ui.menu.*;

public class MainWindow extends JFrame implements ChangeListener, ModelHandler<BrickGraphicsState> {
	private ImagePreparingView imagePreparingView;
	private BrickedView brickedView;
	private JSplitPane splitPane;
	private JPanel legendPanel; // Panel para la leyenda
	private ColorChooserDialog colorChooserDialog;
	private MainController mc;
	private Pipeline pipeline;
	private Rectangle lastNormalPlacement;
	private MosaicZoomPanel zoomPanel;
	private IntegratedImageLayerPanel integratedPanel; // Panel integrado de capas e imagen

	public MainWindow(final MainController mc, final Model<BrickGraphicsState> model, 
			final Pipeline pipeline, RenderingProgressBar renderingProgressBar) {
		super(MainController.APP_NAME);
		this.mc = mc;
		this.pipeline = pipeline;
		model.addModelHandler(this);
		long startTime = System.currentTimeMillis();

		setVisible(true);

		imagePreparingView = new ImagePreparingView(model, mc.getOptionsController(), mc.getToBricksController(), pipeline);
		Log.log("Created left view after " + (System.currentTimeMillis()-startTime) + "ms.");

		brickedView = new BrickedView(mc, model, pipeline);
		zoomPanel = new MosaicZoomPanel(mc.getMosaicZoomController(), brickedView);
		Log.log("Created right view after " + (System.currentTimeMillis()-startTime) + "ms.");

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					model.saveToFile();
				}
				catch (IOException e2) {
					Log.log(e2);
				}
				Log.close();
				System.exit(0);
			}
		});
		getContentPane().addHierarchyBoundsListener(new HierarchyBoundsListener(){
			@Override
			public void ancestorMoved(HierarchyEvent e) {
				updateModel();				
			}
			@Override
			public void ancestorResized(HierarchyEvent e) {
				updateModel();
			}			
			private void updateModel() {
				int state = MainWindow.this.getExtendedState();
				if((state | Frame.NORMAL) == Frame.NORMAL) {
					lastNormalPlacement = MainWindow.this.getBounds();
				}
			}
		});

		// Crear panel derecho que contenga el mosaico y controles de zoom
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(zoomPanel, BorderLayout.NORTH);
		rightPanel.add(brickedView, BorderLayout.CENTER);
		
		// in split pane:
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imagePreparingView, rightPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerSize(16);
		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				int dividerLocation = splitPane.getDividerLocation();
				imagePreparingView.setVisible(dividerLocation >= getMinDividerLocation());
				brickedView.setVisible(splitPane.getWidth() == 0 || dividerLocation <= getMaxDividerLocation());
				pipeline.invalidate();
			}
		});

		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		{
			// Panel lateral izquierdo: Panel integrado de capas e imagen
			JPanel leftPanel = new JPanel(new BorderLayout());
			
			// Crear panel integrado que combina capas e imagen
			integratedPanel = new IntegratedImageLayerPanel(
				mc.getLayerManager(), imagePreparingView, model, brickedView, mc);
			
			// Configurar callback para cambios de capas
			integratedPanel.setOnLayersChangedCallback(() -> {
				// Actualizar capas en el mainController
				if (mc != null) {
					// Forzar actualización de capas en la imagen
					mc.updateImageWithLayers();
				}
				// También repintar la vista
				if (brickedView != null) {
					brickedView.repaint();
				}
			});

			// Asegurar que MainController use la misma instancia del panel integrado
			// para evitar tener dos paneles distintos (uno en el controlador y otro en la UI)
			if (mc != null) {
				mc.setLayerPanel(integratedPanel);

			}
			
			leftPanel.add(integratedPanel, BorderLayout.CENTER);
			leftPanel.setPreferredSize(new Dimension(300, 600));
			cp.add(leftPanel, BorderLayout.WEST);			
		}
		{
			// Panel lateral derecho: Solo Legend
			legendPanel = new JPanel(new BorderLayout());
			legendPanel.setPreferredSize(new Dimension(250, 400));
			cp.add(legendPanel, BorderLayout.EAST);
		}

		{
			// Add drag'n'drop to the panel where it makes sense to drop stuff::
			splitPane.setTransferHandler(new TransferHandler() {
				@Override
				public boolean canImport(TransferHandler.TransferSupport info) {
					Log.log("canImport");
					return info.isDrop() && (info.isDataFlavorSupported(DataFlavor.imageFlavor) ||
							info.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
				}

				@Override
				public boolean importData(TransferHandler.TransferSupport info) {
					Log.log("import");
					if (!canImport(info))
						return false;

					// Get the image that is being dropped.
					Transferable t = info.getTransferable();
					try {
						if(info.isDataFlavorSupported(DataFlavor.imageFlavor)) {
							Log.log("Image is being dropped.");
							BufferedImage data = (BufferedImage)t.getTransferData(DataFlavor.imageFlavor);
							MosaicIO.load(mc, data);							
						}
						else { // File to load
							Log.log("File is being dropped.");
							@SuppressWarnings("rawtypes")
							java.util.List files = (java.util.List)t.getTransferData(DataFlavor.javaFileListFlavor);
							if(files.isEmpty())
								return false;
							File file = (File)files.get(0);
							MosaicIO.load(mc, file);
						}
						return true;
					} 
					catch (Exception e) { 
						Log.log("error");
						return false; 
					}
				}
			});
		}

		cp.add(splitPane, BorderLayout.CENTER);
		cp.add(renderingProgressBar, BorderLayout.SOUTH);

		// Agregar MouseWheelListener global para interceptar Ctrl+rueda
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				// OPTIMIZACIÓN: Solo log en caso de error para evitar spam
				if (e.isControlDown()) {
					// IMPORTANTE: Consumir el evento INMEDIATAMENTE para prevenir zoom del sistema
					e.consume();
					
					try {
						// Convertir coordenadas del evento a coordenadas relativas al BrickedView
						Point eventPoint = e.getPoint();
						
						// Calcular posición relativa al BrickedView de forma simple
						Point brickedViewLocation = brickedView.getLocation();
						
						// Calcular posición relativa al BrickedView
						int relativeX = eventPoint.x - brickedViewLocation.x;
						int relativeY = eventPoint.y - brickedViewLocation.y;
						Point relativePoint = new Point(relativeX, relativeY);
						
						// Llamar método de zoom (logs eliminados para rendimiento)
					} catch (Exception ex) {
						Log.log("Error en mouseWheelMoved: " + ex.getMessage());
						Log.log(ex);
					}
				}
			}
		});

		handleModelChange(model);
		Log.log("LDDMC main window operational after " + (System.currentTimeMillis()-startTime) + "ms.");
	}

	public void finishUpRibbonMenuAndIcon() {
		colorChooserDialog = new ColorChooserDialog(mc, MainWindow.this, pipeline); // Must be made before ribbon!

		Ribbon ribbon = new Ribbon(mc, MainWindow.this);
		//mc.getToBricksController().addComponents(ribbon, mc);
		
		getContentPane().add(ribbon, BorderLayout.NORTH);
		ColorSettingsDialog csd = new ColorSettingsDialog(MainWindow.this, mc.getColorController());
		setJMenuBar(new MainMenu(mc, MainWindow.this, csd));
		setIconImage(Icons.get(32, "icon", "LDDMC").getImage());		

		ribbon.setVisible(false);
		ribbon.setVisible(true);		
	}

	private int getMinDividerLocation() {
		return Math.max(32, imagePreparingView.getPreferredSize().width);
	}

	private int getMaxDividerLocation() {
		return splitPane.getSize().width - splitPane.getDividerSize() - Math.max(32, brickedView.getPreferredSize().width) - 2;
	}

	public ColorChooserDialog getColorChooser() {
		if(colorChooserDialog == null)
			throw new IllegalStateException();
		return colorChooserDialog;
	}

	public BrickedView getBrickedView() {
		if(brickedView == null)
			throw new IllegalStateException();		
		return brickedView;
	}
	
	/**
	 * Establece la leyenda en el panel derecho.
	 */
	public void setLegend(ColorLegend legend) {
		if (legendPanel != null) {
			legendPanel.removeAll();
			legendPanel.add(legend, BorderLayout.CENTER);
			legendPanel.revalidate();
			legendPanel.repaint();
		}
	}

	public ImagePreparingView getImagePreparingView() {
		if(imagePreparingView == null)
			throw new IllegalStateException();
		return imagePreparingView;
	}
	
	public IntegratedImageLayerPanel getLayerPanel() {
		if(integratedPanel == null)
			throw new IllegalStateException();
		return integratedPanel;
	}
	
	public MosaicZoomPanel getZoomPanel() {
		if(zoomPanel == null)
			throw new IllegalStateException();
		return zoomPanel;
	}

	public JSplitPane getSplitPane() {
		if(splitPane == null)
			throw new IllegalStateException();
		return splitPane;
	}

	public Dimension getFinalImageSize() {
		return mc.getMagnifierController().getCoreImageSizeInCoreUnits();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if(e == null || e.getSource() == this)
			return;
		
		updateTitle();
		
		if(splitPane != null)
			repaint();
	}
	
	private void updateTitle() {
		File file = mc.getFile();
		if(file == null)
			setTitle(MainController.APP_NAME);
		else
			setTitle(MainController.APP_NAME + " - " + file.getName());		
	}

	private void setBoundsSafe(Rectangle r) {
		if(r.x < 0)
			r.x = 0;
		if(r.y < 0)
			r.y = 0;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle defaultSize = (Rectangle)BrickGraphicsState.MainWindowPlacement.getDefaultValue();
		if(r.x + r.width > screenSize.width) {
			r.x = 0;
			r.width = Math.min(screenSize.width, defaultSize.width);
		}
		if(r.y + r.height > screenSize.height) {
			r.y = 0;
			r.height = Math.min(screenSize.height, defaultSize.height);
		}

		setBounds(r);		
	}

	@Override
	public void handleModelChange(Model<BrickGraphicsState> model) {
		Rectangle placement = (Rectangle)model.get(BrickGraphicsState.MainWindowPlacement);		
		setBoundsSafe(placement);
		splitPane.setDividerLocation((Integer)model.get(BrickGraphicsState.MainWindowDividerLocation));
	}

	@Override
	public void save(Model<BrickGraphicsState> model) {
		model.set(BrickGraphicsState.MainWindowDividerLocation, splitPane.getDividerLocation());
		model.set(BrickGraphicsState.MainWindowPlacement, lastNormalPlacement);
	}
}
