# 🔍 Debug de Persistencia de Capas - DIAGNÓSTICO CRÍTICO

## 🎯 Problema identificado:
- ✅ Las modificaciones se crean (52,758 modificaciones)
- ✅ El renderizado funciona (pintura visible)  
- ❌ **Las modificaciones se pierden en autosave**

## 📊 Debug añadido:
Ahora el sistema muestra información detallada cuando guardas:
```
DEBUG: LayerManager.autosaveArtifacts - capa '[NOMBRE]' isPaintLayer=[true/false]
DEBUG: LayerManager.autosaveArtifacts - pixelModifications null=[true/false] size=[NÚMERO]
```

## 🧪 Para probar el diagnóstico:

1. **Carga un proyecto** con capas (o el que ya tienes abierto)
2. **Selecciona una capa de pintado** 
3. **Pinta algunos píxeles**
4. **Mueve la capa** (arrastra arriba/abajo en la lista)

## 🎯 Qué esperamos ver:

**Si funciona correctamente:**
```
DEBUG: LayerManager.autosaveArtifacts - capa 'mi capa (Pintado)' isPaintLayer=true
DEBUG: LayerManager.autosaveArtifacts - pixelModifications null=false size=1257
```

**Si está fallando (sospecha actual):**
```
DEBUG: LayerManager.autosaveArtifacts - capa 'mi capa (Pintado)' isPaintLayer=true  
DEBUG: LayerManager.autosaveArtifacts - pixelModifications null=true size=N/A
```

## 💡 Hipótesis del problema:
El método `layer.getPixelModifications()` está retornando `null` en lugar de las modificaciones guardadas, causando que se use el fallback del overlay (que también es null).

¿Puedes cargar un proyecto, pintar en una capa y moverla para ver qué información aparece?