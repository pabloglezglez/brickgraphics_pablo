package mosaic.controllers;

import io.*;

import java.awt.image.*;
import java.io.*;
import javax.swing.filechooser.FileFilter;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;
import colors.parsers.*;
import mosaic.io.*;
import mosaic.io.RecentFilesManager;
import mosaic.rendering.Pipeline;
import mosaic.rendering.RenderingProgressBar;
import mosaic.ui.*;
import mosaic.ui.dialogs.PrintDialog;
import mosaic.ui.dialogs.ToBricksTypeFilterDialog;
import mosaic.ui.panels.IntegratedImageLayerPanel;
import mosaic.layers.LayerManager;
import mosaic.ui.panels.LayerPanel;

/**
 * @author LD
 */
public class MainController implements ModelHandler<BrickGraphicsState> {
	public static final String APP_NAME = "LD Digital Mosaic Creator";
	public static final String APP_NAME_SHORT = "LDDMC";
	public static final String LOG_FILE_NAME = "lddmc.log";
	public static final String STATE_FILE_NAME = "lddmc.kvm";
	public static final int VERSION_MAJOR = 0;
	public static final int VERSION_MINOR = 9;
	public static final int VERSION_MICRO = 4;
	public static final String APP_VERSION = VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_MICRO;
	public static final String HELP_URL = "http://c-mt.dk/software/lddmc/help";
	
	private Model<BrickGraphicsState> model;
	private Pipeline pipeline;
	private List<ChangeListener> listeners;

	private MagnifierController magnifierController;
	private UIController uiController;
	private ColorController colorController;
	private PrintController printController;
	private ToBricksController toBricksController;
	private OptionsController optionsController;
	private StudEditController studEditController;
	private MosaicZoomController mosaicZoomController;	
	
	private MainWindow mw;
	private SaveDialog saveDialog;
	private PrintDialog printDialog;
	private ToBricksTypeFilterDialog toBricksTypeFilterDialog;
	private RecentFilesManager recentFilesManager;
	private LayerManager layerManager;
	private IntegratedImageLayerPanel layerPanel;
	private ColorLegend legend;
	
	// Image (for model state):
	private String imageFileName;
	private DataFile imageDataFile;
	private BufferedImage originalImage; // Imagen base sin capas aplicadas
	private File mosaicFile;

	private MainController() {		
		listeners = new ArrayList<ChangeListener>();		
		mosaicFile = null;

		try {
			Log.initializeLog(LOG_FILE_NAME);
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, "The log file " + LOG_FILE_NAME + " could not be opened for writing.\nLDDMC might not have sufficient permissions.\nLog messages are written to console if available.\nThe error message:\n" + e1.getMessage(), "Failed to open/create log file", JOptionPane.WARNING_MESSAGE);
		}
		long startTime = System.currentTimeMillis();
		Log.log("Initiating components");
		model = new Model<BrickGraphicsState>(STATE_FILE_NAME, BrickGraphicsState.class);
		
		RenderingProgressBar renderingProgressBar = new RenderingProgressBar();
		pipeline = new Pipeline(renderingProgressBar);
		Log.log("Model file loaded");
		handleModelChange(model);
		
		// Set up controllers:
		optionsController = new OptionsController(model);
		colorController = new ColorController(model);
		uiController = new UIController(model);
		magnifierController = new MagnifierController(model, uiController);
		toBricksController = new ToBricksController(this, model);
		printController = new PrintController(model, this, pipeline);
		studEditController = new StudEditController(colorController); // Crear temprano sin BrickedView
		mosaicZoomController = new MosaicZoomController(model);		
		io.Log.log("MosaicZoomController creado: " + (mosaicZoomController != null));
		Log.log("Created controllers after " + (System.currentTimeMillis()-startTime) + "ms.");

		// Initialize layer system BEFORE creating UI
		layerManager = new LayerManager();
		// Note: the IntegratedImageLayerPanel is created by the MainWindow UI
		// and passed to this controller via setLayerPanel(...) to avoid having
		// two separate instances (one in the UI and one here).
		
		// Set up UI:
		mw = new MainWindow(this, model, pipeline, renderingProgressBar);
		printController.setMainWindow(mw);
		
		// Initialize recent files manager
		recentFilesManager = new RecentFilesManager(this, mw);
		
		// The MainWindow creates the IntegratedImageLayerPanel and registers
		// the callback that calls updateImageWithLayers(). The controller's
		// setLayerPanel(...) will be used by MainWindow to provide the
		// shared instance.
		
		// Set up callback para cuando se eliminen todas las capas
		layerManager.setOnAllLayersRemovedCallback(() -> {
			// Restaurar la imagen original cuando no queden capas
			io.Log.log("DEBUG: MainController - Restaurando imagen original al eliminar todas las capas");
			updateImageWithLayers(); // Esto aplicará las capas (vacías) sobre la imagen original
		});
		
		// Establecer la referencia al BrickedView después de crear MainWindow
		studEditController.setBrickedView(mw.getBrickedView());
		
