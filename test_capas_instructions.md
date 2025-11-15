# 🧪 PRUEBA DEL SISTEMA DE CAPAS MEJORADO

## ✅ **Lo que se ha mejorado:**

1. **Coordenadas de mosaico alineadas** - Las capas ahora usan la misma resolución que el grid base
2. **Eyedropper funcional** - Funciona en capas de pintado
3. **Controles integrados** - Tamaño de pincel funciona en capas
4. **Preservación mejorada** - Las pinturas se mantienen al mover capas

## 🎯 **Pasos para probar:**

### **1. Cargar una imagen:**
   - Usa "File" → "Open" para cargar una imagen
   - Generar el mosaico normalmente

### **2. Crear una capa de pintado:**
   - Ve al panel de capas (lado derecho)
   - Haz click en el botón **🎨** (crear capa de pintado)
   - Nombra la capa (ej: "test pintura")

### **3. Seleccionar la capa:**
   - Haz click en la capa que acabas de crear en la lista
   - Asegúrate de que esté seleccionada (resaltada)

### **4. Probar el pincel:**
   - Selecciona la herramienta "Brush" 
   - Elige un color
   - Cambia el tamaño del pincel (Small/Medium/Large)
   - Pinta en el mosaico - ¡debería funcionar perfectamente alineado!

### **5. Probar el eyedropper:**
   - Selecciona la herramienta "Eyedropper"
   - Haz click en una parte del mosaico base → debería tomar ese color
   - Haz click en una parte que pintaste → debería tomar el color de la capa
   - Automáticamente cambiará a "Brush" después de seleccionar

### **6. Probar preservación:**
   - Pinta algo en la capa
   - Ve al panel de capas y mueve la capa (botones ↑ ↓)
   - La pintura debe mantenerse

### **7. Verificar logs:**
   - Los logs mostrarán las operaciones en detalle
   - Buscar mensajes como "Pintura aplicada a capa" y "Eyedropper en capa"

## 🐛 **¿Qué buscar?**

**✅ DEBE FUNCIONAR:**
- Pintura alineada perfectamente con el grid del mosaico
- Eyedropper toma colores correctos tanto de capas como del base
- Tamaño del pincel se respeta en las capas
- Las pinturas se preservan al manipular capas

**❌ SI HAY PROBLEMAS:**
- Pintura desalineada o en resolución diferente
- Eyedropper no funciona en capas
- Tamaño del pincel ignorado
- Pinturas que desaparecen al mover capas

## 📋 **Comandos útiles para debugging:**

```bash
# Ver logs en tiempo real:
tail -f app_test_capas.log

# Buscar operaciones específicas:
grep "Eyedropper en capa" app_test_capas.log
grep "Pintura aplicada a capa" app_test_capas.log
grep "modificaciones de mosaico" app_test_capas.log
```