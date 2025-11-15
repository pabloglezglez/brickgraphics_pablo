# 🚀 Guía de Desarrollo BrickGraphics

## ⚠️ PROBLEMA COMÚN: Cambios No Visibles

### 🔴 **Síntoma**: 
Modificas el código fuente pero los cambios no aparecen en la aplicación.

### 🎯 **Causa Principal**: 
Ejecutar desde JAR precompilado en lugar del código fuente modificado.

---

## ✅ **FLUJO CORRECTO DE DESARROLLO**

### 1. **Modificar Código**
```bash
# Editar archivos en: src/
vim src/mosaic/ui/menu/Ribbon.java
```

### 2. **Compilar Cambios**
```bash
# Compilar archivo específico:
javac -cp "src" -d bin src/mosaic/ui/menu/Ribbon.java

# Compilar todo el proyecto:
find src -name "*.java" -exec javac -cp "src:." -d bin {} +
```

### 3. **Ejecutar Desde Código Fuente**
```bash
# ✅ CORRECTO - Desde código compilado:
java -cp ".:bin" program.JWrapper

# ❌ INCORRECTO - Desde JAR precompilado:
java -jar BrickGraphics.jar
./Ejecutar_BrickGraphics.command
```

---

## 🛠️ **VERIFICACIÓN DE CAMBIOS**

### **Técnica del "Texto de Prueba"**
1. Añadir texto distintivo (ej: ">>> TEST <<<") 
2. Compilar y ejecutar
3. Si no aparece → problema de ejecución
4. Si aparece → cambio funcionando

### **Verificar Procesos Java**
```bash
# Matar procesos anteriores:
pkill -f java

# Verificar que no hay procesos:
ps aux | grep java
```

---

## 📁 **ESTRUCTURA DE ARCHIVOS**

```
brickgraphics_pablo/
├── src/                    ← CÓDIGO FUENTE (editar aquí)
│   └── mosaic/ui/menu/Ribbon.java
├── bin/                    ← CÓDIGO COMPILADO (javac genera aquí)
│   └── mosaic/ui/menu/Ribbon.class
├── BrickGraphics.jar       ← JAR PRECOMPILADO (NO usar para desarrollo)
└── Ejecutar_BrickGraphics.command ← Ejecuta JAR (NO usar para desarrollo)
```

---

## 🚨 **SEÑALES DE ALERTA**

### **Cambios No Aparecen:**
- ✅ Compilación exitosa
- ❌ Cambios no visibles
- ➡️ **Solución**: Verificar comando de ejecución

### **Errores Comunes:**
- Ejecutar desde `BrickGraphics.jar`
- Usar `Ejecutar_BrickGraphics.command`
- Olvidar compilar después de cambios
- Proceso Java anterior aún corriendo

---

## 📋 **CHECKLIST DE DESARROLLO**

Antes de reportar "no funciona":

- [ ] ¿Modifiqué archivos en `src/`?
- [ ] ¿Compilé con `javac`?
- [ ] ¿Ejecuto con `java -cp ".:bin"`?
- [ ] ¿Maté procesos Java anteriores?
- [ ] ¿Veo logs de mi cambio?

---

## 🎯 **COMANDOS RÁPIDOS**

### **Desarrollo Force Paint:**
```bash
# 1. Editar Ribbon.java
# 2. Compilar y ejecutar:
cd /Users/pablo/brickgraphics_pablo
pkill -f java
javac -cp "src" -d bin src/mosaic/ui/menu/Ribbon.java
java -cp ".:bin" program.JWrapper
```

### **Debug de Cambios:**
```bash
# Ver si el archivo .class se actualizó:
ls -la bin/mosaic/ui/menu/Ribbon.class

# Verificar timestamp de compilación:
stat bin/mosaic/ui/menu/Ribbon.class
```

---

## 💡 **CONSEJOS ADICIONALES**

1. **Siempre usar logs de debug** para verificar que el código se ejecuta
2. **Usar texto distintivo** para confirmar cambios
3. **Compilar archivo completo** cuando hay dependencias
4. **Verificar imports** si hay errores de compilación
5. **Backup del JAR original** antes de modificaciones

---

## 🔄 **PARA FUTURAS MODIFICACIONES**

**RECUERDA**: Este problema ya ocurrió con:
- Botón Force Paint (noviembre 2025)
- [Añadir futuros casos aquí]

**SOLUCIÓN ESTÁNDAR**: 
1. Verificar método de ejecución
2. Usar código fuente compilado
3. NO usar JAR precompilado para desarrollo

---

*Última actualización: 15 de noviembre de 2025*