		// Crear ColorLegend después de tener StudEditController completamente inicializado
		legend = new ColorLegend(this, pipeline);
		legend.setBrickedView(mw.getBrickedView());
		
		// Establecer la leyenda en MainWindow
		mw.setLegend(legend);
		
		listeners.add(mw);
		toBricksController.initiateUI(mw);
		optionsController.initiateOptionsDialog(mw);
		
		// Registrar controladores como ModelHandler para que se guarden sus datos
		model.addModelHandler(studEditController); // Para guardar modificaciones manuales
		model.addModelHandler(layerManager); // Para guardar y cargar capas de imagen
		model.addModelHandler(this); // Make sure the load of image file is late in the process when loading a model.
		
		Log.log("LDDMC main window operational after " + (System.currentTimeMillis()-startTime) + "ms.");

		if(colorController.usesBackupColors()) {
			JOptionPane.showMessageDialog(mw, "The file " + ColorSheetParser.COLORS_FILE + " could not be read.\nBackup colors are used.\nTo get all the functionality of this program, please make sure that the file exists and that the program is allowed to read the file.\nThis also applies to the other files and folders of the program.", "Error reading file", JOptionPane.WARNING_MESSAGE);
		}
		
		if(!imageDataFile.isValid()) {
			try {
				MosaicIO.load(this, new File(imageFileName));
			}
			catch (Exception e) {
				Log.log(e);
				Action openAction = MosaicIO.createOpenAction(this, mw);
				openAction.actionPerformed(null);
			}				
		}
		else
			notifyListeners(model);
		
		saveDialog = new SaveDialog(mw);
		printDialog = new PrintDialog(mw, printController, colorController, magnifierController, pipeline);
		toBricksTypeFilterDialog = new ToBricksTypeFilterDialog(toBricksController, mw);
		
