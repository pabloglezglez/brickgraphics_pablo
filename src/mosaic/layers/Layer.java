package mosaic.layers;

import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Color;
import java.util.*;
import java.util.List;

/**
 * Representa una capa que puede ser superpuesta sobre el mosaico base.
 * Soporta transparencia y posicionamiento personalizado.
 */
public class Layer {
    private String name;
    private BufferedImage image;
    
    // NUEVO: Sistema de modificaciones de píxeles como datos estructurados
    private Map<Point, PixelModification> pixelModifications; // Coordenadas relativas a imagen original
    
    private Point position;
    private boolean visible;
    private float opacity;
    
    // Propiedades de transformación de imagen individuales por capa
    private float brightness;
    private float contrast;
    private float saturation;
    private float gamma;
    private float sharpness;
    private float scale;
    private BlendMode blendMode;
    private String imagePath;

    /**
     * Modos de mezcla para las capas
     */
    public enum BlendMode {
        NORMAL,      // Mezcla normal con alpha
        MULTIPLY,    // Multiplicativo
        OVERLAY,     // Superposición
        SCREEN,      // Pantalla
        SOFT_LIGHT   // Luz suave
    }
    
    /**
     * Constructor para crear una nueva capa
     */
    public Layer(String name, BufferedImage image, Point position) {
        this.name = name;
        this.image = image;
        this.position = new Point(position);
        this.opacity = 1.0f;
        this.visible = true;
        this.blendMode = BlendMode.NORMAL;
        
        // Inicializar sistema de modificaciones de píxeles
        this.pixelModifications = new HashMap<>();
        
        // Valores por defecto para transformaciones de imagen
        this.brightness = 1.0f;
        this.contrast = 1.0f;
        this.saturation = 1.0f;
        this.gamma = 1.0f;
        this.sharpness = 1.0f;
        this.scale = 1.0f;
    }
    
    /**
     * Constructor con archivo de origen
     */
    public Layer(String name, BufferedImage image, Point position, String filePath) {
        this(name, image, position);
        this.imagePath = filePath;
    }
    
    // Getters
    public String getName() { return name; }
    public BufferedImage getImage() { return image; }
    
    // NUEVO: Métodos para el sistema de modificaciones de píxeles
    public Map<Point, PixelModification> getPixelModifications() { 
        return new HashMap<>(pixelModifications); 
    }
    
    public boolean hasPixelModifications() {
        return !pixelModifications.isEmpty();
    }
    
    public PixelModification getPixelModification(int x, int y) {
        return pixelModifications.get(new Point(x, y));
    }
    
    // COMPATIBILIDAD: Mantener método para compatibilidad con código existente
    @Deprecated
    public BufferedImage getPaintingOverlay() { 
        // Convertir modificaciones a BufferedImage para compatibilidad
        return convertModificationsToBufferedImage();
    }
    
