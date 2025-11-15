# 🧪 PRUEBA RÁPIDA DEL SISTEMA DE CAPAS

## 📋 **Instrucciones para probar:**

1. **La aplicación ya está ejecutándose** con una capa de pintado: `pintado_1_Pintado_`

2. **Para probar el sistema:**
   - Selecciona la capa `pintado_1_Pintado_` (ya debería estar seleccionada)
   - Elige la herramienta "Brush"
   - Elige un color
   - Pinta en el mosaico

3. **Monitorear los logs:**
   ```bash
   tail -f debug_capas.log | grep "applyBrushToLayer"
   ```

4. **Buscar estos logs específicos:**
   - `"applyBrushToLayer - INICIADO"` - Confirma que se está llamando
   - `"Añadidas X modificaciones"` - Confirma que se están añadiendo modificaciones
   - `"Total modificaciones en capa"` - Confirma el total de modificaciones

5. **Probar preservación:**
   - Después de pintar, mover la capa arriba/abajo
   - Ver si la pintura se mantiene

## 🔍 **Comandos útiles:**

```bash
# Ver logs en tiempo real:
tail -f debug_capas.log

# Buscar logs específicos de pintura de capas:
grep "applyBrushToLayer" debug_capas.log

# Buscar logs de guardado:
grep "modificaciones.*JSON" debug_capas.log

# Ver problemas:
grep -E "(ERROR|WARN)" debug_capas.log | tail -10
```