		pipeline.start();
		Log.log("MainController initiated.");
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Thread() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					Log.log(e);
				}
				new MainController();
			}
		});
	}
	
	private void notifyListeners(Object source) {
		ChangeEvent e = new ChangeEvent(source);
		for(ChangeListener l : listeners) {
			l.stateChanged(e);
		}
	}
	
	public void setImage(BufferedImage image, File imageFile) throws IOException {
		if(imageFile == null) {
			imageDataFile = new DataFile(image);
			imageFileName = "New Image";
		}
		else {
			imageDataFile = new DataFile(imageFile);		
			imageFileName = imageFile.getCanonicalPath();
			
			// Add to recent files when opening an image
			if (recentFilesManager != null) {
				recentFilesManager.addRecentFile(imageFile);
			}
		}
		mosaicFile = null;
		
		// Guardar la imagen original sin capas
		originalImage = image;
		
		// Aplicar capas y establecer en el pipeline
		updateImageWithLayers();
		
		notifyListeners(this);
	}
	
	/**
	 * Aplica las capas sobre la imagen original y actualiza el pipeline
	 */
	public void updateImageWithLayers() {
		io.Log.log("DEBUG: MainController - updateImageWithLayers() llamado");
		if (originalImage == null || layerManager == null) {
			io.Log.log("DEBUG: MainController - Skipping, originalImage o layerManager es null");
			return; // Skip si no están inicializados aún
		}
		
		// Aplicar transformaciones de imagen de fondo y capas sobre la imagen original
		io.Log.log("DEBUG: MainController - Aplicando capas a imagen original...");
		BufferedImage imageWithLayers;
		
		// Verificar si hay transformaciones de imagen de fondo configuradas
		if (layerPanel != null) {
			float brightness = layerPanel.getBackgroundBrightness();
			float contrast = layerPanel.getBackgroundContrast();
			float saturation = layerPanel.getBackgroundSaturation();
			float gamma = layerPanel.getBackgroundGamma();
			float sharpness = layerPanel.getBackgroundSharpness();
			
			io.Log.log("DEBUG: MainController - Llamando applyBackgroundTransformationsAndLayers con valores: " +
							  "brightness=" + brightness + ", contrast=" + contrast + ", saturation=" + saturation + 
							  ", gamma=" + gamma + ", sharpness=" + sharpness);
			
			// Usar método que aplica transformaciones de fondo y luego capas
			imageWithLayers = layerManager.applyBackgroundTransformationsAndLayers(
				originalImage,
				brightness, contrast, saturation, gamma, sharpness
			);
		} else {
			// Fallback al método original si no hay panel de transformaciones
			imageWithLayers = layerManager.applyLayersToImage(originalImage);
		}
		io.Log.log("DEBUG: MainController - Imagen con capas creada");
		
		// Establecer la imagen procesada en el pipeline
		pipeline.setStartImage(imageWithLayers);
		io.Log.log("DEBUG: MainController - Pipeline actualizado con imagen+capas");
		
		// Repintar la vista para mostrar los cambios
		if (mw != null && mw.getBrickedView() != null) {
					mw.getBrickedView().repaint();
					io.Log.log("DEBUG: MainController - BrickedView repintada");
		}
	}
	
	public void loadMosaicFile(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		model.loadFrom(br);
		br.close();
		fis.close();
		
		mosaicFile = file;
		
		// Add to recent files when opening a mosaic file
		if (recentFilesManager != null) {
			recentFilesManager.addRecentFile(file);
		}
		
		// Resetear todas las intensidades de colores a 1.0 al abrir un archivo
		// El método resetColorIntensitiesIfSafe evita el reset en modo SNOT
		colorController.resetColorIntensitiesIfSafe(toBricksController);
		
		notifyListeners(model);
	}
	
	public File getMosaicFile() {
		return mosaicFile;
	}
	
	public void setMosaicFile(File file) {
		this.mosaicFile = file;
		
		// Add to recent files when a file is set
		if (file != null && recentFilesManager != null) {
			recentFilesManager.addRecentFile(file);
		}
	}
	
	public RecentFilesManager getRecentFilesManager() {
		return recentFilesManager;
	}
	
	public File showSaveDialog(String saveMessage, FileFilter... filters) {		
		return saveDialog.showSaveDialog(saveMessage, filters);
	}
	
	public void showToBricksTypeFilterDialog() {
		toBricksTypeFilterDialog.pack();
		toBricksTypeFilterDialog.setVisible(true);
	}
	
	public File getFile() {
		if(imageFileName == null)
			return null;
		return new File(imageFileName);
	}
	
	public Model<BrickGraphicsState> getModel() {
		return model;
	}
	
	public ColorController getColorController() {
		return colorController;
	}
	
	public PrintController getPrintController() {
		return printController;
	}
	
	public MagnifierController getMagnifierController() {
		return magnifierController;
	}

	public UIController getUIController() {
		return uiController;
	}
	
	public OptionsController getOptionsController() {
		return optionsController;
	}

	public ToBricksController getToBricksController() {
		return toBricksController;
	}
	
	public StudEditController getStudEditController() {
		return studEditController;
	}
	
	public MosaicZoomController getMosaicZoomController() {
		return mosaicZoomController;
	}

	public ColorLegend getLegend() {
		return legend;
	}
	
	public PrintDialog getPrintDialog() {
		return printDialog;
	}
	
	public LayerManager getLayerManager() {
		return layerManager;
	}
	
	public IntegratedImageLayerPanel getLayerPanel() {
		return layerPanel;
	}

	/**
	 * Reemplaza el panel de capas integrado que usa el controlador.
	 * Esto permite que la instancia creada por la UI (MainWindow) se comparta
	 * con el controlador para evitar duplicados y valores desincronizados.
	 */
	public void setLayerPanel(IntegratedImageLayerPanel panel) {
		this.layerPanel = panel;
	}
	
	@Override
	public void handleModelChange(Model<BrickGraphicsState> model) {
		imageFileName = (String)model.get(BrickGraphicsState.ImageFileName);
		imageDataFile = (DataFile)model.get(BrickGraphicsState.ImageFile);
		
		// Cargar imagen original desde estado guardado si existe
		DataFile originalImageDataFile = (DataFile)model.get(BrickGraphicsState.OriginalImageFile);
		
		if (originalImageDataFile != null && originalImageDataFile.isValid()) {
			// Restaurar imagen original guardada
			try {
				BufferedImage originalImageFromFile = MosaicIO.removeAlpha(ImageIO.read(originalImageDataFile.fakeStream()));
				originalImage = originalImageFromFile;
				io.Log.log("DEBUG: MainController - Imagen original restaurada desde KMV");
				updateImageWithLayers();
			} catch (IOException e) {
				Log.log("Error cargando imagen original: " + e.getMessage());
				// Fallback a imagen normal
				loadRegularImage();
			}
		} else if(imageDataFile.isValid()) {
			// No hay imagen original guardada, usar imagen normal
			loadRegularImage();
		} else {
			Log.log("INVALID Image data file!");
		}
	}
	
	/**
	 * Carga la imagen normal cuando no hay imagen original guardada
	 */
	private void loadRegularImage() {
		try {
			BufferedImage image = MosaicIO.removeAlpha(ImageIO.read(imageDataFile.fakeStream()));
			// Guardar como imagen original
			originalImage = image;
			// Aplicar capas y establecer en el pipeline
			updateImageWithLayers();
		} catch (IOException e) {
			Log.log(e);
		}
	}

	@Override
	public void save(Model<BrickGraphicsState> model) {
		model.set(BrickGraphicsState.ImageFileName, imageFileName);
		model.set(BrickGraphicsState.ImageFile, imageDataFile);
		
		// Guardar imagen original sin capas si existe
		if (originalImage != null) {
			try {
				DataFile originalImageDataFile = new DataFile(originalImage);
				model.set(BrickGraphicsState.OriginalImageFile, originalImageDataFile);
				io.Log.log("DEBUG: MainController - Imagen original guardada en KMV");
			} catch (Exception e) {
				Log.log("Error guardando imagen original: " + e.getMessage());
				// En caso de error, no guardar imagen original
				model.set(BrickGraphicsState.OriginalImageFile, new DataFile());
			}
		}
	}
}
