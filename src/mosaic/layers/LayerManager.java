package mosaic.layers;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import io.Model;
import io.ModelHandler;
import mosaic.io.BrickGraphicsState;
import mosaic.layers.Layer.BlendMode;

/**
 * Gestor principal del sistema de capas.
 * Maneja la carga, posicionamiento y renderizado de capas sobre el mosaico base.
 */
public class LayerManager implements ModelHandler<BrickGraphicsState> {
    private List<Layer> layers;
    private Layer selectedLayer;
    private boolean layersEnabled;
    private float globalOpacity;
    private Runnable onAllLayersRemovedCallback;
    
    /**
     * Constructor del gestor de capas
     */
    public LayerManager() {
        this.layers = new ArrayList<>();
        this.layersEnabled = true;
        this.globalOpacity = 1.0f;
    }
    
    /**
     * Establece el callback para cuando se eliminen todas las capas
     */
    public void setOnAllLayersRemovedCallback(Runnable callback) {
        this.onAllLayersRemovedCallback = callback;
    }
    
    /**
     * Añade una nueva capa desde un archivo de imagen
     */
    public Layer addLayerFromFile(String filePath, Point position) throws IOException {
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            throw new IOException("El archivo no existe: " + filePath);
        }
        
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("No se pudo cargar la imagen: " + filePath);
        }
        
        // Convertir a ARGB si no tiene canal alpha
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            BufferedImage argbImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = argbImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            image = argbImage;
        }
        
        String layerName = generateLayerName(imageFile.getName());
        Layer layer = new Layer(layerName, image, position, filePath);
        
        addLayer(layer);
        setSelectedLayer(layer);
        
        return layer;
    }
    
    /**
     * Añade una capa existente
     */
    public void addLayer(Layer layer) {
        if (layer != null) {
            layers.add(layer);
            io.Log.log("DEBUG: LayerManager - Añadida capa: " + layer.getName());
        }
    }
    
    /**
     * Elimina una capa
     */
    public boolean removeLayer(Layer layer) {
        if (layer != null && layers.remove(layer)) {
            if (selectedLayer == layer) {
                selectedLayer = layers.isEmpty() ? null : layers.get(layers.size() - 1);
            }
            io.Log.log("DEBUG: LayerManager - Eliminada capa: " + layer.getName());
            
            // Si no quedan capas, notificar para restaurar imagen original
            if (layers.isEmpty() && onAllLayersRemovedCallback != null) {
                io.Log.log("DEBUG: LayerManager - No quedan capas, restaurando imagen original");
                onAllLayersRemovedCallback.run();
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Elimina una capa por índice
     */
    public boolean removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            Layer removedLayer = layers.remove(index);
            if (selectedLayer == removedLayer) {
                selectedLayer = layers.isEmpty() ? null : layers.get(Math.min(index, layers.size() - 1));
            }
            io.Log.log("DEBUG: LayerManager - Eliminada capa: " + removedLayer.getName());
            
            // Si no quedan capas, notificar para restaurar imagen original
            if (layers.isEmpty() && onAllLayersRemovedCallback != null) {
                io.Log.log("DEBUG: LayerManager - No quedan capas, restaurando imagen original");
                onAllLayersRemovedCallback.run();
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Mueve una capa hacia arriba en la pila
     */
    public boolean moveLayerUp(Layer layer) {
        int index = layers.indexOf(layer);
        if (index > 0) {
            Collections.swap(layers, index, index - 1);
            io.Log.log("DEBUG: LayerManager - Movida capa hacia arriba: " + layer.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Mueve una capa hacia abajo en la pila
     */
    public boolean moveLayerDown(Layer layer) {
        int index = layers.indexOf(layer);
        if (index >= 0 && index < layers.size() - 1) {
            Collections.swap(layers, index, index + 1);
            io.Log.log("DEBUG: LayerManager - Movida capa hacia abajo: " + layer.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Duplica una capa
     */
    public Layer duplicateLayer(Layer layer) {
        if (layer != null) {
            Layer duplicate = layer.copy();
            int index = layers.indexOf(layer);
            layers.add(index + 1, duplicate);
            setSelectedLayer(duplicate);
            io.Log.log("DEBUG: LayerManager - Duplicada capa: " + layer.getName());
            return duplicate;
        }
        return null;
    }
    
    /**
     * Renderiza todas las capas visibles sobre el gráfico dado
     */
    public void renderLayers(Graphics2D g2d) {
        if (!layersEnabled) return;
        
        Graphics2D g = (Graphics2D) g2d.create();
        
        // Renderizar capas de abajo hacia arriba
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            layer.render(g, globalOpacity);
        }
        
        g.dispose();
    }
    
    /**
     * Encuentra la capa superior en una posición dada
     */
    public Layer getLayerAt(int x, int y) {
        // Buscar de arriba hacia abajo
        for (Layer layer : layers) {
            if (layer.isVisible() && layer.contains(x, y)) {
                return layer;
            }
        }
        return null;
    }
    
    /**
     * Genera un nombre único para la capa
     */
    private String generateLayerName(String originalName) {
        String baseName = originalName;
        
        // Remover extensión
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        
        // Buscar nombre único
        String layerName = baseName;
        int counter = 1;
        while (hasLayerWithName(layerName)) {
            layerName = baseName + " (" + counter + ")";
            counter++;
        }
        
        return layerName;
    }
    
    /**
     * Verifica si existe una capa con el nombre dado
     */
    private boolean hasLayerWithName(String name) {
        return layers.stream().anyMatch(layer -> layer.getName().equals(name));
    }
    
    /**
     * Combina todas las capas en una sola imagen
     */
    public BufferedImage flattenLayers(int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        
        renderLayers(g2d);
        
        g2d.dispose();
        return result;
    }
    
    // Getters y Setters
    public List<Layer> getLayers() {
        return new ArrayList<>(layers);
    }
    
    public Layer getSelectedLayer() {
        return selectedLayer;
    }
    
    public void setSelectedLayer(Layer layer) {
        if (layers.contains(layer)) {
            this.selectedLayer = layer;
            io.Log.log("DEBUG: LayerManager - Seleccionada capa: " + 
                             (layer != null ? layer.getName() : "null"));
        }
    }
    
    public void setSelectedLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            setSelectedLayer(layers.get(index));
        }
    }
    
    public boolean isLayersEnabled() {
        return layersEnabled;
    }
    
    public void setLayersEnabled(boolean enabled) {
    this.layersEnabled = enabled;
    io.Log.log("DEBUG: LayerManager - Capas " + (enabled ? "habilitadas" : "deshabilitadas"));
    }
    
    public float getGlobalOpacity() {
        return globalOpacity;
    }
    
    public void setGlobalOpacity(float opacity) {
        this.globalOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }
    
    public int getLayerCount() {
        return layers.size();
    }
    
    public boolean isEmpty() {
        return layers.isEmpty();
    }
    
    /**
     * Limpia todas las capas
     */
    public void clear() {
    layers.clear();
    selectedLayer = null;
    io.Log.log("DEBUG: LayerManager - Todas las capas eliminadas");
    }
    
    /**
     * Obtiene información de todas las capas
     */
    public String getLayersInfo() {
        if (layers.isEmpty()) {
            return "No hay capas";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Capas (").append(layers.size()).append("):\n");
        
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            info.append("  [").append(i).append("] ")
                .append(layer.toString())
                .append(layer == selectedLayer ? " (seleccionada)" : "")
                .append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Aplica transformaciones específicas solo a la imagen de fondo usando operaciones optimizadas
     */
    private BufferedImage applyBackgroundTransformations(BufferedImage baseImage, 
                                                         float brightness, float contrast, 
                                                         float saturation, float gamma, 
                                                         float sharpness) {
        if (baseImage == null) return null;
        
        // Crear una copia de la imagen base
        BufferedImage transformedImage = new BufferedImage(
            baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = transformedImage.createGraphics();
        g2d.drawImage(baseImage, 0, 0, null);
        g2d.dispose();
        
        // Aplicar transformaciones pixel por pixel (optimizado)
        int width = transformedImage.getWidth();
        int height = transformedImage.getHeight();
        
        // Obtener array de pixels para procesamiento más rápido
        int[] pixels = new int[width * height];
        transformedImage.getRGB(0, 0, width, height, pixels, 0, width);
        
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            
            // Extraer componentes RGB
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            
            // Aplicar brillo
            red = (int) Math.min(255, Math.max(0, red * brightness));
            green = (int) Math.min(255, Math.max(0, green * brightness));
            blue = (int) Math.min(255, Math.max(0, blue * brightness));
            
            // Aplicar contraste
            red = (int) Math.min(255, Math.max(0, (red - 128) * contrast + 128));
            green = (int) Math.min(255, Math.max(0, (green - 128) * contrast + 128));
            blue = (int) Math.min(255, Math.max(0, (blue - 128) * contrast + 128));
            
            // Aplicar gamma
            if (gamma != 1.0f) {
                red = (int) Math.min(255, Math.max(0, 255 * Math.pow(red / 255.0, 1.0 / gamma)));
                green = (int) Math.min(255, Math.max(0, 255 * Math.pow(green / 255.0, 1.0 / gamma)));
                blue = (int) Math.min(255, Math.max(0, 255 * Math.pow(blue / 255.0, 1.0 / gamma)));
            }
            
            // Aplicar saturación (conversión a HSB y vuelta)
            if (saturation != 1.0f) {
                float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                hsb[1] = Math.min(1.0f, Math.max(0.0f, hsb[1] * saturation));
                int newRgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                red = (newRgb >> 16) & 0xFF;
                green = (newRgb >> 8) & 0xFF;
                blue = newRgb & 0xFF;
            }
            
            // Reconstruir color y almacenar
            pixels[i] = (red << 16) | (green << 8) | blue | (rgb & 0xFF000000); // Mantener alpha
        }
        
        // Aplicar los pixels transformados de vuelta a la imagen
        transformedImage.setRGB(0, 0, width, height, pixels, 0, width);
        
    io.Log.log("DEBUG: Transformaciones aplicadas a " + pixels.length + " píxeles");
        
        // TODO: Aplicar sharpness si es necesario (requiere convolución)
        if (sharpness != 1.0f) {
            io.Log.log("DEBUG: Nitidez no implementada completamente, valor: " + sharpness);
        }
        
        return transformedImage;
    }

    /**
     * Combina la imagen base (con transformaciones de fondo aplicadas) con todas las capas activas.
     * 
     * @param baseImage La imagen original sobre la que aplicar transformaciones y capas
     * @param brightness Brillo para la imagen de fondo (1.0 = normal)
     * @param contrast Contraste para la imagen de fondo (1.0 = normal)  
     * @param saturation Saturación para la imagen de fondo (1.0 = normal)
     * @param gamma Gamma para la imagen de fondo (1.0 = normal)
     * @param sharpness Nitidez para la imagen de fondo (1.0 = normal)
     * @return Una nueva imagen con transformaciones de fondo y capas aplicadas
     */
    public BufferedImage applyBackgroundTransformationsAndLayers(BufferedImage baseImage, 
                                                                 float brightness, float contrast,
                                                                 float saturation, float gamma, 
                                                                 float sharpness) {
        if (baseImage == null) return null;
        
    io.Log.log("DEBUG: Aplicando transformaciones de fondo - Brillo: " + brightness + 
              ", Contraste: " + contrast + ", Saturación: " + saturation + 
              ", Gamma: " + gamma + ", Nitidez: " + sharpness);
        
        // Verificar si hay transformaciones que aplicar
        boolean hasTransformations = brightness != 1.0f || contrast != 1.0f || 
                                   saturation != 1.0f || gamma != 1.0f || sharpness != 1.0f;
        
        BufferedImage transformedBase;
        if (hasTransformations) {
            // Aplicar transformaciones SOLO a la imagen de fondo
            transformedBase = applyBackgroundTransformations(baseImage, 
                                                          brightness, contrast, 
                                                          saturation, gamma, 
                                                          sharpness);
            io.Log.log("DEBUG: Transformaciones aplicadas a imagen de fondo");
        } else {
            // Si no hay transformaciones, usar imagen original
            transformedBase = baseImage;
            io.Log.log("DEBUG: No hay transformaciones, usando imagen original");
        }
        
        // Luego aplicar las capas sobre la imagen de fondo transformada
        BufferedImage result = applyLayersToImage(transformedBase);
    io.Log.log("DEBUG: Capas aplicadas sobre imagen transformada");
        return result;
    }

    /**
     * Combina la imagen base con todas las capas activas.
     * Este método es clave para aplicar las capas sobre la imagen original
     * antes de que sea procesada por el pipeline del mosaico.
     * 
     * @param baseImage La imagen original sobre la que aplicar las capas
     * @return Una nueva imagen con las capas aplicadas, o la imagen original si no hay capas
     */
    public BufferedImage applyLayersToImage(BufferedImage baseImage) {
        if (!layersEnabled || layers.isEmpty() || baseImage == null) {
            return baseImage;
        }

        BufferedImage result = new BufferedImage(
            baseImage.getWidth(), 
            baseImage.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g_ = result.createGraphics();
        g_.drawImage(baseImage, 0, 0, null);

        for (Layer layer : layers) {
            if (!layer.isVisible()) {
                continue;
            }

			// Apply transformations to get the image to be rendered
			BufferedImage layerImage = layer.applyImageTransforms(layer.getImage());
			if (layerImage == null)
				continue;

			int originalWidth = layerImage.getWidth();
			int originalHeight = layerImage.getHeight();
			
			// Handle scaling
			float scale = layer.getScale();
			int newWidth = (int) (originalWidth * scale);
			int newHeight = (int) (originalHeight * scale);
			
			int layerX = layer.getX();
			int layerY = layer.getY();

			if (Math.abs(scale - 1.0f) > 1e-6) {
				if (newWidth > 0 && newHeight > 0) {
					BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = scaledImage.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g.drawImage(layerImage, 0, 0, newWidth, newHeight, null);
					g.dispose();
					layerImage = scaledImage;
					
					// Adjust position to keep center
					layerX += (originalWidth - newWidth) / 2;
					layerY += (originalHeight - newHeight) / 2;
				}
			}

			// Blend modes
			BlendMode mode = layer.getBlendMode();
            float opacity = layer.getOpacity() * globalOpacity;

			if (mode == BlendMode.NORMAL) {
				// Use standard alpha compositing for NORMAL mode
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
				g_.setComposite(ac);
				g_.drawImage(layerImage, layerX, layerY, null);
			}
			else {
				// Manual blending for other modes
				int imageW = result.getWidth();
				int imageH = result.getHeight();
				int layerW = layerImage.getWidth();
				int layerH = layerImage.getHeight();

				for (int y = 0; y < layerH; y++) {
					int canvasY = y + layerY;
					if (canvasY < 0 || canvasY >= imageH)
						continue;
					for (int x = 0; x < layerW; x++) {
						int canvasX = x + layerX;
						if (canvasX < 0 || canvasX >= imageW)
							continue;

						int frontARGB = layerImage.getRGB(x, y);
						int backARGB = result.getRGB(canvasX, canvasY);

						int blendedARGB = Layer.blend(backARGB, frontARGB, mode);

						// Manual alpha compositing
						float pixelAlpha = ((frontARGB >> 24) & 0xff) / 255.0f;
						float finalAlpha = opacity * pixelAlpha;

						int r_back = (backARGB >> 16) & 0xff;
						int g_back = (backARGB >> 8) & 0xff;
						int b_back = (backARGB) & 0xff;

						int r_blend = (blendedARGB >> 16) & 0xff;
						int g_blend = (blendedARGB >> 8) & 0xff;
						int b_blend = (blendedARGB) & 0xff;

						int r = (int) (r_back * (1 - finalAlpha) + r_blend * finalAlpha);
						int g = (int) (g_back * (1 - finalAlpha) + g_blend * finalAlpha);
						int b = (int) (b_back * (1 - finalAlpha) + b_blend * finalAlpha);

						int finalARGB = (backARGB & 0xFF000000) | (r << 16) | (g << 8) | b;
						result.setRGB(canvasX, canvasY, finalARGB);
					}
				}
			}
		}
		g_.dispose();
		return result;
	}
    
    /**
     * Limpia todas las capas de la lista
     */
    public void clearLayers() {
        boolean hadLayers = !layers.isEmpty();
        layers.clear();
        selectedLayer = null;
        
        // Si había capas y ahora no quedan, notificar para restaurar imagen original
        if (hadLayers && onAllLayersRemovedCallback != null) {
            io.Log.log("DEBUG: LayerManager - Todas las capas eliminadas, restaurando imagen original");
            onAllLayersRemovedCallback.run();
        }
    }
    
    /**
     * Notifica cambios en las capas a los listeners
     */
    private void notifyLayersChanged() {
        // Este método puede ser expandido en el futuro para notificar a observers
        // Por ahora, simplemente marca que las capas han cambiado
    }
    
    /**
     * Implementación de ModelHandler para guardar las capas en el archivo KMV
     */
    @Override
    public void save(Model<BrickGraphicsState> model) {
        try {
            // Verificar si el sistema de capas está habilitado
            model.set(BrickGraphicsState.LayersEnabled, !layers.isEmpty());
            
            if (!layers.isEmpty()) {
                StringBuilder layerData = new StringBuilder();
                
                for (int i = 0; i < layers.size(); i++) {
                    Layer layer = layers.get(i);
                    
                    // Serializar datos de la capa en formato JSON simple
                    layerData.append("{");
                    layerData.append("\"name\":\"").append(escapeJson(layer.getName())).append("\",");
                    layerData.append("\"file\":\"").append(escapeJson(layer.getImageFilePath())).append("\",");
                    layerData.append("\"visible\":").append(layer.isVisible()).append(",");
                    layerData.append("\"x\":").append(layer.getX()).append(",");
                    layerData.append("\"y\":").append(layer.getY()).append(",");
                    layerData.append("\"opacity\":").append(layer.getOpacity()).append(",");
                    layerData.append("\"brightness\":").append(layer.getBrightness()).append(",");
                    layerData.append("\"contrast\":").append(layer.getContrast()).append(",");
                    layerData.append("\"saturation\":").append(layer.getSaturation()).append(",");
                    layerData.append("\"gamma\":").append(layer.getGamma()).append(",");
                    layerData.append("\"sharpness\":").append(layer.getSharpness()).append(",");
                    layerData.append("\"scale\":").append(layer.getScale());
                    layerData.append("}");
                    
                    if (i < layers.size() - 1) {
                        layerData.append(",");
                    }
                }
                
                model.set(BrickGraphicsState.LayerData, "[" + layerData.toString() + "]");
            } else {
                model.set(BrickGraphicsState.LayerData, "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // En caso de error, limpiar los datos
            model.set(BrickGraphicsState.LayersEnabled, false);
            model.set(BrickGraphicsState.LayerData, "");
        }
    }
    
    /**
     * Implementación de ModelHandler para cargar las capas desde el archivo KMV
     */
    @Override
    public void handleModelChange(Model<BrickGraphicsState> model) {
        try {
            Object layersEnabledObj = model.get(BrickGraphicsState.LayersEnabled);
            Object layerDataObj = model.get(BrickGraphicsState.LayerData);
            
            Boolean layersEnabled = (layersEnabledObj instanceof Boolean) ? (Boolean) layersEnabledObj : false;
            String layerData = (layerDataObj instanceof String) ? (String) layerDataObj : "";
            
            // Limpiar capas existentes
            clearLayers();
            
            if (layersEnabled != null && layersEnabled && layerData != null && !layerData.trim().isEmpty()) {
                // Parsear datos de capas (JSON simple)
                layerData = layerData.trim();
                if (layerData.startsWith("[") && layerData.endsWith("]")) {
                    layerData = layerData.substring(1, layerData.length() - 1);
                    
                    if (!layerData.isEmpty()) {
                        String[] layerEntries = splitJsonArray(layerData);
                        
                        for (String entry : layerEntries) {
                            try {
                                loadLayerFromJson(entry.trim());
                            } catch (Exception e) {
                                e.printStackTrace();
                                // Continuar con la siguiente capa si hay error
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // En caso de error, mantener lista vacía
            clearLayers();
        }
        
        // Notificar cambios
        notifyLayersChanged();
    }
    
    /**
     * Parsea y carga una capa desde datos JSON
     */
    private void loadLayerFromJson(String jsonData) throws Exception {
        if (!jsonData.startsWith("{") || !jsonData.endsWith("}")) {
            return;
        }
        
        jsonData = jsonData.substring(1, jsonData.length() - 1);
        
        // Extraer valores
        String name = extractJsonValue(jsonData, "name");
        String file = extractJsonValue(jsonData, "file");
        boolean visible = Boolean.parseBoolean(extractJsonValue(jsonData, "visible"));
        int x = Integer.parseInt(extractJsonValue(jsonData, "x"));
        int y = Integer.parseInt(extractJsonValue(jsonData, "y"));
        float opacity = Float.parseFloat(extractJsonValue(jsonData, "opacity"));
        float brightness = Float.parseFloat(extractJsonValue(jsonData, "brightness"));
        float contrast = Float.parseFloat(extractJsonValue(jsonData, "contrast"));
        float saturation = Float.parseFloat(extractJsonValue(jsonData, "saturation"));
        float gamma = Float.parseFloat(extractJsonValue(jsonData, "gamma"));
        float sharpness = Float.parseFloat(extractJsonValue(jsonData, "sharpness"));
        float scale = Float.parseFloat(extractJsonValue(jsonData, "scale"));
        
        // Verificar que el archivo existe
        File imageFile = new File(file);
        if (!imageFile.exists()) {
            return; // No cargar si el archivo no existe
        }
        
        // Crear y configurar la capa
        Layer layer = addLayerFromFile(file, new Point(x, y));
        layer.setName(name);
        layer.setVisible(visible);
        layer.setOpacity(opacity);
        layer.setBrightness(brightness);
        layer.setContrast(contrast);
        layer.setSaturation(saturation);
        layer.setGamma(gamma);
        layer.setSharpness(sharpness);
        layer.setScale(scale);
    }
    
    /**
     * Extrae un valor de una cadena JSON simple
     */
    private String extractJsonValue(String jsonData, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = jsonData.indexOf(searchKey);
        if (startIndex == -1) {
            return "";
        }
        
        startIndex += searchKey.length();
        
        // Saltar espacios
        while (startIndex < jsonData.length() && jsonData.charAt(startIndex) == ' ') {
            startIndex++;
        }
        
        int endIndex;
        if (jsonData.charAt(startIndex) == '"') {
            // Valor string
            startIndex++; // Saltar comilla inicial
            endIndex = jsonData.indexOf('"', startIndex);
            if (endIndex == -1) {
                return "";
            }
        } else {
            // Valor numérico o boolean
            endIndex = jsonData.indexOf(',', startIndex);
            if (endIndex == -1) {
                endIndex = jsonData.length();
            }
        }
        
        return jsonData.substring(startIndex, endIndex);
    }
    
    /**
     * Divide un array JSON en entradas individuales
     */
    private String[] splitJsonArray(String arrayContent) {
        List<String> entries = new ArrayList<>();
        int depth = 0;
        int start = 0;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    entries.add(arrayContent.substring(start, i + 1));
                    start = i + 1;
                    // Buscar la siguiente entrada
                    while (start < arrayContent.length() && 
                           (arrayContent.charAt(start) == ',' || arrayContent.charAt(start) == ' ')) {
                        start++;
                    }
                    i = start - 1; // -1 porque el bucle incrementará
                }
            }
        }
        
        return entries.toArray(new String[0]);
    }
    
    /**
     * Escapa caracteres especiales para JSON
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}