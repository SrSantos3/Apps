# FocusZen 🧘‍♂️ - Bienestar Digital y Fricción Consciente para Android

**FocusZen** es una aplicación nativa para Android desarrollada en **Kotlin** y **Jetpack Compose** diseñada para romper los bucles automáticos de dopamina y el uso impulsivo del móvil mediante **fricción psicológica previa**, **sesiones de uso temporizadas** y **límites diarios con prórrogas penalizadas**.

Inspirada en el modelo contrastado de *one sec* y la disciplina de *StayFree/Opal*.

---

## ✨ Características Principales

1. **Pausa Consciente de 10 Segundos (Fricción Psicológica)**:
   - Al pulsar una app adictiva (Instagram, TikTok, YouTube, etc.), el sistema intercepta inmediatamente la apertura.
   - Presenta un **aro de respiración guiada** (Inhala / Exhala) con respuesta háptica y una pregunta reflexiva: *¿Realmente necesitas entrar ahora?*
   - Opción rápida: **"He cambiado de idea"** (vuelve al escritorio y suma a tu racha de autocontrol).

2. **Definición de Propósito Consciente (Versión 2)**:
   - Tras la pausa de 10 segundos, la app te pregunta: *¿Cuál es tu objetivo para entrar?*
   - Puedes escribir tu motivo personalizado o pulsar una etiqueta rápida (ej. *"💬 Responder mensaje"*, *"🔍 Buscar tutorial"*, *"💼 Trabajo urgente"*).
   - Eliges el tiempo de sesión (5, 10, 15 o 20 minutos).

3. **Píldora Flotante Translúcida (Recordatorio Permanente en Pantalla)**:
   - Al entrar a la app vigilada, aparece una **píldora flotante translúcida minimalista** con tu objetivo (`🎯 [Tu propósito]`).
   - **Sin reloj visible**: para mantener el 100% de tu concentración sin generarte ansiedad.
   - **Totalmente arrastrable**: puedes moverla con el dedo a cualquier zona de la pantalla para que no tape botones ni contenidos.
   - **Botón `✓` de Objetivo Cumplido**: en cuanto terminas tu tarea, tocas el botón y la app te **expulsa inmediatamente al escritorio (Home)**, protegiéndote de quedarte mirando el feed.
   - **Ocultación inteligente**: si sales al escritorio o abres otra aplicación, la píldora se oculta automáticamente y reaparece únicamente al regresar a la app vigilada.

4. **Límites Diarios Individuales y Prórrogas Penalizadas**:
   - Cada aplicación cuenta con una cuota de tiempo máxima al día (ej. 30 min en Instagram).
   - Al superar el límite diario, la aplicación se bloquea.
   - Para solicitar una prórroga de +5 minutos, se impone una **espera obligatoria de 60 a 120 segundos sin tocar la pantalla** para eliminar por completo la recompensa inmediata del cerebro.

4. **Modo Estricto Anti-Trampas**:
   - Protege contra el impulso de ir a los Ajustes del sistema de Android para deshabilitar el servicio de accesibilidad.

5. **Diseño Minimalista Dark con Métricas en Tiempo Real**:
   - Paleta profunda AMOLED con acentos en esmeralda e índigo.
   - Contador de **Impulsos Frenados Hoy** y **Tiempo Estimado Ahorrado**.
   - Indicador de **Puntuación de Enfoque (Focus Score %)**.
   - Barras de progreso de tiempo consumido vs límite diario.

---

## 🛠️ Arquitectura Técnica

- **Lenguaje**: Kotlin 1.9.23
- **Interfaz (UI)**: Jetpack Compose + Material 3 (100% Declarativo)
- **Persistencia Local**: Room Database + DataStore Preferences
- **Concurrencia**: Kotlin Coroutines + Flow
- **Interceptación del Sistema**:
  - `FocusAccessibilityService`: Escucha eventos `TYPE_WINDOW_STATE_CHANGED` para atrapar aperturas de paquetes en milisegundos.
  - `SessionManager`: Administra pases de acceso temporal en memoria concurrente.
  - `UsageStatsHelper`: Consulta `UsageStatsManager` del sistema operativo Android para el recuento exacto de minutos en primer plano.
  - `FrictionActivity`: Ventana traslúcida e inmersiva con animaciones de respiración `Animatable` y temporizadores de cuenta atrás.

---

## 🚀 Cómo Abrir y Ejecutar el Proyecto

1. **Abrir en Android Studio**:
   - Abre **Android Studio (versión Iguana / Jellyfish o superior)**.
   - Selecciona **Open** y elige la carpeta `c:\Users\dsantos\Desktop\Concentracion`.
   - Deja que Gradle sincronice las dependencias automáticamente.

2. **Ejecutar en tu Dispositivo Android o Emulador**:
   - Conecta tu teléfono Android por USB con Depuración USB habilitada (o inicia un emulador con Android 8.0 Oreo o superior / API 26+).
   - Haz clic en **Run 'app'** (`Shift + F10`).

3. **Configuración de Permisos (Asistente en la App)**:
   - Al abrir FocusZen por primera vez, pulsa en el banner superior o accede a la pantalla de permisos:
     1. **Servicio de Accesibilidad**: Activa el conmutador de *FocusZen*.
     2. **Acceso a Estadísticas de Uso**: Concede permiso para que pueda leer los minutos de uso diario.
     3. **Mostrar sobre otras aplicaciones**: Permite superponer el temporizador sobre cualquier app.

4. **Seleccionar tus Apps a Vigilar**:
   - Pulsa el botón `+` en el Dashboard.
   - Selecciona tus redes sociales o juegos más adictivos y ajusta el límite de minutos diarios.