    public Point getPosition() { return new Point(position); }
    public int getX() { return position.x; }
    public int getY() { return position.y; }
    public float getOpacity() { return opacity; }
    public boolean isVisible() { return visible; }
    public String getFilePath() { return imagePath; }
    public String getImageFilePath() { return imagePath; }
    public BlendMode getBlendMode() { return blendMode; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    
    public void setImage(BufferedImage image) { 
        this.image = image; 
        // Limpiar modificaciones que estén fuera de los nuevos límites de imagen
        if (image != null) {
            cleanupModifications(image.getWidth(), image.getHeight());
        }
    }
    
    // NUEVO: Métodos para manejar modificaciones de píxeles
    public void setPixelModifications(Map<Point, PixelModification> modifications) {
        this.pixelModifications = new HashMap<>(modifications);
    }
    
    public void addPixelModification(PixelModification modification) {
        pixelModifications.put(modification.getPosition(), modification);
        io.Log.log("DEBUG: Layer.addPixelModification - añadida modificación en " + modification.getPosition() + " color=" + modification.getColor());
    }
    
    public void removePixelModification(int x, int y) {
        Point pos = new Point(x, y);
        PixelModification removed = pixelModifications.remove(pos);
        if (removed != null) {
            io.Log.log("DEBUG: Layer.removePixelModification - eliminada modificación en (" + x + "," + y + ")");
        }
    }
    
    public void clearPixelModifications() {
        int count = pixelModifications.size();
        pixelModifications.clear();
        io.Log.log("DEBUG: Layer.clearPixelModifications - eliminadas " + count + " modificaciones de '" + name + "'");
    }
    
    // COMPATIBILIDAD: Mantener para código existente
    @Deprecated
    public void setPaintingOverlay(BufferedImage overlay) {
        // Convertir BufferedImage a modificaciones de píxeles
        convertBufferedImageToModifications(overlay);
        if (overlay != null) {
            io.Log.log("DEBUG: Layer.setPaintingOverlay - convertido overlay a " + pixelModifications.size() + " modificaciones para '" + name + "'");
        } else {
            io.Log.log("DEBUG: Layer.setPaintingOverlay - overlay NULL para '" + name + "'");
        }
    }
    
    public void setPosition(Point position) { 
        this.position = new Point(position); 
    }
    
    public void setPosition(int x, int y) { 
        this.position = new Point(x, y); 
    }
    
    public void setOpacity(float opacity) { 
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity)); 
    }
    
    public void setVisible(boolean visible) { 
        this.visible = visible; 
    }
    
    public void setFilePath(String filePath) { 
        this.imagePath = filePath; 
    }
    
    // Getters y setters para propiedades de transformación de imagen
    public float getBrightness() { return brightness; }
    public void setBrightness(float brightness) { this.brightness = brightness; }
    
    public float getContrast() { return contrast; }
    public void setContrast(float contrast) { this.contrast = contrast; }
    
    public float getSaturation() { return saturation; }
    public void setSaturation(float saturation) { this.saturation = saturation; }
    
    public float getGamma() { return gamma; }
    public void setGamma(float gamma) { this.gamma = gamma; }
    
    public float getSharpness() { return sharpness; }
    public void setSharpness(float sharpness) { this.sharpness = sharpness; }
    
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }
    
    public void setBlendMode(BlendMode blendMode) { 
        this.blendMode = blendMode; 
    }

    /**
     * Aplica un trazo circular simple (brush) sobre la capa usando modificaciones de píxeles.
     * @param x centro X en coordenadas de imagen (antes de escala)
     * @param y centro Y en coordenadas de imagen
     * @param radius radio del pincel
     * @param color color ARGB del trazo
     * @return true si se modificó algo
     */
    public boolean applyBrushStroke(int x, int y, int radius, int color) {
        if (image == null) return false;
        
        int w = image.getWidth();
        int h = image.getHeight();
        int r2 = radius * radius;
        boolean modified = false;
        
        // Extraer componentes de color
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        
        Color brushColor = new Color(red, green, blue);
        float alphaFloat = alpha / 255.0f;
        
        // Recorte de bounding box
        int minX = Math.max(0, x - radius);
        int maxX = Math.min(w - 1, x + radius);
        int minY = Math.max(0, y - radius);
        int maxY = Math.min(h - 1, y + radius);
        
        for (int yy = minY; yy <= maxY; yy++) {
            int dy = yy - y;
            for (int xx = minX; xx <= maxX; xx++) {
                int dx = xx - x;
                if (dx*dx + dy*dy <= r2) {
                    // Crear nueva modificación de píxel
                    PixelModification modification = new PixelModification(xx, yy, brushColor, alphaFloat);
                    pixelModifications.put(new Point(xx, yy), modification);
                    modified = true;
                }
            }
        }
        
        if (modified) {
            io.Log.log("DEBUG: Layer.applyBrushStroke - agregadas " + 
                      ((maxX-minX+1) * (maxY-minY+1)) + " modificaciones de píxel en '" + name + 
                      "' centro=(" + x + "," + y + ") radius=" + radius + 
                      " total=" + pixelModifications.size());
        }
        return modified;
    }

    /**
     * Limpia completamente las modificaciones de píxeles.
     */
    public void clearPaintingOverlay() {
        clearPixelModifications();
    }
    
    /**
     * Obtiene las dimensiones de la capa
     */
    public int getWidth() {
        return image != null ? image.getWidth() : 0;
    }
    
    public int getHeight() {
        return image != null ? image.getHeight() : 0;
    }
    
    /**
     * Verifica si un punto está dentro de los límites de la capa considerando el escalado desde el centro
     */
    public boolean contains(int x, int y) {
        if (!visible || image == null) return false;
        
        // Calcular las dimensiones escaladas
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);
        
        // Calcular offset para centrar la imagen escalada
        int offsetX = (originalWidth - scaledWidth) / 2;
        int offsetY = (originalHeight - scaledHeight) / 2;
        
        // Calcular la posición real de la imagen escalada
        int drawX = position.x + offsetX;
        int drawY = position.y + offsetY;
        
        // Verificar si el punto está dentro de la imagen escalada
        return x >= drawX && x < drawX + scaledWidth && 
               y >= drawY && y < drawY + scaledHeight;
    }
    
    /**
     * Renderiza la capa en el contexto gráfico con transformaciones aplicadas
     */
    public void render(Graphics2D g2d, float globalOpacity) {
        if (!visible || image == null) return;
        
        Graphics2D g = (Graphics2D) g2d.create();
        
        // Aplicar transformaciones de imagen para obtener imagen procesada
        BufferedImage processedImage = applyImageTransforms(image);
        
        // Aplicar opacidad global y de capa
        float finalOpacity = opacity * globalOpacity;
        AlphaComposite composite = getComposite(finalOpacity);
        g.setComposite(composite);
        
        // Dibujar imagen procesada con escalado desde el centro
        if (scale != 1.0f) {
            // Calcular nuevas dimensiones
            int originalWidth = processedImage.getWidth();
            int originalHeight = processedImage.getHeight();
            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);
            
            // Calcular offset para centrar la imagen escalada
            // La posición de referencia es el centro de la imagen original
            int offsetX = (originalWidth - scaledWidth) / 2;
            int offsetY = (originalHeight - scaledHeight) / 2;
            
            // Posición final considerando el escalado desde el centro
            int drawX = position.x + offsetX;
            int drawY = position.y + offsetY;
            
            // Dibujar imagen escalada desde el centro
            g.drawImage(processedImage, drawX, drawY, scaledWidth, scaledHeight, null);
            
            // NUEVO: Renderizar modificaciones de píxeles escaladas
            renderPixelModifications(g, drawX, drawY, scaledWidth, scaledHeight, originalWidth, originalHeight);
        } else {
            // Dibujar imagen sin escalado
            g.drawImage(processedImage, position.x, position.y, null);
            
            // NUEVO: Renderizar modificaciones de píxeles sin escalar
            renderPixelModifications(g, position.x, position.y, processedImage.getWidth(), processedImage.getHeight(), processedImage.getWidth(), processedImage.getHeight());
        }
        
        g.dispose();
    }

    public static int blend(int back, int front, BlendMode mode) {
        int r1 = (back >> 16) & 0xff;
        int g1 = (back >> 8) & 0xff;
        int b1 = (back) & 0xff;
        int a1 = (back >> 24) & 0xff;

        int r2 = (front >> 16) & 0xff;
        int g2 = (front >> 8) & 0xff;
        int b2 = (front) & 0xff;
        int a2 = (front >> 24) & 0xff;

        int r, g, b;

        switch (mode) {
            case MULTIPLY:
                r = (r1 * r2) / 255;
                g = (g1 * g2) / 255;
                b = (b1 * b2) / 255;
                break;
            case SCREEN:
                r = 255 - ((255 - r1) * (255 - r2)) / 255;
                g = 255 - ((255 - g1) * (255 - g2)) / 255;
                b = 255 - ((255 - b1) * (255 - b2)) / 255;
                break;
            case OVERLAY:
                r = (r1 < 128) ? (2 * r1 * r2) / 255 : 255 - (2 * (255 - r1) * (255 - r2)) / 255;
                g = (g1 < 128) ? (2 * g1 * g2) / 255 : 255 - (2 * (255 - g1) * (255 - g2)) / 255;
                b = (b1 < 128) ? (2 * b1 * b2) / 255 : 255 - (2 * (255 - b1) * (255 - b2)) / 255;
                break;
            case SOFT_LIGHT:
                r = (int) ((1 - 2 * (float)r2/255) * ((float)r1/255) * ((float)r1/255) + 2 * ((float)r2/255) * ((float)r1/255)) * 255;
                g = (int) ((1 - 2 * (float)g2/255) * ((float)g1/255) * ((float)g1/255) + 2 * ((float)g2/255) * ((float)g1/255)) * 255;
                b = (int) ((1 - 2 * (float)b2/255) * ((float)b1/255) * ((float)b1/255) + 2 * ((float)b2/255) * ((float)b1/255)) * 255;
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                break;
            case NORMAL:
            default:
                r = r2;
                g = g2;
                b = b2;
                break;
        }

        int alpha = Math.min(255, a1 + a2);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Aplica transformaciones de imagen (brillo, contraste, etc.) a la imagen de la capa
     */
    public BufferedImage applyImageTransforms(BufferedImage sourceImage) {
        if (sourceImage == null) return null;
        
        // Si no hay transformaciones, devolver imagen original
        if (brightness == 1.0f && contrast == 1.0f && saturation == 1.0f && 
            gamma == 1.0f && sharpness == 1.0f) {
            return sourceImage;
        }
        
        // Crear una nueva imagen para las transformaciones
        BufferedImage transformed = new BufferedImage(
            sourceImage.getWidth(), 
            sourceImage.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        // Aplicar transformaciones pixel por pixel
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = sourceImage.getRGB(x, y);
                
                // Extraer componentes ARGB
                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                // Aplicar brillo
                if (brightness != 1.0f) {
                    red = clamp((int)(red * brightness));
                    green = clamp((int)(green * brightness));
                    blue = clamp((int)(blue * brightness));
                }
                
                // Aplicar contraste
                if (contrast != 1.0f) {
                    red = clamp((int)((red - 128) * contrast + 128));
                    green = clamp((int)((green - 128) * contrast + 128));
                    blue = clamp((int)((blue - 128) * contrast + 128));
                }
                
                // Aplicar saturación
                if (saturation != 1.0f) {
                    // Convertir a escala de grises y mezclar con original
                    int gray = (int)(0.299 * red + 0.587 * green + 0.114 * blue);
                    red = clamp((int)(gray + saturation * (red - gray)));
                    green = clamp((int)(gray + saturation * (green - gray)));
                    blue = clamp((int)(gray + saturation * (blue - gray)));
                }
                
                // Aplicar gamma
                if (gamma != 1.0f) {
                    red = clamp((int)(255 * Math.pow(red / 255.0, 1.0 / gamma)));
                    green = clamp((int)(255 * Math.pow(green / 255.0, 1.0 / gamma)));
                    blue = clamp((int)(255 * Math.pow(blue / 255.0, 1.0 / gamma)));
                }
                
                // Recombinar componentes
                int newRgb = (alpha << 24) | (red << 16) | (green << 8) | blue;
                transformed.setRGB(x, y, newRgb);
            }
        }
        
        // Aplicar nitidez si es necesario
        if (sharpness != 1.0f && sharpness > 1.0f) {
            transformed = applySharpen(transformed, sharpness);
        }
        
        return transformed;
    }
    
    /**
     * Aplica un filtro de nitidez a la imagen
     */
    private BufferedImage applySharpen(BufferedImage image, float amount) {
        // Kernel de nitidez simple
        float sharpenValue = (amount - 1.0f) * 0.5f;
        float[] sharpenKernel = {
            0, -sharpenValue, 0,
            -sharpenValue, 1 + 4 * sharpenValue, -sharpenValue,
            0, -sharpenValue, 0
        };
        
        return applyConvolution(image, sharpenKernel, 3, 3);
    }
    
    /**
     * Aplica una convolución a la imagen
     */
    private BufferedImage applyConvolution(BufferedImage image, float[] kernel, int kernelWidth, int kernelHeight) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        int halfKernelWidth = kernelWidth / 2;
        int halfKernelHeight = kernelHeight / 2;
        
        for (int y = halfKernelHeight; y < height - halfKernelHeight; y++) {
            for (int x = halfKernelWidth; x < width - halfKernelWidth; x++) {
                float red = 0, green = 0, blue = 0;
                int alpha = (image.getRGB(x, y) >> 24) & 0xFF;
                
                for (int ky = 0; ky < kernelHeight; ky++) {
                    for (int kx = 0; kx < kernelWidth; kx++) {
                        int pixelX = x + kx - halfKernelWidth;
                        int pixelY = y + ky - halfKernelHeight;
                        
                        int rgb = image.getRGB(pixelX, pixelY);
                        float kernelValue = kernel[ky * kernelWidth + kx];
                        
                        red += ((rgb >> 16) & 0xFF) * kernelValue;
                        green += ((rgb >> 8) & 0xFF) * kernelValue;
                        blue += (rgb & 0xFF) * kernelValue;
                    }
                }
                
                int finalRed = clamp((int) red);
                int finalGreen = clamp((int) green);
                int finalBlue = clamp((int) blue);
                
                int newRgb = (alpha << 24) | (finalRed << 16) | (finalGreen << 8) | finalBlue;
                result.setRGB(x, y, newRgb);
            }
        }
        
        return result;
    }
    
    /**
     * Limita un valor entre 0 y 255
     */
    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
    
    /**
     * Obtiene el composite apropiado según el modo de mezcla
     */
    private AlphaComposite getComposite(float alpha) {
        switch (blendMode) {
            case NORMAL:
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            case MULTIPLY:
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            case OVERLAY:
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            case SCREEN:
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            case SOFT_LIGHT:
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            default:
                return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        }
    }
    
    /**
     * Crea una copia de la capa
     */
    public Layer copy() {
        // Crear una copia profunda de la imagen
        BufferedImage imageCopy = null;
        if (image != null) {
            imageCopy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            Graphics2D g = imageCopy.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        // Copiar modificaciones de píxeles
        Map<Point, PixelModification> pixelModsCopy = null;
        if (pixelModifications != null) {
            pixelModsCopy = new HashMap<>(pixelModifications);
        }
        
        // Mantener compatibilidad: copiar overlay de pintura si existe
        BufferedImage overlayCopy = null;
        if (pixelModifications == null || pixelModifications.isEmpty()) {
            BufferedImage overlay = getPaintingOverlay();
            if (overlay != null) {
                overlayCopy = new BufferedImage(overlay.getWidth(), overlay.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = overlayCopy.createGraphics();
                g.drawImage(overlay, 0, 0, null);
                g.dispose();
            }
        }

        Layer copy = new Layer(name + " (copy)", imageCopy, position, imagePath);
        copy.setOpacity(opacity);
        copy.setVisible(visible);
        copy.setBlendMode(blendMode);
        copy.setBrightness(brightness);
        copy.setContrast(contrast);
        copy.setSaturation(saturation);
        copy.setGamma(gamma);
        copy.setSharpness(sharpness);
        copy.setScale(scale);
        
        // NUEVO: Copiar modificaciones de píxeles
        copy.setPixelModifications(pixelModsCopy);
        
        // Asignar overlay de compatibilidad si existe
        if (overlayCopy != null) {
            copy.setPaintingOverlay(overlayCopy);
        }
        
        return copy;
    }
    
    /**
     * Escala la capa al tamaño especificado
     */
    public void scale(int newWidth, int newHeight) {
        if (image == null) return;
        
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        this.image = scaledImage;
    }
    
    // MÉTODOS AUXILIARES PARA EL NUEVO SISTEMA DE MODIFICACIONES
    
    /**
     * Renderiza las modificaciones de píxeles aplicando escalado y transformaciones
     */
    private void renderPixelModifications(Graphics2D g, int drawX, int drawY, int renderWidth, int renderHeight, int originalWidth, int originalHeight) {
        if (pixelModifications.isEmpty()) return;
        
        float scaleX = (float) renderWidth / originalWidth;
        float scaleY = (float) renderHeight / originalHeight;
        
        // Renderizar cada modificación de píxel
        for (PixelModification mod : pixelModifications.values()) {
            Point originalPos = mod.getPosition();
            
            // Calcular posición escalada
            int scaledX = Math.round(originalPos.x * scaleX);
            int scaledY = Math.round(originalPos.y * scaleY);
            
            // Posición final en el canvas
            int finalX = drawX + scaledX;
            int finalY = drawY + scaledY;
            
            // Aplicar color con alpha
            Color color = mod.getColor();
            int alpha = Math.round(mod.getAlpha() * 255);
            Color colorWithAlpha = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            
            g.setColor(colorWithAlpha);
            
            // Dibujar píxel escalado (puede ser más grande que 1x1 si está escalado)
            int pixelWidth = Math.max(1, Math.round(scaleX));
            int pixelHeight = Math.max(1, Math.round(scaleY));
            g.fillRect(finalX, finalY, pixelWidth, pixelHeight);
        }
    }
    
    /**
     * Limpia modificaciones que estén fuera de los límites de la imagen
     */
    private void cleanupModifications(int width, int height) {
        pixelModifications.entrySet().removeIf(entry -> {
            Point pos = entry.getKey();
            return pos.x < 0 || pos.x >= width || pos.y < 0 || pos.y >= height;
        });
    }
    
    /**
     * Convierte las modificaciones de píxeles a BufferedImage para compatibilidad
     */
    private BufferedImage convertModificationsToBufferedImage() {
        if (image == null || pixelModifications.isEmpty()) {
            return null;
        }
        
        BufferedImage overlay = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        
        // Dibujar cada modificación de píxel
        for (PixelModification mod : pixelModifications.values()) {
            Point pos = mod.getPosition();
            if (pos.x >= 0 && pos.x < image.getWidth() && pos.y >= 0 && pos.y < image.getHeight()) {
                Color color = mod.getColor();
                int alpha = Math.round(mod.getAlpha() * 255);
                Color colorWithAlpha = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                g.setColor(colorWithAlpha);
                g.fillRect(pos.x, pos.y, 1, 1);
            }
        }
        
        g.dispose();
        return overlay;
    }
    
    /**
     * Convierte un BufferedImage overlay a modificaciones de píxeles
     */
    private void convertBufferedImageToModifications(BufferedImage overlay) {
        pixelModifications.clear();
        
        if (overlay == null) return;
        
        for (int y = 0; y < overlay.getHeight(); y++) {
            for (int x = 0; x < overlay.getWidth(); x++) {
                int argb = overlay.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                
                // Solo agregar píxeles no transparentes
                if (alpha > 0) {
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    
                    Color color = new Color(r, g, b);
                    float alphaFloat = alpha / 255.0f;
                    PixelModification mod = new PixelModification(x, y, color, alphaFloat);
                    pixelModifications.put(new Point(x, y), mod);
                }
            }
        }
    }
    
    /**
     * Serializa las modificaciones de píxeles a JSON
     */
    public String serializePixelModifications() {
        if (pixelModifications.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (PixelModification mod : pixelModifications.values()) {
            if (!first) json.append(",");
            json.append(mod.toJson());
            first = false;
        }
        json.append("]");
        return json.toString();
    }
    
    /**
     * Deserializa modificaciones de píxeles desde JSON
     */
    public void deserializePixelModifications(String json) {
        pixelModifications.clear();
        
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return;
        }
        
        try {
            json = json.trim();
            if (!json.startsWith("[") || !json.endsWith("]")) {
                throw new IllegalArgumentException("JSON debe ser un array");
            }
            
            json = json.substring(1, json.length() - 1); // Quitar [ ]
            if (json.trim().isEmpty()) return;
            
            // Dividir por objetos JSON
            String[] modStrings = json.split("(?<=})\\s*,\\s*(?=\\{)");
            for (String modString : modStrings) {
                try {
                    PixelModification mod = PixelModification.fromJson(modString.trim());
                    pixelModifications.put(mod.getPosition(), mod);
                } catch (Exception e) {
                    io.Log.log("WARN: Error parseando modificación de píxel: " + modString + " - " + e.getMessage());
                }
            }
            
            io.Log.log("DEBUG: Layer.deserializePixelModifications - cargadas " + pixelModifications.size() + " modificaciones para '" + name + "'");
        } catch (Exception e) {
            io.Log.log("ERROR: Layer.deserializePixelModifications - " + e.getMessage());
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s (%dx%d) at (%d,%d) - %.0f%% opacity", 
                           name, getWidth(), getHeight(), 
                           position.x, position.y, opacity * 100);
    }
}