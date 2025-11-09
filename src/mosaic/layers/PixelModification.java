package mosaic.layers;

import java.awt.Point;
import java.awt.Color;
import java.util.Objects;

/**
 * Representa una modificación de color en una coordenada específica de una capa.
 * Las coordenadas son relativas a la imagen original de la capa (antes de escalado).
 */
public class PixelModification {
    private final Point position;    // Coordenada relativa a la imagen original de la capa
    private final Color color;       // Color aplicado
    private final float alpha;       // Transparencia de la modificación (0-1)
    
    public PixelModification(int x, int y, Color color) {
        this(x, y, color, 1.0f);
    }
    
    public PixelModification(int x, int y, Color color, float alpha) {
        this.position = new Point(x, y);
        this.color = color;
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }
    
    public PixelModification(Point position, Color color, float alpha) {
        this.position = new Point(position);
        this.color = color;
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }
    
    // Getters
    public Point getPosition() { return new Point(position); }
    public int getX() { return position.x; }
    public int getY() { return position.y; }
    public Color getColor() { return color; }
    public float getAlpha() { return alpha; }
    
    /**
     * Calcula la posición transformada aplicando escala desde el centro de la imagen
     */
    public Point getTransformedPosition(int originalWidth, int originalHeight, float scale) {
        if (scale == 1.0f) {
            return new Point(position);
        }
        
        // Calcular centro de la imagen original
        int centerX = originalWidth / 2;
        int centerY = originalHeight / 2;
        
        // Calcular posición relativa al centro
        int relativeX = position.x - centerX;
        int relativeY = position.y - centerY;
        
        // Aplicar escala
        int scaledX = Math.round(relativeX * scale);
        int scaledY = Math.round(relativeY * scale);
        
        // Volver a coordenadas absolutas considerando el nuevo centro
        int newCenterX = Math.round(originalWidth * scale) / 2;
        int newCenterY = Math.round(originalHeight * scale) / 2;
        
        return new Point(scaledX + newCenterX, scaledY + newCenterY);
    }
    
    /**
     * Serializa la modificación a formato JSON
     */
    public String toJson() {
        return String.format("{\"x\":%d,\"y\":%d,\"r\":%d,\"g\":%d,\"b\":%d,\"a\":%.3f}", 
            position.x, position.y, 
            color.getRed(), color.getGreen(), color.getBlue(), 
            alpha);
    }
    
    /**
     * Crea una modificación desde JSON
     */
    public static PixelModification fromJson(String json) {
        try {
            json = json.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) {
                throw new IllegalArgumentException("JSON inválido: " + json);
            }
            
            json = json.substring(1, json.length() - 1); // Quitar { }
            String[] parts = json.split(",");
            
            int x = 0, y = 0, r = 0, g = 0, b = 0;
            float a = 1.0f;
            
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length != 2) continue;
                
                String key = kv[0].trim().replaceAll("\"", "");
                String value = kv[1].trim();
                
                switch (key) {
                    case "x": x = Integer.parseInt(value); break;
                    case "y": y = Integer.parseInt(value); break;
                    case "r": r = Integer.parseInt(value); break;
                    case "g": g = Integer.parseInt(value); break;
                    case "b": b = Integer.parseInt(value); break;
                    case "a": a = Float.parseFloat(value); break;
                }
            }
            
            return new PixelModification(x, y, new Color(r, g, b), a);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parseando JSON: " + json, e);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PixelModification)) return false;
        PixelModification other = (PixelModification) obj;
        return Objects.equals(position, other.position);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(position);
    }
    
    @Override
    public String toString() {
        return String.format("PixelMod[(%d,%d) -> %s α=%.2f]", 
            position.x, position.y, color, alpha);
    }
}